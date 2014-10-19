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

import java.io.*;
import java.util.zip.*;

import org.apache.tools.bzip2.CBZip2InputStream;
import org.apache.tools.bzip2.CBZip2OutputStream;

/**
 *   Creates an input stream from a potentially compressed file;
 *   Determines a compression format by file extension.
 *   Supported for reading: .gz & bz2
 *   Supported for writing: only .gz
 *
 */
public class CompressUtils {
  public static InputStream createInputStream(String fileName) throws IOException {
    InputStream finp = new FileInputStream(fileName);
    if (fileName.endsWith(".gz")) return new GZIPInputStream(finp);
    if (fileName.endsWith(".bz2")) {
      finp.read(new byte[2]); // skip the mark

      return new CBZip2InputStream(finp);
    }
    return finp;
  }
  
  public static OutputStream createOutputStream(String fileName) throws IOException {
    OutputStream foutp = new FileOutputStream(fileName);
    if (fileName.endsWith(".gz")) return new GZIPOutputStream(foutp);
    if (fileName.endsWith(".bz2")) {
      throw new IOException("bz2 is not supported for writing");      
    }
    return foutp;
  }
}
