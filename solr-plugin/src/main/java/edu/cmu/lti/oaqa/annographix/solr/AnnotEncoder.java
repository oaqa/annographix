/*
 *  Copyright 2014 Carnegie Mellon University
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package edu.cmu.lti.oaqa.annographix.solr;

import org.apache.lucene.analysis.payloads.AbstractEncoder;
import org.apache.lucene.analysis.payloads.PayloadEncoder;
import org.apache.lucene.analysis.payloads.PayloadHelper;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.ArrayUtil;

import edu.cmu.lti.oaqa.annographix.util.UtilConst;

/**
 *  Encode annotation-related information as a {@link BytesRef}.
 *  This information includes the following comma-separated values:
 *  start offset, end offset, id, parent id.
 * <p/>
 * See {@link org.apache.lucene.analysis.payloads.PayloadHelper#encodeInt(int, byte[], int)}.
 * 
 * @author Leonid Boytsov
 *    
 **/
//  TODO: Try to use some light-weight compression?
public class AnnotEncoder extends AbstractEncoder 
        implements PayloadEncoder {
  @Override
  public BytesRef encode(char[] buffer, int offset, int length) {
    int sep1pos = -1, sep2pos = -1, sep3pos = -1, sepQty = 0;
    
    for (int i = 0; i < length; ++i) {
      char c= buffer[offset + i];
      if (c == UtilConst.PAYLOAD_ID_SEP_CHAR) {
        ++sepQty;
        if (1 == sepQty) sep1pos = i;
        else if (2 == sepQty) sep2pos = i;
        else if (3 == sepQty) sep3pos = i;
        else {
 // TODO: make the logging work, for some reason I wasn't able to access SOLR logger from this function          
 //       String errData = new String(buffer, offset, length);
 //       System.err.println("Cannot parse payload: " + errData);
          throw new IllegalArgumentException();
        }
      }
    }
    
    int wordStartPos = ArrayUtil.parseInt(buffer, offset, sep1pos);
    int wordEndPos   = ArrayUtil.parseInt(buffer, offset + sep1pos+1, 
                                           sep2pos - sep1pos - 1);
    int annotId      = ArrayUtil.parseInt(buffer, offset + sep2pos+1, 
                                           sep3pos - sep2pos - 1);    
    int parentId     = ArrayUtil.parseInt(buffer, offset + sep3pos+1, 
                                           length - sep3pos-1);
    
    
    BytesRef result = new BytesRef(PayloadHelper.encodeInt(wordStartPos));
    result.append(new BytesRef(PayloadHelper.encodeInt(wordEndPos)));
    result.append(new BytesRef(PayloadHelper.encodeInt(annotId)));
    result.append(new BytesRef(PayloadHelper.encodeInt(parentId)));
    return result;
  }

  /**
   *  Decodes payload data.
   *  
   *  @param buffer   A buffer that stores encoded payload data.
   *  @param res      A result variable: To prevent unnecessary 
   *                  memory allocations re-use the same variable
   *                  among multiple calls to this function.
   */
  public static void decode(BytesRef buffer, 
                            ElemInfoData res /* reuse this variable */) {
    byte[] bytes = buffer.bytes;
    int offset = buffer.offset;
    res.mStartOffset = PayloadHelper.decodeInt(bytes, offset);
    offset += 4;
    res.mEndOffset   = PayloadHelper.decodeInt(bytes, offset);
    offset += 4;
    res.mId          = PayloadHelper.decodeInt(bytes, offset);
    offset += 4;
    res.mParentId    = PayloadHelper.decodeInt(bytes, offset);
  }
}


