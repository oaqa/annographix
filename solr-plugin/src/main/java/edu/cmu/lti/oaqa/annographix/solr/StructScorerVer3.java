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
class StructScorerVer3 extends Scorer {
  private SimScorer mDocScorerTextField; 
  private SimScorer mDocScorerAnnotField;
  private int       mCurrDocId = -1;
  private int       mSpan = Integer.MAX_VALUE;
  private int       mNumMatches = 0;
  private OnePostStateBase[] mAllPosts;
  private OnePostStateBase   mCoverAnnotPost = null;
  private QueryGraphNode[]   mAllGraphNodes;
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
     * We need to package postings in two different ways.
     * 1) Embed them into objects of the type OnePostStateBase and sort an
     * array of the type OnePostStateBase[]. This will allow us to leapfrog
     * efficiently in the function {@see #advance()}.
     * 2) Further embed instances of the type OnePostStateBase into the objects
     * of the type QueryGraphNode. This allows us to resort postings differently.
     * In addition, the latter array won't include a covering annotation.
     */
    
    /** 1. Sorting for efficient {@see #advance()}. */
    ArrayList<OnePostStateBase> allPostList = new ArrayList<OnePostStateBase>();
    for(int i = 0; i < queryParse.getTokens().size(); ++i) 
      allPostList.add(
          OnePostStateBase.createPost(postings[i], queryParse.getTypes().get(i))
      );
    if (coverAnnotPost != null)
      mCoverAnnotPost = 
        OnePostStateBase.createPost(coverAnnotPost, FieldType.FIELD_ANNOTATION);
    mAllPosts = new OnePostStateBase[allPostList.size()];
    allPostList.toArray(mAllPosts);
    
    Arrays.sort(mAllPosts);
    /** 2. Sorting for efficient search within documents. */
    /*
    ArrayList<QueryGraphNode>  allGraphNodes = new ArrayList<QueryGraphNode>();
    
    for (int i = 0; i < postings.length; ++i) {
      **** constructing allGraphNodes won't be simple
      and in fact maybe I don't really need another wrapper class,
      which may also be inefficient??? Can I reuse the OnePostStateBase ???
      *****
    }
    */
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
    int doc = mAllPosts[0].advance(target);

    if (doc == DocIdSetIterator.NO_MORE_DOCS) {
      return mCurrDocId = doc;
    }

    while(true) {
      // second, etc terms 
      int i = 1;
      while(i < mAllPosts.length) {
        OnePostStateBase  td = mAllPosts[i];
        int doc2 = td.getDocID();

        if (doc2 < doc) {
          doc2 = td.advance(doc);
        }

        if (doc2 > doc) {
          break;
        }
        i++;
      }

      if (i == mAllPosts.length) {
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

      doc = mAllPosts[0].nextDoc();

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
