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

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * 
 * Let's check if we can generate most common/classic tokenizers.
 * 
 * @author Leonid Boytsov
 *
 */
public class TestTokenizerWrapper {
  private static final String TOKENIZER_TEXT = 
      "This tokenizer splits the text field into tokens," + 
      " treating whitespace and punctuation as delimiters. " + 
      "Delimiter characters are discarded.";

  @Test
  public void testStandardTokenizerFactory() {
    try {
      TokenizerParams   params = 
          new TokenizerParams("solr.StandardTokenizerFactory");
      params.addArgument("maxTokenLength", "128");
      SolrTokenizerWrapper  o = new SolrTokenizerWrapper(params);
      assertTrue(o.tokenize(TOKENIZER_TEXT, 0).size() > 0);
    } catch (Exception e) {
      e.printStackTrace();
      fail("Exception occurred.");         
    }    
  }
  
  @Test
  public void testClassicTokenizerFactory() {
    try {
      TokenizerParams   params = 
          new TokenizerParams("solr.ClassicTokenizerFactory");
      // no args here
      SolrTokenizerWrapper  o = new SolrTokenizerWrapper(params);
      assertTrue(o.tokenize(TOKENIZER_TEXT, 0).size() > 0);
    } catch (Exception e) {
      e.printStackTrace();
      fail("Exception occurred.");         
    }    
  }
  
  @Test
  public void testKeywordTokenizerFactory() {
    try {
      TokenizerParams   params = 
          new TokenizerParams("solr.KeywordTokenizerFactory");
      // no args here
      SolrTokenizerWrapper  o = new SolrTokenizerWrapper(params);
      assertTrue(o.tokenize(TOKENIZER_TEXT, 0).size() > 0);
    } catch (Exception e) {
      e.printStackTrace();
      fail("Exception occurred.");
    }   
  }
    
  @Test
  public void testLetterTokenizerFactory() {
    try {
      TokenizerParams   params = 
          new TokenizerParams("solr.LetterTokenizerFactory");
      // no args here
      SolrTokenizerWrapper  o = new SolrTokenizerWrapper(params);
      assertTrue(o.tokenize(TOKENIZER_TEXT, 0).size() > 0);
    } catch (Exception e) {
      e.printStackTrace();
      fail("Exception occurred.");         
    }      
  }    

  @Test
  public void testLowerCaseTokenizerFactory() {
    try {
      TokenizerParams   params = 
          new TokenizerParams("solr.LowerCaseTokenizerFactory");
      // no args here
      SolrTokenizerWrapper  o = new SolrTokenizerWrapper(params);
      assertTrue(o.tokenize(TOKENIZER_TEXT, 0).size() > 0);
    } catch (Exception e) {
      e.printStackTrace();
      fail("Exception occurred.");         
    }      
  }
  
  @Test
  public void testNGramTokenizerFactory() {
    try {
      TokenizerParams   params = 
          new TokenizerParams("solr.NGramTokenizerFactory");
      params.addArgument("minGramSize", "2");
      params.addArgument("maxGramSize", "3");
      SolrTokenizerWrapper  o = new SolrTokenizerWrapper(params);
      assertTrue(o.tokenize(TOKENIZER_TEXT, 0).size() > 0);
    } catch (Exception e) {
      e.printStackTrace();
      fail("Exception occurred.");         
    }      
  }  
}
