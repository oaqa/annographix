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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.io.File;
import java.io.FileWriter;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.uima.UimaContext;

import org.apache.uima.analysis_component.CasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.cas.*;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Level;
import org.apache.uima.util.ProcessTrace;

import edu.cmu.lti.oaqa.annographix.util.CompressUtils;
import edu.cmu.lti.oaqa.annographix.util.UtilConst;
import edu.cmu.lti.oaqa.annographix.util.XmlHelper;

class OneAnnotationData { 
    public OneAnnotationData(int id, 
                          String fieldName, 
                          int     start, 
                          int     end,
                          int     tagValue, 
                          int     parentId, 
                          String  textValue) {
    this.mId = id;
    this.mFieldName = fieldName;
    this.mStart = start;
    this.mEnd = end;
    this.mTagValue = tagValue;
    this.mParentId = parentId;
    this.mTextValue = textValue;
  }
    public int       mId;
    public String    mFieldName;
    public int       mStart;
    public int       mEnd;
    public int       mTagValue;
    public int       mParentId;
    public String    mTextValue;
}
/**
 * Extracts annotations from CAS objects and creates input files
 * for the {@link edu.cmu.lti.oaqa.annographix.apps.SolrIndexApp}.
 * 
 * @author Leonid Boytsov
 */
public class AnnotationWriter extends CasAnnotator_ImplBase {
  /** Result of parsing a config file. */
  private Map<String, MappingReader.TypeDescr> mMappingConfig;

  private BufferedWriter  mTextFileWriter; 
  private BufferedWriter  annotFileWriter; 

  private String          viewName;
  private String          textFieldName;
  
  /**
   * <p>This will provide unique ids within a single annotating thread only.
   * It is not a problem for annographix (b/c it cares only about uniqueness
   * of annotation ids within a single document), but will be problematic
   * if generate several annotation files in parallel and fed them to Indri.
   * Indri doesn't seem to tolerate non-unique annotation ids.</p>
   * 
   * <p><b>NOTE:</b> annotation ids must start with 1, b/c 0 is used to
   * denote a missing parent annotation.
   * </p>
   */
  private int mAnnotationId = 1; 

  public class FilePair {
    public File         f;
    public OutputStream fw;

    public FilePair(File _f, OutputStream _fw) { f = _f; fw = _fw; } 
  }; 

  private FilePair InitOutFile(UimaContext context, 
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
		
		return new FilePair(outFile, fileWriter);
  }

  @Override
  public void initialize(UimaContext context) throws ResourceInitializationException {
    super.initialize(context);
    
    viewName = (String) context.getConfigParameterValue(UtilConst.CONFIG_VIEW_NAME);
    
    if (viewName == null)
      throw new ResourceInitializationException(
                    new Exception("Missing parameter: " + UtilConst.CONFIG_VIEW_NAME));    
    
    textFieldName = (String) context.getConfigParameterValue(UtilConst.CONFIG_TEXT4ANNOT_FIELD);
    
    if (textFieldName == null)
      throw new ResourceInitializationException(
                    new Exception("Missing parameter: " + UtilConst.CONFIG_TEXT4ANNOT_FIELD));    

    FilePair pTxt = InitOutFile(context, "outTextFile");

    mTextFileWriter = new BufferedWriter(new OutputStreamWriter(pTxt.fw));
 
    FilePair pAnnot = InitOutFile(context, "outAnnotFile");

    annotFileWriter = new BufferedWriter(new OutputStreamWriter(pAnnot.fw));
    
    /* create the mapping configuration */
    MappingReader fieldMappingReader = new MappingReader();
    String mappingFileParam = (String)context.getConfigParameterValue("mappingFile");
    
    if (mappingFileParam == null) {
      throw new ResourceInitializationException(
        ResourceInitializationException.CONFIG_SETTING_ABSENT,
        new Object[] { "mappingFile" });
    }
    
    try{
      mMappingConfig = fieldMappingReader.getConf(new FileInputStream(mappingFileParam));
    } catch (Exception e) {
      context.getLogger().log(Level.SEVERE, "Unable to initialize mapping configuration properly");
      throw new ResourceInitializationException(e);
    }
  }
  
  @Override
  public void typeSystemInit(TypeSystem typeSystem) throws AnalysisEngineProcessException {
    super.typeSystemInit(typeSystem);
    for (String key : mMappingConfig.keySet()) {
      Type type = typeSystem.getType(key);
      if (type==null) {
        throw new AnalysisEngineProcessException("required_feature_structure_missing_from_cas",
                new Object[]{key});
      }
      
      MappingReader.TypeDescr desc = mMappingConfig.get(key);
      
      if (type.getFeatureByBaseName(desc.tag)==null) {
          throw new AnalysisEngineProcessException("required_attribute_missing", 
                  new Object[]{desc.tag, type});
      }
    }
  }

