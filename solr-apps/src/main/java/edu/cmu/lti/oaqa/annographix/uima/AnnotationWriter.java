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
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

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
import org.apache.uima.fit.util.JCasUtil;

import edu.cmu.lti.oaqa.annographix.solr.UtilConst;
import edu.cmu.lti.oaqa.annographix.util.CompressUtils;
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
public class AnnotationWriter extends CasAnnotator_ImplBase {
  /** 
   * A parameter to specify an annotation mapping file. 
   * This file defines which annotations and which annotation
   * attributes should be stored. The annotation mapping file
   * also specifies which parent-child links should be memorized. 
   */
  public static final String PARAM_MAPPING_FILE = "mapping_file";
  /**
   *  A parameter to specify a UIMA type that keeps document-related 
   *  information such as the document ID. 
   */
  public static final String PARAM_DOC_INFO_TYPE = "docinfo_type";
  /**
   * A parameter to specify the getter function for the  document-number/id 
   * attribute name. The UIMA type given by the parameter PARAM_DOC_INFO_TYPE 
   * should  have this getter. It seems that the name of the function is 
   * made as follows : get + attribute name with first letter uppercased.
   * However, Leo is not 100% sure and, therefore, asks you to provide
   * an exact name of the getter function.
   *  
   */
  public static final String PARAM_DOCNO_GETTER = "docno_getter";
                          
  /** A name of the UIMA type that keeps document related information */
  private String mDocInfoType;
  /** A note of the attribute to store a unique document ID. */
  private String mDocNoGetter;
  
  /** a class of the type mDocInfoType */
  private Class<? extends Annotation>  mDocInfoTypeClass;
  /** a method to obtain the document number */
  private Method                       mGetDoNoMethod;
 

  /** Result of parsing the annotation mapping file. */
  private Map<String, MappingReader.TypeDescr> mMappingConfig;

  private BufferedWriter  mTextFileWriter; 
  private BufferedWriter  annotFileWriter; 

  /** A view (SOFA) name to be used to extract annotations */
  private String          mViewName;
  /** 
   * A name of the field (also the name of the tag in the XML file)
   * that keeps annotated text (but not the annotations themselves).
   */
  private String          mTextFieldName;
  
  /**
   * <p>
   * This class members provides unique IDs within a single annotating thread only.
   * It is not a problem for annographix (b/c it cares only about uniqueness
   * of annotation IDs within a single document), but will be problematic
   * if generate several annotation files in parallel and fed them to Indri.
   * Indri doesn't tolerate non-unique annotation IDs.
   * </p>
   * 
   * <p>
   * <b>NOTE:</b> annotation IDs must start with 1, b/c 0 is used to
   * denote a missing parent annotation.
   * </p>
   */
  private int mAnnotationId = 1; 

  /**
   * See {@link CasAnnotator_ImplBase#initialize(UimaContext)}.
   */
  @Override
  public void initialize(UimaContext context) throws ResourceInitializationException {
    super.initialize(context);
    
    /* 
     * A view name can be optional.
     */
    mViewName = 
          (String) context.getConfigParameterValue(UtilConst.CONFIG_VIEW_NAME);
        
    mTextFieldName = (String) 
              context.getConfigParameterValue(UtilConst.CONFIG_TEXT4ANNOT_FIELD);
    
    if (mTextFieldName == null)
      throw new ResourceInitializationException(
                    new Exception("Missing parameter: " + 
                                  UtilConst.CONFIG_TEXT4ANNOT_FIELD));   
    
    mDocInfoType = 
        (String) context.getConfigParameterValue(PARAM_DOC_INFO_TYPE);
    
    if (mDocInfoType == null)
      throw new ResourceInitializationException(
          new Exception("Missing parameter: " + PARAM_DOC_INFO_TYPE));

    
    try {
      mDocInfoTypeClass = Class.forName(mDocInfoType).asSubclass(Annotation.class);
    } catch (ClassNotFoundException e) {
      throw new ResourceInitializationException(
          new Exception("Type: '" + mDocInfoType + "' doesn't exist!"));
    }
    
    mDocNoGetter = 
        (String) context.getConfigParameterValue(PARAM_DOCNO_GETTER);
    
    if (mDocNoGetter == null)
      throw new ResourceInitializationException(
          new Exception("Missing parameter: " + PARAM_DOCNO_GETTER));
    
    Exception eMemorize = null;
    try {
      mGetDoNoMethod = mDocInfoTypeClass.getMethod(mDocNoGetter);
    } catch (NoSuchMethodException e1) {
      eMemorize = e1;
    } catch (SecurityException e1) {
      eMemorize = e1;
    }
    if (eMemorize != null) {
      throw new ResourceInitializationException(
          new Exception(String.format(          
                                  "Cannot obtain attribute-reading function, " +
                                  "type: '%s', attribute '%s', exception '%s'",
                                  mDocInfoType, mDocNoGetter, eMemorize.toString()))); 
    }
    
    FilePair pTxt = InitOutFile(context, "out_text_file");

    mTextFileWriter = new BufferedWriter(new OutputStreamWriter(pTxt.fw));
 
    FilePair pAnnot = InitOutFile(context, "out_annot_file");

    annotFileWriter = new BufferedWriter(new OutputStreamWriter(pAnnot.fw));
    
    /* create the mapping configuration */
    MappingReader fieldMappingReader = new MappingReader();
    String mappingFileParam = 
                    (String)context.getConfigParameterValue(PARAM_MAPPING_FILE);
    
    if (mappingFileParam == null) {
      throw new ResourceInitializationException(
          new Exception("Missing parameter: " + PARAM_MAPPING_FILE)
      );
    }
    
    try{
      mMappingConfig = fieldMappingReader.getConf(new FileInputStream(mappingFileParam));
    } catch (Exception e) {
      context.getLogger().log(Level.SEVERE, "Unable to initialize mapping configuration properly");
      throw new ResourceInitializationException(e);
    }
  }
  
