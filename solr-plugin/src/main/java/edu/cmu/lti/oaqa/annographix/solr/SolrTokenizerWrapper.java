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

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.io.IOException;
import java.io.StringReader;

import org.apache.lucene.analysis.tokenattributes.*;
import org.apache.lucene.analysis.util.TokenizerFactory;
import org.apache.lucene.analysis.*;

/**
 * 
 * A helper wrapper class for SOLR tokenizers. It helps instantiate
 * tokenizers by name and use them to tokenize strings. 
 * 
 * @author Leonid Boytsov
 *
 */
public class SolrTokenizerWrapper {
  /** 
   * Encapsulates tokenization results + a reusable token stream class.
   */
  public class TokRes {

    ArrayList<AnnotationProxy>    mTokens = new ArrayList<AnnotationProxy>();
  };
  
  /**
   * A tokenizer: shouldn't be shared among threads (i.e., each thread
   * should use its own copy).
   * 
   * <p>
   * The tokenizer can be specified using a short or full class name.
   * However, only the full class name can be used to directly initialize
   * the tokenizer. In the case of a short name, we need to use
   * a tokenizer ID corresponding to this short name. For example,
   * the ID for <b>solr.StandardTokenizerFactory</b> is <b>standard</b>. 
   * </p>
   * <p>
   * Sadly, Leo couldn't find a standard an API function to obtain this
   * short tokenizer ID from a name specified in a configuration file.
   * Thus, a custom code is used, which guesses this short ID based
   * on the short class name specified in the config. If this guessing 
   * code fails for any reason (e.g., it may stop working in 
   * future Lucene/SOLR versions), one can specify the full class name 
   * in the SOLR schema file. However, it seems to be working fine
   * with Standard SOLR tokenizer factories.
   * </p>
   * <p>
   * For instance, instead of <br>
   * &lt;tokenizer class="solr.StandardTokenizerFactory" maxTokenLength="255"/&gt;<br>
   * one can explicitly specify the full class name:<br>
   * &lt;tokenizer 
        class="org.apache.lucene.analysis.standard.StandardTokenizerFactory" 
        maxTokenLength="255" /&lt;<br>        
   * </p>
   * 
   * @param tokClassName   a short or full name of the tokenizer class.
   * @param tokClassArgs   key-value pairs specifying tokenizer arguments,
   *                       keys are argument names, values are argument values.
   *                        
   * @throws ClassNotFoundException 
   * @throws SecurityException 
   * @throws NoSuchMethodException 
   * @throws InvocationTargetException 
   * @throws IllegalArgumentException 
   * @throws IllegalAccessException 
   * @throws InstantiationException 
   */
  public SolrTokenizerWrapper(TokenizerParams params) 
                           throws InstantiationException, 
                                   IllegalAccessException, 
                                   IllegalArgumentException, 
                                   InvocationTargetException, 
                                   NoSuchMethodException, 
                                   SecurityException, 
                                   ClassNotFoundException {
    String tokClassName = params.getTokClassName();
    
    Map<String,String> tokClassArgs = params.getTokClassArgs();
    
    if (tokClassName.startsWith(SHORT_NAME_PREFIX) && 
        tokClassName.endsWith(SHORT_NAME_SUFFIX)) {
    /*
     * If it is a standard SOLR name, let's try to rewrite the class
     * using the rewriting convention.
     */ 
        
      tokClassName = tokClassName.substring(SHORT_NAME_PREFIX.length());
      tokClassName = tokClassName.substring(0, tokClassName.length() - 
                                            SHORT_NAME_SUFFIX.length());
      tokClassName = tokClassName.toLowerCase();
      
      mTokStreamFactory = 
          (TokenizerFactory)TokenizerFactory.forName(tokClassName, tokClassArgs);
    } else {
    // Load by full class name
      mTokStreamFactory = (TokenizerFactory)Class.forName(tokClassName)
              .asSubclass(TokenizerFactory.class).getConstructor(Map.class)
              .newInstance(tokClassArgs);
    }        
  }
  
  /**
   * Carry out tokenization.
   * 
   * @param text            input text.
   * @param startTokenId    an initial token ID to use.
   * 
   * @return    an array of annotation entries, which memorize tokens
   *            and their offsets.
   *            
   * @throws IOException 
   */
  public ArrayList<AnnotationProxy> tokenize(String text, 
                                             int startTokenId) throws IOException {
    ArrayList<AnnotationProxy>  res = 
        new ArrayList<AnnotationProxy>();
    
    StringReader tokInput = new StringReader(text);
    if (mTokStream == null) mTokStream = mTokStreamFactory.create(tokInput);
    else mTokStream.setReader(tokInput);
    
    PositionIncrementAttribute posIncrAttribute = 
        mTokStream.addAttribute(PositionIncrementAttribute.class);
    OffsetAttribute offsetAtt = mTokStream.addAttribute(OffsetAttribute.class);
    
    mTokStream.reset();
    
    while (mTokStream.incrementToken()) {
      final int posIncr = posIncrAttribute.getPositionIncrement();
      if (posIncr > 0) {
        int start = offsetAtt.startOffset();
        int end = offsetAtt.endOffset();
        res.add(new AnnotationProxy(startTokenId++,
                                    "", // empty label
                                    start,
                                    end,
                                    0,
                                    text.substring(start, end)
                                    ));
      }
    }
    
    mTokStream.close();
    
    return res;
  }
  
  TokenizerFactory              mTokStreamFactory;
  Tokenizer                     mTokStream = null;
  
  static final String SHORT_NAME_PREFIX = "solr.";
  static final String SHORT_NAME_SUFFIX = "TokenizerFactory";
  
  /**
   * A simple test function to check basic functionality manually.
   * 
   * @param args    args[0] is a tokenizer factory class; 
   *                args[1] is a sentence to tokenize;
   *                args[2] represents space-separated parameter pairs in the 
   *                        form key=value.
   * 
   * @throws Exception
   */
   public  static void main(String args[]) throws Exception {
     String     tokClassName = args[0];
     String     text         = args[1];
     String     params       = args[2];
     
     
     // Let's print all available tokenizers
     for (String e: TokenizerFactory.availableTokenizers()) {
       System.out.println("###: " + e);
     }
     
     Map<String,String> tokClassArgs  = new HashMap<String, String>();
     for (String part : params.split("\\s+")) 
     if (!part.isEmpty()){
       String tmp[] = part.split("=");
       tokClassArgs.put(tmp[0], tmp[1]);
       System.out.println(String.format("Adding a parameter: %s=%s", tmp[0], tmp[1]));
     }
     
     System.out.println("Creating a tokenizer: " + tokClassName);

     SolrTokenizerWrapper   tokenizer = 
         new SolrTokenizerWrapper (
             new TokenizerParams(tokClassName, tokClassArgs));
     
     for (AnnotationProxy e: tokenizer.tokenize(text, 100)) {
       e.debugPrint();
     }
   }
  }