  @Override
  public void process(CAS aCAS) throws AnalysisEngineProcessException {
		JCas jcas = null;
		
		try {
			jcas = aCAS.getJCas();
		} catch (CASException e) {
			throw new AnalysisEngineProcessException(e);
		}
		
	  String docId = null;
/*	    
	  FSIterator<Annotation> it = jcas.getAnnotationIndex(InputElement.type).iterator();
	    
	  if (it.hasNext()) {
	    InputElement elem = (InputElement) it.next();
	    docId = elem.getQuuid();
	  } else {
	    throw new AnalysisEngineProcessException(new Exception("Bug: missing InputElement!"));
	  }
*/	  
	  XmlHelper xmlHlp = new XmlHelper();

    try {
      JCas viewJCAs = jcas.getView(viewName);
      /*
       *  Let's make a sanity check here, even though it's a bit expensive:
       *  The text in the view should match the text in the annotation field.
       */
      String docText = aCAS.getDocumentText();
      Map<String, String> docFields = xmlHlp.parseXMLIndexEntry(docText);
      String annotText = docFields.get(textFieldName);
      
      if (annotText == null) {
        throw new Exception("Missing field: " + textFieldName + 
                            " docId: " + docId);
      }
      if (!annotText.equals(viewJCAs.getDocumentText())) {
        throw new Exception("Non-matching annotation texts for docId: " + docId);        
      }
      
      String docNo = docFields.get(UtilConst.INDEX_DOCNO);
      if (docNo == null) {
        throw new Exception("Missing field: " + UtilConst.INDEX_DOCNO + 
                          " docId: " + docId);
      }
      
      writeText(docText);
      writeAnnotations(docNo, viewJCAs);
    } catch (Exception e) {
      throw new AnalysisEngineProcessException(e);
    }
    
  }

  private void writeAnnotations(String docId, JCas jcas) throws AnalysisEngineProcessException {    
    // First obtain unique annotation IDs
     
     Map<Annotation,Integer>  annotIds = new HashMap<Annotation,Integer>();
     
     FSIterator<Annotation> iter = 
                         jcas.getAnnotationIndex(Annotation.type).iterator();
     
     while (iter.hasNext()) {
       Annotation elem = iter.next();           
       annotIds.put(elem, mAnnotationId++);
     }

     // Now, we make a second pass and serialize annotations
     HashSet<Integer>               writtenIds = new HashSet<Integer>(); 
     ArrayList<OneAnnotationData>   annotList = new ArrayList<OneAnnotationData>();

     for (String key : mMappingConfig.keySet()) {
       Type                      type = jcas.getTypeSystem().getType(key);
       MappingReader.TypeDescr   desc = mMappingConfig.get(key); 
       
       iter = jcas.getAnnotationIndex(type).iterator();

       while (iter.hasNext()) { 
         Annotation elem   = iter.next();
         
         Feature feature = type.getFeatureByBaseName(desc.tag);
         String  tagValue = elem.getFeatureValueAsString(feature);
         
         int parentId = 0;
         
         if (desc.parent != null) {
           
           FeatureStructure parent = (Annotation) 
                 elem.getFeatureValue(type.getFeatureByBaseName(desc.parent)); 
               
           if (parent != null) parentId = annotIds.get((Annotation)parent);
         }
         

         annotList.add(new OneAnnotationData(annotIds.get(elem), 
                         UtilConst.combineFieldValue(
                             desc.fieldPrefix, tagValue),
                         elem.getBegin(), elem.getEnd(),
                         0,
                         parentId,
                         elem.getCoveredText()));
       }
     }
     
     try {
       int qty = annotList.size();
       
       boolean annotWritten[] = new boolean[qty]; 
           
       /*
        * 
        * This is to ensure that parentIds are 
        * written always before the child entries
        * that references their parents
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
           OneAnnotationData elem = annotList.get(i);
           
           if (elem.mParentId == 0 || writtenIds.contains(elem.mParentId)) {
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

  /*
   * documentText should already come in the right format.
   */
  private void writeText(String documentText) 
                         throws ParserConfigurationException, TransformerException, IOException {
    mTextFileWriter.write(documentText);
    mTextFileWriter.write('\n');
  }

  private void writeOneAnnotation(String docNo, 
                          OneAnnotationData elem) throws IOException {
    annotFileWriter.write(docNo            + "\t");
    annotFileWriter.write("TAG\t");
    annotFileWriter.write(elem.mId               + "\t");
    annotFileWriter.write(elem.mFieldName        + "\t");
    annotFileWriter.write(elem.mStart            + "\t");
    annotFileWriter.write((elem.mEnd - elem.mStart)    + "\t");
    annotFileWriter.write(elem.mTagValue         + "\t");
    annotFileWriter.write(elem.mParentId         + "\t");
    annotFileWriter.write(UtilConst.PATTERN_WHITESPACE.matcher(elem.mTextValue).
                          replaceAll(" ") + "\n");
  }

  /**
  * Called when the entire collection is completed.
  * 
  * @see org.apache.uima.collection.CasConsumer#collectionProcessComplete(ProcessTrace)
  */
  @Override
  public void collectionProcessComplete() throws AnalysisEngineProcessException {
    try {
      if (annotFileWriter != null) {
        annotFileWriter.close();
  		}
  		if (mTextFileWriter != null) {
  			mTextFileWriter.close();
  		}
    } catch (IOException e) {
      throw new AnalysisEngineProcessException(e);
    }
	}

   /**
    * Called if clean up is needed in case of exiting due to errors.
    * 
    * @see org.apache.uima.resource.Resource#destroy()
    */
	public void destroy() {
		if (annotFileWriter != null) {
			try {
				annotFileWriter.close();
			} catch (IOException e) {
				// ignore IOException on destroy
			}
		}
		if (mTextFileWriter != null) {
			try {
				mTextFileWriter.close();
			} catch (IOException e) {
				// ignore IOException on destroy
			}
		}
	}
}

