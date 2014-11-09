/*
 * Copyright 2014 Carnegie Mellon University
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
 *
 */
package edu.cmu.lti.oaqa.annographix.solr;

/**
 * 
 * A proxy class to store some basic annotation data, in particular, 
 * labels, unique annotation IDs, and parent IDs for annotations that 
 * have parent references.  
 * 
 * @author Leonid Boytsov
 *
 */
public class AnnotationProxy {
  /*
   * Final immutable objects are constant!
   * They are assigned in the constructor and cannot
   * be changed afterwards.
   */  
  /** annotation ID */
  public final int       mId;
  /** annotation label */
  public final String    mLabel;
  /** start offset (inclusive). */
  public final int       mStart;
  /** end offset (exclusive). */
  public final int       mEnd;
  /** parent ID */
  public final int       mParentId;
  /** covered text */
  public final String    mText;  
  /**
   * 
   * @param id          annotation ID.
   * @param label       annotation label.
   * @param start       start offset (inclusive).
   * @param end         end offset (exclusive).
   * @param parentId    parent ID.
   * @param text        covered text.
   */
  public AnnotationProxy(int    id, 
                        String  label, 
                        int     start, 
                        int     end,
                        int     parentId, 
                        String  text) {
    this.mId      = id;
    this.mLabel   = label;
    this.mStart   = start;
    this.mEnd     = end;
   
    this.mParentId  = parentId;
    this.mText = text;
  }
  
  public void debugPrint() {
    System.out.println(String.format(
                          "[id=%d,label=%s,start=%d,end=%d,parentId=%d,text=%s]",
                            mId, mLabel, mStart, mEnd, mParentId, mText));
  }
}
