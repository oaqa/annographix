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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.util.TokenizerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import edu.cmu.lti.oaqa.annographix.util.CompressUtils;
import edu.cmu.lti.oaqa.annographix.util.UtilConst;
import edu.cmu.lti.oaqa.annographix.util.XmlHelper;

/**
 * This class reads documents and their respective annotations
 * from files previously created by an annotation pipeline.
 * 
 * @author Leonid Boytsov
 *
 */
public class DocumentReader {
  /**
   * @param docTextFile   file with documents, 
   *                       one document in Indri format, 
   *                       inside <DOC>...</DOC>.  
   * @param docAnnotFile  file with annotations in Indri format.
   * @param batchQty      a batch size.
   * @param obj           an indexing object.
   * @throws Exception 
   */
  public void readDoc(
                 String tokClassName,
                 Map<String,String> tokClassAttrs,
                 String docTextFile, 
                 String docAnnotFile, 
                 int batchQty, 
                 DocumentIndexer obj) 
                     throws Exception {
    BufferedReader  inpText = new BufferedReader(
        new InputStreamReader(CompressUtils.createInputStream(docTextFile)));
    BufferedReader  inpAnnot = new BufferedReader(
        new InputStreamReader(CompressUtils.createInputStream(docAnnotFile)));
    
    AnnotationEntry prevEntry = null;
    
    /*
     * 
     * Here we try to re-write a tokenizer name to
     * a short form, which is used internally by Solr.
     * Sadly, @Leo couldn't find a standard an API
     * function to do so. Anyways, when this rewriting code
     * fails, one can specify the fully-qualified class name
     * in the SOLR schema file. 
     *
     * For instance, instead of 
      <tokenizer class="solr.StandardTokenizerFactory" maxTokenLength="255"/>
     * one can specify:
      <tokenizer 
          class="org.apache.lucene.analysis.standard.StandardTokenizerFactory" 
          maxTokenLength="255"
      />
     *
     *
     */  
    
    TokenizerFactory tokStreamFactory = null;

    String prefix = "solr.";
    String suffix = "TokenizerFactory";

    boolean bIsWhiteSpaceTokenizer = tokClassName.contains("WhitespaceTokenizerFactory");    
    
    if (tokClassName.startsWith(prefix) && 
        tokClassName.endsWith(suffix)) {
    /*
     * If it is a standard SOLR name, let's try to rewrite the class
     * using the rewriting convention.
     */ 
        
      tokClassName = tokClassName.substring(prefix.length());
      tokClassName = tokClassName.substring(0, tokClassName.length() - suffix.length());
      tokClassName = tokClassName.toLowerCase();
      
      tokStreamFactory = (TokenizerFactory)TokenizerFactory.forName(tokClassName, tokClassAttrs);
    } else {
    // Load by full class name
      tokStreamFactory = (TokenizerFactory)Class.forName(tokClassName)
              .asSubclass(TokenizerFactory.class).getConstructor(Map.class)
              .newInstance(tokClassAttrs);
    }
      
    

    if (tokStreamFactory == null)
      throw new Exception("Cannot load class: " + tokClassName);

    Tokenizer tokStream = null; 
   
    String docText = XmlHelper.readNextXMLIndexEntry(inpText);
    
    XmlHelper xmlHlp = new XmlHelper();
    
    for (int docNum = 1; 
         docText!= null; 
         ++docNum, docText = XmlHelper.readNextXMLIndexEntry(inpText)) {
      
      // 1. Read the document text
      Map<String, String> docFields = null;
            
      try {
        docFields = xmlHlp.parseXMLIndexEntry(docText);
      } catch (SAXException e) {
        System.err.println("Parsing error, offending DOC:\n" + docText);
        throw new Exception("Parsing error.");
      }

      /*
       * Note the final here: we shouldn't modify a document string here, 
       * such changes may screw annotation offsets!
       */
      String docText4Anot = docFields.get(UtilConst.TEXT4ANNOT_FIELD); 
          
      if (docText4Anot == null) {
        System.err.println("Parsing error, offending DOC:\n" + docText);
        throw new Exception("Can't find the field: '" + docText4Anot + "'");
      }
      
      String docno = docFields.get(UtilConst.INDEX_DOCNO);
      
      if (docno == null) {
        System.err.println("Parsing error, offending DOC:\n" + docText);
        throw new Exception("Can't find the field: '" + 
                            UtilConst.INDEX_DOCNO + "'");
      }

      
      // 2. Read document annotations
      ArrayList<AnnotationEntry> annots = new ArrayList<AnnotationEntry>();
      
      
      while (prevEntry == null || prevEntry.mDocNo.equals(docno)) {
        String annotLine = null;
        if (prevEntry == null) {
          annotLine = inpAnnot.readLine();
          
          if (null == annotLine) break;
          
          try {
            prevEntry = AnnotationEntry.parseLine(annotLine);
          } catch (NumberFormatException | EntryFormatException e) {
            throw new Exception("Failed to parse annotation line: '" 
                                  + annotLine + "'");
          }                  
        }
        
        if (prevEntry.mDocNo.equals(docno)) {
          annots.add(prevEntry);
        } 
        
        /*
         *  Don't clear prevEntry in this case:
         *  we will need it in the following documents.
         */
        if (!prevEntry.mDocNo.equals(docno)) {
          break;
        }
        /*
         *  However, we need to discard the annotation
         *  entry that represents an already processed
         *  or skipped document. 
         *  
         */
        prevEntry = null;
      }
      
      /*
       *  3. pass a parsed document + annotation to the indexer
       *  NOTE: reusing tokStream, originally tokStream is NULL,
       *        consume document will create it during the first call.
       */
      
      tokStream = obj.consumeDocument(bIsWhiteSpaceTokenizer, 
                          tokStreamFactory, tokStream, 
                          docFields, annots);
      
      if (docNum % batchQty == 0) obj.sendBatch();
    }
    
    obj.sendBatch();
    inpAnnot.close();
    inpText.close();
  }

}
