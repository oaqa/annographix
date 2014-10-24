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
import org.apache.lucene.search.similarities.Similarity.SimScorer;
import org.apache.lucene.util.Bits;
import org.apache.solr.search.SyntaxError;

import edu.cmu.lti.oaqa.annographix.solr.StructQueryParse.FieldType;

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
  private final StructQueryParse              mQueryParse;
  /** query tokens (annotation or plain text) */
  private final ArrayList<String>             mTokens;
  /** token types */
  private final ArrayList<FieldType>          mTokenTypes;
  
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
    
    mTokens = mQueryParse.getTokens();
    mTokenTypes = mQueryParse.getTypes();
    
    for (int i = 0; i < mTokens.size(); ++i) {
      mTerms.add(new Term(
                          mTokenTypes.get(i) == FieldType.FIELD_TEXT ? 
                          mTextFieldName : mAnnotFieldName,
                          mTokens.get(i)
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
    public String toString() { return "weight(" + getQuery() + ")"; }
    
    /*
     *  Leo believes that getValueForNormalization() & normalize() are not
     *  relevant (and will not be called) in this implementation, because
     *  there is only one way to create an instance of this weight class.
     *  Namely, via a constructor. The constructor, then, generate instances
     *  of two weight calsses based on the term and collection statistics.
     * 
     */
    @Override
    public float getValueForNormalization() {
      throw new RuntimeException(
          "Bug: getValueForNormalization() is not supposed to be called ");
    }

    @Override
    public void normalize(float queryNorm, float topLevelBoost) {
      throw new RuntimeException(
          "Bug: normalize() is not supposed to be called ");
    }    
    
    private boolean termNotInReader(AtomicReader reader, Term term) throws IOException {
      return reader.docFreq(term) == 0;
    }
    
    @Override
    public Scorer scorer(AtomicReaderContext context, 
                          boolean scoreDocsInOrder,
                          boolean topScorer, 
                          Bits acceptDocs) throws IOException {

      final AtomicReader        reader = context.reader();
      final Bits                liveDocs = acceptDocs;
      DocsAndPositionsEnum[]    postingsEnum = 
                                // mTerms is from the enclosing class
                                       new DocsAndPositionsEnum[mTerms.size()];
      
      // mTextFieldName & mAnnotFieldName come from the enclosing class 
      final Terms fieldTermsTextField = reader.terms(mTextFieldName);
      final Terms fieldTermsAnnotField = reader.terms(mAnnotFieldName);
      if (fieldTermsTextField == null) {
        throw new RuntimeException("Bug: fieldTermsTextField is null");
      }
      if (fieldTermsAnnotField == null) {
        throw new RuntimeException("Bug: fieldTermsAnnotField is null");
      }
      
      // Reuse TermsEnum below:
      final TermsEnum termTextFieldEnum = fieldTermsTextField.iterator(null);
      final TermsEnum termAnnotFieldEnum = fieldTermsTextField.iterator(null);
      
      for (int i = 0; i < mTerms.size(); ++i) {
        String termDesc = "'" + mTokens.get(i) + "' type: " + mTokenTypes.get(i);
        final Term t = mTerms.get(i);
        final TermState state = mTermContexts.get(i).get(context.ord);
        
        if (state == null) { // term doesn't exist in this segment 
          if (!termNotInReader(reader, t)) {
            throw new RuntimeException("Bug: no termstate found but term " + 
                                       termDesc + " exists in reader");
          }
          return null;
        }
        
        DocsAndPositionsEnum    post = null;
        
        if (mTokenTypes.get(i) == FieldType.FIELD_TEXT) {
          termTextFieldEnum.seekExact(t.bytes(), state);
          // Text field must include offsets
          post = termTextFieldEnum.docsAndPositions(liveDocs, null,
                                        DocsAndPositionsEnum.FLAG_OFFSETS |
                                        DocsAndPositionsEnum.FLAG_FREQS);
          if (null == post) {
            if (!termTextFieldEnum.seekExact(t.bytes())) {
              throw new RuntimeException("Bug: no termstate found but term " + 
                                          termDesc + " exists in reader");
            }
            // term does exist, but has no positions and/or offsets
            throw new IllegalStateException("field \"" + t.field() + 
                "\" was indexed without position and/or offset data; cannot run "
                // Query text comes from the enclosing class
                + " sub-query: '" + mQueryText + "'" 
                + " (term=" + t.text() + ")");
          }
        } else {
          // Annotation field must include payloads
          termAnnotFieldEnum.seekExact(t.bytes(), state);
          post = termAnnotFieldEnum.docsAndPositions(liveDocs, null, 
                                        DocsAndPositionsEnum.FLAG_PAYLOADS |
                                        DocsAndPositionsEnum.FLAG_FREQS);
          if (null == post) {
            if (!termAnnotFieldEnum.seekExact(t.bytes())) {
              throw new RuntimeException("Bug: no termstate found but term " + 
                                          termDesc + " exists in reader");
            }
            // term does exist, but has no positions and/or payloads
            throw new IllegalStateException("field \"" + t.field() + 
                "\" was indexed without position and/or payload data; cannot run "
                // Query text comes from the enclosing class
                + " sub-query: '" + mQueryText + "'" 
                + " (term=" + t.text() + ")");            
          }          
        }
        postingsEnum[i] = post;
      }
      
      **** CONTINUE HERE *****
    }
    
    @Override
    public StructQueryVer3 getQuery() { return StructQueryVer3.this; }
    
    @Override
    public Explanation explain(AtomicReaderContext context, int doc) throws IOException {
      Scorer scorer = scorer(context, true, false, context.reader().getLiveDocs());
      /* 
       * Leo TODO: This one needs to be tested.  
       */

      if (scorer != null) {
        int newDoc = scorer.advance(doc);
        if (newDoc == doc) {
          ComplexExplanation result = new ComplexExplanation();
          float freq = ((StructScorerVer3)scorer).freq();
          
          // 1. frequency explanation for the text field
          SimScorer docScorerTextField 
                            = mSimilarity.simScorer(mWeightTextField, context);
          result.setDescription("weight("+getQuery()+" in "+doc+") " + 
              "[" + mSimilarity.getClass().getSimpleName() + "], result of:");
          Explanation scoreExplanationTextField = 
              docScorerTextField.explain(doc, 
                                new Explanation(freq, "textFieldFreq=" + freq));          
          result.addDetail(scoreExplanationTextField);          
          
          // 2. frequency explanation for the annotation field
          SimScorer docScorerAnnotField 
                            = mSimilarity.simScorer(mWeightAnnotField, context);
          Explanation scoreExplanationAnnotField = 
              docScorerAnnotField.explain(doc, 
                                new Explanation(freq, "annotFieldFreq=" + freq));          
          result.addDetail(scoreExplanationAnnotField);          

          // 3. The value is a sum
          result.setValue(
                          scoreExplanationTextField.getValue() + 
                          scoreExplanationAnnotField.getValue()
                          );
          result.setMatch(true);
          return result;
        }
      }

      return new ComplexExplanation(false, 0.0f, "no matching term");
    }    
  }
}
