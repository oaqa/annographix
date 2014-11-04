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
 * Testing an exponential search algorithm of finding the first
 * element whose offset exceeds a given one.
 * 
 * @author Leonid Boytsov
 *
 */
public class FindElemLargerOffsetTest {
  /**
   * A helper function that creates an array of the type ElemInfoData[]
   * and fills in offsets values. All other fields in ElemInfoData are
   * set to defaults.
   * 
   * @param offsets     an array of offsets
   * @return            an array of elements whose <b>start</b> offsets
   *                    are set equal to offsets specified by the input parameter.
   */
  static ElemInfoData[] fillArray(int ... offsets) throws Exception {
    ElemInfoData[] res = new ElemInfoData[offsets.length];
    
    for (int i = 0; i < offsets.length; ++i) {
      res[i] = new ElemInfoData();
      res[i].mStartOffset = offsets[i];
      if (i > 0 && offsets[i] < offsets[i-1]) {
        throw new Exception("Wrong test data, monotonicity violated for i" + (i-1));
      }
      if (offsets[i] < 0) {
        throw new Exception("Wrong test data, offset < 0 for i = " + i);
      }
    }
    
    return res;
  }
  
  static StartOffsetElemInfoDataComparator  mOffsetComp = new StartOffsetElemInfoDataComparator();
  static ElemInfoData                       mSearchKey = new ElemInfoData();
  
  @Test
  public void testTiny() {
    try {
      for (int k = 0; k < 10; ++k)
        genericTest(1, k, 0, 2, 2, 4, 8, 16, 128);
    } catch (Exception e) {
      e.printStackTrace();
      fail("Exception occurred.");      
    }
  }
  
  @Test
  public void testMedium() {
    try {
      int offsets[] = new int[1024];
      for (int i = 0; i < offsets.length; ++i) offsets[i] = 2*i;
      
      for (int k = 0; k < 10; ++k)
        genericTest(4, k, offsets);
    } catch (Exception e) {
      e.printStackTrace();
      fail("Exception occurred.");      
    }
  }  

  
  @Test
  public void testLarge() {
    try {
      int offsets[] = new int[1024*8];
      for (int i = 0; i < offsets.length; ++i) offsets[i] = 2*i;
      
      for (int k = 0; k < 5; ++k)
        genericTest(1024, k, offsets);
    } catch (Exception e) {
      e.printStackTrace();
      fail("Exception occurred.");      
    }
  }  
 
  
  /**
   * Brute force test, which tries to launch search from multiple starting
   * points.
   * 
   * @param  step               a brute-force test parameter, see the code below,
   *                            it specifies a selection step to choose starting
   *                            points for the offset search.
   * @param  forwardIterQty     a maximum number of forward iterations to carry out,
   *                            before starting a full-blown exponential search. 
   * @param offsets             offsets to use.
   * @throws Exception
   */
  private void genericTest(int step, int forwardIterQty,
                           int ... offsets) throws Exception {
    ElemInfoData[] dt = fillArray(offsets);
    
    for (int i = 1; i < offsets.length; ++i) {
      if (offsets[i] > offsets[i-1]) {
        int prevOff = offsets[i-1];
        for (int minIndx = i - 1 ; minIndx >= 0 ; minIndx -= step) {
          int res = 
              OnePostStateBase.findElemLargerOffset(dt, dt.length, 
                                                    mOffsetComp, 
                                                    mSearchKey, 
                                                    forwardIterQty,
                                                    prevOff, minIndx);
          assertTrue(String.format("Expected %d, but got", i, res), res == i);
          
          // What happens if we search for the largest offset available at the array boundary?
          res = 
              OnePostStateBase.findElemLargerOffset(dt, i + 1, 
                                                    mOffsetComp, 
                                                    mSearchKey, 
                                                    forwardIterQty,
                                                    offsets[i], minIndx);    

          assertTrue(String.format("Expected %d, but got", i + 1, res), res == (i+1));
        }
      }
      // What if minIndx >= array size?
      int res = 
          OnePostStateBase.findElemLargerOffset(dt, i+1, 
                                                mOffsetComp, 
                                                mSearchKey, 
                                                forwardIterQty,
                                                offsets[i], i + 1);
      assertTrue(String.format("Expected %d, but got", i, res), res == (i+1));           
    }
  }
}
