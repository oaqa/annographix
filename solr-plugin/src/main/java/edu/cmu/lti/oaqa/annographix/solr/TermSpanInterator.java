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
 * A base class for classes that help you iterate 
 * over possible matches inside a document.
 * 
 * @author Leonid Boytsov
 *
 */
public abstract class TermSpanInterator {
  /**
   * Finds the next covering span.
   * 
   * @return    false if we reach the end of the document or true,
   *            if the next span is found.
   */
  public abstract boolean nextSpan();
  /**
   * This function should be called once we moved to the next doc.
   */
  public abstract void reset();
  
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
    
    OnePostStateBase    elem = mElemSorted[startPostIndex];

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
      if (!mElemSorted[k].checkConstraints(startPostIndex)) {
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
  protected TermSpanInterator(OnePostStateBase[] elemSorted) {
    mElemSorted     = elemSorted;
    mStartElemIndx  = new int[mElemSorted.length];
    mEndElemIndx    = new int[mElemSorted.length];

    for (int i = 0; 
        i < mElemSorted.length && mElemSorted[i].getConnectQty() > 0; 
        ++i) {
      mPostConnectQty = i + 1;
    }
  }

  /** Postings of query element. */
  protected OnePostStateBase[]         mElemSorted;
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
   * move from annotation to annotation. 
   * 
   */
  protected  int[]                     mEndElemIndx;  
  /** A number of connected postings. */
  protected  int                       mPostConnectQty = 0;  
  /** The current end of span offset (exclusive), updated by a child class */
  protected int                        mCurrSpanEndOffset = -1;
}

