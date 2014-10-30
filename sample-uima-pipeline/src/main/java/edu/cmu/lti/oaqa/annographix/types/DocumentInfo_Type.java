
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

/** Document information
 * Updated by JCasGen Wed Oct 29 22:25:36 EDT 2014
 * @generated */
public class DocumentInfo_Type extends Annotation_Type {
  /** @generated */
  @Override
  protected FSGenerator getFSGenerator() {return fsGenerator;}
  /** @generated */
  private final FSGenerator fsGenerator = 
    new FSGenerator() {
      public FeatureStructure createFS(int addr, CASImpl cas) {
  			 if (DocumentInfo_Type.this.useExistingInstance) {
  			   // Return eq fs instance if already created
  		     FeatureStructure fs = DocumentInfo_Type.this.jcas.getJfsFromCaddr(addr);
  		     if (null == fs) {
  		       fs = new DocumentInfo(addr, DocumentInfo_Type.this);
  			   DocumentInfo_Type.this.jcas.putJfsFromCaddr(addr, fs);
  			   return fs;
  		     }
  		     return fs;
        } else return new DocumentInfo(addr, DocumentInfo_Type.this);
  	  }
    };
  /** @generated */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = DocumentInfo.typeIndexID;
  /** @generated 
     @modifiable */
  @SuppressWarnings ("hiding")
  public final static boolean featOkTst = JCasRegistry.getFeatOkTst("edu.cmu.lti.oaqa.annographix.types.DocumentInfo");
 
  /** @generated */
  final Feature casFeat_DocNo;
  /** @generated */
  final int     casFeatCode_DocNo;
  /** @generated */ 
  public String getDocNo(int addr) {
        if (featOkTst && casFeat_DocNo == null)
      jcas.throwFeatMissing("DocNo", "edu.cmu.lti.oaqa.annographix.types.DocumentInfo");
    return ll_cas.ll_getStringValue(addr, casFeatCode_DocNo);
  }
  /** @generated */    
  public void setDocNo(int addr, String v) {
        if (featOkTst && casFeat_DocNo == null)
      jcas.throwFeatMissing("DocNo", "edu.cmu.lti.oaqa.annographix.types.DocumentInfo");
    ll_cas.ll_setStringValue(addr, casFeatCode_DocNo, v);}
    
  



  /** initialize variables to correspond with Cas Type and Features
	* @generated */
  public DocumentInfo_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl)this.casType, getFSGenerator());

 
    casFeat_DocNo = jcas.getRequiredFeatureDE(casType, "DocNo", "uima.cas.String", featOkTst);
    casFeatCode_DocNo  = (null == casFeat_DocNo) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_DocNo).getCode();

  }
}



    