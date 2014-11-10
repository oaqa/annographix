package edu.cmu.lti.oaqa.annographix.uima;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.CasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.*;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Level;

import edu.cmu.lti.oaqa.annographix.solr.*;
import edu.cmu.lti.oaqa.annographix.util.*;

public abstract class AnnotationConsumer extends CasAnnotator_ImplBase {

  /** 
   * A parameter to specify an annotation mapping file. 
   * This file defines which annotations and which annotation
   * attributes should be stored. The annotation mapping file
   * also specifies which parent-child links should be memorized. 
   */
  private static final String PARAM_MAPPING_FILE = "mapping_file";
  /** Result of parsing the annotation mapping file. */
  private Map<String, MappingReader.TypeDescr> mMappingConfig;
  /** A view (SOFA) name to be used to extract annotations */
  protected String mViewName;
  
  /** 
   * A name of the field (also the name of the tag in the XML file)
   * that keeps annotated text (but not the annotations themselves).
   */
  protected String          mTextFieldName;  
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
  protected static final String NL = System.getProperty("line.separator");
  
  /**
   * 
   * @param startValue
   * @throws Exception
   */
  public void resetAnnotationId(int startValue) throws Exception {
    mAnnotationId = startValue;
  };

  /**
   * Here we check that the user specified correct names of types and attributes.   
   */
  @Override
  public void typeSystemInit(TypeSystem typeSystem)
      throws AnalysisEngineProcessException {
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
      String attr = desc.valueAttr;
      if (attr != null // value attribute can be null
          && type.getFeatureByBaseName(attr)==null) {
        throw new AnalysisEngineProcessException(
            new Exception(String.format(
                "Annotation mapping file: attribute '%s' of the type type '%s' does not exist", 
                attr, key)
                ));
      }

      // 3. If we have a parent, let's check that the attribute also exists
      if ((attr = desc.parentAttr) !=  null)  {
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
   * Opens file for writing.
   * 
   * @param context             UIMA context variable.
   * @param fileParamName       A file name.
   * @return                    A an output stream.
   * @throws ResourceInitializationException
   */
  protected OutputStream initOutFile(UimaContext context, 
                                     String fileParamName) 
                                         throws ResourceInitializationException {
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
  /**
   * See {@link CasAnnotator_ImplBase#initialize(UimaContext)}.
   */
  @Override
  public void initialize(UimaContext context) throws ResourceInitializationException {
    super.initialize(context);
    
    mViewName = 
          (String) context.getConfigParameterValue(UtilConst.CONFIG_VIEW_NAME);
 
    if (mViewName == null)
      throw new ResourceInitializationException(
                    new Exception("Missing parameter: " + 
                                  UtilConst.CONFIG_VIEW_NAME));
        
    mTextFieldName = (String) 
          context.getConfigParameterValue(UtilConst.CONFIG_TEXT4ANNOT_FIELD);
    
    if (mTextFieldName == null)
    throw new ResourceInitializationException(
                new Exception("Missing parameter: " + 
                              UtilConst.CONFIG_TEXT4ANNOT_FIELD));       
        
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
   * A function that does all the CAS processing.
   * 
   * @param     viewJCas    a specified {@link org.apache.uima.jcas.JCas} view.
   * @param     docNo       a unique document ID.
   * @param     docText     a complete text of a 2-level indexable XML document.
   * @param     docFields   a complete key-value map of indexable fields,
   *                        which are extracted from the 2-level indexable
   *                        document.
   */
  protected abstract void doProcess(JCas viewJCas,
                                    String docNo,
                                    String docText,
                                    Map<String, String> docFields                                    
                                    )
      throws AnalysisEngineProcessException;
  /** 
   * A clean-up function that is called from the functions 
   * {@link #collectionProcessComplete()} and {@link #destroy()}.
   *  
   * @throws IOException
   */
  protected abstract void cleanUp() throws IOException;

  /**
   * Main CAS processing function, see {@link CasAnnotator_ImplBase#process(CAS)}.
   */
  @Override
  public void process(CAS aCAS) throws AnalysisEngineProcessException {
    JCas jcas = null, viewJCas;
    
    // mViewName != null            
    try {
        jcas = aCAS.getJCas();
        viewJCas = jcas.getView(mViewName);
        
        String docText = aCAS.getDocumentText();

        Map<String, String> docFields = XmlHelper.parseXMLIndexEntry(docText);
        
        String docNo = docFields.get(UtilConst.TAG_DOCNO);
        if (docNo == null) {
          throw new Exception("Missing field: " + UtilConst.TAG_DOCNO);
        }            
        
        doProcess(viewJCas, docNo, docText, docFields);
    } catch (Exception e) {
        throw new AnalysisEngineProcessException(e);
    }            
  }

  /**
   * A helper function that extracts annotations from a 
   * {@link org.apache.uima.jcas.JCas} object.
   * 
   * @param     jcas    a {@link org.apache.uima.jcas.JCas} object,
   *                    which represents a view containing annotations. 
   * @param     origAnnotList   a list of annotations to process
   * 
   * @throws AnalysisEngineProcessException
   */
  protected ArrayList<AnnotationProxy> generateAnnotations(
                                            JCas jcas,
                                            List<Annotation> origAnnotList) {
    /*
     *  First, obtain unique annotation IDs and
     *  memorize the relationship between annotation 
     *  pointers and IDs.
     */
     
     Map<Annotation,Integer>  annotIds = new HashMap<Annotation,Integer>();
     
     for(Annotation elem: origAnnotList) {           
       annotIds.put(elem, mAnnotationId++);
     }
  
     // Now that we have the IDs, let's serialize annotations
  
     ArrayList<AnnotationProxy>   annotList = new ArrayList<AnnotationProxy>();
  
     for (String key : mMappingConfig.keySet()) {
       Type                      type = jcas.getTypeSystem().getType(key);
       MappingReader.TypeDescr   desc = mMappingConfig.get(key); 
       
       FSIterator<Annotation> iter = jcas.getAnnotationIndex(type).iterator();
  
       while (iter.hasNext()) { 
         Annotation elem   = iter.next();
         
         // We don't need this annotation now
         if (!annotIds.containsKey(elem)) continue;
         
         String  labelValue = "";
         if (desc.valueAttr != null) {
           Feature feature = type.getFeatureByBaseName(desc.valueAttr);
           labelValue = elem.getFeatureValueAsString(feature);
         }
         
         int parentId = 0;
         
         if (desc.parentAttr != null) {
           
           FeatureStructure parent = (Annotation) 
                 elem.getFeatureValue(type.getFeatureByBaseName(desc.parentAttr)); 
               
           if (parent != null) parentId = annotIds.get((Annotation)parent);
         }
         
  
         annotList.add(new AnnotationProxy(annotIds.get(elem), 
                         UtilConst.combineFieldValue(
                             desc.fieldPrefix, labelValue),
                         elem.getBegin(), elem.getEnd(),
                         parentId,
                         elem.getCoveredText()));
       }
     }
     return annotList;
  }

  /**
    * Called after we processed the entire collection, we close the 
    * output files here, see
    * also {@link CasAnnotator_ImplBase#collectionProcessComplete()}.
    */
  @Override
  public void collectionProcessComplete()
      throws AnalysisEngineProcessException {
        try {
          cleanUp();
        } catch (IOException e) {
          throw new AnalysisEngineProcessException(e);
        }
      }

  /**
    * Some clean up in the case of an emergency exit, see also
    * {@link org.apache.uima.resource.Resource#destroy()}.
    */
  public void destroy() {
    try {
      cleanUp();
    } catch (IOException e) {
        // ignore IOException on destroy
    }
  }
}
