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
      
      StructQueryParser p = new StructQueryParser(query);
      
      assertTrue(p.compareTo(tokens_, labels_, types_, constrType_, dependId_, connectQty_));
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

      StructQueryParser p = new StructQueryParser(query);
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

      StructQueryParser p = new StructQueryParser(query);
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
      
      StructQueryParser p = new StructQueryParser(query);
      
      assertTrue(p.compareTo(tokens_, labels_, types_, constrType_, dependId_, connectQty_));
    } catch (Exception e) {
      e.printStackTrace();
      fail("Exception occurred.");      
    }
  }    
}
