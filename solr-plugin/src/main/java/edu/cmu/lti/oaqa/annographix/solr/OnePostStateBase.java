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
import java.util.ArrayList;

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

import edu.cmu.lti.oaqa.annographix.solr.StructQueryParse.FieldType;
import edu.stanford.nlp.util.ArrayUtils;

/** 
 * This a base helper class to read annotation and token info from posting lists. 
 * 
 * @author Leonid Boytsov
 *
 */
public abstract class OnePostStateBase {
  public static int NO_MORE_DOCS = org.apache.lucene.search.DocIdSetIterator.NO_MORE_DOCS;
  // TODO change to a larger value
  protected static int INIT_SIZE = 1;
  
  public static OnePostStateBase createPost(DocsAndPositionsEnum posting, FieldType type) {
    return type == FieldType.FIELD_ANNOTATION ? 
                      new OnePostStateAnnot(posting):new OnePostStateToken(posting);
  }
  
  public OnePostStateBase(DocsAndPositionsEnum posting) {
    mPosting = posting;
    extendElemInfo(INIT_SIZE);
  }
  
  /**
   * @return a current document id.
   */
  public int getDocID() { return mDocId; }
  /**
   * @return an i-th element
   */
  public ElemInfoData getElement(int i) { return mElemInfo[i]; }
  /**
   * @return a number of elements in a current document.
   */
  public int getQty() { return mQty; }
  /**
   * @return a cost associated with the posting.
   */
  public long getPostCost() { return mPosting.cost(); }
  /**
   * @return a number of connected postings.
   */
  public int getConnectQty() { return mConnectQty; }
  
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
  
  /**
   * Memorize constraints associated with the given posting.
   * <p>The list of constraints includes a list of constraint types,
   * as well as the list of constraint nodes. Unfortunately, it is 
   * not possible to initialize such a list in a constructor, because
   * some of the elements will not exist at the time when constructor is
   * called. 
   * 
   * @param connectQty      an overall number of connected 
   *                        postings (not necessarily directly adjacent nodes).
   * @param constrType      a list of constraint types.
   * @param constrNode      a list of constraint/dependent nodes.
   * @throws Exception
   */
  public void setConstraints(int connectQty,
                             ArrayList<StructQueryParse.ConstraintType> constrType,
                             ArrayList<OnePostStateBase> constrNode) {
    mConnectQty = connectQty;
    if (constrNode.size() != constrType.size()) {
      throw new RuntimeException("Bug: constrType.size() != constrNode.size()");
    }
    mConstrType = new StructQueryParse.ConstraintType[constrType.size()];
    mConstrNode = new OnePostStateBase[constrNode.size()];
    
    for (int i = 0; i < constrType.size(); ++i) {
      mConstrType[i] = constrType.get(i);
      mConstrNode[i] = constrNode.get(i);
    }    
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
  
  protected StructQueryParse.ConstraintType[]     mConstrType = null;
  protected OnePostStateBase[]                    mConstrNode = null;
  protected int                                   mConnectQty = 0;
}

/**
 * This a helper class to read annotation info from posting lists' payloads.
 * 
 * @author Leonid boytsov
 *
 */
class OnePostStateAnnot extends OnePostStateBase {

  /**
   * @param posting     an already initialized posting list.
   */
  public OnePostStateAnnot(DocsAndPositionsEnum posting) {
    super(posting);
  }

  /**
   * Read next element {@link edu.cmu.lti.oaqa.annographix.solr.OnePostStateBase#readDocElements()}.
   */
  @Override
  protected void readDocElements() throws IOException {
    mQty = mPosting.freq();
    // Ensure we have enough space to store 
    extendElemInfo(mQty);
    for (int i = 0; i < mQty; ++i) {
      mPosting.nextPosition();   
      AnnotEncoder.decode(mPosting.getPayload(), mElemInfo[i]);
    }
  }

}

/**
 * This a helper class to read token info from posting lists.
 * 
 * @author Leonid boytsov
 *
 */
class OnePostStateToken extends OnePostStateBase {

  /**
    * @param posting    an already initialized posting list.
    */
  public OnePostStateToken(DocsAndPositionsEnum posting) {
    super(posting);
  }

  /**
   * Read next element {@link edu.cmu.lti.oaqa.annographix.solr.OnePostStateBase#readDocElements()}.
   */
  @Override
  protected void readDocElements() throws IOException {
    mQty = mPosting.freq();
    // Ensure we have enough space to store 
    extendElemInfo(mQty);
    for (int i = 0; i < mQty; ++i) {
      mPosting.nextPosition();
      ElemInfoData dt = mElemInfo[i];
      dt.mId = -1;
      dt.mParentId = -1;
      dt.mStartOffset = mPosting.startOffset();
      dt.mEndOffset = mPosting.endOffset();
    }
  }

}