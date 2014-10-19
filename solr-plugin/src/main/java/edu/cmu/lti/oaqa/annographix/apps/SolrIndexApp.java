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
package edu.cmu.lti.oaqa.annographix.apps;

import java.io.StringReader;
import java.util.HashMap;

import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import edu.cmu.lti.oaqa.annographix.solr.SolrUtils;
import edu.cmu.lti.oaqa.annographix.util.UtilConst;
import edu.cmu.lti.oaqa.annographix.util.XmlHelper;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * An application that reads text files produced by an annotation
 * pipeline and indexes their content using Solr.
 * <p>
 * The first text file contains documents. The document
 * text is enclosed between tags <DOC></DOC> and occupies
 * exactly one line.
 * <p>
 * The second text file contains annotations in Indri format.
 * 
 * @author Leonid Boytsov
 *
 */
public class SolrIndexApp {
  
  static void Usage(String err) {
    System.err.println("Error: " + err);
    System.err.println("Usage: -i <Text File> -a <Annotation File> " +
                       "-u <Target Server URI> " + 
                       " [ -n <Bach Size> default " + batchQty + " ]");

    System.exit(1);
  }

  public static void main(String[] args) {
    Options options = new Options();
    
    options.addOption("t", null, true, "Text File");
    options.addOption("a", null, true, "Annotation File");
    options.addOption("u", null, true, "Solr URI");
    options.addOption("n", null, true, "Batch size");

    CommandLineParser parser = new org.apache.commons.cli.GnuParser(); 
    
    try {
      CommandLine cmd = parser.parse(options, args);
      
      if (cmd.hasOption("t")) {
        docTextFile = cmd.getOptionValue("t");
      } else {
        Usage("Specify Text File");
      }
      
      if (cmd.hasOption("a")) {
        docAnnotFile = cmd.getOptionValue("a");
      } else {
        Usage("Specify Annotation File");
      }
      
      if (cmd.hasOption("u")) {
        solrURI = cmd.getOptionValue("u");
      } else {
        Usage("Specify Solr URI");
      }
      
      if (cmd.hasOption("n")) {
        batchQty = Integer.parseInt(cmd.getOptionValue("n"));
      }
            
      parseConfig(solrURI);  
      
      System.out.println("Config is fine!");
      /*
      (new DocumentReader()).readDoc(solrConf.mTokClassName, 
                                    solrConf.mTokClassAttr,
                                    docTextFile, docAnnotFile, batchQty,
                                    new SolrDocumentIndexer(solrURI));
      */
    } catch (ParseException e) {
      Usage("Cannot parse arguments");
    } catch(Exception e) {
      System.err.println("Terminating due to an exception: " + e);
      System.exit(1);
    }    
    
  }
  /**
   * The function checks the following: (1) there are no omit* attributes ; 
   * (2) all attributes from the mandAttrs list are present <em>and are set to TRUE</em>.
   * 
   * @param fieldName        a name of the field.
   * @param fieldNode        an XML node representing the field.
   * @param mandAttrs        a list of mandatory attributes.
   * 
   * @throws                 Exception
   */
  private static void checkFieldAttrs(String fieldName, Node fieldNode, String ... mandAttrs) 
    throws Exception {
    HashMap<String, String> attNameVals = new HashMap<String, String>();
    
    NamedNodeMap attrs = fieldNode.getAttributes();
    
    if (null == attrs) {
      if (mandAttrs.length > 0) 
        throw new Exception("Field: " + fieldNode.getLocalName() + 
            " should have attributes");
      return;
    }
          
    for (int i = 0; i < attrs.getLength(); ++i) {
      Node e = attrs.item(i);
      String nm = e.getNodeName();
      attNameVals.put(nm, e.getNodeValue());
      if (nm.startsWith("omit")) {
        throw new Exception("Field: '" + fieldName + "' " +  
            " shouldn't have any omit* attributes");
      }
    }
    for (String s: mandAttrs) {
      if (!attNameVals.containsKey(s)) {
        throw new Exception("Field: " + fieldName  + "' " +  
            " should have an attribute '" + s  + "'");
      }
      String val = attNameVals.get(s);
      if (val.compareToIgnoreCase("true") != 0) {
        throw new Exception("Field: '" + fieldName + "' " +  
            "attribute '" + s + "' should have the value 'true'");                
      }
    }
  }

