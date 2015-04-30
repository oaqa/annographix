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
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.xml.sax.SAXException;

import edu.cmu.lti.oaqa.annographix.solr.*;
import edu.cmu.lti.oaqa.annographix.util.CompressUtils;
import edu.cmu.lti.oaqa.annographix.util.XmlHelper;

/**
 * An simple application that reads text files produced by an annotation
 * pipeline and indexes their content using SOLR.
 * <p>
 * <b>An apparent limitation: can index only text fields.</b>
 * <p>
 * The input file contains documents. The document
 * text is enclosed between tags &lt;DOC&gt; and &lt;DOC&gt; and occupies
 * exactly one line.
 * 
 * @author Leonid Boytsov
 *
 */
public class SolrSimpleIndexApp {
  static void Usage(String err) {
    System.err.println("Error: " + err);
    System.err.println("Usage: -i <Input File> " +
                       "-u <Target Server URI> " + 
                       " [ -n <Bach Size> default " + batchQty + " ]");

    System.exit(1);
  }

  public static void main(String[] args) {
    Options options = new Options();
    
    options.addOption("i", null, true, "Input File");
    options.addOption("u", null, true, "Solr URI");
    options.addOption("n", null, true, "Batch size");

    CommandLineParser parser = new org.apache.commons.cli.GnuParser(); 
    
    try {
      CommandLine cmd = parser.parse(options, args);
      
      if (cmd.hasOption("i")) {
        inputFile = cmd.getOptionValue("i");
      } else {
        Usage("Specify Input File");
      }
      
      if (cmd.hasOption("u")) {
        solrURI = cmd.getOptionValue("u");
      } else {
        Usage("Specify Solr URI");
      }
      
      if (cmd.hasOption("n")) {
        batchQty = Integer.parseInt(cmd.getOptionValue("n"));
      }
      
      
      SolrServerWrapper       solrServer = new SolrServerWrapper(solrURI);
      
      BufferedReader  inpText = new BufferedReader(
          new InputStreamReader(CompressUtils.createInputStream(inputFile)));
      
      XmlHelper xmlHlp = new XmlHelper();
      
      String docText = XmlHelper.readNextXMLIndexEntry(inpText);
                  
      for (int docNum = 1; 
           docText!= null; 
           ++docNum, docText = XmlHelper.readNextXMLIndexEntry(inpText)) {
        
        // 1. Read document text
        Map<String, String>         docFields = null;
        HashMap<String, Object>     objDocFields = new HashMap<String, Object>(); 
              
        try {
          docFields = xmlHlp.parseXMLIndexEntry(docText);
        } catch (SAXException e) {
          System.err.println("Parsing error, offending DOC:" + NL + docText);
          throw new Exception("Parsing error.");
        }
        
        for (Map.Entry<String, String> e : docFields.entrySet()) {
          //System.out.println(e.getKey() + " " + e.getValue());
          objDocFields.put(e.getKey(), e.getValue());
        }
        
        solrServer.indexDocument(objDocFields);
        if ((docNum-1) % batchQty == 0)  solrServer.indexCommit();
      }
      solrServer.indexCommit();
            
    } catch (ParseException e) {
      Usage("Cannot parse arguments");
    } catch(Exception e) {
      System.err.println("Terminating due to an exception: " + e);
      System.exit(1);
    }    
    
  }

  static String inputFile = null, solrURI = null;
  static int    batchQty = 100;
  private final static String NL = System.getProperty("line.separator");
}
