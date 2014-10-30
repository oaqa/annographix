/*
 *  Copyright 2014 Carnegie Mellon University
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  
 *  The author re-used some of the code from org.apache.lucene.search.PhraseQuery,
 *  which was also licensed under the Apache License 2.0.
 *  
 */
package edu.cmu.lti.oaqa.annographix.solr;

/**
 * <p>A span iterator base class. This class helps you iterate 
 * over possible matches inside a document. This is achieved
 * by moving a window inside the document and checking whether:
 * </p>
 * 
 * <ol> 
 * <li>All query elements are present in this window.
 * <li>Constraints defined by the query graph are satisfied.
 * </ol>
 * 
 * <p>A window selection algorithm is provided by child classes.</p>
 *  
 * 
 * @author Leonid Boytsov
 *
 */
public abstract class TermSpanIterator {  
  /**
   * This function initiates span iterators. It should be called after 
   * fetching the information for the next document. It assumes that each 
   * query element is found in the document at least once.
   */
  public abstract void initSpanIteration();
  
  /**
   * Finds the next covering span.
   * One needs to call {@link #initSpanIteration()} (every time
   * after we fetched the next document) before calling this function. 
   * 
   * @return    false if we reach the end of the document or true,
   *            if the next span is found.
   */  
  public boolean nextSpan() {
    while (nextSpanInternal()) {
      /*
       *  Let's check if we can find all elements inside the span.
       *  We will only check for their presence, without checking
       *  if constraints are satisfied.
       */
      boolean bOk = true;
      
      for (int k = 0; k < mPostSorted.length; ++k) {
        OnePostStateBase post = mPostSorted[k];
        int   elemQty = post.getQty();
        
        int nextStart = post.findElemIndexWithLargerOffset(
                                                    mCurrSpanEndOffset - 1, 
                                                    mStartElemIndx[k],
                                                    MIN_LINEAR_SEARCH_QTY);
        if (nextStart >= elemQty) {
          /*
           *  Because we cannot find at least one element with the start
           *  offset >= mCurrSpanEndOffset, no match is possible
           *  (a match should include all elements). Thus, we can save some
           *  effort by forcibly terminating the in-document iteration earlier.  
           */
          finishSpanIteration();
          return false;
        }
        
        ElemInfoData nextElem = post.getElement(nextStart);
        if (nextElem.mStartOffset >= mCurrSpanEndOffset) {
          /*
           *  uups the next found element is actually to the right of the current
           *  covering annotation (square brackets denote annotations):
           *  
           *  [ current covering annotation ] [next element]
           *  
           */
          mStartElemIndx[k] = nextStart;
          bOk = false;
          break;
        }
        /*
         *  p will get us the index of the FIRST element with start offset beyond covering,
         *  or the value elemQty...
         */
        int nextEnd = 
            post.findElemIndexWithLargerOffset(mCurrSpanEndOffset - 1,
                                               mStartElemIndx[k] + 1,
                                               MIN_LINEAR_SEARCH_QTY);
        /*
         *  If we subtract one from nextEnd, 
         *  we obtain the index of the LAST element 
         *  whose start offset is within the covering annotation.
         *  This is what we need, because mEndElemIndx[k] is
         *  treated exclusively.
         */
        
        mEndElemIndx[k] = nextEnd;
      }
      
      if (bOk) {
        return true;
      }
    }
    
    return false;
  }
  
  
  /**
   * Checks if span constraints can be satisfied.
   * 
   * <p>This is a recursive function that accepts a starting posting index.
   * Postings are sorted in the order of decreasing connectedness (i.e.,
   * most constraining nodes go first). The key heuristic is 
   * i) to select most connected elements first ii) add more elements
   * to see if these additions violate any constraints related to
   * previously selected nodes.</p>
   * 
   * <p>Because we 
   * add elements in the order of decreasing connectedness, we can expect 
   * more combinations to be pruned early (due to our inability to satisfy 
   * constraints). This may reduce the volume of recursion.</p>
   * 
   * <p>If, in contrast, we start with elements that have no constraints,
   * there may be a lot of combinations and all the combinations are valid
   * until we add a constraining node. Early introduction of the constraining node
   * may prevent considering all these combinations. Remember that we do
   * not care about the overall number of valid combinations, only whether or 
   * not a valid combination exists within a span.
   * </p>
   * 
   * @param     startPostIndex     the current posting index, should be 0 when we call
   *                               this recursive function the first time we enter a recursion.
   * 
   * @return    true if we can find a combination of elements within
   *            the span that satisfy the constraints, or false otherwise.
   */
  public boolean checkSpanConstraints(int startPostIndex) {
    if (startPostIndex >= mPostConnectQty) {
    // We checked successfully all the constraints, yay!
      return true;
    }
    
    OnePostStateBase    elem = mPostSorted[startPostIndex];

    for (int elemIndx = mStartElemIndx[startPostIndex];
         elemIndx < mEndElemIndx[startPostIndex];
         ++elemIndx) {
      elem.setCurrElemIndex(elemIndx);
      /*
       * Note that spans are sorted by start offset,
       * so it is possible that end offsets are not changing
       * monotonically. Hence, we don't break the first time
       * we see the end offset beyond the boundary.
       * We ignore the element and proceed to check the next one,
       * because it may still be completely within the span.
       * 
       * An example (square brackets denote annotation boundary):
       * 
       *    [      ]
       *     [   ]
       * 
       */
      if (elem.getCurrElement().mEndOffset > mCurrSpanEndOffset)
        continue;
      
      
      // Check constraints related to earlier postings
      boolean bOk = true;
      for (int k = 0; k < startPostIndex; ++k)
      if (!mPostSorted[k].checkConstraints(startPostIndex)) {
        bOk = false; 
        break;
      }
      
      if (bOk) {
        /*
         *  Ok, we are good for post indices 0..startPostIndex (no
         *  constraints are violated), let's see if we can find some 
         *  valid combinations of positions that conform with positions 
         *  selected for postings 0..startPostIndex and each other. 
         */
        boolean f = checkSpanConstraints(startPostIndex + 1);
        if (f) return true; // At least one combination exists!
      }
    }

    return false;
  }

