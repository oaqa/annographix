package edu.cmu.lti.oaqa.annographix.uima;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.CasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Level;

import edu.cmu.lti.oaqa.annographix.solr.AnnotationProxy;
import edu.cmu.lti.oaqa.annographix.solr.UtilConst;

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
  private String mViewName;
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
  
  /** A function that does all the CAS processing */
  protected abstract void doProcess(String docText, JCas jcas, JCas viewJCas) 
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
    } catch (CASException e) {
        throw new AnalysisEngineProcessException(e);
    }
  
    doProcess(aCAS.getDocumentText(), jcas, viewJCas);            
  }

  /**
   * A helper function that extracts annotations from a 
   * {@link org.apache.uima.jcas.JCAS} object.
   * 
   * @param     docId   a unique document ID.
   * @param     jcas    a {@link org.apache.uima.jcas.JCAS} interface to the CAS.
   * 
   * @throws AnalysisEngineProcessException
   */
  protected ArrayList<AnnotationProxy> generateAnnotations(JCas jcas) {
    /*
     *  First, obtain unique annotation IDs and
     *  memorize the relationship between annotation 
     *  pointers and IDs.
     */
     
     Map<Annotation,Integer>  annotIds = new HashMap<Annotation,Integer>();
     
     FSIterator<Annotation> iter = 
                         jcas.getAnnotationIndex(Annotation.type).iterator();
     
     while (iter.hasNext()) {
       Annotation elem = iter.next();           
       annotIds.put(elem, mAnnotationId++);
     }
  
     // Now that we have the IDs, let's serialize annotations
  
     ArrayList<AnnotationProxy>   annotList = new ArrayList<AnnotationProxy>();
  
     for (String key : mMappingConfig.keySet()) {
       Type                      type = jcas.getTypeSystem().getType(key);
       MappingReader.TypeDescr   desc = mMappingConfig.get(key); 
       
       iter = jcas.getAnnotationIndex(type).iterator();
  
       while (iter.hasNext()) { 
         Annotation elem   = iter.next();
         
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
  
  protected String getViewName() { return mViewName; }
}