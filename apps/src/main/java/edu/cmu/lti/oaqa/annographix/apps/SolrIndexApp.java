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
import java.util.Map;

import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import edu.cmu.lti.oaqa.annographix.solr.*;
import edu.cmu.lti.oaqa.annographix.util.XmlHelper;

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
  public static String TEXT_FIELD_ARG = "textField";
  public static String ANNOT_FIELD_ARG = "annotField";
  
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
    options.addOption(OptionBuilder
                        .withLongOpt(TEXT_FIELD_ARG)
                        .withDescription("Text field name")
                        .hasArg()
                          .create()
                      );
    options.addOption(OptionBuilder
                        .withLongOpt(ANNOT_FIELD_ARG)
                        .withDescription("Annotation field name")
                        .hasArg()
                          .create()
                      );    


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
      
      String textFieldName  = UtilConst.DEFAULT_TEXT4ANNOT_FIELD;
      String annotFieldName = UtilConst.DEFAULT_ANNOT_FIELD;
      
      if (cmd.hasOption(TEXT_FIELD_ARG)) {
        textFieldName = cmd.getOptionValue(TEXT_FIELD_ARG);
      }
      if (cmd.hasOption(ANNOT_FIELD_ARG)) {
        annotFieldName = cmd.getOptionValue(ANNOT_FIELD_ARG);
      }
      
      System.out.println(String.format(
                            "Annotated text field: '%s', annotation field: '%s'",
                            textFieldName, annotFieldName));

      // Ignoring return value here
      SolrUtils.parseAndCheckConfig(solrURI, textFieldName, annotFieldName);  
      
      System.out.println("Config is fine!");
      
      DocumentReader.readDoc(docTextFile, textFieldName, 
                            docAnnotFile, batchQty,
                            new SolrDocumentIndexer(solrURI, 
                                                    textFieldName,
                                                    annotFieldName));
  
    } catch (ParseException e) {
      Usage("Cannot parse arguments");
    } catch(Exception e) {
      System.err.println("Terminating due to an exception: " + e);
      System.exit(1);
    }    
    
  }

  static String docTextFile = null, docAnnotFile = null, solrURI = null;
  static int    batchQty = 100;
}
