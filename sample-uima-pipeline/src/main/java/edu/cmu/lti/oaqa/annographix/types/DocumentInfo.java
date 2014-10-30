

/* First created by JCasGen Wed Oct 29 22:25:36 EDT 2014 */
package edu.cmu.lti.oaqa.annographix.types;

import org.apache.uima.jcas.JCas; 
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP_Type;

import org.apache.uima.jcas.tcas.Annotation;


/** Document information
 * Updated by JCasGen Wed Oct 29 22:25:36 EDT 2014
 * XML source: /home/leo/SourceTreeGit/annographix/sample-uima-pipeline/src/main/resources/types/MainTypes.xml
 * @generated */
public class DocumentInfo extends Annotation {
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(DocumentInfo.class);
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
  protected DocumentInfo() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * @generated */
  public DocumentInfo(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** @generated */
  public DocumentInfo(JCas jcas) {
    super(jcas);
    readObject();   
  } 

  /** @generated */  
  public DocumentInfo(JCas jcas, int begin, int end) {
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
  //* Feature: DocNo

  /** getter for DocNo - gets Document id/number.
   * @generated */
  public String getDocNo() {
    if (DocumentInfo_Type.featOkTst && ((DocumentInfo_Type)jcasType).casFeat_DocNo == null)
      jcasType.jcas.throwFeatMissing("DocNo", "edu.cmu.lti.oaqa.annographix.types.DocumentInfo");
    return jcasType.ll_cas.ll_getStringValue(addr, ((DocumentInfo_Type)jcasType).casFeatCode_DocNo);}
    
  /** setter for DocNo - sets Document id/number. 
   * @generated */
  public void setDocNo(String v) {
    if (DocumentInfo_Type.featOkTst && ((DocumentInfo_Type)jcasType).casFeat_DocNo == null)
      jcasType.jcas.throwFeatMissing("DocNo", "edu.cmu.lti.oaqa.annographix.types.DocumentInfo");
    jcasType.ll_cas.ll_setStringValue(addr, ((DocumentInfo_Type)jcasType).casFeatCode_DocNo, v);}    
  }

    