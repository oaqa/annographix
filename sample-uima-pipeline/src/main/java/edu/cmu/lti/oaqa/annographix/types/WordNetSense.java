

/* First created by JCasGen Wed Oct 29 22:25:36 EDT 2014 */
package edu.cmu.lti.oaqa.annographix.types;

import org.apache.uima.jcas.JCas; 
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP_Type;

import org.apache.uima.jcas.tcas.Annotation;


/** Encoded WordNet sense for the word or some of it related synsets.
 * Updated by JCasGen Wed Oct 29 22:25:36 EDT 2014
 * XML source: /home/leo/SourceTreeGit/annographix/sample-uima-pipeline/src/main/resources/types/MainTypes.xml
 * @generated */
public class WordNetSense extends Annotation {
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(WordNetSense.class);
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
  protected WordNetSense() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * @generated */
  public WordNetSense(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** @generated */
  public WordNetSense(JCas jcas) {
    super(jcas);
    readObject();   
  } 

  /** @generated */  
  public WordNetSense(JCas jcas, int begin, int end) {
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
  //* Feature: Sense

  /** getter for Sense - gets Sense name, e.g., female_person_n_18_0.
   * @generated */
  public String getSense() {
    if (WordNetSense_Type.featOkTst && ((WordNetSense_Type)jcasType).casFeat_Sense == null)
      jcasType.jcas.throwFeatMissing("Sense", "edu.cmu.lti.oaqa.annographix.types.WordNetSense");
    return jcasType.ll_cas.ll_getStringValue(addr, ((WordNetSense_Type)jcasType).casFeatCode_Sense);}
    
  /** setter for Sense - sets Sense name, e.g., female_person_n_18_0. 
   * @generated */
  public void setSense(String v) {
    if (WordNetSense_Type.featOkTst && ((WordNetSense_Type)jcasType).casFeat_Sense == null)
      jcasType.jcas.throwFeatMissing("Sense", "edu.cmu.lti.oaqa.annographix.types.WordNetSense");
    jcasType.ll_cas.ll_setStringValue(addr, ((WordNetSense_Type)jcasType).casFeatCode_Sense, v);}    
   
    
  //*--------------*
  //* Feature: Parent

  /** getter for Parent - gets Link to a parent (containing) node
   * @generated */
  public Annotation getParent() {
    if (WordNetSense_Type.featOkTst && ((WordNetSense_Type)jcasType).casFeat_Parent == null)
      jcasType.jcas.throwFeatMissing("Parent", "edu.cmu.lti.oaqa.annographix.types.WordNetSense");
    return (Annotation)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((WordNetSense_Type)jcasType).casFeatCode_Parent)));}
    
  /** setter for Parent - sets Link to a parent (containing) node 
   * @generated */
  public void setParent(Annotation v) {
    if (WordNetSense_Type.featOkTst && ((WordNetSense_Type)jcasType).casFeat_Parent == null)
      jcasType.jcas.throwFeatMissing("Parent", "edu.cmu.lti.oaqa.annographix.types.WordNetSense");
    jcasType.ll_cas.ll_setRefValue(addr, ((WordNetSense_Type)jcasType).casFeatCode_Parent, jcasType.ll_cas.ll_getFSRef(v));}    
  }

    