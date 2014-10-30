
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

/** Part-Of-Speach (POS) tag.
 * Updated by JCasGen Thu Oct 30 13:00:49 EDT 2014
 *  */
public class POS_Type extends Annotation_Type {
  /**  */
  @Override
  protected FSGenerator getFSGenerator() {return fsGenerator;}
  /**  */
  private final FSGenerator fsGenerator = 
    new FSGenerator() {
      public FeatureStructure createFS(int addr, CASImpl cas) {
  			 if (POS_Type.this.useExistingInstance) {
  			   // Return eq fs instance if already created
  		     FeatureStructure fs = POS_Type.this.jcas.getJfsFromCaddr(addr);
  		     if (null == fs) {
  		       fs = new POS(addr, POS_Type.this);
  			   POS_Type.this.jcas.putJfsFromCaddr(addr, fs);
  			   return fs;
  		     }
  		     return fs;
        } else return new POS(addr, POS_Type.this);
  	  }
    };
  /**  */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = POS.typeIndexID;
  /**  
     @modifiable */
  @SuppressWarnings ("hiding")
  public final static boolean featOkTst = JCasRegistry.getFeatOkTst("edu.cmu.lti.oaqa.annographix.types.POS");
 
  /**  */
  final Feature casFeat_Tag;
  /**  */
  final int     casFeatCode_Tag;
  /**  */ 
  public String getTag(int addr) {
        if (featOkTst && casFeat_Tag == null)
      jcas.throwFeatMissing("Tag", "edu.cmu.lti.oaqa.annographix.types.POS");
    return ll_cas.ll_getStringValue(addr, casFeatCode_Tag);
  }
  /**  */    
  public void setTag(int addr, String v) {
        if (featOkTst && casFeat_Tag == null)
      jcas.throwFeatMissing("Tag", "edu.cmu.lti.oaqa.annographix.types.POS");
    ll_cas.ll_setStringValue(addr, casFeatCode_Tag, v);}
    
  



  /** initialize variables to correspond with Cas Type and Features
	*  */
  public POS_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl)this.casType, getFSGenerator());

 
    casFeat_Tag = jcas.getRequiredFeatureDE(casType, "Tag", "uima.cas.String", featOkTst);
    casFeatCode_Tag  = (null == casFeat_Tag) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_Tag).getCode();

  }
}



    
