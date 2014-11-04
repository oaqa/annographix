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
  /** a maximum number of forward iterations to carry out,
      before starting a full-blown exponential search. */
  private static final int FORWARD_ITER_QTY = 5;
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
        
        int nextStartIdx = post.findElemLargerOffset(FORWARD_ITER_QTY,
                                                     mCurrSpanStartOffset - 1, 
                                                     mStartElemIndx[k]);
        mStartElemIndx[k] = nextStartIdx;        
        
        if (nextStartIdx >= elemQty) {
          /*
           *  We cannot find at least one element with the start
           *  offset >= mCurrSpanEndOffset. Because, starting offsets
           *  are non-decreasing, the same will be true for all following
           *  spans.
           *  
           *  Because are considering only AND queries, no further
           *  complete matches are possible. Thus, we can save some
           *  effort by forcibly terminating the in-document iteration earlier.  
           */
          finishSpanIteration();
          return false;
        }
        
        ElemInfoData nextElem = post.getElement(nextStartIdx);
        if (nextElem.mStartOffset >= mCurrSpanEndOffset) {
          /*
           *  uups the next found element is actually to the right of the current
           *  covering annotation (square brackets denote annotations):
           *  
           *  [ current covering annotation ] [next element]
           *  
           */
          bOk = false;
          break;
        }
        /*
         *  p will get us the index of the FIRST element with start offset beyond covering,
         *  or the value elemQty...
         */
        int nextEnd = 
            post.findElemLargerOffset(FORWARD_ITER_QTY,
                                     mCurrSpanEndOffset - 1,
                                     mStartElemIndx[k] + 1);
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
   * <p>Constraints include:
   * parent-child and containment relationship, as well as containment
   * inside the span. Because ending offsets of indexed elements do not
   * necessarily increase monotonically while we monotonically increase 
   * starting offsets, the element fitting into the span may follow 
   * an element that can be fit into the span only partially. To ensure
   * that all elements inside the span are considered, we need to iterate
   * until the starting offset becomes >= than the span's end offset.
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
    if (startPostIndex >= mPostSorted.length) {
    // We checked successfully all the constraints, yay!
      return true;
    }
    
    OnePostStateBase    elem = mPostSorted[startPostIndex];

    for (int elemIndx = mStartElemIndx[startPostIndex];
         elemIndx < mEndElemIndx[startPostIndex];
         ++elemIndx) {
      elem.setCurrElemIndex(elemIndx);
      /*
       * Note that spans are sorted by start offset and for elements
       * with indices in the range 
       * [mStartElemIndx[startPostIndex],mEndElemIndx[startPostIndex]] starting
       * offsets are between the start and end offset of the current span.
       * 
       * However, it is not guaranteed that end offsets fit into the current span.
       * Due to non-monotonicity of end offsets (as the function of start offset),
       * one element may fall out the current span while some following elements
       * may still fit in.
       *  
       * Hence, we don't exit the loop the first time we see the end offset
       * beyond the span boundary. Instead, we ignore such element and 
       * proceed to checking the next one.
       * 
       * An example (square brackets denote annotation boundary):
       *    [     ]       span
       *    [      ]      element 1 (fits in only partially)
       *     [   ]        element 2 (fits in completely)
       */
      ElemInfoData currElement = elem.getCurrElement();
      if (currElement.mEndOffset > mCurrSpanEndOffset)
        continue;
          
      boolean bOk = true;
      /*
       *  Check constraints related to earlier postings,
       *  but only if we are dealing with connected components.
       *  We have two types of such constraints: when the current
       *  node is constraint and the current node is constraining.
       *  
       *  Recall that posts are sorted in such a way,
       *  that posts with indices >= mPostConnectQty are not
       *  constraining other posts and 
       *  are not being constrained by other posts.
       */
      if (startPostIndex < mPostConnectQty) {
        // Current node is constraining
        if (startPostIndex > 0 &&
            !mPostSorted[startPostIndex].checkConstraints(0, startPostIndex - 1)) {
          bOk = false;
        } else {
          for (int k = 0; k < startPostIndex; ++k)
         // Current node is constraint
            if (!mPostSorted[k].checkConstraints(startPostIndex, startPostIndex)) {
              bOk = false; 
              break;
            } 
        }
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
    mCurrSpanEndOffset   = 0;
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
  protected int                        mCurrSpanEndOffset = 0;
  /** 
   * The start offset of the current span, 
   * updated by a child's class {@link #nextSpanInternal()}. 
   */
  protected int                        mCurrSpanStartOffset = -1;
}

