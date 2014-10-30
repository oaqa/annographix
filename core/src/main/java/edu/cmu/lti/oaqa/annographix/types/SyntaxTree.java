

/* First created by JCasGen Thu Oct 30 13:00:49 EDT 2014 */
package edu.cmu.lti.oaqa.annographix.types;

import org.apache.uima.jcas.JCas; 
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP_Type;

import org.apache.uima.jcas.tcas.Annotation;


/** A syntax tree (PENN style).
 * Updated by JCasGen Thu Oct 30 13:00:49 EDT 2014
 * XML source: /home/leo/SourceTreeGit/annographix/core/src/main/resources/types/MainTypes.xml
 *  */
public class SyntaxTree extends Annotation {
  /** 
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(SyntaxTree.class);
  /** 
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int type = typeIndexID;
  /**   */
  @Override
  public              int getTypeIndexID() {return typeIndexID;}
 
  /** Never called.  Disable default constructor
   *  */
  protected SyntaxTree() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   *  */
  public SyntaxTree(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /**  */
  public SyntaxTree(JCas jcas) {
    super(jcas);
    readObject();   
  } 

  /**  */  
  public SyntaxTree(JCas jcas, int begin, int end) {
    super(jcas);
    setBegin(begin);
    setEnd(end);
    readObject();
  }   

  /** <!-- begin-user-doc -->
    * Write your own initialization here
    * <!-- end-user-doc -->
   modifiable */
  private void readObject() {/*default - does nothing empty block */}
     
 
    
  //*--------------*
  //* Feature: Tree

  /** getter for Tree - gets A complete tree (in the bracketed format).
   *  */
  public String getTree() {
    if (SyntaxTree_Type.featOkTst && ((SyntaxTree_Type)jcasType).casFeat_Tree == null)
      jcasType.jcas.throwFeatMissing("Tree", "edu.cmu.lti.oaqa.annographix.types.SyntaxTree");
    return jcasType.ll_cas.ll_getStringValue(addr, ((SyntaxTree_Type)jcasType).casFeatCode_Tree);}
    
  /** setter for Tree - sets A complete tree (in the bracketed format). 
   *  */
  public void setTree(String v) {
    if (SyntaxTree_Type.featOkTst && ((SyntaxTree_Type)jcasType).casFeat_Tree == null)
      jcasType.jcas.throwFeatMissing("Tree", "edu.cmu.lti.oaqa.annographix.types.SyntaxTree");
    jcasType.ll_cas.ll_setStringValue(addr, ((SyntaxTree_Type)jcasType).casFeatCode_Tree, v);}    
  }

    
