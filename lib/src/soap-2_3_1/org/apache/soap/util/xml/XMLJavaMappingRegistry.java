/*
 * The Apache Software License, Version 1.1
 *
 *
 * Copyright (c) 2000 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "SOAP" and "Apache Software Foundation" must
 *    not be used to endorse or promote products derived from this
 *    software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache",
 *    nor may "Apache" appear in their name, without prior written
 *    permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation and was
 * originally based on software copyright (c) 2000, International
 * Business Machines, Inc., http://www.apache.org.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */

package org.apache.soap.util.xml;

import java.io.*;
import java.util.*;
import org.w3c.dom.*;
import org.apache.soap.util.*;
import org.apache.soap.Constants;
import org.apache.soap.rpc.SOAPContext;
import org.apache.soap.encoding.soapenc.MimePartSerializer;
import org.apache.soap.encoding.SOAPMappingRegistry;

/**
 * An <code>XMLJavaMappingRegistry</code> ...
 *
 * @author Matthew J. Duftler (duftler@us.ibm.com)
 * @author Sanjiva Weerawarana (sanjiva@watson.ibm.com)
 * @author Francisco Curbera (curbera@us.ibm.com)
 */
public class XMLJavaMappingRegistry
{
  private Hashtable sReg        = new Hashtable();
  private Hashtable dsReg       = new Hashtable();
  private Hashtable xml2JavaReg = new Hashtable();
  private Hashtable java2XMLReg = new Hashtable();
  private String defaultEncodingStyle = null;
  
  /** Set the default encoding style.  If the query*() calls
   * are invoked with a null encodingStyleURI parameter, we'll
   * use this instead.
   */
  public void setDefaultEncodingStyle(String defEncStyle)
  {
      defaultEncodingStyle = defEncStyle;
  }

  // To register the default, set both types to null.
  public void mapTypes(String encodingStyleURI, QName elementType,
                       Class javaType, Serializer s, Deserializer ds)
  {
    String java2XMLKey = getKey(javaType, encodingStyleURI);
    String xml2JavaKey = getKey(elementType, encodingStyleURI);

    if (s != null)
    {
      sReg.put(java2XMLKey, s);
    }

    if (ds != null)
    {
      dsReg.put(xml2JavaKey, ds);
    }

    // Only map types if both types are provided.
    if (elementType != null && javaType != null)
    {
      java2XMLReg.put(java2XMLKey, elementType);
      xml2JavaReg.put(xml2JavaKey, javaType);
    }
  }

  /**
   * This version returns null if the serializer is not found. It is 
   * intended for internal usage (its used for chaining registries,
   * for example).
   */
  protected Serializer querySerializer_(Class javaType,
                                        String encodingStyleURI)
  {
    if (encodingStyleURI == null) {
      encodingStyleURI = defaultEncodingStyle;
    }
    String java2XMLKey = getKey(javaType, encodingStyleURI);
    Serializer s = (Serializer)sReg.get(java2XMLKey);

    if (s != null)
    {
      return s;
    }
    else
    {
      java2XMLKey = getKey(null, encodingStyleURI);
      return (Serializer)sReg.get(java2XMLKey);
    }
  }

  /**
   * This version calls the protected method to do the work and if its
   * not found throws an exception. 
   */
  public Serializer querySerializer(Class javaType, String encodingStyleURI)
    throws IllegalArgumentException
  {
    Serializer s = querySerializer_(javaType, encodingStyleURI);
    if (s != null)
    {
      return s;
    }
    else
    {
      throw new IllegalArgumentException("No Serializer found to " +
                                         "serialize a '" +
                                         getClassName(javaType) +
                                         "' using encoding style '" +
                                         encodingStyleURI + "'.");
    }
  }

  /**
   * This version returns null if the deserializer is not found. It is 
   * intended for internal usage (its used for chaining registries,
   * for example).
   */
  protected Deserializer queryDeserializer_(QName elementType,
                                            String encodingStyleURI)
  {
    if (encodingStyleURI == null) {
      encodingStyleURI = defaultEncodingStyle;
    }

    String xml2JavaKey = getKey(elementType, encodingStyleURI);
    Deserializer ds = (Deserializer)dsReg.get(xml2JavaKey);

    if (ds != null)
    {
      return ds;
    }
    else
    {
      xml2JavaKey = getKey(null, encodingStyleURI);
      return (Deserializer)dsReg.get(xml2JavaKey);
    }
  }

