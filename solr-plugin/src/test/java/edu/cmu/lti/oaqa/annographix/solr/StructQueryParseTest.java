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
public class StructQueryParseTest {
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
      int    componentId_[] = {0, 1, 2, 3};
      
      StructQueryParseVer3 p = new StructQueryParseVer3(query);
      
      assertTrue(p.compareTo(tokens_, labels_, types_, constrType_, dependId_, 
                             connectQty_, componentId_));
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

      StructQueryParseVer3 p = new StructQueryParseVer3(query);
    } catch (Exception e) {
      System.out.println("Error message: " + e);
      bGotException = true;      
    }
    
    assertTrue("Failed to generate an exception for repeated label.", bGotException);
  }  
  
  /**
   * Let's have two chains of constrained tokens.
   */
  @Test
  public void testTwoChains() {
    String query = " #parent(1,2) #parent(2,3) #covers(4,5) #covers(5,6) " +
                   "@1:a1 @2:a2 @3:a3 ~4:t1 ~5:t2 ~6:t3 " 
                   ;
    
    try {
      String tokens_[]  = {"a1", "a2", "a3", "t1", "t2", "t3"};
      String labels_[]  = {"1", "2", "3", "4", "5", "6"};
      String types_[]   = {"FIELD_ANNOTATION", "FIELD_ANNOTATION", "FIELD_ANNOTATION",
                           "FIELD_TEXT", "FIELD_TEXT", "FIELD_TEXT"};
      String constrType_[] = {"CONSTRAINT_PARENT",
                              "CONSTRAINT_PARENT",
                              "",
                              "CONSTRAINT_CONTAINS", 
                              "CONSTRAINT_CONTAINS", 
                              ""};
      String dependId_[]   = {"1","2","",
                              "4", "5", ""}; 
      int    connectQty_[] = {3, 3, 3, 3, 3, 3};
      int    componentId_[] = {0, 0, 0, 1, 1, 1}; 
      
      StructQueryParseVer3 p = new StructQueryParseVer3(query);
      
      assertTrue(p.compareTo(tokens_, labels_, types_, constrType_, dependId_, 
                             connectQty_, componentId_));
    } catch (Exception e) {
      e.printStackTrace();
      fail("Exception occurred.");      
    }
  }  

  /**
   * Check that a parent relationship is not valid between a token and
   * an annotation.
   */
  @Test
  public void testParentCantConnectAnnot2Token() {
    String query = " #parent(1,2) @1:annot ~2:keyword ";
    
    boolean bGotException = false;
    
    try {

      StructQueryParseVer3 p = new StructQueryParseVer3(query);
    } catch (Exception e) {
      System.out.println("Error message: " + e);
      bGotException = true;      
    }
    
    assertTrue("Failed to generate an exception for invalid parent-child relantionship.", bGotException);
  }
  
  /**
   * Check that a parent relationship is not valid between an annotation and
   * a token.
   */
  @Test
  public void testParentCantConnectToken2Annot() {
    String query = " #parent(2,1) @1:annot ~2:keyword ";
    
    boolean bGotException = false;
    
    try {

      StructQueryParseVer3 p = new StructQueryParseVer3(query);
    } catch (Exception e) {
      System.out.println("Error message: " + e);
      bGotException = true;      
    }
    
    assertTrue("Failed to generate an exception for invalid parent-child relantionship.", bGotException);
  }   

  /**
   * Linkage tests.
   * <ol>
   * <li>Let's have of constrained tokens;
   * <li>Let's check if a containment constraint works similarity to a parent 
   * constraint with respect to element bounding (it is supposed to connect any
   * elements).
   * <li>In particular, let's check if an annotation can cover a token and vice versa.
   * </ol>
   */
  @Test
  public void testLinkageCircular() {
    String query = " #parent(1,2) @1:a1 #covers(2,3) @2:a2 #covers(3,1) ~3:t1 " +
                   " ~4:t2 #covers(5,6) #covers(6,5) @5:a3 ~6:t3"
                   ;
    
    try {
      String tokens_[]  = {"a1", "a2", "t1", "t2", "a3", "t3"};
      String labels_[]  = {"1",  "2",  "3",  "4",  "5",  "6"};
      String types_[]   = {"FIELD_ANNOTATION", "FIELD_ANNOTATION", "FIELD_TEXT",  
                           "FIELD_TEXT", "FIELD_ANNOTATION",  "FIELD_TEXT"};
      String constrType_[] = {"CONSTRAINT_PARENT",
                              "CONSTRAINT_CONTAINS",
                              "CONSTRAINT_CONTAINS",
                              "", 
                              "CONSTRAINT_CONTAINS", 
                              "CONSTRAINT_CONTAINS"};
      String dependId_[]   = {"1","2","0",
                              "", "5", "4"}; 
      int    connectQty_[] = {3, 3, 3, 0, 2, 2};
      int    componentId_[] = {0, 0, 0, 1, 2, 2};
      
      StructQueryParseVer3 p = new StructQueryParseVer3(query);
      
      assertTrue(p.compareTo(tokens_, labels_, types_, constrType_, dependId_, 
                             connectQty_, componentId_));
    } catch (Exception e) {
      e.printStackTrace();
      fail("Exception occurred.");      
    }
  }
  
  /**
   * Let's do some realistic example.
   */
  @Test
  public void testSRL1() {
    String query = " ~1:it ~2:grew ~3:along ~4:the ~5:river ~6:nile " +  
                   " @ner:ner_loc #covers(ner,5,6) " + // id = 6
                   " @v:srl_v #covers(v,2) #parent(v,a1,am-loc) " +  // id = 7
                   " @a1:srl_a1 #covers(a1,1) " +   // id = 8
                   " @am-loc:srl_am-loc #covers(am-loc,3,4,5,6) " // id = 9 
                   ;
    
    try {
      String tokens_[]  = {"it",
                           "grew",
                           "along",
                           "the",
                           "river",
                           "nile",
                           "ner_loc",
                           "srl_v",
                           "srl_a1",
                           "srl_am-loc"};
      
      String labels_[]  = {"1",
                           "2",
                           "3",
                           "4",
                           "5",
                           "6",
                           "ner",
                           "v",
                           "a1",
                           "am-loc"};
      
      String types_[]   = {"FIELD_TEXT", 
                          "FIELD_TEXT",
                          "FIELD_TEXT",
                          "FIELD_TEXT",
                          "FIELD_TEXT",
                          "FIELD_TEXT",
                          "FIELD_ANNOTATION",
                          "FIELD_ANNOTATION",
                          "FIELD_ANNOTATION",
                          "FIELD_ANNOTATION"};

      String constrType_[] = {"", "", "", "", "", "",
      "CONSTRAINT_CONTAINS,CONSTRAINT_CONTAINS", // NER
      "CONSTRAINT_CONTAINS,CONSTRAINT_PARENT,CONSTRAINT_PARENT", // SRL_V
      "CONSTRAINT_CONTAINS", // SRL_A1
      // SRL_AM-LOC
      "CONSTRAINT_CONTAINS,CONSTRAINT_CONTAINS,CONSTRAINT_CONTAINS,CONSTRAINT_CONTAINS" 
                            };
      String dependId_[]   = {"", "", "", "", "", "",
                              "4,5", // NER
                              "1,8,9", // SRL_V
                              "0", // SR_A1
                              "2,3,4,5" // SRL_AM-LOC
                             }; 
      int    connectQty_[] = {10, 10, 10, 10, 10, 10, 10, 10, 10, 10}; 
      int    componentId_[] = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
      
      StructQueryParseVer3 p = new StructQueryParseVer3(query);
      
      assertTrue(p.compareTo(tokens_, labels_, types_, constrType_, dependId_, 
                            connectQty_, componentId_));
    } catch (Exception e) {
      e.printStackTrace();
      fail("Exception occurred.");      
    }
  }
}
