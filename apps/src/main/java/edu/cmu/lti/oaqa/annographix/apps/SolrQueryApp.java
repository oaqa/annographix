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

import java.util.*;
import java.io.*;

import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;
import org.apache.solr.common.SolrDocumentList;

import edu.cmu.lti.oaqa.annographix.solr.*;

/**
 * An application that reads queries from a text file,
 * submits them to a SOLR server, and saves results.
 * 
 * @author Leonid Boytsov
 *
 */
public class SolrQueryApp {
  static void Usage(String err) {
    System.err.println("Error: " + err);
    System.err.println("Usage: " 
                       + "-u <Target Server URI> "
                       + "-q <Query file> "
                       + "-n <Max # of results> ");
    System.exit(1);
  }

  public static void main(String[] args) {
    Options options = new Options();
    
    options.addOption("u", null, true, "Solr URI");
    options.addOption("q", null, true, "Qyery");
    options.addOption("n", null, true, "Max # of results");

    CommandLineParser parser = new org.apache.commons.cli.GnuParser(); 
    
    try {
      CommandLine cmd = parser.parse(options, args);
      String queryFile= null, solrURI = null;

      if (cmd.hasOption("u")) {
        solrURI = cmd.getOptionValue("u");
      } else {
        Usage("Specify Solr URI");
      }
      
      SolrServerWrapper solr = new SolrServerWrapper(solrURI);
 
      if (cmd.hasOption("q")) {
        queryFile = cmd.getOptionValue("q");
      } else {
        Usage("Specify Query file");
      }
      
      int numRet = 100;
      
      if (cmd.hasOption("n")) {
        numRet = Integer.parseInt(cmd.getOptionValue("n"));
      }      
      
      List<String> fieldList = new ArrayList<String>();
      fieldList.add(UtilConst.ID_FIELD);
      fieldList.add("score");
      
      Long totalTime = 0L;
      
      int qty = 0;
      for (String t : FileUtils.readLines(new File(queryFile))) {
        t = t.trim();
        if (t.isEmpty()) continue;
        int ind = t.indexOf('|');
        if (ind < 0) throw new Exception("Wrong format, line: '" + t + "'");
        String qID = t.substring(0, ind);
        String q = t.substring(ind + 1);
        
        Long tm1 = System.currentTimeMillis();
        SolrDocumentList res = solr.runQuery(q, fieldList, numRet);
        Long tm2 = System.currentTimeMillis();
        System.out.println(qID + " Obtained: " + res.getNumFound() + 
                          " entries in " + (tm2 - tm1)  + " ms");
        totalTime += (tm2 - tm1);
        ++qty;
      }
      System.out.println(String.format("Average query time: %f", (double)totalTime / qty));
          
      solr.close();
    } catch (ParseException e) {
      Usage("Cannot parse arguments");
    } catch(Exception e) {
      System.err.println("Terminating due to an exception: " + e);
      System.exit(1);
    }    
    
  }

}
