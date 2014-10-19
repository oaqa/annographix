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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.lucene.analysis.util.AbstractAnalysisFactory;

import edu.cmu.lti.oaqa.annographix.solr.DocumentReader;
import edu.cmu.lti.oaqa.annographix.solr.SolrDocumentIndexer;
import edu.cmu.lti.oaqa.annographix.util.UtilConst;
import edu.cmu.lti.oaqa.annographix.util.XmlHelper;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

class ParseConfRes {
  Map<String, String>   mTokClassAttr;
  String                mTokClassName;
  ParseConfRes() {
    mTokClassName = null;
    mTokClassAttr = new HashMap<String,String>();
    mTokClassAttr.put(AbstractAnalysisFactory.LUCENE_MATCH_VERSION_PARAM, UtilConst.LUCENE_VERSION);
  }
};


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
            
      ParseConfRes solrConf = parseConfig(solrURI);          
      
      (new DocumentReader()).readDoc(solrConf.mTokClassName, 
                                    solrConf.mTokClassAttr,
                                    docTextFile, docAnnotFile, batchQty,
                                    new SolrDocumentIndexer(solrURI));
      
    } catch (ParseException e) {
      Usage("Cannot parse arguments");
    } catch(Exception e) {
      System.err.println("Terminating due to an exception: " + e);
      System.exit(1);
    }    
    
  }

  
  private static ParseConfRes parseConfig(String solrURI) throws Exception {
    ParseConfRes res = new ParseConfRes();
    
    // Let's figure out the tokenizer name
    String getSchemaURI = solrURI 
        + "/admin/file?file=schema.xml&contentType=text/xml:charset=utf-8";
    
    URL obj = new URL(getSchemaURI);
    HttpURLConnection con = (HttpURLConnection) obj.openConnection();
    
    // optional default is GET
    con.setRequestMethod("GET");
 
    //add request header
    con.setRequestProperty("User-Agent", UtilConst.USER_AGENT);
    
    int responseCode = con.getResponseCode();
    if (responseCode != 200) {
      System.err.println("Cannot read schema definition, using URI: " + getSchemaURI);
    }
    
    BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
    String         respLine;
    StringBuffer   resp = new StringBuffer();
 
    while ((respLine = in.readLine()) != null) {
      resp.append(respLine);
    }
    
    in.close();
    
    String respText = resp.toString();
    
    /*
     *  TODO: could be re-written using org.apache.solr.schema
     *        So, far, however, this solr class threw some exceptions :-(
     *        It is possible that we were using it somehow incorrectly.
     */
    
    try {
      DocumentBuilder dbld = DocumentBuilderFactory.newInstance().newDocumentBuilder();
      Document doc = dbld.parse(new InputSource(new StringReader(respText)));
      
      Node root = XmlHelper.getNode("schema", doc.getChildNodes());
      Node fields = XmlHelper.getNode("fields", root.getChildNodes());
      Node types  = XmlHelper.getNode("types", root.getChildNodes());
      
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
              for (int k = 0; k < attrs.getLength(); ++k) {
                Node oneAttr = attrs.item(k);
                if (!oneAttr.getNodeName().equalsIgnoreCase("class")) {
                  res.mTokClassAttr.put(oneAttr.getNodeName(), oneAttr.getNodeValue());
                }
              }
            }
          }
       
        }
      }
        
      for (int i = 0; i < fields.getChildNodes().getLength(); ++i) {
        Node oneField = fields.getChildNodes().item(i);
        
        if (oneField.getAttributes()==null) continue;
        Node nameAttr = oneField.getAttributes().getNamedItem("name");
        if (nameAttr == null) {
          continue;
        }
        
        String fieldName = nameAttr.getNodeValue();
        
        if (
            fieldName.equalsIgnoreCase(UtilConst.ANNOT_FIELD) ||
            fieldName.equalsIgnoreCase(UtilConst.TEXT4ANNOT_FIELD)
            ) {
          Node typeAttr = oneField.getAttributes().getNamedItem("type");
          
          if (typeAttr != null) {
            String val = typeAttr.getNodeValue();
            String className = typeTokenizers.get(val.toLowerCase());
            if (className == null) {
              throw new Exception("Cannot find the tokenizer class for the field: " 
                                   + fieldName);
            }
            if (fieldName.equalsIgnoreCase(UtilConst.TEXT4ANNOT_FIELD)) {
              res.mTokClassName = className;
            } else {
              if (!className.equals(UtilConst.ANNOT_FIELD_TOKENIZER)) {
                throw new Exception("The field: '" + UtilConst.ANNOT_FIELD + "' " +
                                    " should be configured to use the tokenizer: " +
                                    UtilConst.ANNOT_FIELD_TOKENIZER);
              }
            }
          } else {
            throw new Exception("Missing type for the annotation field: " 
                                + fieldName);
          }
        }
      }
      
      
    } catch (SAXException e) {      
      System.err.println("Can't parse SOLR response:\n" + respText);
      throw e;
    }
    
    if (res.mTokClassName == null) {
      throw new Exception("Did you explicitly configure the tokenizer for the field: " +
                          UtilConst.TEXT4ANNOT_FIELD);
    }
    
    return res;
  }


  static String docTextFile = null, docAnnotFile = null, solrURI = null;
  static int    batchQty = 100;
}
