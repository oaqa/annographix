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

import java.util.PriorityQueue;

/**
 * 
 * See {@link TermSpanIterator}, covering windows are document spans
 * that contain all query elements and whose length is within the threshold
 * 
 * @author Leonid Boytsov
 *
 */

public class TermSpanIteratorMaxLen extends TermSpanIterator {
  /**
   * Constructor.
   * 
   * @param     postSorted  see {@link TermSpanIterator#TermSpanIterator(OnePostStateBase[])}
   * @param     maxSpanLen  a maximum length of the span covering all query elements.
   * @param     maxSpanCheckConstrIter  a maximum number of brute-force iterations
   *                                    to carry out in a span before giving up.
   */
  TermSpanIteratorMaxLen(OnePostStateBase[] postSorted,
                         int maxSpanLen, int maxSpanCheckConstrIter) {
    super(postSorted, maxSpanCheckConstrIter);
    
    mMaxSpanLen = maxSpanLen;
    mQueueObjs = new ElemInfoDataHolder[postSorted.length];
    for (int i = 0; i < postSorted.length; ++i)
      mQueueObjs[i] = new ElemInfoDataHolder(postSorted[i]);
    mStartOffsetQueue = new PriorityQueue<ElemInfoDataHolder>();
  }
  
  /**
   * See {@link TermSpanIterator#initSpanIteration()}. 
   */
  @Override
  public void initSpanIteration() {
    initSpanIterationBase();
    mStartOffsetQueue.clear();
    
    mLargestStartOffset = -1;
    
    for (int i = 0; i < mPostSorted.length; ++i) {
      ElemInfoDataHolder obj = mQueueObjs[i];
      obj.reset();
      mLargestStartOffset = Math.max(obj.getCurrOffset(), mLargestStartOffset);
      mStartOffsetQueue.add(obj);
    }
  }

  /**
   * See {@link TermSpanIterator#nextSpanInternal()}
   */
  @Override
  protected boolean nextSpanInternal() {
    while (!mStartOffsetQueue.isEmpty()) {
      ElemInfoDataHolder bottom = mStartOffsetQueue.poll();
      
      // Here start should be < mLargestStartOffset 
      int start = bottom.getCurrOffset();
      
      boolean bRet = false;
      
      int minOffsetToExceed = mLargestStartOffset - mMaxSpanLen - 1;
      
      if (start > mCurrSpanStartOffset /*
                                        * ignore if it's the same starting offset,
                                        * we care only about unique starting points.
                                        */
          && start > minOffsetToExceed 
              /* 
               * If the start offset is too far from the end offset,
               * we cannot have all query terms in a given span.
               */
          ) {
        /*
         *  Signal we found a span that may contain all elements. Again,
         *  here we do not know exactly if they are exactly within the span,
         *  because we only check the difference between element starting
         *  offsets, see also a comment inside TermSpanIterator#nextSpan().        
         */
        bRet = true;
        mCurrSpanStartOffset = start;
        mCurrSpanEndOffset   = start + mMaxSpanLen;
      }
      
      if (bottom.findNextElemLargerOffset(minOffsetToExceed)) {
        // May be the new end highest start offset
        mLargestStartOffset = Math.max(mLargestStartOffset, bottom.getCurrOffset());
        mStartOffsetQueue.add(bottom);
      } else {
        return false; // Exhausted all possibilities with one element
      }
      
      if (bRet) return true;
    }
      
    return false;
  }
  
  /**
   * See {@link TermSpanIterator#finishSpanIteration()}
   */  
  @Override
  protected void finishSpanIteration() {
    mStartOffsetQueue.clear();
  }
  
  private int                           mMaxSpanLen = 0;
  ElemInfoDataHolder[]                  mQueueObjs = null;
  PriorityQueue<ElemInfoDataHolder>     mStartOffsetQueue = null;
  private int                           mLargestStartOffset = -1;
}

/**
 *  A helper wrapper class to iterate over posting entries within a document.
 *  Objects of these class will be placed into the queue.
 *
 */
class ElemInfoDataHolder implements Comparable<ElemInfoDataHolder> {
  ElemInfoDataHolder(OnePostStateBase post) {
    mPost = post;
  }
  
  /**
   * Resets to zero the index indicating the current entry.  
   */
  public void reset() {
    mData = mPost.getAllElements();
    mQty = mPost.getQty();
    mCurr = 0;
  }
  
  /**
   * @return the current offset as long as mCurr < mQty.
   */
  public int getCurrOffset() {
    return mData[mCurr].mStartOffset;
  }

  /**
   * "Fast forward" the index of the current entry until the start
   * offset becomes larger than a given one. Note that it makes at 
   * least one move.
   * 
   * @return true   if the pointer still points to a valid element (after the move).
   */
  public boolean findNextElemLargerOffset(int offsetToExceed) {
    if (mCurr < mQty) {
      ++mCurr;
    
      if (mCurr < mQty) {
        if (getCurrOffset() > offsetToExceed) return true;
        mCurr = mPost.findElemLargerOffset(TermSpanIterator.FORWARD_ITER_QTY, 
                                           offsetToExceed, mCurr);          
        return mCurr < mQty;
      }
    }
    return false;
  }
  
  @Override
  public int compareTo(ElemInfoDataHolder o) {
    return getCurrOffset() - o.getCurrOffset();  
  }
  

  private OnePostStateBase  mPost;  
  private ElemInfoData[]    mData;
  private int               mCurr = 0;
  private int               mQty = 0;
}
