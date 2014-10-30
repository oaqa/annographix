

/* First created by JCasGen Thu Oct 30 13:00:49 EDT 2014 */
package edu.cmu.lti.oaqa.annographix.types;

import org.apache.uima.jcas.JCas; 
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP_Type;

import org.apache.uima.jcas.tcas.Annotation;


/** 
 * Updated by JCasGen Thu Oct 30 13:00:49 EDT 2014
 * XML source: /home/leo/SourceTreeGit/annographix/core/src/main/resources/types/MainTypes.xml
 *  */
public class Chunk extends Annotation {
  /** 
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(Chunk.class);
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
  protected Chunk() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   *  */
  public Chunk(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /**  */
  public Chunk(JCas jcas) {
    super(jcas);
    readObject();   
  } 

  /**  */  
  public Chunk(JCas jcas, int begin, int end) {
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
  //* Feature: ChunkType

  /** getter for ChunkType - gets A chunk type, e.g., NP or VP.
   *  */
  public String getChunkType() {
    if (Chunk_Type.featOkTst && ((Chunk_Type)jcasType).casFeat_ChunkType == null)
      jcasType.jcas.throwFeatMissing("ChunkType", "edu.cmu.lti.oaqa.annographix.types.Chunk");
    return jcasType.ll_cas.ll_getStringValue(addr, ((Chunk_Type)jcasType).casFeatCode_ChunkType);}
    
  /** setter for ChunkType - sets A chunk type, e.g., NP or VP. 
   *  */
  public void setChunkType(String v) {
    if (Chunk_Type.featOkTst && ((Chunk_Type)jcasType).casFeat_ChunkType == null)
      jcasType.jcas.throwFeatMissing("ChunkType", "edu.cmu.lti.oaqa.annographix.types.Chunk");
    jcasType.ll_cas.ll_setStringValue(addr, ((Chunk_Type)jcasType).casFeatCode_ChunkType, v);}    
  }

    