  /**
   * Here we check that the user specified correct names of types and attributes.   
   */
  @Override
  public void typeSystemInit(TypeSystem typeSystem) throws AnalysisEngineProcessException {
    super.typeSystemInit(typeSystem);
    for (String key : mMappingConfig.keySet()) {
      // 1. Check if the type exists
      Type type = typeSystem.getType(key);
      if (type==null) {
        throw new AnalysisEngineProcessException(
                    new Exception(String.format(
                         "Annotation mapping file: Specified type '%s' does not exist", key
                                               )
                                  ));
      }
      
      MappingReader.TypeDescr desc = mMappingConfig.get(key);
      // 2. Check if the type attribute exists
      String attr = desc.tag;
      if (type.getFeatureByBaseName(attr)==null) {
        throw new AnalysisEngineProcessException(
            new Exception(String.format(
                "Annotation mapping file: attribute '%s' of the type type '%s' does not exist", 
                                       attr, key)
                          ));
      }
      
      // 3. If we have a parent, let's check that the attribute also exists
      if ((attr = desc.parent) !=  null)  {
        if (type.getFeatureByBaseName(attr)==null) {
          throw new AnalysisEngineProcessException(
              new Exception(String.format(
                  "Annotation mapping file: attribute '%s' of the type type '%s' does not exist", 
                                         attr, key)
                           ));        
        }
      }
    }
  }

  /**
   * Main CAS processing function, see {@link CasAnnotator_ImplBase#process(CAS)}.
   */
  @Override
  public void process(CAS aCAS) throws AnalysisEngineProcessException {
    JCas jcas = null;
    
    try {
        jcas = aCAS.getJCas();
    } catch (CASException e) {
        throw new AnalysisEngineProcessException(e);
    }
        
    String docId = null;


    XmlHelper xmlHlp = new XmlHelper();

    try {      
      JCas viewJCas = mViewName != null ? jcas.getView(mViewName) : jcas;
      
      FSIterator<Annotation> it = (FSIterator<Annotation>) 
                                JCasUtil.iterator(viewJCas, mDocInfoTypeClass);
      
      if (it.hasNext()) {
        Exception eMemorize = null;
        Annotation docInfo = it.next();
        try {
          docId = (String) mGetDoNoMethod.invoke(docInfo);
        } catch (IllegalAccessException e1) {
          eMemorize = e1;
        } catch (IllegalArgumentException e1) {
          eMemorize = e1;
        } catch (InvocationTargetException e1) {
          eMemorize = e1;
        }
        if (eMemorize != null) {
          throw new AnalysisEngineProcessException(
                      new Exception(
                            String.format("Cannot obtain document identifier from " +
                                          " the annotation, type '%s' " +
                                          " document number/id attribute '%s'",
                                          mDocInfoType, mDocNoGetter)));    
        }
      } else {
        throw new AnalysisEngineProcessException(new 
            Exception("Missing annotation of the type: '" + mDocInfoType + "'"));
      }
      
      if (it.hasNext()) {
        throw new AnalysisEngineProcessException(new 
            Exception("More than one annotation of the type: '" + mDocInfoType + "'"));
      }      

      /*
       *  Let's make a sanity check here, even though it's a bit expensive:
       *  The text in the view should match the text in the annotation field.
       */
      String docText = aCAS.getDocumentText();
      Map<String, String> docFields = xmlHlp.parseXMLIndexEntry(docText);
      String annotText = docFields.get(mTextFieldName);
      
      if (annotText == null) {
        throw new Exception("Missing field: " + mTextFieldName + 
                            " docId: " + docId);
      }
      String jcasText = viewJCas.getDocumentText();
      if (!annotText.equals(jcasText)) {
        System.err.println("Annotation (jcasView) text:");
        System.err.println(jcasText);
        System.err.println("====================================");
        System.err.println("Document text:");
        System.err.println(annotText);
        System.err.println("====================================");

        throw new Exception(String.format("Non-matching annotation texts for docId: %d " + docId + 
                            " view name: %s " + 
                            "text length: %d jcasView text length: %d ", docId, mViewName,  
                                          annotText.length(), jcasText.length()));        
      }
      
      String docNo = docFields.get(UtilConst.INDEX_DOCNO);
      if (docNo == null) {
        throw new Exception("Missing field: " + UtilConst.INDEX_DOCNO + 
                          " docId: " + docId);
      }
      
      writeText(docText);
      writeAnnotations(docNo, viewJCas);
    } catch (Exception e) {
      throw new AnalysisEngineProcessException(e);
    }
    
  }