  /**
   * This functions performs sanity check of a SOLR instance configuration
   * file. We need to ensure that 1) the field that stores annotations 
   * uses the whitespace tokenizer; 2) the annotated text field stores both
   * offsets and positions. 
   * <p>Ideally, one should be able to parse the config using standard
   * SOLR class: org.apache.solr.schema.IndexSchema.
   * However, this seems to be impossible, because the schema loader tries
   * to read/parse all the files (e.g., a stopword file) mentioned in the schema 
   * definition. These files are not accessible, thogh, because they clearly
   * "sit" on a SOLR server file system. 
   * 
   * 
   * @param solrURI     the URI of the SOLR instance.
   * @throws Exception
   */
  private static void parseConfig(String solrURI) throws Exception {    
    String respText = SolrUtils.getSolrSchema(solrURI);
        
    try {
      DocumentBuilder dbld = DocumentBuilderFactory.newInstance().newDocumentBuilder();
      Document doc = dbld.parse(new InputSource(new StringReader(respText)));
      
      Node root = XmlHelper.getNode("schema", doc.getChildNodes());
      Node fields = XmlHelper.getNode("fields", root.getChildNodes());
      Node types  = XmlHelper.getNode("types", root.getChildNodes());
      
      // Let's read which tokenizers are specified for declared field types  
      HashMap<String, String> typeTokenizers = new HashMap<String, String>();
      for (int i = 0; i < types.getChildNodes().getLength(); ++i) {
        Node oneType = types.getChildNodes().item(i);
        
        if (oneType.getAttributes() == null) continue;
        if (!oneType.getNodeName().equalsIgnoreCase("fieldType")) continue;
        Node nameAttr = oneType.getAttributes().getNamedItem("name");
        if (nameAttr != null) {
          String name = nameAttr.getNodeValue();
          
          Node tmp = XmlHelper.getNode("analyzer", oneType.getChildNodes());
          if (tmp != null) {
            tmp = XmlHelper.getNode("tokenizer", tmp.getChildNodes());
            if (tmp != null) {
              NamedNodeMap attrs = tmp.getAttributes();
              if (null == attrs)
                throw new Exception("No attributes found for the tokenizer description, " + 
                                    " type: " + name);  
              Node tokAttr = attrs.getNamedItem("class");
              if (tokAttr != null)
                typeTokenizers.put(name.toLowerCase(),  tokAttr.getNodeValue());
              else throw new Exception("No class specified for the tokenizer description, " + 
                  " type: " + name);
            }
          }       
        }
      }

      // Read a list of fields, check if they are configured properly
      boolean annotFieldPresent = false;
      boolean text4AnnotFieldPresent = false;
      
      for (int i = 0; i < fields.getChildNodes().getLength(); ++i) {
        Node oneField = fields.getChildNodes().item(i);
        
        if (oneField.getAttributes()==null) continue;
        Node nameAttr = oneField.getAttributes().getNamedItem("name");
        if (nameAttr == null) {
          continue;
        }
        
        String fieldName = nameAttr.getNodeValue();

        // This filed must be present, use the whitespace tokenizer, and index positions      
        if (fieldName.equalsIgnoreCase(UtilConst.ANNOT_FIELD)) {
          checkFieldAttrs(fieldName, oneField, "termPositions");
          annotFieldPresent = true;
          Node typeAttr = oneField.getAttributes().getNamedItem("type");
          
          if (typeAttr != null) {
            String val = typeAttr.getNodeValue();
            String className = typeTokenizers.get(val.toLowerCase());
            if (className == null) {
              throw new Exception("Cannot find the tokenizer class for the field: " 
                                   + fieldName);
            }

            if (!className.equals(UtilConst.ANNOT_FIELD_TOKENIZER)) {
              throw new Exception("The field: '" + UtilConst.ANNOT_FIELD + "' " +
                                  " should be configured to use the tokenizer: " +
                                  UtilConst.ANNOT_FIELD_TOKENIZER);
            }
          } else {
            throw new Exception("Missing type for the annotation field: " 
                                + fieldName);
          }
        } else if (fieldName.equalsIgnoreCase(UtilConst.TEXT4ANNOT_FIELD)) {
        // This field must be present, and index positions as well as offsets
          text4AnnotFieldPresent = true;
          checkFieldAttrs(fieldName, oneField, "termPositions", "termOffsets");
        }
      }
      if (!annotFieldPresent) {
        throw new Exception("Missing field: " + UtilConst.ANNOT_FIELD);
      }
      if (!text4AnnotFieldPresent) {
        throw new Exception("Missing field: " + UtilConst.TEXT4ANNOT_FIELD);
      }           
    } catch (SAXException e) {      
      System.err.println("Can't parse SOLR response:\n" + respText);
      throw e;
    }
  }


  static String docTextFile = null, docAnnotFile = null, solrURI = null;
  static int    batchQty = 100;
}
