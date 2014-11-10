package edu.cmu.lti.oaqa.annographix.solr;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;

import org.apache.lucene.search.Query;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.search.QParser;
import org.apache.solr.search.SyntaxError;

public class StructRetrQParserVer3 extends QParser {
  /** a query boost */
  float     mBoost = 1.0f;
  /** a current parser version */
  String    mVersion = "3";
  /** 
   * A size (in the # of chars) of the window where we look for occurrences.
   * For simplicity, we want to be the default value large, however, 
   * too large a value will lead to an overflow. 
   */
  Integer   mSpan = Integer.MAX_VALUE / 8;
  /**
   * The maximum number of brute-force iterations that we carry out
   * before giving up on constraint checking for the <b>current span</b>.
   */
  int mMaxSpanCheckConstrIter = UtilConst.DEFAULT_MAX_SPAN_CHECK_ITER; 
  
  
  /** a name of the text field that is annotated */
  String    mTextFieldName;
  /** a name of the field that stores annotations for the text field mTextFieldName */
  String    mAnnotFieldName;
  /** A label of a top-level covering annotation; equal to null, if there is none. */
  String    mCoverAnnotLabel;
  
  
  public final static String PARAM_BOOST    = "boost";
  public final static String PARAM_VERSION  = "ver";
  public final static String PARAM_SPAN     = "span";
  public final static String PARAM_COVER_ANNOT = "cover_annot";
  public final static String PARAM_TEXT_FIELD = UtilConst.CONFIG_TEXT4ANNOT_FIELD;
  public final static String PARAM_ANNOT_FIELD = UtilConst.CONFIG_ANNOTATION_FIELD;
  public final static String PARAM_MAX_SPAN_CONSTR_ITER = "max_iter";
  /** 
   * This array <b>must</b> contain all parameter names, when a new parameter
   * is introduced, its name must be added here. 
   */
  public final static String mValidParamNames[] = {PARAM_BOOST,
                                                   PARAM_VERSION,
                                                   PARAM_SPAN,
                                                   PARAM_COVER_ANNOT,
                                                   PARAM_TEXT_FIELD,
                                                   PARAM_ANNOT_FIELD,
                                                   PARAM_MAX_SPAN_CONSTR_ITER};
  public final static HashSet<String> mParamNameDict = new HashSet<String>
                                                    (Arrays.asList(mValidParamNames));
  Iterator<String> mParamNameIter;
  
  
  public StructRetrQParserVer3(String qstr, 
                            SolrParams localParams, 
                            SolrParams params,
      SolrQueryRequest req) {
    super(qstr, localParams, params, req);
    
    if (localParams.getFloat(PARAM_BOOST) != null)
      mBoost    = localParams.getFloat(PARAM_BOOST);
    
    if (localParams.get(PARAM_VERSION) != null)
      mVersion  = localParams.get(PARAM_VERSION).trim();
    
    mAnnotFieldName = localParams.get(PARAM_ANNOT_FIELD,
        UtilConst.DEFAULT_ANNOT_FIELD);
    
    mCoverAnnotLabel = localParams.get(PARAM_COVER_ANNOT);
    
    if (localParams.getInt(PARAM_SPAN) != null) {
      mSpan = localParams.getInt(PARAM_SPAN);
    }
    
    if (localParams.getInt(PARAM_MAX_SPAN_CONSTR_ITER) != null) {
      mMaxSpanCheckConstrIter = localParams.getInt(PARAM_MAX_SPAN_CONSTR_ITER);
    }
    
    mTextFieldName = localParams.get(PARAM_TEXT_FIELD, 
                                     UtilConst.DEFAULT_TEXT4ANNOT_FIELD);
    
    
    mParamNameIter = localParams.getParameterNamesIterator();
  }  

  @Override
  public Query parse() throws SyntaxError {
    /* Let's check that a user didn't specify the wrong parameter name */
    while (mParamNameIter.hasNext()) {
      String name = mParamNameIter.next();
      
      if (!mParamNameDict.contains(name) && 
          !name.equals("v") &&
          !name.equals("type")) {
        throw new SyntaxError("Invalid query parameter '" + name + "'");
      } 
    }
    
    /**
     *  <p>
     *  Note lowercasing here. Also beware of lowercasing 
     *  in the indexing app {@see SolrDocumentIndexer.consumeDocument}.
     *  </p>
     */    
    String text = qstr.trim().toLowerCase();
    
    // TODO: in the future we may support more parsers
    if (!mVersion.equalsIgnoreCase("3")) {
      throw new SyntaxError("Only version 3 is currently supported.");
    }
    
    Query  subQuery = parseVer3(text);
    subQuery.setBoost(mBoost);
    return subQuery;
  }
  
  private Query parseVer3(String text) throws SyntaxError {       
    return new StructQueryVer3(text, 
                               mSpan, mCoverAnnotLabel,
                               mTextFieldName, mAnnotFieldName,
                               mMaxSpanCheckConstrIter);
  }  
}