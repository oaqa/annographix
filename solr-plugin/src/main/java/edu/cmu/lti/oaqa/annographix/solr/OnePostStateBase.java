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
import java.util.Arrays;
import java.util.Comparator;

import org.apache.lucene.index.DocsAndPositionsEnum;

import edu.cmu.lti.oaqa.annographix.solr.StructQueryParse.ConstraintType;
import edu.cmu.lti.oaqa.annographix.solr.StructQueryParse.FieldType;

/** 
 * This a base helper class to read annotation and token info from posting lists. 
 * This class should not be shared among threads, because it has thread-unsafe
 * function {@link #findElemLargerOffset(int, int, int)}. It is totally safe to use,
 * though, as long as each thread creates <b>its own copy (or 
 * multiple copies if needed) of the class</b> .
 * 
 * @author Leonid Boytsov
 *
 */
public abstract class OnePostStateBase {
  public static int NO_MORE_DOCS = org.apache.lucene.search.DocIdSetIterator.NO_MORE_DOCS;
  /** The initial size of the array containing matching entries inside a single document. */
  protected static int INIT_SIZE_ELEM_INFO = 512; 
  
  /**
   * 
   * Creates a wrapper of the right type.
   * 
   * @param posting     posting to wrap.
   * @param token       textual representation.
   * @param type        posting type: annotation or regular token.
   * @param connectQty  number of postings connected with a given node/posting 
   *                    via a query graph.
   * @param minCompPostCost a minimum cost among postings associated with the subset
   *                    of nodes connected to this posting.
   * @param componentId a unique ID associated with the subset of nodes associated 
   *                    with the subset of nodes connected to this posting.
   * 
   * @return            reference to the newly created object
   */
  public static OnePostStateBase createPost(DocsAndPositionsEnum posting, 
                                            String    token,
                                            FieldType type,
                                            int connectQty,
                                            long minCompPostCost,
                                            int componentId) {
    return type == FieldType.FIELD_ANNOTATION ? 
                      new OnePostStateAnnot(token, type, posting, 
                                            connectQty, minCompPostCost, componentId):
                      new OnePostStateText(token, type, posting, 
                                            connectQty, minCompPostCost, componentId);
  }
  
  public OnePostStateBase(String token, FieldType type,
                          DocsAndPositionsEnum posting, 
                          int connectQty,
                          long minCompPostCost,
                          int componentId) {
    mToken  = token;
    mFieldType = type;
    mPosting = posting;
    mConnectQty = connectQty;
    mMinCompPostCost = minCompPostCost;
    mComponentId = componentId;
    extendElemInfo(INIT_SIZE_ELEM_INFO);
  }
  
  /**
   * @return a current document id.
   */
  public int getDocID() { return mDocId; }
  
  /**
   * @return an i-th element
   */
  public ElemInfoData getElement(int i) { return mSortedElemInfo[i]; }
  
  /**
   * @return all elements
   */
  public ElemInfoData[] getAllElements() { return mSortedElemInfo; }
  
  /**
   * @return a current element
   */
  public ElemInfoData getCurrElement() { return mSortedElemInfo[mCurrElemIndx]; }
  
  /**
   * Changes the internal index pointing to the current element, 
   * without checking if the index is invalid.
   * 
   * @param index   an index to set.
   */
  public void setCurrElemIndex(int index) {
    mCurrElemIndx = index;
  }
  
  /**
   * @return an internal element index
   */
  public int getCurrElemIndex() { return mCurrElemIndx; }

  /**
   * Find an element with an offset larger than the specified one.  
   * 
   * <p>
   * This function assumes that elements are sorted by in the order of 
   * non-decreasing offsets. It relies on the efficient exponential search. 
   * </p> 
   *
   * @param     sortedElemInfo    an array of elements sorted in the order of
   *                              non-decreasing offsets.
   * @param     sortedElemQty     an array may contain a large number of pre-allocated
   *                              elements, however, we need to use only this
   *                              number of elements.               
   * @param     reusableOffComp   a reusable comparator object.                                     
   * @param     reusableKey       a reusable key (we reuse the key to avoid an
   *                              extra memory allocation).
   * @param     linSearchIterQty  a maximum number of forward iterations to carry out,
   *                              before starting a full-blown exponential search. 
   * @param     offsetToExceed    find elements with offset greater than this value.
   * @param     minIndx           search among elements with the index at least as
   *                              larger as this index.
   *                            
   *  
   * @return    the index (>= minIndx) of the first element 
   *            whose offset > minOffset or {@link #getQty()} if such
   *            element cannot be found. 
   *             
   *  
   */

