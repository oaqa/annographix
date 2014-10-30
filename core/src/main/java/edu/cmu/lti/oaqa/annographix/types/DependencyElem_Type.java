
/* First created by JCasGen Thu Oct 30 13:00:49 EDT 2014 */
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

/** A Stanford dependency.
 * Updated by JCasGen Thu Oct 30 13:00:49 EDT 2014
 *  */
public class DependencyElem_Type extends Annotation_Type {
  /**  */
  @Override
  protected FSGenerator getFSGenerator() {return fsGenerator;}
  /**  */
  private final FSGenerator fsGenerator = 
    new FSGenerator() {
      public FeatureStructure createFS(int addr, CASImpl cas) {
  			 if (DependencyElem_Type.this.useExistingInstance) {
  			   // Return eq fs instance if already created
  		     FeatureStructure fs = DependencyElem_Type.this.jcas.getJfsFromCaddr(addr);
  		     if (null == fs) {
  		       fs = new DependencyElem(addr, DependencyElem_Type.this);
  			   DependencyElem_Type.this.jcas.putJfsFromCaddr(addr, fs);
  			   return fs;
  		     }
  		     return fs;
        } else return new DependencyElem(addr, DependencyElem_Type.this);
  	  }
    };
  /**  */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = DependencyElem.typeIndexID;
  /**  
     @modifiable */
  @SuppressWarnings ("hiding")
  public final static boolean featOkTst = JCasRegistry.getFeatOkTst("edu.cmu.lti.oaqa.annographix.types.DependencyElem");
 
  /**  */
  final Feature casFeat_DepType;
  /**  */
  final int     casFeatCode_DepType;
  /**  */ 
  public String getDepType(int addr) {
        if (featOkTst && casFeat_DepType == null)
      jcas.throwFeatMissing("DepType", "edu.cmu.lti.oaqa.annographix.types.DependencyElem");
    return ll_cas.ll_getStringValue(addr, casFeatCode_DepType);
  }
  /**  */    
  public void setDepType(int addr, String v) {
        if (featOkTst && casFeat_DepType == null)
      jcas.throwFeatMissing("DepType", "edu.cmu.lti.oaqa.annographix.types.DependencyElem");
    ll_cas.ll_setStringValue(addr, casFeatCode_DepType, v);}
    
  
 
  /**  */
  final Feature casFeat_Parent;
  /**  */
  final int     casFeatCode_Parent;
  /**  */ 
  public int getParent(int addr) {
        if (featOkTst && casFeat_Parent == null)
      jcas.throwFeatMissing("Parent", "edu.cmu.lti.oaqa.annographix.types.DependencyElem");
    return ll_cas.ll_getRefValue(addr, casFeatCode_Parent);
  }
  /**  */    
  public void setParent(int addr, int v) {
        if (featOkTst && casFeat_Parent == null)
      jcas.throwFeatMissing("Parent", "edu.cmu.lti.oaqa.annographix.types.DependencyElem");
    ll_cas.ll_setRefValue(addr, casFeatCode_Parent, v);}
    
  



  /** initialize variables to correspond with Cas Type and Features
	*  */
  public DependencyElem_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl)this.casType, getFSGenerator());

 
    casFeat_DepType = jcas.getRequiredFeatureDE(casType, "DepType", "uima.cas.String", featOkTst);
    casFeatCode_DepType  = (null == casFeat_DepType) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_DepType).getCode();

 
    casFeat_Parent = jcas.getRequiredFeatureDE(casType, "Parent", "edu.cmu.lti.oaqa.annographix.types.DependencyElem", featOkTst);
    casFeatCode_Parent  = (null == casFeat_Parent) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_Parent).getCode();

  }
}



    
