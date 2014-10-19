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
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.util.TokenizerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;

import edu.cmu.lti.oaqa.annographix.util.UtilConst;
import edu.cmu.lti.oaqa.annographix.nlp.StanfordLemmatizer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

public class SolrDocumentIndexer implements DocumentIndexer {
  private static final boolean DEBUG_PRINT = false;

  /**
   * 
   * This is a helper class that reads annotated documents and
   * submit indexing requests to a SOLR server.
   * 
   * @author Leonid Boytsov
   * 
   */
  public SolrDocumentIndexer(String solrURI) throws Exception {
    mDocFactory           = DocumentBuilderFactory.newInstance();
    mDocFactory.setValidating(false);
    mDocBuilder           = mDocFactory.newDocumentBuilder();
    mTransformerFactory   = TransformerFactory.newInstance();
    mTransformer          = mTransformerFactory.newTransformer();
    mTargetServer         = new SolrUtils(solrURI);
  }
  
  /**
   * This function assumes that tokens are separated by whitespaces.
   */
  @Override
  public Tokenizer consumeDocument(boolean bIsWhiteSpaceTokenizer,
                              TokenizerFactory tokStreamFactory,
                              Tokenizer tokStream,
                              Map<String, String> docFields,
                              ArrayList<AnnotationEntry> annots) 
                              throws Exception{
    String origDocText = docFields.get(UtilConst.TEXT4ANNOT_FIELD);
    String docNo       = docFields.get(UtilConst.INDEX_DOCNO);
    
    final String docText = UtilConst.
                  replaceWhiteSpaces(UtilConst.removeBadUnicode(origDocText)).    
                  replace("?", " ");
    /*
     * The previous transformation replaces some characters with spaces, 
     * but it shouldn't change the string length or positions or regular characters!
     * Let's make a simple sanity check proving that this is true:
     */
    if (docText.length() != origDocText.length())
      throw new Exception("Bug: Character replacement procedure changed the length of the string!");
    
    /*
     * Don't change the size of the string: 
     * This will screw annotation offsets.
     */
    if (docText.length() != origDocText.length()) {
      throw new Exception("Bug: the size of the document was changed during a transformation!");
    }
    
    
    if (null == mBatchXML) {
      mBatchXML  = mDocBuilder.newDocument();
      mAddNode   = mBatchXML.createElement("add");
      mBatchXML.appendChild(mAddNode);
    }
    
    DocumentFragment FragRoot = mBatchXML.createDocumentFragment();
    Element oneDoc = mBatchXML.createElement("doc");
    FragRoot.appendChild(oneDoc);
    
    addField(oneDoc, UtilConst.ID_FIELD, docNo);
    /*
     * It will be the Solr responsibility to stem the words
     * properly. Here, we will only stem keywords stored with
     * annotations.
     */
    addField(oneDoc, UtilConst.TEXT4ANNOT_FIELD, docText);
    
    for (Entry<String, String> e: docFields.entrySet()) {
      String key = e.getKey(), value = e.getValue();
      if (!key.equalsIgnoreCase(UtilConst.ID_FIELD) &&
          !key.equalsIgnoreCase(UtilConst.INDEX_DOCNO) &&
          !key.equalsIgnoreCase(UtilConst.TEXT4ANNOT_FIELD)) {
        addField(oneDoc, key, value);
      }
    }
    
       
    /*
     *  Let's obtain all starting positions.
     *  This is going to be a bit tricky, b/c we need
     *  to obtain the same set of positions as an actual
     *  SOLR tokenizer. This is why we instantiate an
     *  actual SOLR tokenizer specified in the configuration
     *  file (which we read over HTTP) and use it here.  
     */

    StringReader tokInput = new StringReader(docText);    
    if (tokStream == null) tokStream = tokStreamFactory.create(tokInput); 
    else tokStream.setReader(tokInput);
    
    TreeMap<Integer, Integer> charToWordPos = getWordBoundaries(tokStream);

    /*
     * TODO: comment it out some day
     */
    //sanityCheckForTokenization(bIsWhiteSpaceTokenizer, docNo, docText, charToWordPos);

    
    // Create annotation representation
    StringBuilder annotString = new StringBuilder();
    
    int errorFind = 0;
    
    for (AnnotationEntry e: annots) {
      // Replace potential occurrences of the payload char
      String annotLabel = UtilConst.removeBadUnicode(e.mLabel.
                                       replace(UtilConst.PAYLOAD_CHAR, ' '));
      
      String                        annotText = "";
      Map.Entry<Integer,Integer>    itEnd = null;
      Map.Entry<Integer,Integer>    itStart = null;
      
      /* 
       * Some special annotations, e.g., dependency tree root have zero
       * length. They don't correspond to any real span of text. 
       */
      if (e.mCharLen > 0) {
        // Remove chars that may stump payload parser
        annotText = UtilConst.preparePayloadToken(
            docText.substring(e.mStartChar, e.mStartChar + e.mCharLen));
        
      
        itStart = charToWordPos.lowerEntry(e.mStartChar+1);
  
        /*
         * Sometimes this happens due to annoator's error. Ideally,
         * we could avoid this by supplying our own tokens. Yet,
         * it is not always possible.
         * 
         * @TODO: in a better future pipeline, annotators better
         *        use the SOLR tokenizer.
         * 
         * In the case of Senna, e.g., the Senna's tokenizer weirdly
         * tokenizes: "'Monsewer' Eddie Gray ."
         * as 'Mo nsewer ' Eddie Gray .
         * 
         * Another example, when something is preceeded by ------ and
         * ------- is included into the SRL argument. However, it is 
         * not tokenized as a word by Solr. 
         * 
         * Then, the first annotation doesn't match any real token.
         * Here, let's just ignore such errors, unless  we have a lot of 
         * them in a document.
         * 
         */
        if (null == itStart) {
          String error = "WARNING: Cannot find a start-word position for  annotation, " + 
                               " docNo: " + docNo 
                               + " char pos start : " + e.mStartChar 
                               + " char length : " + e.mCharLen +
                               " annotId: " + e.mAnnotId;
          System.err.println(error);
          errorFind++;
          if (errorFind > UtilConst.MAX_ERROR_POS_FIND) {
            throw new Exception("Couldn't find start/end position more than " 
                                + UtilConst.MAX_ERROR_POS_FIND +
                                " times. Something may be wrong here.");
          }
          continue;
        }
        
        itEnd = 
            charToWordPos.lowerEntry(e.mStartChar+ e.mCharLen);
  
        if (null == itEnd) {
          String error = "Cannot find an end-word position for  annotation, " + 
                               " docNo: " + docNo  
                               + " char pos start : " + e.mStartChar 
                               + " char length : " + e.mCharLen + 
                               " annotId: " + e.mAnnotId;
          System.err.println(error);
          errorFind++;
          if (errorFind > UtilConst.MAX_ERROR_POS_FIND) {
            throw new Exception("Couldn't find start/end position more than " 
                                + UtilConst.MAX_ERROR_POS_FIND +
                                " something is probably wrong here.");
          }          
          continue;
        }
      }
      
      /*
       *  We need to index each word from the annotation separately as well
       *  as a SINGLE #any annotation.
       */
      createPayloadStr(annotString,
          e,
          itStart != null ? itStart.getValue() : 0,
          itEnd   != null ? itEnd.getValue() : 0,
          annotLabel,
          UtilConst.STRING_ANY
      );                
      
                     
      for (String token: 
                        UtilConst.removeBadUnicode(annotText)
                        // ignore repeating whitespaces
                        .split("\\s+")) {                         
        createPayloadStr(annotString,
            e,
            itStart != null ? itStart.getValue() : 0,
            itEnd   != null ? itEnd.getValue() : 0,
            annotLabel,
            StanfordLemmatizer.stemWord(token)
        );                
      }
    }
    
    // Let's hardwire lowercasing it, also do the same in a query plugin
    addField(oneDoc, UtilConst.ANNOT_FIELD, annotString.toString().toLowerCase()); 
    
    mAddNode.appendChild(FragRoot);
    
   
    
    tokStream.close();
    return tokStream;
  }
  
