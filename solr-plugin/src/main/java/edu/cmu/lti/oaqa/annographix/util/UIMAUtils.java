/*
 *  Copyright 2013 Carnegie Mellon University
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

import org.apache.uima.UimaContext;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;

public class UIMAUtils {
  
  public static FSIterator<Annotation> getIterator(JCas aJCas, int typeIndexID) {
    AnnotationIndex<Annotation> Index = 
        aJCas.getJFSIndexRepository().getAnnotationIndex(typeIndexID);
    return Index.iterator();           
  }
  
  public static FSIterator<Annotation> getSubIterator(JCas aJCas, int typeIndexID,
                               Annotation Cover, boolean strict) {
    AnnotationIndex<Annotation> Index = 
        aJCas.getJFSIndexRepository().getAnnotationIndex(typeIndexID);
    return Index.subiterator(Cover, false, strict);           
  }
  
  public static Double getNumericValue(UimaContext aContext,
                                       String paramName,
                                       Double defaultValue)
                                       throws ResourceInitializationException {
    Double res = defaultValue;
    
    Object o = aContext.getConfigParameterValue(paramName);
    
    if (null != o) {
      if (o instanceof Integer) res = (double)(Integer)o;
      else if (o instanceof Float) res = (double)(Float)o;
      else if (o instanceof Double) res = (Double)o;
      else throw new ResourceInitializationException(
            new Exception(String.format("Wrong type (%s) of the paramter '%s'",
                                        o.getClass().getCanonicalName(), paramName)));
    }
    
    return res;
  }
}
