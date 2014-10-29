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


/**
 * 
 * A simple helper class to hold annotation descriptions,
 * which were read from an Indri-style annotation file.
 * 
 * @author Leonid Boytsov
 *
 */
public class AnnotationEntry implements Comparable<AnnotationEntry> {
  public AnnotationEntry(String docNo, 
                         int annotId, int parentId, 
                         String label, 
                         int startChar, int charLen) {
    mDocNo      = docNo;
    mAnnotId    = annotId;
    mParentId   = parentId;
    mLabel      = label;
    mStartChar  = startChar;
    mCharLen    = charLen;
  }
  public static AnnotationEntry parseLine(String line) 
                                            throws 
                                            NumberFormatException,
                                            EntryFormatException {
    String parts[] = line.split("\\t");
    
    if (parts.length < 8) throw new EntryFormatException();
    
    return new AnnotationEntry(parts[0],
                                Integer.parseInt(parts[2]),
                                Integer.parseInt(parts[7]),
                                parts[3],
                                Integer.parseInt(parts[4]),
                                Integer.parseInt(parts[5]));
  }
  String            mDocNo;
  int               mAnnotId;
  int               mParentId;
  public String     mLabel;
  public int        mStartChar;
  public int        mCharLen;
  
  @Override
  public int compareTo(AnnotationEntry o) {
    if (!mDocNo.equals(o.mDocNo)) {
      return mDocNo.compareTo(o.mDocNo);
    }
    if (mStartChar != o.mStartChar) {
      return mStartChar - o.mStartChar;
    }
    /*
     *  If start offsets are equal, but the current object 
     *  has smaller size, it will go earlier
     */
    return mCharLen - o.mCharLen;
  }
}
