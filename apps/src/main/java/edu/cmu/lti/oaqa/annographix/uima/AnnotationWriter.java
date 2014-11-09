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
package edu.cmu.lti.oaqa.annographix.uima;

import java.io.*;
import java.util.*;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.CasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;

import edu.cmu.lti.oaqa.annographix.solr.*;

/**
 * <p>
 * This class is essentially a CAS consumer that extracts annotations 
 * as well ass their parent-child links and stores them for subsequent
 * indexing. Which annotations and which parent-child links are 
 * stored is defined by an additional configuration file that
 * is called an <b>annotation mapping file</b>.
 * </p>
 * 
 * <p>
 * This CAS consumer creates an indexer file in a format similar to the
 * Indri's TRECTEX format, and an an offset annotation file (again in Indri format).
 * These files can be indexed by {@link edu.cmu.lti.oaqa.annographix.apps.SolrIndexApp}.
 * </p>
 * 
 * <p>
 * The produce fully Indri-compatible output you need to set the flag
 * {@link #PARAM_INDRI_FOMAT}. If set, the annotator will change the output
 * as follows:
 * </p>
 * <ol>
 * <li>The document text will be written in true TRECTEXT format, which
 *     is not fully XML compatible.
 * <li>Annotations are written in such an order that parents go <b>before</b> 
 *     children that reference them. This is clearly not always possible: 
 *     if loops are detected the annotator will fail.
 * </ol>
 * 
 * @author Leonid Boytsov
 */
public class AnnotationWriter extends AnnotationConsumer {
  private static final String PARAM_INDRI_FOMAT = "indri_format";
  
  private BufferedWriter  mTextFileWriter; 
  private BufferedWriter  mAnnotFileWriter;
  /** Indri-compatibility flag */ 
  private Boolean         mIndriFormat = false;
  
  /**
   * See {@link CasAnnotator_ImplBase#initialize(UimaContext)}.
   */
  @Override
  public void initialize(UimaContext context) throws ResourceInitializationException {
    super.initialize(context);
    
    mIndriFormat = (Boolean)context.getConfigParameterValue(PARAM_INDRI_FOMAT);
    
    if (mIndriFormat == null) mIndriFormat = false;
    
    OutputStream textFileOut = initOutFile(context, "out_text_file");

    mTextFileWriter = new BufferedWriter(new OutputStreamWriter(textFileOut));
 
    OutputStream annotFileOut = initOutFile(context, "out_annot_file");

    mAnnotFileWriter = new BufferedWriter(new OutputStreamWriter(annotFileOut));    
  }
  
  /**
   *  See {@link AnnotationConsumer.doProcess(JCas, String, String, Map)}
   */
  @Override
  protected void doProcess(JCas viewJCas, 
                           String docNo, 
                           String docText,
                           Map<String, String> docFields) 
                                        throws AnalysisEngineProcessException {

    try {
      /*
       *  Let's make a sanity check here, even though it comes at a small
       *  additional cost:
       *  The text in the view should match the text in the annotation field.
       */
      String annotText = docFields.get(mTextFieldName);
      
      if (annotText == null) {
        throw new Exception("Missing field: " + mTextFieldName + 
                            " docNo: " + docNo);
      }
      String jcasText = viewJCas.getDocumentText();
      
      if (annotText.compareTo(jcasText) != 0) {
        throw new Exception(String.format(
                    "Non-matching annotation texts for docNo: %s " + 
                    " view name: %s " + 
                    "text length: %d jcasView text length: %d ", 
                    docNo, mViewName, annotText.length(), jcasText.length()));        
      }
      
      writeText(docText);
      writeAnnotations(docNo, viewJCas);
    } catch (Exception e) {
      throw new AnalysisEngineProcessException(e);
    }    
  }

