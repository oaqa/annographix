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
import java.util.*;

import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.search.similarities.Similarity.SimScorer;

import edu.cmu.lti.oaqa.annographix.solr.StructQueryParse.FieldType;

/**
 * A comparator class that helps sort postings in the order of increasing cost.
 * 
 * @author Leonid Boytsov
 *
 */
class SortPostByCost implements Comparator<OnePostStateBase> {

  @Override
  public int compare(OnePostStateBase o1, OnePostStateBase o2) {
    long d = o1.getPostCost() - o2.getPostCost();
    return d == 0 ? 0 : (d < 0 ? -1 : 1);    
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


/**
 * 
 * A custom scorer class.
 * <p>This is workhorse class that does most of the hard work in three steps:</p>
 * <ol>
 * <li>Finds documents containing all query elements (by leapfrogging).
 * <li>Inside a found document, identify all spans containing all query elements.
 *    A span is specified by either i) the maximum size in the # of characters;
 *    ii) a label of the covering annotation.
 * <li>Inside each span, find at least one configuration (as specified by
 *    element offsets) that satisfies all constraints (as expressed by parent-child,
 *    or containment relationship) of the query.
 * </ol>
 * 
 * @author Leonid Boytsov
 *
 */
public class StructScorerVer3 extends Scorer {
  private SimScorer mDocScorerTextField; 
  private SimScorer mDocScorerAnnotField;
  private int       mCurrDocId = -1;
  private int       mSpan = Integer.MAX_VALUE;
  private int       mNumMatches = 0;
  /** 
   * All postings (+the posting of the covering annotation if the latter exists)
   * sorted in the order of increasing cost.
   */
  private OnePostStateBase[] mAllPostsSortedByCost;
  /**
   * Token/annotation only postings sorted first in the order of decreasing
   * connectedness, then in the order of increasing posting cost.
   */
  private OnePostStateBase[] mPostSortedByConnectCost;
  /**
   * A number of connected postings.
   */
  int     postConnectQty = 0;
  
  private OnePostStateBase   mCoverAnnotPost = null;

  /**
   * @param weight          An instance of the weight class that created this scorer.
   * @param mQueryParse     A parsed query.
   * @param postings        All postings except the covering annotation postings.
   * @param coverAnnotPost  The covering annotation posting, or null, if there's none.
   * @param span            The maximum span size in the # number of characters.
   * @param docScorerTextField  A similarity scorer for the text field.
   * @param docScorerAnnotField A similarity scorer for the annotation field.
   */
  public StructScorerVer3(Weight weight,
                          StructQueryParse queryParse, 
                          DocsAndPositionsEnum[] postings,
                          DocsAndPositionsEnum   coverAnnotPost,
                          int span,
                          SimScorer docScorerTextField, 
                          SimScorer docScorerAnnotField) {
    super(weight);
    mDocScorerAnnotField = docScorerAnnotField;
    mDocScorerTextField = docScorerTextField;
    mSpan = span;
    
    /**
     * We need to wrap up postings using objects of the type OnePostStateBase.
     * Then, we create two arrays of the type OnePostStateBase sorted differently.
     * 1) The first array should include all the postings including the
     * posting for the covering annotation (if it exists). This array
     * will be sorted in the increasing order of postings costs, which would 
     * allow us to leapfrog efficiently in the function {@see #advance()}.
     * 
     * 2) The second array should not contain a covering annotation and may be sorted
     * differently. More specifically, the comparison function will first 
     * compare the number of postings connected to the given nodes. A posting
     * with a larger number of connected postings is placed before a posting
     * with a smaller number of connected postings. In the case of a tie (i.e.,
     * an equal number of connected postings) a posting with the smaller cost
     * should be placed earlier.
     */
    
    /** 1. Sorting for efficient leapfrogging. */
    int tokQty = queryParse.getTokens().size();
    
    ArrayList<OnePostStateBase> allPostListUnsorted 
                                            = new ArrayList<OnePostStateBase>();

    for(int i = 0; i < tokQty; ++i) 
      allPostListUnsorted.add(
          OnePostStateBase.createPost(postings[i], queryParse.getTypes().get(i))
      );
    if (coverAnnotPost != null) {
      mCoverAnnotPost = 
        OnePostStateBase.createPost(coverAnnotPost, FieldType.FIELD_ANNOTATION);
      allPostListUnsorted.add(mCoverAnnotPost);
    }
    mAllPostsSortedByCost = new OnePostStateBase[allPostListUnsorted.size()];
    allPostListUnsorted.toArray(mAllPostsSortedByCost);
    
    Arrays.sort(mAllPostsSortedByCost, new SortPostByCost());
    
    /** 2. Sorting for efficient search within documents. */
    
    // First we need create arrays of constraints
    mPostSortedByConnectCost = new OnePostStateBase[tokQty];    
    for(int i = 0; i < tokQty; ++i) {
      mPostSortedByConnectCost[i] = allPostListUnsorted.get(i);
      ArrayList<OnePostStateBase>   constrNode = new ArrayList<OnePostStateBase>(); 
      
      for (int depId : queryParse.getDependIds(i))
        constrNode.add(allPostListUnsorted.get(depId));
      
      mPostSortedByConnectCost[i].setConstraints(
          queryParse.getConnectQty(i), 
          queryParse.getConstrTypes(i), 
          constrNode);
    }

    Arrays.sort(mPostSortedByConnectCost, new SortPostByConnectQtyAndCost());
  }

  /* (non-Javadoc)
   * @see org.apache.lucene.search.Scorer#score()
   */
  @Override
  public float score() throws IOException {
    // TODO Auto-generated method stub
    return mDocScorerAnnotField.score(mCurrDocId, freq()) +
           mDocScorerTextField.score(mCurrDocId, freq());
  }

  /* (non-Javadoc)
   * @see org.apache.lucene.index.DocsEnum#freq()
   */
  @Override
  public int freq() throws IOException {
    return mNumMatches;
  }

  /* (non-Javadoc)
   * @see org.apache.lucene.search.DocIdSetIterator#docID()
   */
  @Override
  public int docID() {
    return mCurrDocId;
  }

  /**
   * Advance to the next doc after {@see #docID()}.
   * <p>
   * In the ExactPhraseScorer in Solr, this function is highly optimized.
   * It is necessary, b/c the {@link #advance(int)} function is relatively costly 
   * while checking for an exact phrase occurrence can be implemented very
   * efficiently.
   * </p>
   * <p>  
   * In contrast, structured queries are expensive to evaluate. Thus, a clever 
   * optimization likely wouldn't make a lot of sense here. 
   * In fact, in a SloppyPhraseScorer (Solr 4.6) a simple advancement 
   * algorithm is used, which is not based on the galloping  intersection.
   * </p>
   * <p>If an optimized implementation of the {@link #nextDoc()} function does
   * not make sense for the sloppy phrase checker, it should be even
   * less useful for the (mostly) more expensive structured query scorer.
   */
  @Override
  public int nextDoc() throws IOException {
    return advance(mCurrDocId + 1);  
  }

  /* (non-Javadoc)
   * @see org.apache.lucene.search.DocIdSetIterator#advance(int)
   */
  @Override
  public int advance(int target) throws IOException {
    // first (least-costly, i.e., rarest) term
    int doc = mAllPostsSortedByCost[0].advance(target);

    if (doc == DocIdSetIterator.NO_MORE_DOCS) {
      return mCurrDocId = doc;
    }

    while(true) {
      // second, etc terms 
      int i = 1;
      while(i < mAllPostsSortedByCost.length) {
        OnePostStateBase  td = mAllPostsSortedByCost[i];
        int doc2 = td.getDocID();

        if (doc2 < doc) {
          doc2 = td.advance(doc);
        }

        if (doc2 > doc) {
          break;
        }
        i++;
      }

      if (i == mAllPostsSortedByCost.length) {
        /*
         *  found all elements in a document, 
         *  let's check compute the number of in-document occurrence.
         */
        mCurrDocId = doc;
        mNumMatches = computeFreq();

        if (mNumMatches != 0) {
          return mCurrDocId;
        }
      }

      doc = mAllPostsSortedByCost[0].nextDoc();

      if (doc == DocIdSetIterator.NO_MORE_DOCS) {
        return mCurrDocId = doc;
      }
    }  
  }

  private int computeFreq() {
    // TODO Auto-generated method stub
    return 0;
  }

  /* (non-Javadoc)
   * @see org.apache.lucene.search.DocIdSetIterator#cost()
   */
  @Override
  public long cost() {
    // TODO Auto-generated method stub
    return 0;
  }
}