  /**
   * This version calls the protected method to do the work and if its
   * not found throws an exception. 
   */
  public Deserializer queryDeserializer(QName elementType,
                                        String encodingStyleURI)
    throws IllegalArgumentException
  {
    Deserializer ds = queryDeserializer_(elementType, encodingStyleURI);
    if (ds != null)
    {
      return ds;
    }
    else
    {
      throw new IllegalArgumentException("No Deserializer found to " +
                                         "deserialize a '" + elementType +
                                         "' using encoding style '" +
                                         encodingStyleURI + "'.");
    }
  }

  /**
   * This version returns null if the element type is not found. It is 
   * intended for internal usage (its used for chaining registries,
   * for example).
   */
  protected QName queryElementType_(Class javaType, String encodingStyleURI)
  {
    if (encodingStyleURI == null) {
      encodingStyleURI = defaultEncodingStyle;
    }

    String java2XMLkey = getKey(javaType, encodingStyleURI);
    return (QName)java2XMLReg.get(java2XMLkey);
  }

  /**
   * This version calls the protected method to do the work and if its
   * not found throws an exception. 
   */
  public QName queryElementType(Class javaType, String encodingStyleURI)
    throws IllegalArgumentException
  {
    QName elementType = queryElementType_(javaType, encodingStyleURI);
    if (elementType != null)
    {
      return elementType;
    }
    else
    {
      throw new IllegalArgumentException("No mapping found for '" +
                                         getClassName(javaType) +
                                         "' using encoding style '" +
                                         encodingStyleURI + "'.");
    }
  }

  /**
   * This version returns null if the Java type is not found. It is 
   * intended for internal usage (its used for chaining registries,
   * for example).
   */
  protected Class queryJavaType_(QName elementType, String encodingStyleURI)
  {
    if (encodingStyleURI == null) {
      encodingStyleURI = defaultEncodingStyle;
    }

    String xml2JavaKey = getKey(elementType, encodingStyleURI);
    return (Class)xml2JavaReg.get(xml2JavaKey);
  }

  /**
   * This version calls the protected method to do the work and if its
   * not found throws an exception. 
   */
  public Class queryJavaType(QName elementType, String encodingStyleURI)
    throws IllegalArgumentException
  {
    Class javaType = queryJavaType_(elementType, encodingStyleURI);
    if (javaType != null)
    {
      return javaType;
    }
    else
    {
      throw new IllegalArgumentException("No mapping found for '" +
                                         elementType +
                                         "' using encoding style '" +
                                         encodingStyleURI + "'.");
    }
  }

  public void marshall(String inScopeEncStyle, Class javaType, Object src,
                       Object context, Writer sink, NSStack nsStack,
                       SOAPContext ctx)
    throws IllegalArgumentException, IOException
  {
    Serializer s = (Serializer)querySerializer(javaType, inScopeEncStyle);

    s.marshall(inScopeEncStyle, javaType, src, context,
               sink, nsStack, this, ctx);
  }

  public Bean unmarshall(String inScopeEncStyle, QName elementType,
                         Node src, SOAPContext ctx)
    throws IllegalArgumentException
  {
      Deserializer ds = null;
      try {
          ds = (Deserializer)queryDeserializer(elementType,
                                               inScopeEncStyle);
      } catch(IllegalArgumentException iae) {
          // If the element contains an href= parameter and could not be
          // resolved , use MimePartSerializer.
          String href = ((Element)src).getAttribute(Constants.ATTR_REFERENCE);
          if (href != null && !href.equals(""))
              ds = SOAPMappingRegistry.partSer;
          else
              throw iae;
      }

    return ds.unmarshall(inScopeEncStyle, elementType, src, this, ctx);
  }

  private static String getKey(Object type, String encodingStyleURI)
  {
    return type + " + " + encodingStyleURI;
  }

  protected static String getClassName(Class javaType)
  {
    return javaType != null ? StringUtils.getClassName(javaType) : "null";
  }
}
