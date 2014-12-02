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
import org.apache.solr.common.SolrDocument;
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
  /** 
   * This is a run name to place in a QREL file, currently we don't
   * employ it anyware and, therefore, opt to use a fake run name value.  
   */
  private static final String TREC_RUN = "fakerun";

  static void Usage(String err) {
    System.err.println("Error: " + err);
    System.err.println("Usage: " 
                       + "-u <Target Server URI> "
                       + "-q <Query file> "
                       + "-n <Max # of results> "
                       + "-o <An optional TREC-style qrel file> "
                       + "-w <Do a warm-up before each query call?>");
    System.exit(1);
  }

  public static void main(String[] args) {
    Options options = new Options();
    
    options.addOption("u", null, true, "Solr URI");
    options.addOption("q", null, true, "Qyery");
    options.addOption("n", null, true, "Max # of results");
    options.addOption("o", null, true, "An optional TREC-style qrel file runid");
    options.addOption("w", null, false, "Do a warm-up query call, before each query");

    CommandLineParser parser = new org.apache.commons.cli.GnuParser(); 
    
    BufferedWriter qrelFile = null;
    
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
      
      if (cmd.hasOption("o")) {
        qrelFile = new BufferedWriter(new FileWriter(new File(cmd.getOptionValue("o"))));
      }
      
      List<String> fieldList = new ArrayList<String>();
      fieldList.add(UtilConst.ID_FIELD);
      fieldList.add("score");
      
      double totalTime = 0;
      double retQty = 0;
      
      ArrayList<Double> queryTimes = new ArrayList<Double>();
      
      boolean bDoWarmUp = cmd.hasOption("w");
      
      if (bDoWarmUp) {
        System.out.println("Using a warmup step!");
      }
      
      int queryQty = 0;
      for (String t : FileUtils.readLines(new File(queryFile))) {
        t = t.trim();
        if (t.isEmpty()) continue;
        int ind = t.indexOf('|');
        if (ind < 0) throw new Exception("Wrong format, line: '" + t + "'");
        String qID = t.substring(0, ind);
        String q = t.substring(ind + 1);
        
        SolrDocumentList res = null;
        
        if (bDoWarmUp) {
          res = solr.runQuery(q, fieldList, numRet);
        }
        
        Long tm1 = System.currentTimeMillis();
        res = solr.runQuery(q, fieldList, numRet);
        Long tm2 = System.currentTimeMillis();
        retQty += res.getNumFound();
        System.out.println(qID + " Obtained: " + res.getNumFound() + 
                          " entries in " + (tm2 - tm1)  + " ms");
        double delta = (tm2 - tm1);
        totalTime += delta;
        queryTimes.add(delta);
        ++queryQty;
        
        if (qrelFile != null) {

          ArrayList<SolrRes> resArr = new ArrayList<SolrRes>();
          for (SolrDocument doc : res) {
            String id  = (String)doc.getFieldValue(UtilConst.ID_FIELD);
            float  score = (Float)doc.getFieldValue(UtilConst.SCORE_FIELD);
            resArr.add(new SolrRes(id, "", score));
          }
          SolrRes[] results = resArr.toArray(new SolrRes[resArr.size()]);
          Arrays.sort(results);
          
          SolrEvalUtils.saveTrecResults(qID, 
                                   results, 
                                   qrelFile, 
                                   TREC_RUN, 
                                   results.length);
          
        }
      }
      double devTime = 0, meanTime = totalTime / queryQty;
      for (int i = 0; i < queryQty; ++i) {
        double d = queryTimes.get(i) - meanTime;
        devTime += d * d;
      }
      devTime = Math.sqrt(devTime / (queryQty - 1));
      System.out.println(String.format(
                              "Query time, mean/standard dev: %.2f/%.2f (ms)", 
                                       meanTime, devTime));
      System.out.println(String.format(
                            "Avg # of docs returned: %.2f", retQty / queryQty));
          
      solr.close();
      qrelFile.close();
    } catch (ParseException e) {
      Usage("Cannot parse arguments");
    } catch(Exception e) {
      System.err.println("Terminating due to an exception: " + e);
      System.exit(1);
    }    
    
  }

}
