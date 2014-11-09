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
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import edu.cmu.lti.oaqa.annographix.solr.*;
import edu.cmu.lti.oaqa.annographix.util.*;

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
 * This CAS consumer creates an indexer file in TRECTEXT Indri format,
 * and an an offset annotation file (again in Indri format).
 * These files can be indexed by {@link edu.cmu.lti.oaqa.annographix.apps.SolrIndexApp}.
 * </p>
 * 
 * @author Leonid Boytsov
 */
public class AnnotationWriter extends AnnotationConsumer {
  private BufferedWriter  mTextFileWriter; 
  private BufferedWriter  annotFileWriter; 

  /** 
   * A name of the field (also the name of the tag in the XML file)
   * that keeps annotated text (but not the annotations themselves).
   */
  private String          mTextFieldName;
  
  /**
   * See {@link CasAnnotator_ImplBase#initialize(UimaContext)}.
   */
  @Override
  public void initialize(UimaContext context) throws ResourceInitializationException {
    super.initialize(context);
             
    mTextFieldName = (String) 
              context.getConfigParameterValue(UtilConst.CONFIG_TEXT4ANNOT_FIELD);
    
    if (mTextFieldName == null)
      throw new ResourceInitializationException(
                    new Exception("Missing parameter: " + 
                                  UtilConst.CONFIG_TEXT4ANNOT_FIELD));   
    
    
    OutputStream textFileOut = InitOutFile(context, "out_text_file");

    mTextFileWriter = new BufferedWriter(new OutputStreamWriter(textFileOut));
 
    OutputStream annotFileOut = InitOutFile(context, "out_annot_file");

    annotFileWriter = new BufferedWriter(new OutputStreamWriter(annotFileOut));
    
  }
  
  @Override
  protected void doProcess(String docText, JCas jcas, JCas viewJCas) 
                                        throws AnalysisEngineProcessException {
    XmlHelper xmlHlp = new XmlHelper();

    try {      

            
      /*
       *  Let's make a sanity check here, even though it comes at a small
       *  additional cost:
       *  The text in the view should match the text in the annotation field.
       */
      Map<String, String> docFields = xmlHlp.parseXMLIndexEntry(docText);
      
      String docNo = docFields.get(UtilConst.INDEX_DOCNO);
      if (docNo == null) {
        throw new Exception("Missing field: " + UtilConst.INDEX_DOCNO);
      }            
      
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
                    docNo, getViewName(), annotText.length(), jcasText.length()));        
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
     for (AnnotationProxy elem : generateAnnotations(jcas)) {
       try {
         writeOneAnnotation(docId, elem);
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
    annotFileWriter.write(docNo                     + "\t");
    annotFileWriter.write("TAG\t"); // Indri'specific
    annotFileWriter.write(elem.mId                  + "\t");
    annotFileWriter.write(elem.mLabel               + "\t");
    annotFileWriter.write(elem.mStart               + "\t");
    annotFileWriter.write((elem.mEnd - elem.mStart) + "\t");
    annotFileWriter.write("0\t"); // Indri'specific
    annotFileWriter.write(elem.mParentId            + "\t");
    annotFileWriter.write(UtilConst.PATTERN_WHITESPACE.matcher(elem.mText).
                          replaceAll(" ") + NL);
  }

 /**
   * Closes output files.
   * 
   * @throws IOException
   */
  protected void cleanUp() throws IOException {
    if (annotFileWriter != null) {
      annotFileWriter.close();
    }
    if (mTextFileWriter != null) {
      mTextFileWriter.close();
    }
  }

 /**
   * Opens file for writing.
   * 
   * @param context             UIMA context variable.
   * @param fileParamName       A file name.
   * @return                    A an output stream.
   * @throws ResourceInitializationException
   */
  private OutputStream InitOutFile(UimaContext context, 
                               String fileParamName) throws ResourceInitializationException {
    // extract configuration parameter settings
    String oFileName = (String) context.getConfigParameterValue(fileParamName);
    // Output file should be specified in the descriptor
    if (oFileName == null) {
        throw new ResourceInitializationException(
                ResourceInitializationException.CONFIG_SETTING_ABSENT,
                new Object[] { fileParamName });
    }
    // If specified output directory does not exist, try to create it
    File outFile = new File(oFileName);
    if (outFile.getParentFile() != null
            && !outFile.getParentFile().exists()) {
        if (!outFile.getParentFile().mkdirs())
            throw new ResourceInitializationException(
                    ResourceInitializationException.RESOURCE_DATA_NOT_VALID,
                    new Object[] { oFileName, fileParamName });
    }
    OutputStream fileWriter = null;
    
    try {
        fileWriter = CompressUtils.createOutputStream(outFile.getAbsolutePath());
    } catch (IOException e) {
        throw new ResourceInitializationException(e);
    }
    
    return fileWriter;
  }
}

