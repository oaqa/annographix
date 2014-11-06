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
   * Find elements fitting into the span and satisfying constraints.
   * Note that we carry out a separate check for each component (components
   * are not connected to each other) starting from the component with
   * the largest number of nodes/elements (or the smallest 
   * minimum posting cost if there are ties).
   * 
   * @return true if we can find elements satisfying span constrains
   */
  public boolean checkSpanConstraints() {
    int prevCompId = -1;
    // We want to consider components in the order they are sorted 
    for (int i = 0; i < mPostSorted.length; ++i) {
      int compId = mPostSorted[i].getComponentId();
      if (compId == prevCompId) continue; // This component was checked already
      prevCompId = compId;
      if (!checkSpanConstraintsForOneComponent(mCompStartId[compId],
                                               mCompEndId[compId], 
                                               mCompStartId[compId]))
        return false;
    }
    return true;
  }  
  
  /**
   * Checks if span constraints can be satisfied for one component. 
   * There may be more than one component (a sub-graph) that are
   * disconnected from each other. This function checks only one.
   * Note that postings are already sorted in such a way that elements/nodes/postings
   * belonging to the same component occupy a contiguous range of indices
   * of the array mPostSorted.
   * 
   * <p>Constraints include:
   * parent-child and containment relationship, as well as containment
   * inside the span. Because ending offsets of indexed elements do not
   * necessarily increase monotonically while we monotonically increase 
   * starting offsets, the element fitting into the span may follow 
   * an element that can fits into the span only partially. To ensure
   * that all elements inside the span are considered, we need to iterate
   * until the starting offset becomes >= than the span's end offset.
   * 
   * 
   * @param     compStartId        an id of the first element in the component (inclusive).
   * @param     compEndId          an id of the last element in the component (inclusive).
   * @param     startPostIndex     the current posting index, should be 0 when we call
   *                               this recursive function the first time we enter a recursion.
   * 
   * @return    true if we can find a combination of elements within
   *            the span that satisfy the constraints, or false otherwise.
   */
  public boolean checkSpanConstraintsForOneComponent(
                                      int compStartId,
                                      int compEndId,
                                      int startPostIndex) {
    if (startPostIndex > compEndId) {
    // We checked successfully all the constraints for this component
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
       * However, it is not guaranteed for end offsets.
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
       *     [   ]        element 2 (follows element 1, but fits into the span)
       */
      ElemInfoData currElement = elem.getCurrElement();
      if (currElement.mEndOffset > mCurrSpanEndOffset)
        continue;
          
      boolean bOk = true;
      /*
       *  Check constraints related to earlier postings
       *  that belong to the same component.
       *  We can disregard postings from other components,
       *  because there are no constraints shared with them.
       */
      if (startPostIndex  > compStartId) {
        // Current node is constraining, compStartId <= startPostIndex - 1
        if (!mPostSorted[startPostIndex].checkConstraints(compStartId, 
                                                          startPostIndex - 1)) {
          bOk = false;
        } else {
          for (int k = compStartId; k < startPostIndex; ++k)
         // Current node is being constrained by previous component nodes
            if (!mPostSorted[k].checkConstraints(startPostIndex, startPostIndex)) {
              bOk = false; 
              break;
            } 
        }
      }
         
      if (bOk) {
        /*
         *  Ok, we are good for post indices from compStartId to startPostIndex.
         *  That is, there are combinations of elements fully fitting into 
         *  the span and satisfying all the constraints.
         */
        if (startPostIndex == compEndId) return true; // all checks are done
        /*
         *  Let's see if this can be extended to node components with indices
         *  larger than startPostIndex. 
         */
        boolean f = checkSpanConstraintsForOneComponent(
                                    compStartId, 
                                    compEndId,
                                    startPostIndex + 1);
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

    boolean[] seen = new boolean[mPostSorted.length];
        
    for (int i = 0; i < mPostSorted.length; ++i) { 
      mCompQty = Math.max(mCompQty, mPostSorted[i].getComponentId() + 1);
    }
    
    mCompStartId = new int[mCompQty];
    mCompEndId = new int[mCompQty];
    
    for (int i = 0; i < mPostSorted.length; ++i) {
      int compId = mPostSorted[i].getComponentId();
      mCompEndId[compId] = i;

      if (i==0 || 
          compId != mPostSorted[i-1].getComponentId()) {
      /*
       *  New component? 
       *  Check if we encountered it previously:
       *  we shouldn't have seen it if there're no bugs. 
       */
        if (seen[compId]) {
          throw new RuntimeException(
              "Bug: non-contiguous placement of components, " + 
              "encountered compId=" + compId + " again.");
        }
        seen[compId] = true;
        mCompStartId[compId] = i; 
      }
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
   * the function {@link #checkSpanConstraints()}. This is done in this way
   * because annotations/tokens are sorted only by the start offsets,
   * while the end offset doesn't increase or decrease monotonically as we
   * move from annotation to annotation. However, when the start offset
   * becomes to the right of the covering annotation, no further elements can
   * be found inside the covering annotation.
   * 
   */
  protected int[]                     mEndElemIndx;  
  
  /** A number of disjoint sub-graphs/components. */
  protected int                       mCompQty = 0; 
  /** Components' start indices */
  protected int[]                     mCompStartId;
  /** Components' end indices */
  protected int[]                     mCompEndId;  
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

