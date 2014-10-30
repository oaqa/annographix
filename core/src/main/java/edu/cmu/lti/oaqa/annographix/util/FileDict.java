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
package edu.cmu.lti.oaqa.annographix.util;

import java.io.File;
import java.util.HashSet;

import org.apache.commons.io.FileUtils;

/**
 *   A stupid class to keep the list of (stop)words and
 *   supports the contains operation. Ignores empty lines,
 *   or lines that start with '#'.
 *   
 *   One of the things that are much faster to code yourself,
 *   as compared to finding somebody else's implementation.
 *
 *  @author Leonid Boytsov
 */
public class FileDict {
  private HashSet<String>   mDict = new HashSet<String>();
  private boolean           mToLower = false;
  
  public boolean contains(String str) {
    if (mToLower) str = str.toLowerCase();
    return mDict.contains(str);
  }
  
  public FileDict(String file, boolean toLower) throws Exception {
    mToLower = toLower;
    for (String s: FileUtils.readLines(new File(file))) {
      s = s.trim();
      if (s.isEmpty() ||  s.startsWith("#")) continue;
      if (mToLower) s = s.toLowerCase();
      mDict.add(s);
    }    
  }

}