  static public int findElemLargerOffset(
                            ElemInfoData[]  sortedElemInfo,
                            int             sortedElemQty,
                            StartOffsetElemInfoDataComparator reusableOffComp,                                  
                            ElemInfoData    reusableKey,
                            int linSearchIterQty,
                            int offsetToExceed, 
                            int minIndx) {
    minIndx = Math.max(0, minIndx);
    
    for (int i = 0; 
        i < linSearchIterQty && minIndx < sortedElemQty; 
        ++i, ++minIndx) {
      if (sortedElemInfo[minIndx].mStartOffset > offsetToExceed) return minIndx;
    }   
    
    if (minIndx >= sortedElemQty) return sortedElemQty;
    if (sortedElemInfo[minIndx].mStartOffset > offsetToExceed) return minIndx;
    int d = 1;
    int indx1 = minIndx, indx2 = -1;
    /*
     *  Loop invariant:
     *      minIndx < indx1 < sortedElemQty && 
     *      sortedElemInfo[indx1] <= offsetToExceed
     */
    while (true) {
      indx2 = indx1 + d;
      if (indx2 < sortedElemQty) {
        if (sortedElemInfo[indx2].mStartOffset > offsetToExceed) {
          break;
        } else {
          indx1 = indx2;
        }
      } else {
        // sortedElemQty - 1 >= indx1 >= minIndx
        if (sortedElemInfo[sortedElemQty - 1].mStartOffset <= offsetToExceed) {
          return sortedElemQty;
        }
        /*
         *  The last elements has the offset larger than the specified
         *  function parameter. Yet, it may not be the first such offset.
         *  Find the first one offset > offsetToExceed using the binary search.
         */
        indx2 = sortedElemQty;
        break;
      }
      int maxDiff = (sortedElemQty - indx1);
      if (d <= (maxDiff >> 1))
        d <<= 1;
      else 
        d = maxDiff;
    }
    /*
     * After exiting the loop it is guaranteed that:
     * 1) sortedElemInfo[indx2].mStartOffset > offsetToExceed
     * Due to loop invariant:
     * 2) sortedElemInfo[indx1].mStartOffset <= offsetToExceed
     * 3) minIndx < indx1 < sortedElemQty 
     */
    
    reusableKey.mStartOffset = offsetToExceed;
    // Search from indx1 inclusive to indx2 exclusive
    int res = Arrays.binarySearch(sortedElemInfo, 
                              indx1, indx2,
                              reusableKey, 
                              reusableOffComp);
    if (res >= 0) {
      /*
       *  Find the first element larger than res
       */
      while (res < indx2 && 
             sortedElemInfo[res].mStartOffset == offsetToExceed)  {
        ++res;
      }
    } else {
      /*
       *  found an order-preserving insertion point, which represents one
       *  of the following:
       *  1) End of the array
       *  2) An index of an element larger than offsetToExceed  
       */
      res = -(res+1);
    }
    return res;    
  }
  
  /**
   * Find an element with an offset larger than the specified one,
   * see {@link #findElemLargerOffset(ElemInfoData[], int, StartOffsetElemInfoDataComparator, ElemInfoData, int, int, int)}.
   *
   * @param     linSearchIterQty  a maximum number of forward iterations to carry out,
   *                              before starting a full-blown exponential search. 
   * @param     offsetToExceed    find elements with offsets larger than this value.
   * @param     minIndx           find elements with indices >= than this value.
   * 
   * @return                a minimum index of the element whose offset is
   *                        larger than the specified parameter, or the
   *                        number of elements, if no such element exists.
   */
  public int findElemLargerOffset(int linSearchIterQty,
                                  int offsetToExceed, 
                                  int minIndx) {
    return findElemLargerOffset(mSortedElemInfo, mQty, 
                                mOffsetComp, mSearchKey,
                                linSearchIterQty, offsetToExceed, minIndx);
  }
  
  /**
   * @return a number of elements in a current document.
   */
  public int getQty() { return mQty; }
  
  /**
   * @return a cost associated with the posting.
   */
  public long getPostCost() { return mPosting.cost(); }
  
  /**
   * @return a number of postings connected with a given node/posting 
   *         via a query graph.
   */
  public int getConnectQty() { return mConnectQty; }

