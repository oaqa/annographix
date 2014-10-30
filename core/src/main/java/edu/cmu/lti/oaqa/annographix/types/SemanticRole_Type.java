
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

/** An ASSERT predicate-target-argument annotation.  See the Feature attribute to find out more.
 * Updated by JCasGen Thu Oct 30 13:00:49 EDT 2014
 *  */
public class SemanticRole_Type extends Annotation_Type {
  /**  */
  @Override
  protected FSGenerator getFSGenerator() {return fsGenerator;}
  /**  */
  private final FSGenerator fsGenerator = 
    new FSGenerator() {
      public FeatureStructure createFS(int addr, CASImpl cas) {
  			 if (SemanticRole_Type.this.useExistingInstance) {
  			   // Return eq fs instance if already created
  		     FeatureStructure fs = SemanticRole_Type.this.jcas.getJfsFromCaddr(addr);
  		     if (null == fs) {
  		       fs = new SemanticRole(addr, SemanticRole_Type.this);
  			   SemanticRole_Type.this.jcas.putJfsFromCaddr(addr, fs);
  			   return fs;
  		     }
  		     return fs;
        } else return new SemanticRole(addr, SemanticRole_Type.this);
  	  }
    };
  /**  */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = SemanticRole.typeIndexID;
  /**  
     @modifiable */
  @SuppressWarnings ("hiding")
  public final static boolean featOkTst = JCasRegistry.getFeatOkTst("edu.cmu.lti.oaqa.annographix.types.SemanticRole");
 
  /**  */
  final Feature casFeat_Label;
  /**  */
  final int     casFeatCode_Label;
  /**  */ 
  public String getLabel(int addr) {
        if (featOkTst && casFeat_Label == null)
      jcas.throwFeatMissing("Label", "edu.cmu.lti.oaqa.annographix.types.SemanticRole");
    return ll_cas.ll_getStringValue(addr, casFeatCode_Label);
  }
  /**  */    
  public void setLabel(int addr, String v) {
        if (featOkTst && casFeat_Label == null)
      jcas.throwFeatMissing("Label", "edu.cmu.lti.oaqa.annographix.types.SemanticRole");
    ll_cas.ll_setStringValue(addr, casFeatCode_Label, v);}
    
  
 
  /**  */
  final Feature casFeat_Parent;
  /**  */
  final int     casFeatCode_Parent;
  /**  */ 
  public int getParent(int addr) {
        if (featOkTst && casFeat_Parent == null)
      jcas.throwFeatMissing("Parent", "edu.cmu.lti.oaqa.annographix.types.SemanticRole");
    return ll_cas.ll_getRefValue(addr, casFeatCode_Parent);
  }
  /**  */    
  public void setParent(int addr, int v) {
        if (featOkTst && casFeat_Parent == null)
      jcas.throwFeatMissing("Parent", "edu.cmu.lti.oaqa.annographix.types.SemanticRole");
    ll_cas.ll_setRefValue(addr, casFeatCode_Parent, v);}
    
  
 
  /**  */
  final Feature casFeat_Children;
  /**  */
  final int     casFeatCode_Children;
  /**  */ 
  public int getChildren(int addr) {
        if (featOkTst && casFeat_Children == null)
      jcas.throwFeatMissing("Children", "edu.cmu.lti.oaqa.annographix.types.SemanticRole");
    return ll_cas.ll_getRefValue(addr, casFeatCode_Children);
  }
  /**  */    
  public void setChildren(int addr, int v) {
        if (featOkTst && casFeat_Children == null)
      jcas.throwFeatMissing("Children", "edu.cmu.lti.oaqa.annographix.types.SemanticRole");
    ll_cas.ll_setRefValue(addr, casFeatCode_Children, v);}
    
   /**  */
  public int getChildren(int addr, int i) {
        if (featOkTst && casFeat_Children == null)
      jcas.throwFeatMissing("Children", "edu.cmu.lti.oaqa.annographix.types.SemanticRole");
    if (lowLevelTypeChecks)
      return ll_cas.ll_getRefArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_Children), i, true);
    jcas.checkArrayBounds(ll_cas.ll_getRefValue(addr, casFeatCode_Children), i);
	return ll_cas.ll_getRefArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_Children), i);
  }
   
  /**  */ 
  public void setChildren(int addr, int i, int v) {
        if (featOkTst && casFeat_Children == null)
      jcas.throwFeatMissing("Children", "edu.cmu.lti.oaqa.annographix.types.SemanticRole");
    if (lowLevelTypeChecks)
      ll_cas.ll_setRefArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_Children), i, v, true);
    jcas.checkArrayBounds(ll_cas.ll_getRefValue(addr, casFeatCode_Children), i);
    ll_cas.ll_setRefArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_Children), i, v);
  }
 



  /** initialize variables to correspond with Cas Type and Features
	*  */
  public SemanticRole_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl)this.casType, getFSGenerator());

 
    casFeat_Label = jcas.getRequiredFeatureDE(casType, "Label", "uima.cas.String", featOkTst);
    casFeatCode_Label  = (null == casFeat_Label) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_Label).getCode();

 
    casFeat_Parent = jcas.getRequiredFeatureDE(casType, "Parent", "edu.cmu.lti.oaqa.annographix.types.SemanticRole", featOkTst);
    casFeatCode_Parent  = (null == casFeat_Parent) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_Parent).getCode();

 
    casFeat_Children = jcas.getRequiredFeatureDE(casType, "Children", "uima.cas.FSArray", featOkTst);
    casFeatCode_Children  = (null == casFeat_Children) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_Children).getCode();

  }
}



    
