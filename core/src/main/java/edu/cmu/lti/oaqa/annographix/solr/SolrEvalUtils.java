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

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.File;
import java.util.regex.*;
import java.util.ArrayList;

import org.apache.commons.io.FileUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

public class SolrEvalUtils {
  /**
   * @param topicId     question/query id
   * @param idField     the name of the unique id field
   * @param results     entries previously obtained by a search in SOLR
   * @param trecFile    the already open stream
   * @param runId       TREC requires this to identify runs
   * @param maxNum      the maximum # of entries to save
   * @throws IOException 
   */
  public static void saveTrecResults(
                              String           topicId,
                              String           idField,
                              SolrRes[]        results,
                              BufferedWriter   trecFile,
                              String           runId,
                              int maxNum) throws IOException {

    for (int i = 0; i < Math.min(results.length, maxNum); ++i) {
      trecFile.write(String.format("%s\tQ0\t%s\t%d\t%f\t%s\n",
                                   topicId, results[i].mDocId, 
                                   (i+1), results[i].mScore, runId));
    }
  }
  
  public static void saveEvalResults(
                              String            questionTemplate,
                              String            topicId,
                              String            idField,
                              String            textField,
                              SolrRes[]        results,
                              ArrayList<String> allKeyWords,
                              String            docDirName,
                              int maxNum) throws Exception {
    File    docRootDir = new File(docDirName);
    
    if (!docRootDir.exists()) {
      if (!docRootDir.mkdir()) 
        throw new Exception("Cannot create: " + docRootDir.getAbsolutePath());      
    }
    
    String  docDirPrefix = docDirName + "/" + topicId;
    File    docDir = new File(docDirPrefix);
    
    if (!docDir.exists()) {
      if (!docDir.mkdir()) 
        throw new Exception("Cannot create: " + docDir.getAbsolutePath());
    }
    
    // Let's precompile replacement regexps
    Pattern [] replReg  = new Pattern[allKeyWords.size()];
    String  [] replRepl = new String[allKeyWords.size()];
    
    for (int i = 0; i < allKeyWords.size(); ++i) {
      replReg[i] = Pattern.compile("(^| )(" + allKeyWords.get(i) + ")( |$)",
                                    Pattern.CASE_INSENSITIVE);
      replRepl[i] = "$1<b>$2</b>$3";
    }
    
    
    for (int docNum = 0; docNum < Math.min(results.length, maxNum); ++docNum) {
      String                docId   = results[docNum].mDocId;
      ArrayList<String>     docText = results[docNum].mDocText;
      StringBuilder         sb = new StringBuilder();
            
      sb.append("<html><body><br>");
      sb.append("<p><i>" + questionTemplate + "</i></p><hr><br>");
      
      for (String s: docText) {
        for (int k = 0; k < replReg.length; ++k) {
          while (true) {
            /*
             *  When words share a space, the replacement will not work in the first pass
             *  Imagine you have 
             *  word1 word2
             *  And both words need to be replaced. In the first pass, only 
             *  word1 is replaced. 
             */
            String sNew = replReg[k].matcher(s).replaceAll(replRepl[k]);
            if (sNew.equals(s)) break;
            s = sNew;
          }
        }

        sb.append(s);
        sb.append("<br>");
      }
      
      sb.append("</body></html>");
      
      String text = sb.toString();
    
      File  df = new File(docDirPrefix + "/" + docId + ".html");      

      // Don't overwrite docs!
      if (!df.exists()) {        
        try {
          FileUtils.write(df, text);
        } catch (IOException e) {
          throw new Exception("Cannot write to file: " + df.getAbsolutePath() 
              + " reason: " + e); 
        }
      } else {
        System.out.println(String.format(
            "WARNING: ignoring already created document for topic %s docId %s",
            topicId, docId));
      }
    }
  }

}
