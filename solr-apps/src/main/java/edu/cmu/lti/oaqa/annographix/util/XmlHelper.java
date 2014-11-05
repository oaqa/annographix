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
package edu.cmu.lti.oaqa.annographix.util;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import edu.cmu.lti.oaqa.annographix.solr.UtilConst;

public class XmlHelper {
  public static final String AQUAINT_TEXT = "TEXT";
  public static final String AQUAINT_BODY = "BODY";
  private static final String AQUAINT_TEXT_OPEN_TAG = "<" + AQUAINT_TEXT + ">";
  private static final String AQUAINT_TEXT_CLOSE_TAG =   "</" + AQUAINT_TEXT + ">";
  private static final String AQUAINT_TEXT_SELFCLOSE_TAG = "<" + AQUAINT_TEXT + "/>";
  
  /*
   * Functions getNode and getNodeValue are 
   * based on the code from Dr. Dobbs Journal:
   * http://www.drdobbs.com/jvm/easy-dom-parsing-in-java/231002580
   */
  
  // This function ignores the case
  public static Node getNode(String tagName, NodeList nodes) {
    for ( int x = 0; x < nodes.getLength(); x++ ) {
        Node node = nodes.item(x);
        if (node.getNodeName().equalsIgnoreCase(tagName)) {
            return node;
        }
    }
 
    return null;
  }
 
  /*
   * Works correctly only for text nodes.
   */
  public static String getNodeValue( Node node ) {
    NodeList childNodes = node.getChildNodes();
    for (int x = 0; x < childNodes.getLength(); x++ ) {
        Node data = childNodes.item(x);
        if ( data.getNodeType() == Node.TEXT_NODE )
            return data.getTextContent();
    }
    return "";
  }

  private DocumentBuilderFactory mDocBuildFact;
  
  public XmlHelper() {
    mDocBuildFact = DocumentBuilderFactory.newInstance();
    mDocBuildFact.setNamespaceAware(false); // no namespaces in our XMLs
    mDocBuildFact.setValidating(false);
  }
  
  public String genXMLIndexEntry(Map <String,String> fields) 
      throws ParserConfigurationException, 
            TransformerException {
    DocumentBuilderFactory docFactory = mDocBuildFact.newInstance();
    DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
   
    Document doc = docBuilder.newDocument();
    doc.setXmlVersion("1.1");
    Element rootElement = doc.createElement(UtilConst.INDEX_DOC_ENTRY);
    doc.appendChild(rootElement);
   
    for (String key: fields.keySet()) {
      Element elemText = doc.createElement(key);
      elemText.appendChild(doc.createTextNode(fields.get(key)));
      rootElement.appendChild(elemText);
    }
    
    DOMSource source = new DOMSource(doc);
    
    
    TransformerFactory transformerFactory = TransformerFactory.newInstance();
    Transformer transformer = transformerFactory.newTransformer();
    
    StringWriter stringBuffer = new StringWriter();
    StreamResult result = new StreamResult(stringBuffer);    
    transformer.transform(source, result);
    String docStr = stringBuffer.toString();
    // Let's remove <?xml version="1.1" encoding="UTF-8" standalone="no"?>
    return removeHeader(docStr);
  }
  
  public String removeHeader(String docStr) {
    // Let's remove <?xml version="1.1" encoding="UTF-8" standalone="no"?>
    int indx = docStr.indexOf('>');
    // If indx == -1, indx + 1 == 0 => we output the complete string
    String docNoXmlHead = docStr.substring(indx + 1);
    return docNoXmlHead;    
  }
  
  /**
   *  Parse a standard two-level XML entry in a semi-Indri format.
   * 
   */
  public Map<String, String> parseXMLIndexEntry(String text) throws Exception {
    HashMap<String, String> res = new HashMap<String,String>();
 
    
    Document doc = parseDocument(text);
    
    Node root = XmlHelper.getNode(UtilConst.INDEX_DOC_ENTRY, doc.getChildNodes());
    if (root == null) {
      System.err.println("Parsing error, offending document:\n" + text);
      throw new Exception("No " + UtilConst.INDEX_DOC_ENTRY);
    }  

    NodeList nodes = root.getChildNodes();
    for ( int x = 0; x < nodes.getLength(); x++ ) {
      Node node = nodes.item(x);
      
      if (node.getNodeType() == Node.ELEMENT_NODE) {
          res.put(node.getNodeName(), getNodeValue(node));
      }
    }    
    
    return res;
  }
  
