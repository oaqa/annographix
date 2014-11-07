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

import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.analysis.util.AbstractAnalysisFactory;

/**
 * A helper class that keeps info necessary to create a SOLR/Lucene tokenizer.
 * 
 * @author Leonid Boytsov
 *
 */
public class TokenizerParams {
  /** tokenizer class name, e.g., solr.WhitespaceTokenizerFactory */
  String                mTokClassName;
  /** tokenizer arguments */
  Map<String, String>   mTokClassAttr;

  
  TokenizerParams(String tokClassName) {
    mTokClassName = tokClassName;
    mTokClassAttr = new HashMap<String,String>();
    mTokClassAttr.put(AbstractAnalysisFactory.LUCENE_MATCH_VERSION_PARAM, UtilConst.LUCENE_VERSION);
  }
}