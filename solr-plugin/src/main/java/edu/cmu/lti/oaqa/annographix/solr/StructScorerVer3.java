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

import edu.cmu.lti.oaqa.annographix.solr.StructQueryParseVer3.FieldType;


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
  private int       mSpan;
  private int       mMaxSpanCheckConstrIter;
  private int       mNumMatches = 0;
  
  /** 
   * All postings (+the posting of the covering annotation if the latter exists)
   * sorted in the order of increasing cost.
   */
  private OnePostStateBase[] mAllPostsSortedByCost;
  /**
   * Text/annotation only postings sorted first in the order of decreasing
   * connectedness, then in the order of increasing posting cost.
   */
  private OnePostStateBase[] mPostSortByConnQtyMinCostCompIdPostCost;
  /**
   * A number of connected postings.
   */
  int     postConnectQty = 0;
  
  
  private TermSpanIterator           mTermSpanIterator;
  
  private OnePostStateBase           mCoverAnnotPost = null;
  private long mCost = 0;


  /**
   * @param weight          An instance of the weight class that created this scorer.
   * @param queryParse      A parsed query.
   * @param postings        All postings except the covering annotation postings.
   * @param coverAnnotPost  The covering annotation posting, or null, if there's none.
   * @param span            The maximum span size in the # number of characters.
   * @param docScorerTextField  A similarity scorer for the text field.
   * @param docScorerAnnotField A similarity scorer for the annotation field.
   * @param maxSpanCheckConstrIter    The maximum number of brute-force iterations that we carry out
   *                                  before giving up on constraint checking for the <b>current span</b>. 
   */
  public StructScorerVer3(Weight weight,
                          StructQueryParseVer3 queryParse, 
                          DocsAndPositionsEnum[] postings,
                          DocsAndPositionsEnum   coverAnnotPost,
                          int span,
                          SimScorer docScorerTextField, 
                          SimScorer docScorerAnnotField, 
                          int maxSpanCheckConstrIter) {
    super(weight);

    mDocScorerAnnotField = docScorerAnnotField;
    mDocScorerTextField = docScorerTextField;
    mSpan = span;
    mMaxSpanCheckConstrIter = maxSpanCheckConstrIter;
    
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

    if (tokQty > 0) {
      long minPostCompCost[] = new long[tokQty];
      for (int i = 0; i < tokQty; ++i) {
        minPostCompCost[i] = Long.MAX_VALUE;
      }
      for (int i = 0; i < tokQty; ++i) { 
        int compId = queryParse.getComponentId(i);
        minPostCompCost[compId] = Math.min(minPostCompCost[compId], 
                                           postings[i].cost());
      }
      
      for (int i = 0; i < tokQty; ++i) { 
        int compId = queryParse.getComponentId(i);
        allPostListUnsorted.add(
            OnePostStateBase.createPost(postings[i], 
                                        queryParse.getTokens().get(i),
                                        queryParse.getTypes().get(i),
                                        queryParse.getConnectQty(i),
                                        minPostCompCost[compId],
                                        compId)
        );
      }
    }
    if (coverAnnotPost != null) {
      mCoverAnnotPost = 
        OnePostStateBase.createPost(coverAnnotPost,
                                    "", 
                                    FieldType.FIELD_ANNOTATION,
                                    0, 0, 0);
      allPostListUnsorted.add(mCoverAnnotPost);
    }
    mAllPostsSortedByCost = new OnePostStateBase[allPostListUnsorted.size()];
    allPostListUnsorted.toArray(mAllPostsSortedByCost);
    
    Arrays.sort(mAllPostsSortedByCost, new SortPostByCost());
    // a heuristic let the cost be equal to the size of the shortest posting
    mCost = mAllPostsSortedByCost[0].getPostCost();
    
    /** 2. Sorting for efficient search within documents. */
    
    // First we need create arrays of constraints
    mPostSortByConnQtyMinCostCompIdPostCost = new OnePostStateBase[tokQty];    
    for(int i = 0; i < tokQty; ++i) {
      mPostSortByConnQtyMinCostCompIdPostCost[i] = allPostListUnsorted.get(i);
      ArrayList<OnePostStateBase>   constrNode = new ArrayList<OnePostStateBase>(); 
      
      for (int depId : queryParse.getDependIds(i))
        constrNode.add(allPostListUnsorted.get(depId));
      
      mPostSortByConnQtyMinCostCompIdPostCost[i].setConstraints(queryParse.getConstrTypes(i), 
                                                 constrNode);
    }
    // Sort postings and assign sort indexes...
    Arrays.sort(mPostSortByConnQtyMinCostCompIdPostCost, 
                new SortByConnQtyMinCostCompIdPostCost());
    
    for (int i = 0; i < tokQty; ++i) {
      mPostSortByConnQtyMinCostCompIdPostCost[i].setSortIndex(i);
    }
    
    // Let's create an "index" for more efficient incremental constraint verification
    for (int i = 0; i < tokQty; ++i) {
      mPostSortByConnQtyMinCostCompIdPostCost[i]
          .buildConstraintIndex(mPostSortByConnQtyMinCostCompIdPostCost);
    }
    
    
    /*
     *  Let's create a span iterator. It is defined by either
     *  a covering span or by a maximum span lengths. A combination
     *  of these different methods is not supported. If the covering
     *  annotation is specified, this method takes precedence over
     *  explicit span length.
     */
    if (mCoverAnnotPost != null) {
      mTermSpanIterator = 
          new TermSpanIteratorCoverAnnot(mPostSortByConnQtyMinCostCompIdPostCost,
                                         mCoverAnnotPost, mMaxSpanCheckConstrIter); 
    } else {
      mTermSpanIterator = 
          new TermSpanIteratorMaxLen(mPostSortByConnQtyMinCostCompIdPostCost,
                                     mSpan, mMaxSpanCheckConstrIter);
      
    }
  }

  /**
   * @return a score associated with a given number of matches inside the document.
   */
  @Override
  public float score() throws IOException {
    return mDocScorerAnnotField.score(mCurrDocId, freq()) +
           mDocScorerTextField.score(mCurrDocId, freq());
  }

  /**
   * @return a number of matches inside the document.
   */
  @Override
  public int freq() throws IOException {
    return mNumMatches;
  }

  /**
   * @return the current document id.
   */
  @Override
  public int docID() {
    return mCurrDocId;
  }

  /**
   * Advance to the next doc after {@link #docID()}.
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
    if (mCurrDocId == DocIdSetIterator.NO_MORE_DOCS) {
      return mCurrDocId;
    }
    return advance(mCurrDocId + 1);  
  }

  /**
   * Move to the first document with id &gt;= target.
   * 
   * @param target      find a document at least this large.
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
         *  found all query elements in a document, 
         *  let's compute a number of matches inside
         *  this document
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

  /**
   * This function computes a number of spans (as represented by the 
   * maximum span size or a covering annotation) where a query graph
   * matches a graph of document tokens.
   *  
   * @return    number of matches inside a document.
   */
  private int computeFreq() {
    mTermSpanIterator.initSpanIteration();
    int qty = 0;
    while (mTermSpanIterator.nextSpan()) {
      if (mTermSpanIterator.checkSpanConstraints()) ++qty;
    }
    return qty;
  }

  /** 
   * Returns the estimated cost of this 
   * scorer {@link org.apache.lucene.search.DocIdSetIterator#cost()}.
   */
  @Override
  public long cost() {
    return mCost ;
  }
  
  /**
   * A comparator class to sort postings in the order of increasing cost.
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
   * A comparator class to sort postings by several attributes:
   * <ul>
   * <li>A number of postings/nodes it is connected with -- decreasing.
   * <li>A minimum cost of postings among connected nodes -- increasing.
   * <li>A component ID -- increasing.
   * <li>A posting cost -- increasing.
   * </ul>
   * 
   * <p>It is not hard to see that connected postings will always be 
   * adjacent. Better connected components are prioritized, as well as
   * components with the smallest minimum posting cost. Inside a single
   * component, a posting with lower score is prioritized.</p>
   *
   */
  class SortByConnQtyMinCostCompIdPostCost implements Comparator<OnePostStateBase> {

    @Override
    public int compare(OnePostStateBase o1, OnePostStateBase o2) {
      // 1. By connectedness -- decreasing.
      if (o1.getConnectQty() != o2.getConnectQty()) {
      /*
       *  if the first element has more connections, we return a value < 0, 
       *  so that this entry will be ranked before entries with a smaller
       *  number of connections.
       */
        return o2.getConnectQty() - o1.getConnectQty();
      }
      /*
       * 2. By minimum posting cost inside the component, increasing.
       * Note that for connected postings (i.e., the component IDs are
       * equal) this minimum posting cost is the same....
       */
      
      long d = o1.getMinCompPostCost() - o2.getMinCompPostCost();
      if (d != 0)
        return d < 0 ? -1 : 1;
      /*
       * ... to make connected postings all go together, among postings
       * with the same minimum posting cost, we sort by component id.
       */
      if (o1.getComponentId() != o2.getComponentId())
        return o1.getComponentId() - o2.getComponentId();
        
      // 4. Finally simply compare the cost (for connected nodes). 
      d = o1.getPostCost() - o2.getPostCost();
      return d == 0 ? 0 : (d < 0 ? -1 : 1);    
    }  
    
  }
  
}

