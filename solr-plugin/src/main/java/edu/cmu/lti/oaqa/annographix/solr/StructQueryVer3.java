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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;

import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.util.Bits;
import org.apache.solr.search.SyntaxError;

import edu.cmu.lti.oaqa.annographix.util.UtilConst;

/**
  *  A rather advanced structured query: for tokens and annotations
  *  in a window of a given size, we can specify a set of constraints.
  *
  * @author Leonid Boytsov
  *
  */

class StructQueryVer3 extends Query {

  public StructQueryVer3(String text, 
                         int span, 
                         String textFieldName, String annotFieldName)
                         throws SyntaxError
  {
    StructQueryParser qParsed = new StructQueryParser(text);
    

  }
}