  /**
   * A helper function that extracts annotations from a 
   * {@link org.apache.uima.jcas.JCAS} object and stores them.
   * 
   * @param     docId   a unique document ID.
   * @param     jcas    a {@link org.apache.uima.jcas.JCAS} interface to the CAS.
   * 
   * @throws AnalysisEngineProcessException
   */
  private void writeAnnotations(String docId, JCas jcas) 
                                throws AnalysisEngineProcessException {
    ArrayList<Annotation>   origAnnotList = new ArrayList<Annotation>();
    FSIterator<Annotation> it = 
        jcas.getAnnotationIndex(Annotation.type).iterator();
    
    while (it.hasNext()) origAnnotList.add(it.next());
    
    ArrayList<AnnotationProxy> annotProxyList = 
                                    generateAnnotations(jcas, origAnnotList);
    if (!mIndriFormat) {    
      for (AnnotationProxy elem : annotProxyList 
          ) {
        try {
          writeOneAnnotation(docId, elem);
        } catch (IOException e) {
          throw new AnalysisEngineProcessException(e);
        }
      }
    } else {
      try {
        int qty = annotProxyList.size();

        boolean annotWritten[] = new boolean[qty]; 
        HashSet<Integer> writtenIds = new HashSet<Integer>();

        /*
         * 
         * This is to ensure that parentIds are 
         * written always before the child entries
         * that references their parents (as required by Indri).
         * 
         * TODO: implement a more efficient version for complex graph structure.
         * 
         * This algorithm is not the most efficient one.
         * Yet, it is complexity is O(qty * maxTreeDepth),
         * which in many cases will be close to O(qty),
         * because the maxTreeDepth will be small.
         * 
         */
        int     writtenQty = 0;

        while (true) {
          boolean bChanged = false;

          for (int i = 0; i < qty; ++i) 
            if (!annotWritten[i]) {
              AnnotationProxy elem = annotProxyList.get(i);

              /*
               * Save annotation if either:
               *    1) the parent is already written;
               *    2) there are no parent.
               */
              if (elem.mParentId <= 0 || writtenIds.contains(elem.mParentId)) {
                bChanged = true;
                writeOneAnnotation(docId, elem);
                annotWritten[i] = true;
                writtenIds.add(elem.mId);
                ++writtenQty;
              }           
            }

          if (!bChanged) {
            if (writtenQty < qty) {
              throw new AnalysisEngineProcessException(new
                  Exception("Annotation graph has loops, docId = " + docId + 
                      " wrote " + qty + " out of " + writtenQty));
            }
            break;
          }
        }
      } catch (IOException e) {
        throw new AnalysisEngineProcessException(e);
      }       
    }
  }
  
  /**
   * Writes a text of the document that is already properly
   * formatted (i.e., has Indri TEXTTREC format).
   */
  private void writeText(String documentText) 
                         throws ParserConfigurationException, TransformerException, IOException {
    if (mIndriFormat) {
      // This introduces a new line before and after each tag
      documentText = documentText.replaceAll(">", ">"+NL)
                                 .replaceAll("<", NL+"<");
    }
    mTextFileWriter.write(documentText);
    mTextFileWriter.write(NL);
  }

  /**
   * Saves on annotation in the annotation offset file.
   * 
   * @param     docNo       unique document identifier.
   * @param     elem        an object of the type {@link AnnotationProxy} that
   *                        keeps annotation-related data.
   * @throws IOException
   */
  private void writeOneAnnotation(String docNo, 
                                  AnnotationProxy elem) throws IOException {
    mAnnotFileWriter.write(docNo                     + "\t");
    mAnnotFileWriter.write("TAG\t"); // Indri'specific
    mAnnotFileWriter.write(elem.mId                  + "\t");
    mAnnotFileWriter.write(elem.mLabel               + "\t");
    mAnnotFileWriter.write(elem.mStart               + "\t");
    mAnnotFileWriter.write((elem.mEnd - elem.mStart) + "\t");
    mAnnotFileWriter.write("0\t"); // Indri'specific
    mAnnotFileWriter.write(elem.mParentId            + "\t");
    mAnnotFileWriter.write(UtilConst.PATTERN_WHITESPACE.matcher(elem.mText).
                          replaceAll(" ") + NL);
  }

 /**
   * Closes output files.
   * 
   * @throws IOException
   */
  protected void cleanUp() throws IOException {
    if (mAnnotFileWriter != null) {
      mAnnotFileWriter.close();
      mAnnotFileWriter = null;
    }
    if (mTextFileWriter != null) {
      mTextFileWriter.close();
      mTextFileWriter = null;
    }
  }

}

