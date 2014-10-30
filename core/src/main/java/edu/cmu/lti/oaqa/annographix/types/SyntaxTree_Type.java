
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

/** A syntax tree (PENN style).
 * Updated by JCasGen Thu Oct 30 13:00:49 EDT 2014
 *  */
public class SyntaxTree_Type extends Annotation_Type {
  /**  */
  @Override
  protected FSGenerator getFSGenerator() {return fsGenerator;}
  /**  */
  private final FSGenerator fsGenerator = 
    new FSGenerator() {
      public FeatureStructure createFS(int addr, CASImpl cas) {
  			 if (SyntaxTree_Type.this.useExistingInstance) {
  			   // Return eq fs instance if already created
  		     FeatureStructure fs = SyntaxTree_Type.this.jcas.getJfsFromCaddr(addr);
  		     if (null == fs) {
  		       fs = new SyntaxTree(addr, SyntaxTree_Type.this);
  			   SyntaxTree_Type.this.jcas.putJfsFromCaddr(addr, fs);
  			   return fs;
  		     }
  		     return fs;
        } else return new SyntaxTree(addr, SyntaxTree_Type.this);
  	  }
    };
  /**  */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = SyntaxTree.typeIndexID;
  /**  
     @modifiable */
  @SuppressWarnings ("hiding")
  public final static boolean featOkTst = JCasRegistry.getFeatOkTst("edu.cmu.lti.oaqa.annographix.types.SyntaxTree");
 
  /**  */
  final Feature casFeat_Tree;
  /**  */
  final int     casFeatCode_Tree;
  /**  */ 
  public String getTree(int addr) {
        if (featOkTst && casFeat_Tree == null)
      jcas.throwFeatMissing("Tree", "edu.cmu.lti.oaqa.annographix.types.SyntaxTree");
    return ll_cas.ll_getStringValue(addr, casFeatCode_Tree);
  }
  /**  */    
  public void setTree(int addr, String v) {
        if (featOkTst && casFeat_Tree == null)
      jcas.throwFeatMissing("Tree", "edu.cmu.lti.oaqa.annographix.types.SyntaxTree");
    ll_cas.ll_setStringValue(addr, casFeatCode_Tree, v);}
    
  



  /** initialize variables to correspond with Cas Type and Features
	*  */
  public SyntaxTree_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl)this.casType, getFSGenerator());

 
    casFeat_Tree = jcas.getRequiredFeatureDE(casType, "Tree", "uima.cas.String", featOkTst);
    casFeatCode_Tree  = (null == casFeat_Tree) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_Tree).getCode();

  }
}



    