  /**
   * @return a minimum cost among postings associated with the subset
   *         of nodes connected to this posting.   
   */
  public long getMinCompPostCost() { return mMinCompPostCost; }
  
  
  /**
   * @return a unique ID associated with the subset of nodes that
   *         are connected to this one.   
   */
  public int getComponentId() { return mComponentId; }
  
  
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
    if (mDocId != NO_MORE_DOCS) {
      readDocElements();
    }
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
    if (mDocId != NO_MORE_DOCS) {
      readDocElements();
    }
    return mDocId;
  }  
  
  /**
   * Memorize constraints associated with the given posting.
   * 
   * <p>
   * The list of constraints includes a list of constraint types,
   * as well as the list of constraint nodes. Unfortunately, it is 
   * not possible to initialize such a list in a constructor, because
   * some of the elements may not exist at the time when constructor is
   * called.
   * </p>
   * 
   * @param     constrType      a list of constraint types.
   * @param     constrNode      a list of constraint/dependent nodes.
   * 
   * @throws Exception
   */
  public void setConstraints(ArrayList<StructQueryParse.ConstraintType> constrType,
                             ArrayList<OnePostStateBase> constrNode) {
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
   * Memorize an index in the sorted array of postings
   * 
   * @param     sortIndx        an index in the array of postings sorted
   *                            using SortPostByConnectQtyAndCost.
   */
  public void setSortIndex(int sortIndx) {
    mSortIndx = sortIndx;
  }
  
  /**
   * @return    an index in the array of postings sorted
   *            using SortPostByConnectQtyAndCost.
   */
  public int getSortIndex() { return mSortIndx; }
  
  /**
   * Check if constraints involving the current node and the node
   * with the index in the range [sortIndxMin, sortIndxMax] are satisfied.
   *  
   * <p>
   * The complexity of this operation is O(Nconstr). 
   * In reality, we expect Nconstr to be quite small (around 2-5). 
   * Thus, iterating over an array is faster than 
   * retrieving data from a TreeMap.  
   * </p>
   * 
   * @param     minSortIndx the minimum id of the node participating in a constraint.
   * @param     maxSortIndx the minimum id of the node participating in a constraint.
   * 
   * @return    true if constraints are satisfied and false otherwise.
   */
  public boolean checkConstraints(int minSortIndx,
                                  int maxSortIndx) {
    for (int k = 0; k < mConstrType.length; ++k) {
      OnePostStateBase  nodeOther = mConstrNode[k];
      
      int otherSortIndx = nodeOther.getSortIndex();
      if (otherSortIndx >= minSortIndx && 
          otherSortIndx <= maxSortIndx) { 
        ElemInfoData    eCurr = mSortedElemInfo[mCurrElemIndx];
        ElemInfoData    eOther = nodeOther.getCurrElement();
        
        if (mConstrType[k] == ConstraintType.CONSTRAINT_PARENT){
          if (eCurr.mId != eOther.mParentId) return false;
        } else {
          // mConstrType[k] == ConstraintType.CONSTRAINT_CONTAINS
          if (eOther.mStartOffset < eCurr.mStartOffset ||
              eOther.mEndOffset > eCurr.mEndOffset) return false;
        }
      }
    }
    return true;
  }
  
  /**
   * This function read one-document tokens/annotations start and end offsets.
   * All read entries are supposed to be sorted by the start offset!
   */
  protected abstract void readDocElements()  throws IOException;
  
  protected void readDocElementsBase() throws IOException {
    mCurrElemIndx=0;
    mQty = mPosting.freq();
    // Ensure we have enough space to store 
    extendElemInfo(mQty);    
  }
  
  /**
   * Re-allocate the mElemInfo array if necessary.
   * 
   * @param newCapacity  a new minimum number of elements to accommodate.
   */
  protected void extendElemInfo(int newCapacity) {
    if (newCapacity > mSortedElemInfo.length) {
      ElemInfoData oldElemInfo[] = mSortedElemInfo;
      mSortedElemInfo = new ElemInfoData[newCapacity * 2];
      for (int i = 0; i < oldElemInfo.length; ++i) {
        mSortedElemInfo[i] = oldElemInfo[i]; // re-use previously allocated memory
      }
      for (int i = oldElemInfo.length; i < mSortedElemInfo.length; ++i) {
        mSortedElemInfo[i] = new ElemInfoData();
      }
    }
  }
  
  protected String                  mToken;
  protected FieldType               mFieldType;
  protected DocsAndPositionsEnum    mPosting;
  /** 
   * The current document id, should always be -1 in the beginning,
   * before {@link #advance(int)} or {@link #nextDoc()} is called.
   */
  protected int                     mDocId=-1;
  protected int                     mCurrElemIndx=0;
  protected int                     mQty = 0;
  /** Elements are supposed to be started by offset. */
  protected ElemInfoData[]          mSortedElemInfo = new ElemInfoData[0];
  protected ElemInfoData            mSearchKey = new ElemInfoData();
  protected StartOffsetElemInfoDataComparator   mOffsetComp = 
                                      new StartOffsetElemInfoDataComparator();
  
  protected StructQueryParse.ConstraintType[]     mConstrType = null;
  protected OnePostStateBase[]                    mConstrNode = null;
  protected int                                   mConnectQty = 0;
  protected long                                  mMinCompPostCost = Long.MAX_VALUE;
  protected int                                   mComponentId = -1;
  protected int                                   mSortIndx = -1;

}

class StartOffsetElemInfoDataComparator implements Comparator<ElemInfoData> {

  @Override
  public int compare(ElemInfoData o1, ElemInfoData o2) {
    return o1.mStartOffset - o2.mStartOffset;
  }
}

