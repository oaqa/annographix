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

import org.apache.lucene.search.Query;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.search.QParser;
import org.apache.solr.search.QParserPlugin;
import org.apache.solr.search.SyntaxError;

/**
 * A plugin to create a custom structured-retrieval parser.
 * 
 * @author Leonid Boytsov
 * 
 */
public class ParserPlugin  extends QParserPlugin {
  @Override
  public QParser createParser(String qstr, 
                             SolrParams localParams,
                             SolrParams params, 
                             SolrQueryRequest req) {
    return new StructRetrQParser(qstr, localParams, params, req);
  }

  @Override
  public void init(@SuppressWarnings("rawtypes") NamedList args) {
    // we are not using arguments in this plugin.    
  }
}


class StructRetrQParser extends QParser {
  float     mBoost = 1.0f;
  Integer   mVersion = 3;
  Integer   mSpan = Integer.MAX_VALUE;
  
  public final static String PARAM_BOOST    = "boost";
  public final static String PARAM_VERSION  = "ver";
  public final static String PARAM_SPAN     = "span";
  
  public StructRetrQParser(String qstr, 
                            SolrParams localParams, 
                            SolrParams params,
      SolrQueryRequest req) {
    super(qstr, localParams, params, req);
    
    if (localParams.getFloat(PARAM_BOOST) != null)
      mBoost    = localParams.getFloat(PARAM_BOOST);
    
    if (localParams.getInt(PARAM_VERSION) != null)
      mVersion  = localParams.getInt(PARAM_VERSION);
    
    if (localParams.getInt(PARAM_SPAN) != null) 
      mSpan = localParams.getInt(PARAM_SPAN);
  }
  


  @Override
  public Query parse() throws SyntaxError {
    
    /**
     *  <p>Note lowercasing here. Also beware of lowercasing 
     *  in the indexing app {@see SolrDocumentIndexer.consumeDocument}.
     *  </p>
     */    
    String text = qstr.trim().toLowerCase();
    
    // TODO: in the future we may support more parsers
    if (mVersion.equals(3)) {
      throw new SyntaxError("Only version 3 is currently supported.");
    }
    
    Query  subQuery = parseVer3(text);
    subQuery.setBoost(mBoost);
    return subQuery;
  }
  
  private Query parseVer3(String text) throws SyntaxError {       
    StructQueryParser qParsed = new StructQueryParser(text);
    
    return null;
    //return new MixedTwoLevelQuery(mSpan, tokens, tokenRels, nexusIds, text);
  }  
}