  /**
   * A helper function that extracts annotations from CAS and stores them.
   * 
   * @param     docId   a unique document ID.
   * @param     jcas    a {@link org.apache.uima.jcas.JCAS} interface to the CAS.
   * 
   * @throws AnalysisEngineProcessException
   */
  private void writeAnnotations(String docId, JCas jcas) 
                                throws AnalysisEngineProcessException {    
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
        * This is to ensure that parentIds are 
        * written always before the child entries
        * that references their parents
        * 
        * TODO: implement a more efficient version for complex graph structure.
        * 
        * This algorithm is not the most efficient one.
        * Yet, it is complexity is O(qty * maximum length of the directed path),
        * which in many cases will be close to O(qty),
        * because the maximum length of the directed path in a annotation graph
        * is expected to be small.
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

  /**
   * Writes a text of the document that is already properly
   * formatted (i.e., has Indri TEXTTREC format).
   */
  private void writeText(String documentText) 
                         throws ParserConfigurationException, TransformerException, IOException {
    mTextFileWriter.write(documentText);
    mTextFileWriter.write('\n');
  }

  /**
   * Saves on annotation in the annotation offset file.
   * 
   * @param     docNo       unique document identifier.
   * @param     elem        an object of the type {@link OneAnnotationData} that
   *                        keeps annotation-related data.
   * @throws IOException
   */
  private void writeOneAnnotation(String docNo, 
                                  OneAnnotationData elem) throws IOException {
    annotFileWriter.write(docNo                     + "\t");
    annotFileWriter.write("TAG\t");
    annotFileWriter.write(elem.mId                  + "\t");
    annotFileWriter.write(elem.mFieldName           + "\t");
    annotFileWriter.write(elem.mStart               + "\t");
    annotFileWriter.write((elem.mEnd - elem.mStart) + "\t");
    annotFileWriter.write(elem.mTagValue            + "\t");
    annotFileWriter.write(elem.mParentId            + "\t");
    annotFileWriter.write(UtilConst.PATTERN_WHITESPACE.matcher(elem.mTextValue).
                          replaceAll(" ") + "\n");
  }

 /**
  * Called after we processed the entire collection, we close the 
  * output files here, see
  * also {@link CasAnnotator_ImplBase#collectionProcessComplete()}.
  */
  @Override
  public void collectionProcessComplete() throws AnalysisEngineProcessException {
    try {
      closeOutputFiles();
    } catch (IOException e) {
      throw new AnalysisEngineProcessException(e);
    }
  }

  /**
   * Closes output files.
   * 
   * @throws IOException
   */
  private void closeOutputFiles() throws IOException {
    if (annotFileWriter != null) {
      annotFileWriter.close();
    }
    if (mTextFileWriter != null) {
      mTextFileWriter.close();
    }
  }

 /**
  * Some clean up in the case of an emergency exit, see also
  * {@link org.apache.uima.resource.Resource#destroy()}.
  */
  public void destroy() {
    try {
      closeOutputFiles();
    } catch (IOException e) {
        // ignore IOException on destroy
    }
  }
    
  /**
   * A helper class to store the result of opening a file for writing, 
   * which comes as two related objects of the types File and OutputStream.
   */
  private class FilePair {
    public File         f;
    public OutputStream fw;

    public FilePair(File _f, OutputStream _fw) { f = _f; fw = _fw; } 
  }; 

  /**
   * Opens file for writing.
   * 
   * @param context             UIMA context variable.
   * @param fileParamName       A file name.
   * @return                    A an instance of the class {@link FilePair}.
   * @throws ResourceInitializationException
   */
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
}


