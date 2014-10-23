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

import static org.junit.Assert.*;
import org.junit.Test;

/**
 * 
 * These are several simple tests for the structured query parser.
 * 
 * @author Leonid Boytsov
 *
 */
public class StructQueryParserTest {
  /**
   * Simple query without any constraints.
   */
  @Test
  public void testNoConstr() {
    String query = "@:annotation ~:keyword @1:labeled-annotation ~2:labeled-keyword";
    
    try {
      String tokens_[]  = {"annotation", "keyword", 
                           "labeled-annotation", "labeled-keyword"};
      String labels_[]  = {"", "", "1", "2"};
      String types_[]   = {"FIELD_ANNOTATION", "FIELD_TEXT", "FIELD_ANNOTATION", "FIELD_TEXT"};
      String constrType_[] = {"","","",""};
      String dependId_[]   = {"","","",""}; 
      int    connectQty_[] = {0, 0, 0, 0};
      
      StructQueryParser p = new StructQueryParser(query);
      
      assertTrue(p.compareTo(tokens_, labels_, types_, constrType_, dependId_, connectQty_));
    } catch (Exception e) {
      e.printStackTrace();
      fail("Exception occurred.");      
    }
  }
  
  /**
   * Simple query without any constraints.
   */
  @Test
  public void testDuplicateLabel() {
    String query = "@abcd1:annotation ~:keyword ~abcd1:duplicate-label";
    
    boolean bGotException = false;
    
    try {

      StructQueryParser p = new StructQueryParser(query);
    } catch (Exception e) {
      System.out.println("Error message: " + e);
      bGotException = true;      
    }
    
    assertTrue("Failed to generate an exception for repeated label.", bGotException);
  }  

}