  private void sanityCheckForTokenization(boolean bIsWhiteSpaceTokenizer,
      String docNo,
      String docText, TreeMap<Integer, Integer> charToWordPos) throws Exception {
    // Some stupid old debugging code, but let's keep it for a while
    {
      int wordPos = -1;    
      char prevChar = ' ';    
      for (int charPos = 0; charPos < docText.length(); ++charPos) {
        char c = docText.charAt(charPos);

        boolean b = bIsWhiteSpaceTokenizer ? 
                                (c != ' '):
                                (Character.isAlphabetic(c) || Character.isDigit(c)); 
        if (b && ' ' == prevChar) {
          ++wordPos;
          /* Let's see if we can find this start pos */
          Integer othPos = charToWordPos.get(charPos);
          if (othPos == null)
          
            throw new Exception("DEBUG_TOKENIZER: " +
                " Cannot find a start-word position for a whitespace, " + 
                " docNo: " + docNo 
                + " char pos : " + charPos + "\nOffending doc:\n" + docText);

          if (bIsWhiteSpaceTokenizer) {
            if (othPos !=  wordPos) {
              throw new Exception("DEBUG_TOKENIZER: "  + 
                  "Word position mistamtch, " + 
                  " docNo: " + docNo 
                  + " char pos : " + charPos + 
                  "expected: " + wordPos + " got: " + othPos
                  + "\nOffending doc:\n" + docText);                  
            }
          } else {
            if (othPos <  wordPos) {
              throw new Exception("DEBUG_TOKENIZER: "  + 
                  "Word position mistamtch, " + 
                  " docNo: " + docNo 
                  + " char pos : " + charPos + 
                  "expected NOT LESS than  : " + wordPos + " got: " + othPos
                  + "\nOffending doc:\n" + docText);                  
            }            
          }
        }
        prevChar = c;
      }
    }
 
  }

