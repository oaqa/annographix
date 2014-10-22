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

import java.io.IOException;

import org.apache.lucene.index.DocsAndPositionsEnum;
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
import org.apache.lucene.search.DocIdSetIterator;

import edu.stanford.nlp.util.ArrayUtils;

/** 
 * This a base helper class to read annotation and token info from posting lists. 
 * 
 * @author Leonid Boytsov
 *
 */
public abstract class OnePostStateBase implements Comparable <OnePostStateBase> {
  public static int NO_MORE_DOCS = org.apache.lucene.search.DocIdSetIterator.NO_MORE_DOCS;
  // TODO change to a larger value
  protected static int INIT_SIZE = 1;
  
  public OnePostStateBase(DocsAndPositionsEnum posting) {
    mPosting = posting;
    extendElemInfo(INIT_SIZE);
  }
  
  /**
   * @return a current document id.
   */
  public int getDocId() { return mDocId; }
  /**
   * @return an i-th element
   */
  public ElemInfoData getElement(int i) { return mElemInfo[i]; }
  /**
   * @return a number of elements in a current document.
   */
  public int getQty() { return mQty; }
  
  /**
   * Move to the first document with id >= docId.
   * 
   * @param docId           find a document with id at least this large.
   * @return                the first document with id >= docId or {@link OnePostStateBase#NO_MORE_DOCS} if no docs are available with ids >= docId.
   * @throws IOException
   */
  public int advance(int docId) throws IOException {
    mQty = 0;
    if (mDocId != NO_MORE_DOCS)
      mDocId = mPosting.advance(docId);
    if (mDocId != NO_MORE_DOCS)
      readDocElements();
    return mDocId;
  }
  
  /**
   * Move to the next available document.
   * 
   * @return    the next document iD or {@link OnePostStateBase#NO_MORE_DOCS} if no docs are available.
   * @throws IOException
   */
  public int nextDoc() throws IOException {
    mQty = 0;
    if (mDocId != NO_MORE_DOCS)
      mDocId = mPosting.nextDoc();
    if (mDocId != NO_MORE_DOCS)
      readDocElements();
    return mDocId;
  }  
  
  @Override
  /**
   * This function is used to order postings for efficient leapfrog-based
   * intersection of postings.
   */
  public int compareTo(OnePostStateBase o) {
    long d = getPostPost() - o.getPostPost();
    return d == 0 ? 0 : (d < 0 ? -1 : 1);
  }

  public long getPostPost() {
    return mPosting.cost();
  }  
  
  /**
   * This function read one-document tokens/annotations start and end offsets.
   */
  protected abstract void readDocElements()  throws IOException;
  
  /**
   * Re-allocate the mElemInfo array if necessary.
   * 
   * @param newCapacity  a new minimum number of elements to accommodate.
   */
  protected void extendElemInfo(int newCapacity) {
    if (newCapacity > mElemInfo.length) {
      ElemInfoData oldElemInfo[] = mElemInfo;
      mElemInfo = new ElemInfoData[newCapacity * 2];
      for (int i = 0; i < oldElemInfo.length; ++i) {
        mElemInfo[i] = oldElemInfo[i]; // re-use previously allocated memory
      }
      for (int i = oldElemInfo.length; i < mElemInfo.length; ++i) {
        mElemInfo[i] = new ElemInfoData();
      }
    }
  }
  
  protected DocsAndPositionsEnum    mPosting;
  protected int                     mDocId;
  protected int                     mQty = 0;
  protected ElemInfoData            mElemInfo[] = new ElemInfoData[0];  
}
