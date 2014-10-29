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
 * 
 * See {@link TermSpanIterator}, covering windows are provided by an annotation,
 * i.e., we find all occurrences of this annotation in the document.
 * 
 * @author Leonid Boytsov
 *
 */
public class TermSpanIteratorCoverAnnot extends TermSpanIterator {
  /**
   * Constructor.
   * 
   * @param     postSorted  see {@link TermSpanIterator#TermSpanIterator(OnePostStateBase[])}
   * @param     coverAnnot  a pointer to the covering annotator posting wrapper.
   */
  TermSpanIteratorCoverAnnot(OnePostStateBase[] postSorted,
                             OnePostStateBase   coverAnnot) {
    super(postSorted);
    mCoverAnnot = coverAnnot;
  }
  
  /**
   * See {@link TermSpanIterator#initSpanIteration()}
   */
  @Override
  public void initSpanIteration() {
    initSpanIterationBase();
    mCoverAnnotQty = mCoverAnnot.getQty();
    mAnnotIndx = -1;
  }

  /**
   * See {@link TermSpanIterator#nextSpanInternal()}
   */
  @Override
  protected boolean nextSpanInternal() {
    if (++mAnnotIndx >= mCoverAnnotQty) return false;
    ElemInfoData    elem = mCoverAnnot.getElement(mAnnotIndx);
    
    mCurrSpanStartOffset    = elem.mStartOffset;
    mCurrSpanEndOffset      = elem.mEndOffset;

    return true;
  }
  
  /**
   * See {@link TermSpanIterator#finishSpanIteration()}
   */  
  @Override
  protected void finishSpanIteration() {
    mAnnotIndx = mCoverAnnotQty;
  }
  

  
  private int               mAnnotIndx = -1;
  private int               mCoverAnnotQty = 0;
  private OnePostStateBase  mCoverAnnot;  
}