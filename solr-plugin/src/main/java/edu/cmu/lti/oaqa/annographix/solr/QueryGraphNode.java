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

import java.util.ArrayList;

/**
 * 
 * A representation of a single graph node obtained via query parsing. 
 * 
 * @author Leonid Boytsov
 *
 */
public class QueryGraphNode implements Comparable<QueryGraphNode> {
  
  
  /** An associated posting class **/
  private OnePostStateBase                      mPost = null;
  private StructQueryParse.ConstraintType[]     mConstrType = null;
  private QueryGraphNode[]                      mConstrNode = null;
  private int                                   mConnectQty = 0;
  
  public QueryGraphNode[] createGraphNodes() {
    return null;
  }
  
  protected QueryGraphNode(OnePostStateBase post,
                        ArrayList<StructQueryParse.ConstraintType> constrType,
                        ArrayList<QueryGraphNode> constrNode) throws Exception {
    if (constrNode.size() != constrType.size()) {
      throw new Exception("Bug: constrType.size() != constrNode.size()");
    }
    mPost = post;
    mConstrType = new StructQueryParse.ConstraintType[constrType.size()];
    mConstrNode = new QueryGraphNode[constrNode.size()];
    for (int i = 0; i < constrType.size(); ++i) {
      mConstrType[i] = constrType.get(i);
      mConstrNode[i] = constrNode.get(i);
    }
  }
  
  public int getConnectQty() {
    return mConnectQty;
  }

  @Override
  public int compareTo(QueryGraphNode o) {
    if (mConnectQty != o.mConnectQty) {
    /*
     *  if mConnectQty > o.mConnectQty we return a value < 0, 
     *  so that an entry with a larger Qty values goes first.
     */
      return o.mConnectQty - mConnectQty;
    }
    
    assert(mPost != null && o.mPost != null);
    
    long diff = mPost.getPostCost() - o.mPost.getPostCost();
    return diff < 0 ? - 1 : 
                    diff > 0 ? 1 : 0;
  }

}
