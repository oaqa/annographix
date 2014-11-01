/*
 * Copyright 2014 Carnegie Mellon University
 *
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
 *
 */

package edu.cmu.lti.oaqa.annographix.uima;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * This class reads the configuration, which defines a mapping of annotation to indexable fields.
 * 
 * @author Leonid Boytsov (re-using a bit of code from UIMA-Solr config reader)
 */
public class MappingReader {
  public class TypeDescr {
    public String fieldPrefix;
    public String tag;
    public String parent;
    public TypeDescr(String _fieldPrefix, String _tag, String _parent) {
      fieldPrefix = _fieldPrefix;
      tag = _tag;
      parent = _parent;
    }
  }
  
  public Map<String, TypeDescr> getConf(InputStream input) throws ParserConfigurationException, SAXException, IOException  {

    SAXParser parser = SAXParserFactory.newInstance().newSAXParser();

    MappingHandler handler = new MappingHandler(this);
    parser.parse(input, handler); //parse the stream

    input.close();
    return handler.getMapping();
  }

  private static class MappingHandler extends DefaultHandler {
    private static final String TYPE          = "type";
    private static final String MAPPING       = "annotationMapping";
    private static final String NAME_ATTR     = "name";
    private static final String VALUE_ATTR    = "valueAttr";
    private static final String FIELD_PREFIX  = "fieldPrefix";
    private static final String PARENT_ATTR   = "parentAttr";
    
    private boolean                 InRoot = false;
    private Map<String, TypeDescr>  mapping;
    
    private MappingReader           holder;
    
    public MappingHandler(MappingReader _holder) {
      holder = _holder;
      mapping = new HashMap<String, TypeDescr>();
    }
    
    public Map<String, TypeDescr> getMapping() { return mapping; }

    @Override
    public void startElement(String uri, String localName, String name, Attributes attributes) throws SAXException {
      if (TYPE.equals(name)) {
        if (InRoot) {
          String type = attributes.getValue(NAME_ATTR);
          String tag = attributes.getValue(VALUE_ATTR);
          if (type == null) {
            throw new SAXException("Missing attribute: '" + NAME_ATTR + "'");
          }
          String fieldPrefix = attributes.getValue(FIELD_PREFIX);
          if (fieldPrefix == null) {
            throw new SAXException("Missing attribute: '" + FIELD_PREFIX + "'");
          }
          String parent = attributes.getValue(PARENT_ATTR);
          if (mapping.containsKey(type)) {
            throw new SAXException("Duplicate type: '" + type + "'");
          } else {
            mapping.put(type, holder.new TypeDescr(fieldPrefix, tag, parent));
          }
        } else {
          throw new SAXException("The root tag: '" + MAPPING + "' is missing");
        }
      } else if (MAPPING.equals(name)) {
        if (InRoot) {
          throw new SAXException("The tag '" + MAPPING + "' shouldn't be nested!");
        }
        InRoot = true;
      } else {
        throw new SAXException("Unknown root tag: '" + name + "'");
      }
    }

    @Override
    public void characters(char[] chars, int start, int length) throws SAXException {
      /*
        ... String.valueOf(chars, start, length);
      */
    }

    @Override
    public void endElement(String uri, String localName, String name) throws SAXException {
      if (MAPPING.equals(name)) {
        InRoot = false;
      }
    }
  }
}
