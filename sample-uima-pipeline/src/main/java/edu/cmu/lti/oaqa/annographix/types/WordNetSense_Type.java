
/* First created by JCasGen Wed Oct 29 22:25:36 EDT 2014 */
package edu.cmu.lti.oaqa.annographix.types;

import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.FSGenerator;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.FeatureImpl;
import org.apache.uima.cas.Feature;
import org.apache.uima.jcas.tcas.Annotation_Type;

/** Encoded WordNet sense for the word or some of it related synsets.
 * Updated by JCasGen Wed Oct 29 22:25:36 EDT 2014
 * @generated */
public class WordNetSense_Type extends Annotation_Type {
  /** @generated */
  @Override
  protected FSGenerator getFSGenerator() {return fsGenerator;}
  /** @generated */
  private final FSGenerator fsGenerator = 
    new FSGenerator() {
      public FeatureStructure createFS(int addr, CASImpl cas) {
  			 if (WordNetSense_Type.this.useExistingInstance) {
  			   // Return eq fs instance if already created
  		     FeatureStructure fs = WordNetSense_Type.this.jcas.getJfsFromCaddr(addr);
  		     if (null == fs) {
  		       fs = new WordNetSense(addr, WordNetSense_Type.this);
  			   WordNetSense_Type.this.jcas.putJfsFromCaddr(addr, fs);
  			   return fs;
  		     }
  		     return fs;
        } else return new WordNetSense(addr, WordNetSense_Type.this);
  	  }
    };
  /** @generated */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = WordNetSense.typeIndexID;
  /** @generated 
     @modifiable */
  @SuppressWarnings ("hiding")
  public final static boolean featOkTst = JCasRegistry.getFeatOkTst("edu.cmu.lti.oaqa.annographix.types.WordNetSense");
 
  /** @generated */
  final Feature casFeat_Sense;
  /** @generated */
  final int     casFeatCode_Sense;
  /** @generated */ 
  public String getSense(int addr) {
        if (featOkTst && casFeat_Sense == null)
      jcas.throwFeatMissing("Sense", "edu.cmu.lti.oaqa.annographix.types.WordNetSense");
    return ll_cas.ll_getStringValue(addr, casFeatCode_Sense);
  }
  /** @generated */    
  public void setSense(int addr, String v) {
        if (featOkTst && casFeat_Sense == null)
      jcas.throwFeatMissing("Sense", "edu.cmu.lti.oaqa.annographix.types.WordNetSense");
    ll_cas.ll_setStringValue(addr, casFeatCode_Sense, v);}
    
  
 
  /** @generated */
  final Feature casFeat_Parent;
  /** @generated */
  final int     casFeatCode_Parent;
  /** @generated */ 
  public int getParent(int addr) {
        if (featOkTst && casFeat_Parent == null)
      jcas.throwFeatMissing("Parent", "edu.cmu.lti.oaqa.annographix.types.WordNetSense");
    return ll_cas.ll_getRefValue(addr, casFeatCode_Parent);
  }
  /** @generated */    
  public void setParent(int addr, int v) {
        if (featOkTst && casFeat_Parent == null)
      jcas.throwFeatMissing("Parent", "edu.cmu.lti.oaqa.annographix.types.WordNetSense");
    ll_cas.ll_setRefValue(addr, casFeatCode_Parent, v);}
    
  



  /** initialize variables to correspond with Cas Type and Features
	* @generated */
  public WordNetSense_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl)this.casType, getFSGenerator());

 
    casFeat_Sense = jcas.getRequiredFeatureDE(casType, "Sense", "uima.cas.String", featOkTst);
    casFeatCode_Sense  = (null == casFeat_Sense) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_Sense).getCode();

 
    casFeat_Parent = jcas.getRequiredFeatureDE(casType, "Parent", "uima.tcas.Annotation", featOkTst);
    casFeatCode_Parent  = (null == casFeat_Parent) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_Parent).getCode();

  }
}



    