  private TreeMap<Integer, Integer> getWordBoundaries(TokenStream tokStream) throws IOException {
    TreeMap<Integer, Integer> charToWordPos = new TreeMap<Integer, Integer>();

    PositionIncrementAttribute posIncrAttribute = tokStream.addAttribute(PositionIncrementAttribute.class);
    OffsetAttribute offsetAtt = tokStream.addAttribute(OffsetAttribute.class);
    
    tokStream.reset();    
    
    int wordNum = 0;
    
    while (tokStream.incrementToken()) {
      final int posIncr = posIncrAttribute.getPositionIncrement();
      if (posIncr > 0) {
        int off = offsetAtt.startOffset();
        charToWordPos.put(off, wordNum);
        wordNum++;
      }
    }
    
    return charToWordPos;    
  }

  /**
   * 
   * This function  creates a payload string in the format that can
   * be understood by the indexing application.
   * 
   * @param annotString   A buffer where to we append the result.
   * @param e             An annotation entry read from the Indri-style annotation file
   * @param itStart       A start position. 
   * @param itEnd         An end position.
   * @param annotLabel    An annotation label
   * @param token         A token to be appended (either a real keyword or UtilConst.STRING_ANY).  
   */
  private void createPayloadStr(
                             StringBuilder   annotString,
                             AnnotationEntry e,
                             int      iStart,
                             int      iEnd,
                             String   annotLabel,
                             String   token
                             ) {
    StringBuilder oneAnnot = new StringBuilder();    
    
    
    oneAnnot.append(annotLabel);
    oneAnnot.append(UtilConst.VALUE_SEPARATOR);      
    oneAnnot.append(token);
    oneAnnot.append(UtilConst.PAYLOAD_CHAR);
    
    oneAnnot.append(iStart);
    oneAnnot.append(UtilConst.PAYLOAD_ID_SEP_CHAR);
    oneAnnot.append(iEnd);
    oneAnnot.append(UtilConst.PAYLOAD_ID_SEP_CHAR);      
    oneAnnot.append(e.mAnnotId);
    oneAnnot.append(UtilConst.PAYLOAD_ID_SEP_CHAR);
    oneAnnot.append(e.mParentId);
    oneAnnot.append(' ');
    
    String str = oneAnnot.toString();
        
        
    /*
    *  TODO: @leo  if you go beyond this limit
    *              the keyword will be truncated by Solr
    *              and the payload parser will likely fail.
    */
    if (str.length() < UtilConst.MAX_WORD_LEN) {
      annotString.append(str);
    } else {
      System.err.println("WARNING, ignoring a long token: " + str);
    }  
  }
  
  private void addField(Element oneDoc, String fieldName, String fieldValue) {
    Element  XmlDocId = mBatchXML.createElement("field");
    XmlDocId.setAttribute("name", fieldName);
    XmlDocId.setTextContent(fieldValue);
    oneDoc.appendChild(XmlDocId);
  }
  
  String mSolrURI;

  @Override
  public void sendBatch() throws Exception {
    if (mBatchXML == null) return;
    
    System.out.println("Sending batch!");
    
    StringWriter OutStr = new StringWriter();
    StreamResult result = new StreamResult(OutStr);
  
    DOMSource source = new DOMSource(mBatchXML);
    mTransformer.transform(source, result);

    String xml = OutStr.getBuffer().toString();

    System.out.println("Submitting XML");

    if (DEBUG_PRINT) System.out.println(xml);
    
    mTargetServer.submitXML(xml);
    System.out.println("Issuing a commit");
    mTargetServer.indexCommit();
    System.out.println("Batch is submitted!");    
    
    mBatchXML = null;
    mAddNode = null;
  }
  
  private DocumentBuilderFactory  mDocFactory;
  private DocumentBuilder         mDocBuilder;
  private TransformerFactory      mTransformerFactory;
  private Transformer             mTransformer;
  
  private SolrUtils               mTargetServer;
  private Document                mBatchXML = null;
  private Element                 mAddNode = null;
}
