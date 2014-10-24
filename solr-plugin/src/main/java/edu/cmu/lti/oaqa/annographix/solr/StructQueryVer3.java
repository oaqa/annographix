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
 */
package edu.cmu.lti.oaqa.annographix.solr;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;

import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.util.Bits;
import org.apache.solr.search.SyntaxError;

import edu.cmu.lti.oaqa.annographix.solr.StructQueryParse.FieldType;
import edu.cmu.lti.oaqa.annographix.util.UtilConst;

/**
  *  A rather advanced structured query: for tokens and annotations
  *  in a window of a given size, we can specify a set of constraints.
  *  
  *  @author Leonid Boytsov
  *
  */

class StructQueryVer3 extends Query {
  /** query text */
  private String    mQueryText;
  /** a name of the text field that is annotated */
  private String    mTextFieldName;
  /** a name of the field that stores annotations for the text field mTextFieldName */
  private String    mAnnotFieldName;
  /** A size (in the # of chars) of the window where we look for occurrences. */
  private int       mSpan;
  /** Lucene term objects */
  private ArrayList<Term>   mTerms = new ArrayList<Term>();
  
  /** a parsed query */
  StructQueryParse mQueryParse;
  
  /**
   * Constructor.
   * 
   * @param text            A text of query.
   * @param span            A size (in the # of chars) of the window where we
   *                        look for occurrences.
   * @param textFieldName   A name of the text field that is annotated.
   * @param annotFieldName  A name of the field that stores annotations for the text field mTextFieldName
   * @throws SyntaxError
   */
  public StructQueryVer3(String text, 
                         int span, 
                         String textFieldName, 
                         String annotFieldName)
                         throws SyntaxError
  {
    mQueryText = text;
    mTextFieldName = textFieldName;
    mAnnotFieldName = annotFieldName;
    
    mQueryParse = new StructQueryParse(mQueryText);
    
    final ArrayList<String>     tokens = mQueryParse.getTokens();
    final ArrayList<FieldType>  types  = mQueryParse.getTypes();
    
    for (int i = 0; i < tokens.size(); ++i) {
      mTerms.add(new Term(
                          types.get(i) == FieldType.FIELD_TEXT ? 
                          mTextFieldName : mAnnotFieldName,
                          tokens.get(i)
                         )
                );
    }
  }
  
  @Override
  /** 
   * Prints query text, which does not include all query parameters. 
   * 
   */
  public String toString(String field) {
    return mQueryText;
  }  
  
  /**
   * @see org.apache.lucene.search.Query#extractTerms(Set)
   */
  @Override
  public void extractTerms(Set<Term> queryTerms) {
    for (Term t: mTerms) queryTerms.add(t);
  }
  
  @Override
  public Weight createWeight(IndexSearcher searcher) throws IOException {
    return new StructQueryVer3Weight(searcher);
  }   
  
  /**
   *  A structured query. 
   *  Modeled partly after {@link org.apache.lucene.search.PhraseQuery}.
   */  
  private class StructQueryVer3Weight extends Weight {
    /** Represents a similarity function defined in SOLR config */
    private final Similarity                            mSimilarity;
    /** A similarity weight for the text-field tokens */
    private final Similarity.SimWeight                  mWeightTextField;
    /** A similarity weight for the annotation-field tokens */
    private final Similarity.SimWeight                  mWeightAnnotField;
    private transient ArrayList<TermContext>            mTermContexts =
                                                new ArrayList<TermContext>();
    
    public StructQueryVer3Weight(IndexSearcher searcher) throws IOException {
      mSimilarity = searcher.getSimilarity();
      final IndexReaderContext readerContext = searcher.getTopReaderContext();
      
      ArrayList<TermStatistics> termStatsTextFieldLst = new ArrayList<TermStatistics>();
      ArrayList<TermStatistics> termStatsAnnotFieldLst = new ArrayList<TermStatistics>();
      
      // mQueryParse comes from the enclosing class
      final ArrayList<String>     tokens = mQueryParse.getTokens();
      final ArrayList<FieldType>  types  = mQueryParse.getTypes();
      
      for (int i = 0; i < tokens.size(); ++i) {
        // mTerms comes from the enclosing class
        final Term term = mTerms.get(i);
        TermContext ctx = TermContext.build(readerContext, term);
        mTermContexts.add(ctx);
        TermStatistics stat = searcher.termStatistics(term, ctx);
        if (types.get(i) == FieldType.FIELD_TEXT)
          termStatsTextFieldLst.add(stat);
        else 
          termStatsAnnotFieldLst.add(stat);
          
      }
      // We need to compute two similarity weights for two different fields
      TermStatistics[] termStatsTextField = 
                              new TermStatistics[termStatsTextFieldLst.size()];
      TermStatistics[] termStatsAnnotField = 
          new TermStatistics[termStatsAnnotFieldLst.size()];
      
      termStatsTextFieldLst.toArray(termStatsTextField);
      termStatsAnnotFieldLst.toArray(termStatsAnnotField);
      
      mWeightTextField = mSimilarity.computeWeight(getBoost(),
                                searcher.collectionStatistics(mTextFieldName),
                                termStatsTextField);
      mWeightAnnotField = mSimilarity.computeWeight(getBoost(),
                                searcher.collectionStatistics(mAnnotFieldName),
                                termStatsAnnotField);            
    }
    @Override
    public String toString() { return "weight(" + StructQueryVer3.this + ")"; }
    
    @Override
    public Scorer scorer(AtomicReaderContext context, 
                          boolean scoreDocsInOrder,
                          boolean topScorer, 
                          Bits acceptDocs) throws IOException {
      return null;
    }
  }
}
