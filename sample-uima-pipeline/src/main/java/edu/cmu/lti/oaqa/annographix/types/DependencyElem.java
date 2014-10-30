

/* First created by JCasGen Wed Oct 29 22:25:36 EDT 2014 */
package edu.cmu.lti.oaqa.annographix.types;

import org.apache.uima.jcas.JCas; 
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP_Type;

import org.apache.uima.jcas.tcas.Annotation;


/** A Stanford dependency.
 * Updated by JCasGen Wed Oct 29 22:25:36 EDT 2014
 * XML source: /home/leo/SourceTreeGit/annographix/sample-uima-pipeline/src/main/resources/types/MainTypes.xml
 * @generated */
public class DependencyElem extends Annotation {
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(DependencyElem.class);
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int type = typeIndexID;
  /** @generated  */
  @Override
  public              int getTypeIndexID() {return typeIndexID;}
 
  /** Never called.  Disable default constructor
   * @generated */
  protected DependencyElem() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * @generated */
  public DependencyElem(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** @generated */
  public DependencyElem(JCas jcas) {
    super(jcas);
    readObject();   
  } 

  /** @generated */  
  public DependencyElem(JCas jcas, int begin, int end) {
    super(jcas);
    setBegin(begin);
    setEnd(end);
    readObject();
  }   

  /** <!-- begin-user-doc -->
    * Write your own initialization here
    * <!-- end-user-doc -->
  @generated modifiable */
  private void readObject() {/*default - does nothing empty block */}
     
 
    
  //*--------------*
  //* Feature: DepType

  /** getter for DepType - gets A dependency type
   * @generated */
  public String getDepType() {
    if (DependencyElem_Type.featOkTst && ((DependencyElem_Type)jcasType).casFeat_DepType == null)
      jcasType.jcas.throwFeatMissing("DepType", "edu.cmu.lti.oaqa.annographix.types.DependencyElem");
    return jcasType.ll_cas.ll_getStringValue(addr, ((DependencyElem_Type)jcasType).casFeatCode_DepType);}
    
  /** setter for DepType - sets A dependency type 
   * @generated */
  public void setDepType(String v) {
    if (DependencyElem_Type.featOkTst && ((DependencyElem_Type)jcasType).casFeat_DepType == null)
      jcasType.jcas.throwFeatMissing("DepType", "edu.cmu.lti.oaqa.annographix.types.DependencyElem");
    jcasType.ll_cas.ll_setStringValue(addr, ((DependencyElem_Type)jcasType).casFeatCode_DepType, v);}    
   
    
  //*--------------*
  //* Feature: Parent

  /** getter for Parent - gets Link to parent (governor) node
   * @generated */
  public DependencyElem getParent() {
    if (DependencyElem_Type.featOkTst && ((DependencyElem_Type)jcasType).casFeat_Parent == null)
      jcasType.jcas.throwFeatMissing("Parent", "edu.cmu.lti.oaqa.annographix.types.DependencyElem");
    return (DependencyElem)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((DependencyElem_Type)jcasType).casFeatCode_Parent)));}
    
  /** setter for Parent - sets Link to parent (governor) node 
   * @generated */
  public void setParent(DependencyElem v) {
    if (DependencyElem_Type.featOkTst && ((DependencyElem_Type)jcasType).casFeat_Parent == null)
      jcasType.jcas.throwFeatMissing("Parent", "edu.cmu.lti.oaqa.annographix.types.DependencyElem");
    jcasType.ll_cas.ll_setRefValue(addr, ((DependencyElem_Type)jcasType).casFeatCode_Parent, jcasType.ll_cas.ll_getFSRef(v));}    
  }

    