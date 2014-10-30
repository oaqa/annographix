

/* First created by JCasGen Wed Oct 29 22:25:36 EDT 2014 */
package edu.cmu.lti.oaqa.annographix.types;

import org.apache.uima.jcas.JCas; 
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP_Type;

import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.tcas.Annotation;


/** An ASSERT predicate-target-argument annotation.  See the Feature attribute to find out more.
 * Updated by JCasGen Wed Oct 29 22:25:36 EDT 2014
 * XML source: /home/leo/SourceTreeGit/annographix/sample-uima-pipeline/src/main/resources/types/MainTypes.xml
 * @generated */
public class SemanticRole extends Annotation {
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(SemanticRole.class);
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
  protected SemanticRole() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * @generated */
  public SemanticRole(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** @generated */
  public SemanticRole(JCas jcas) {
    super(jcas);
    readObject();   
  } 

  /** @generated */  
  public SemanticRole(JCas jcas, int begin, int end) {
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
  //* Feature: Label

  /** getter for Label - gets Semantic role played by the predicate argument or target
   * @generated */
  public String getLabel() {
    if (SemanticRole_Type.featOkTst && ((SemanticRole_Type)jcasType).casFeat_Label == null)
      jcasType.jcas.throwFeatMissing("Label", "edu.cmu.lti.oaqa.annographix.types.SemanticRole");
    return jcasType.ll_cas.ll_getStringValue(addr, ((SemanticRole_Type)jcasType).casFeatCode_Label);}
    
  /** setter for Label - sets Semantic role played by the predicate argument or target 
   * @generated */
  public void setLabel(String v) {
    if (SemanticRole_Type.featOkTst && ((SemanticRole_Type)jcasType).casFeat_Label == null)
      jcasType.jcas.throwFeatMissing("Label", "edu.cmu.lti.oaqa.annographix.types.SemanticRole");
    jcasType.ll_cas.ll_setStringValue(addr, ((SemanticRole_Type)jcasType).casFeatCode_Label, v);}    
   
    
  //*--------------*
  //* Feature: Parent

  /** getter for Parent - gets Link to parent ("TARGET") role annotation
   * @generated */
  public SemanticRole getParent() {
    if (SemanticRole_Type.featOkTst && ((SemanticRole_Type)jcasType).casFeat_Parent == null)
      jcasType.jcas.throwFeatMissing("Parent", "edu.cmu.lti.oaqa.annographix.types.SemanticRole");
    return (SemanticRole)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((SemanticRole_Type)jcasType).casFeatCode_Parent)));}
    
  /** setter for Parent - sets Link to parent ("TARGET") role annotation 
   * @generated */
  public void setParent(SemanticRole v) {
    if (SemanticRole_Type.featOkTst && ((SemanticRole_Type)jcasType).casFeat_Parent == null)
      jcasType.jcas.throwFeatMissing("Parent", "edu.cmu.lti.oaqa.annographix.types.SemanticRole");
    jcasType.ll_cas.ll_setRefValue(addr, ((SemanticRole_Type)jcasType).casFeatCode_Parent, jcasType.ll_cas.ll_getFSRef(v));}    
   
    
  //*--------------*
  //* Feature: Children

  /** getter for Children - gets 
   * @generated */
  public FSArray getChildren() {
    if (SemanticRole_Type.featOkTst && ((SemanticRole_Type)jcasType).casFeat_Children == null)
      jcasType.jcas.throwFeatMissing("Children", "edu.cmu.lti.oaqa.annographix.types.SemanticRole");
    return (FSArray)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((SemanticRole_Type)jcasType).casFeatCode_Children)));}
    
  /** setter for Children - sets  
   * @generated */
  public void setChildren(FSArray v) {
    if (SemanticRole_Type.featOkTst && ((SemanticRole_Type)jcasType).casFeat_Children == null)
      jcasType.jcas.throwFeatMissing("Children", "edu.cmu.lti.oaqa.annographix.types.SemanticRole");
    jcasType.ll_cas.ll_setRefValue(addr, ((SemanticRole_Type)jcasType).casFeatCode_Children, jcasType.ll_cas.ll_getFSRef(v));}    
    
  /** indexed getter for Children - gets an indexed value - 
   * @generated */
  public SemanticRole getChildren(int i) {
    if (SemanticRole_Type.featOkTst && ((SemanticRole_Type)jcasType).casFeat_Children == null)
      jcasType.jcas.throwFeatMissing("Children", "edu.cmu.lti.oaqa.annographix.types.SemanticRole");
    jcasType.jcas.checkArrayBounds(jcasType.ll_cas.ll_getRefValue(addr, ((SemanticRole_Type)jcasType).casFeatCode_Children), i);
    return (SemanticRole)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefArrayValue(jcasType.ll_cas.ll_getRefValue(addr, ((SemanticRole_Type)jcasType).casFeatCode_Children), i)));}

  /** indexed setter for Children - sets an indexed value - 
   * @generated */
  public void setChildren(int i, SemanticRole v) { 
    if (SemanticRole_Type.featOkTst && ((SemanticRole_Type)jcasType).casFeat_Children == null)
      jcasType.jcas.throwFeatMissing("Children", "edu.cmu.lti.oaqa.annographix.types.SemanticRole");
    jcasType.jcas.checkArrayBounds(jcasType.ll_cas.ll_getRefValue(addr, ((SemanticRole_Type)jcasType).casFeatCode_Children), i);
    jcasType.ll_cas.ll_setRefArrayValue(jcasType.ll_cas.ll_getRefValue(addr, ((SemanticRole_Type)jcasType).casFeatCode_Children), i, jcasType.ll_cas.ll_getFSRef(v));}
  }

    