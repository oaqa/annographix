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

public class TermSpanIteratorCoverAnnot extends TermSpanInterator {
  /**
   * Constructor.
   * 
   * @param     elemSorted  see {@link TermSpanInterator#TermSpanInterator(OnePostStateBase[])}
   * @param     coverAnnot  a pointer to the covering annotator posting wrapper.
   */
  TermSpanIteratorCoverAnnot(OnePostStateBase[] elemSorted,
                             OnePostStateBase   coverAnnot) {
    super(elemSorted);
    mCoverAnnot = coverAnnot;
  }
  /**
   * See {@link TermSpanIterator#reset}.
   */
  @Override
  public void reset() {
    mCurrSpanEndOffset = -1;
    mCoverAnnotQty = mCoverAnnot.getQty();
    mAnnotIndx = -1;
    for (int i = 0; i < mElemSorted.length; ++i)
      mStartElemIndx[i] = -1;
  }  
  /**
   * See {@link TermSpanInterator#nextSpan()}.
   * 
   */
  @Override
  public boolean nextSpan() {
    ++mAnnotIndx;
    
    while (mAnnotIndx < mCoverAnnotQty) {
      /*
       *  Check if we can find appropriate start and end positions
       *  for other postings.
       */
      ElemInfoData  coverAnnot = mCoverAnnot.getElement(mAnnotIndx);

      boolean bOk = true;
      
      for (int k = 0; k < mElemSorted.length; ++k) {
        OnePostStateBase elemPost = mElemSorted[k];
        int   elemQty = elemPost.getQty();
        
        int nextStart = elemPost.findElemIndexWithLargerOffset(
                                                    coverAnnot.mStartOffset - 1, 
                                                    mStartElemIndx[k]);
        if (nextStart >= elemQty) {
          /*
           *  Because we cannot find at least one element with the start
           *  offset >= mCoverAnnot.mStartOffset, no match is possible
           *  (a match should include all elements).  
           */
          mAnnotIndx = mCoverAnnotQty;
          return false;
        }
        ElemInfoData nextElem = elemPost.getElement(nextStart);
        if (nextElem.mStartOffset >= coverAnnot.mEndOffset) {
          /*
           *  uups the next found element is actually to the right of the current
           *  covering annotation (square brackets denote annotatins):
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
            elemPost.findElemIndexWithLargerOffset(coverAnnot.mEndOffset - 1,
                                                   mStartElemIndx[k] + 1);
        /*
         *  If we subtract one from nextEnd, 
         *  we obtain the index of the LAST element 
         *  whose start offset is within a covering annotation.
         *  This is what we need, because mEndElemIndx[k] is
         *  treated exclusively.
         */
        mEndElemIndx[k] = nextEnd;
      }
      
      if (bOk) {
        mCurrSpanEndOffset = coverAnnot.mEndOffset;
        return true;
      }
    }
    
    return false;
  }
  
  private int               mAnnotIndx = -1;
  private int               mCoverAnnotQty = 0;
  private OnePostStateBase  mCoverAnnot;

}