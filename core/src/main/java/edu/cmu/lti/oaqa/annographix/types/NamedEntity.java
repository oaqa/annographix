

/* First created by JCasGen Thu Oct 30 13:00:49 EDT 2014 */
package edu.cmu.lti.oaqa.annographix.types;

import org.apache.uima.jcas.JCas; 
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP_Type;

import org.apache.uima.jcas.tcas.Annotation;


/** Named entity.
 * Updated by JCasGen Thu Oct 30 13:00:49 EDT 2014
 * XML source: /home/leo/SourceTreeGit/annographix/core/src/main/resources/types/MainTypes.xml
 *  */
public class NamedEntity extends Annotation {
  /** 
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(NamedEntity.class);
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
  protected NamedEntity() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   *  */
  public NamedEntity(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /**  */
  public NamedEntity(JCas jcas) {
    super(jcas);
    readObject();   
  } 

  /**  */  
  public NamedEntity(JCas jcas, int begin, int end) {
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
  //* Feature: EntityType

  /** getter for EntityType - gets A named entity type, such as ORG, LOC, or PER.
   *  */
  public String getEntityType() {
    if (NamedEntity_Type.featOkTst && ((NamedEntity_Type)jcasType).casFeat_EntityType == null)
      jcasType.jcas.throwFeatMissing("EntityType", "edu.cmu.lti.oaqa.annographix.types.NamedEntity");
    return jcasType.ll_cas.ll_getStringValue(addr, ((NamedEntity_Type)jcasType).casFeatCode_EntityType);}
    
  /** setter for EntityType - sets A named entity type, such as ORG, LOC, or PER. 
   *  */
  public void setEntityType(String v) {
    if (NamedEntity_Type.featOkTst && ((NamedEntity_Type)jcasType).casFeat_EntityType == null)
      jcasType.jcas.throwFeatMissing("EntityType", "edu.cmu.lti.oaqa.annographix.types.NamedEntity");
    jcasType.ll_cas.ll_setStringValue(addr, ((NamedEntity_Type)jcasType).casFeatCode_EntityType, v);}    
   
    
  //*--------------*
  //* Feature: Parent

  /** getter for Parent - gets Link to a parent (containing) node
   *  */
  public Annotation getParent() {
    if (NamedEntity_Type.featOkTst && ((NamedEntity_Type)jcasType).casFeat_Parent == null)
      jcasType.jcas.throwFeatMissing("Parent", "edu.cmu.lti.oaqa.annographix.types.NamedEntity");
    return (Annotation)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((NamedEntity_Type)jcasType).casFeatCode_Parent)));}
    
  /** setter for Parent - sets Link to a parent (containing) node 
   *  */
  public void setParent(Annotation v) {
    if (NamedEntity_Type.featOkTst && ((NamedEntity_Type)jcasType).casFeat_Parent == null)
      jcasType.jcas.throwFeatMissing("Parent", "edu.cmu.lti.oaqa.annographix.types.NamedEntity");
    jcasType.ll_cas.ll_setRefValue(addr, ((NamedEntity_Type)jcasType).casFeatCode_Parent, jcasType.ll_cas.ll_getFSRef(v));}    
  }

    
