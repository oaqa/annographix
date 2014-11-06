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

import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.search.QParser;
import org.apache.solr.search.QParserPlugin;

/**
 * A plugin to create a custom structured-retrieval parser.
 * 
 * @author Leonid Boytsov
 * 
 */
public class ParserPluginVer3  extends QParserPlugin {
  @Override
  public QParser createParser(String qstr, 
                             SolrParams localParams,
                             SolrParams params, 
                             SolrQueryRequest req) {
    return new StructRetrQParserVer3(qstr, localParams, params, req);
  }

  @Override
  public void init(@SuppressWarnings("rawtypes") NamedList args) {
    // we are not using arguments in this plugin.    
  }
}