  /**
   *  Parse a more complex (3-level) entry in the AQUAINT format.
   *  We extract all 2-d level fields, plus only one (TEXT) 3-d level field.
   *  
   */
  public Map<String, String> parseXMLAQUAINTEntry(String docText) throws Exception {
    HashMap<String, String> res = new HashMap<String,String>();
     
    TransformerFactory transformerFactory = TransformerFactory.newInstance();
    Transformer transformer = transformerFactory.newTransformer();  
    Document doc = parseDocument(docText);
    
    Node root = XmlHelper.getNode(UtilConst.INDEX_DOC_ENTRY, doc.getChildNodes());
    if (root == null) {
      System.err.println("Parsing error, offending document:\n" + docText);
      throw new Exception("No " + UtilConst.INDEX_DOC_ENTRY);
    }  

    NodeList nodes = root.getChildNodes();
    for ( int x = 0; x < nodes.getLength(); x++ ) {
      Node node = nodes.item(x);
      
      if (node.getNodeType() == Node.ELEMENT_NODE) {
         String nodeName = node.getNodeName();
         if (!nodeName.equals(AQUAINT_BODY)) {
           res.put(nodeName, getNodeValue(node));
         } else {
           String textField = "";
           NodeList bodyNodes = node.getChildNodes();
           for ( int y = 0; y < bodyNodes.getLength(); ++ y) {
             Node bodyNode = bodyNodes.item(y);
             if (bodyNode.getNodeType() == Node.ELEMENT_NODE &&
                 bodyNode.getNodeName().equals(AQUAINT_TEXT)) {
               StringWriter stringBuffer = new StringWriter();
               StreamResult result = new StreamResult(stringBuffer);    
               transformer.transform(new DOMSource(bodyNode), result);
               
               /*
                * After getting XML, we need to remove the header AND 
                * the enclosing text tags.
                */
               textField = removeHeader(stringBuffer.toString()).trim();

               if (textField.startsWith(AQUAINT_TEXT_OPEN_TAG))
                 textField = textField.substring(AQUAINT_TEXT_OPEN_TAG.length());
               if (textField.endsWith(AQUAINT_TEXT_CLOSE_TAG))
                 textField = textField.substring(0, textField.length() - AQUAINT_TEXT_CLOSE_TAG.length());
               if (textField.equals(AQUAINT_TEXT_SELFCLOSE_TAG))
                 textField = "";
             }             
           }
           res.put(AQUAINT_TEXT, textField);
         }
      }
    }    
    
    return res;
  }
  
  
  
  private static final 
                String CLOSING_TAG = "</"  + UtilConst.INDEX_DOC_ENTRY + ">";
  
  public static String readNextXMLIndexEntry(BufferedReader inpText) throws IOException {
    String docLine = inpText.readLine();
    
    if (docLine == null) return null;
    
    StringBuilder docBuffer = new StringBuilder();
        
    boolean foundEnd = false;
    
    do {
      docBuffer.append(docLine); docBuffer.append('\n');
      if (docLine.trim().endsWith(CLOSING_TAG)) {
        foundEnd = true;
        break;        
      }
      docLine = inpText.readLine();
    } while (docLine != null);
    
    return foundEnd ? docBuffer.toString() : null;
  }
  
  public Document parseDocument(String docLine) 
      throws ParserConfigurationException, SAXException, IOException {
    DocumentBuilder dbld = mDocBuildFact.newDocumentBuilder();
    // Should be 1.1, otherwise some symbols won't be recognized as valid
    String xml = String.format(
        "<?xml version=\"%s\"  encoding=\"%s\" ?>%s",
        UtilConst.XML_VERSION, UtilConst.ENCODING_NAME, docLine);
    return dbld.parse(new ByteArrayInputStream(xml.getBytes(UtilConst.ENCODING_NAME)));
  }
 
}