  /**
   * Constructor.
   * 
   * @param elemSorted  a sorted (first by connectedness, then by cost) 
   *                    array of posting states.
   */
  protected TermSpanIterator(OnePostStateBase[] elemSorted) {
    mPostSorted     = elemSorted;
    mStartElemIndx  = new int[mPostSorted.length];
    mEndElemIndx    = new int[mPostSorted.length];

    for (int i = 0; 
        i < mPostSorted.length && mPostSorted[i].getConnectQty() > 0; 
        ++i) {
      mPostConnectQty = i + 1;
    }
  }
  
  /**
   * This function should be called from every child {@link #initSpanIteration()} function.
   */
  protected void initSpanIterationBase() {
    mCurrSpanEndOffset   = -1;
    mCurrSpanStartOffset = -1;
    for (int i = 0; i < mPostSorted.length; ++i)
      mStartElemIndx[i] = 0;

  }
  /**
   * This function moves to the next span. It updates mCurrSpanStartOffset
   * and mCurrSpanEndOffset.
   * 
   * @return    true if successful, and false, if no further spans are found.
   */
  protected abstract boolean nextSpanInternal();
  
  /**
   * This function should be implemented in the child class, it is
   * used in the function {@link #nextSpan()} to signal the early
   * termination of the iteration procedure over the spans.
   */
  protected abstract void finishSpanIteration();

  /** Postings of query element. */
  protected OnePostStateBase[]         mPostSorted;
  /** Starting element index (inclusive) inside the span. */
  protected  int[]                     mStartElemIndx;
  /** 
   * Ending element (exclusive) index of elements that may be inside the span. 
   * If we subtract one from this ending element index, we obtain an index 
   * of the annotation whose start offset is always < span' end offset, 
   * but the end offset may actually be larger than the span's end offset. 
   * Because of this, we have an additional check inside
   * the function {@link #checkSpanConstraints(int)}. This is done in this way
   * because annotations/tokens are sorted only by the start offsets,
   * while the end offset doesn't increase or decrease monotonically as we
   * move from annotation to annotation. However, when the start offset
   * becomes to the right of the covering annotation, no further elements can
   * be found inside the covering annotation.
   * 
   */
  protected  int[]                     mEndElemIndx;  
  /** A number of connected postings. */
  protected  int                       mPostConnectQty = 0;  
  /** 
    * The end offset of the current span (exclusive), 
    * updated by a child's class {@link #nextSpanInternal()}. 
    */
  protected int                        mCurrSpanEndOffset = -1;
  /** 
   * The start offset of the current span, 
   * updated by a child's class {@link #nextSpanInternal()}. 
   */
  protected int                        mCurrSpanStartOffset = -1;
  /**
   * A number of linear (sequential) search iterations to carry out
   * before resorting to binary search. This is used to find the next
   * element within a given span.
   */
  protected static final int MIN_LINEAR_SEARCH_QTY = 0;
  
}

