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

import java.util.*;

/**
 * A base class for classes that help you iterate 
 * over possible matches inside a document.
 * 
 * @author Leonid Boytsov
 *
 */
public abstract class TermSpanInterator {
  /** Postings of query element. */
  private OnePostStateBase[]        mElemSorted;
  /** Starting element ids inside the span. */
  private int[]                     mStartElemId;
  /** Ending element ids inside the span. */
  private int[]                     mEndElemId;
  /** A number of connected postings. */
  private int                       mPostConnectQty = 0;  

  /**
   * Finds the next covering span.
   * 
   * @return    false if we reach the end of the document or true,
   *            if the next span is found.
   */
  public abstract boolean nextSpan();
  public abstract boolean checkSpanConstraints(); 

  /**
   * @param elemSorted  a sorted (first by connectedness, then by cost) 
   *                    array of posting states.
   */
  private TermSpanInterator(OnePostStateBase[] elemSorted) {
    mElemSorted = elemSorted;
    mStartElemId = new int[mElemSorted.length];
    mEndElemId = new int[mElemSorted.length];
    for (int i = 0; 
        i < mElemSorted.length && mElemSorted[i].getConnectQty() > 0;
        ++i) {
      mPostConnectQty = i + 1;      
    }
  }
  
  /**
   * A comparator class that helps sort postings first in the order of
   * decreasing connectedness, and next in the order 
   */
  class SortPostByConnectQtyAndCost implements Comparator<OnePostStateBase> {

    @Override
    public int compare(OnePostStateBase o1, OnePostStateBase o2) {
      if (o1.getConnectQty() != o2.getConnectQty()) {
      /*
       *  if the first element has more connections, we return a value < 0, 
       *  so that this entry will be ranked before entries with a smaller
       *  number of connections.
       */
        return o2.getConnectQty() - o1.getConnectQty();
      }    
      
      long d = o1.getPostCost() - o2.getPostCost();
      return d == 0 ? 0 : (d < 0 ? -1 : 1);    
    }  
  }  
}
