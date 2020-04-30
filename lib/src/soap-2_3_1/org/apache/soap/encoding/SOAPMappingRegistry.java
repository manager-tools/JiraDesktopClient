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

package org.apache.soap.encoding;

import java.io.*;
import java.util.*;
import java.math.*;
import org.w3c.dom.*;
import org.apache.soap.util.*;
import org.apache.soap.util.xml.*;
import org.apache.soap.*;
import org.apache.soap.rpc.*;
import org.apache.soap.encoding.literalxml.*;
import org.apache.soap.encoding.soapenc.*;
import org.apache.soap.encoding.xmi.*;

/**
 * A <code>SOAPMappingRegistry</code> object is an
 * <code>XMLJavaMappingRegistry</code> with pre-registered
 * serializers and deserializers to support <em>SOAP</em>.
 *
 * @author Matthew J. Duftler (duftler@us.ibm.com)
 * @author Sanjiva Weerawarana (sanjiva@watson.ibm.com)
 * @author Francisco Curbera (curbera@us.ibm.com)
 * @author Sam Ruby (rubys@us.ibm.com)
 * @author Glen Daniels (gdaniels@allaire.com)
 */
public class SOAPMappingRegistry extends XMLJavaMappingRegistry
{
  private static SOAPMappingRegistry baseReg1999;
  private static SOAPMappingRegistry baseReg2000;
  private static SOAPMappingRegistry baseReg2001;

  private SOAPMappingRegistry parent = null;
  private String schemaURI;

  private static String soapEncURI = Constants.NS_URI_SOAP_ENC;
  
  private static QName arrayQName = new QName(soapEncURI, "Array");

  // create all the standard serializers/deserializers as static vars.
  // these fill into all the various base registries.

  private static StringDeserializer stringDeser = new StringDeserializer();
  private static IntDeserializer intDeser =  new IntDeserializer();
  private static DecimalDeserializer decimalDeser =  new DecimalDeserializer();
  private static FloatDeserializer floatDeser =  new FloatDeserializer();
  private static DoubleDeserializer doubleDeser =  new DoubleDeserializer();
  private static BooleanDeserializer booleanDeser =  new BooleanDeserializer();
  private static LongDeserializer longDeser =  new LongDeserializer();
  private static ShortDeserializer shortDeser =  new ShortDeserializer();
  private static ByteDeserializer byteDeser =  new ByteDeserializer();
  private static HexDeserializer hexDeser =  new HexDeserializer();

  private static QNameSerializer qNameSer = new QNameSerializer();
  private static ParameterSerializer paramSer = new ParameterSerializer();
  private static ArraySerializer arraySer = new ArraySerializer();
  private static VectorSerializer vectorSer = new VectorSerializer();
  private static HashtableSerializer hashtableSer = new HashtableSerializer();
  private static XMLParameterSerializer xmlParamSer =
    new XMLParameterSerializer();
  private static DateSerializer dateSer = new DateSerializer();
  private static CalendarSerializer calSer = new CalendarSerializer();
  private static UrTypeDeserializer objDeser = new UrTypeDeserializer();
  public static MimePartSerializer partSer = new MimePartSerializer();
  
  /**
   * The following stuff is here to deal with the slight differences
   * between 1999 schema and 2000/10 schema.  This system allows us to
   * register type mappings for both sets of QNames, and to default to
   * whichever one is set as current.
   * 
   * !!! The order of the elements in these arrays is critical.  Be
   *     careful when editing.
   */
  private static QName schema1999QNames [] = {
    Constants.string1999QName,
    Constants.int1999QName,
    Constants.int1999QName,
    Constants.decimal1999QName,
    Constants.float1999QName,
    Constants.float1999QName,
    Constants.double1999QName,
    Constants.double1999QName,
    Constants.boolean1999QName,
    Constants.boolean1999QName,
    Constants.long1999QName,
    Constants.long1999QName,
    Constants.short1999QName,
    Constants.short1999QName,
    Constants.byte1999QName,
    Constants.byte1999QName,
    Constants.hex1999QName,
    Constants.qName1999QName,
    Constants.date1999QName,
    Constants.timeInst1999QName,
    Constants.object1999QName,
    Constants.object1999QName,
    Constants.object1999QName,
    Constants.object1999QName,
    Constants.object1999QName,
  };
  
  private static QName schema2000QNames [] = {
    Constants.string2000QName,
    Constants.int2000QName,
    Constants.int2000QName,
    Constants.decimal2000QName,
    Constants.float2000QName,
    Constants.float2000QName,
    Constants.double2000QName,
    Constants.double2000QName,
    Constants.boolean2000QName,
    Constants.boolean2000QName,
    Constants.long2000QName,
    Constants.long2000QName,
    Constants.short2000QName,
    Constants.short2000QName,
    Constants.byte2000QName,
    Constants.byte2000QName,
    Constants.hex2000QName,
    Constants.qName2000QName,
    Constants.date2000QName,
    Constants.timeInst2000QName,
    Constants.object2000QName,
    Constants.object2000QName,
    Constants.object2000QName,
    Constants.object2000QName,
    Constants.object2000QName,
  };
  
  private static QName schema2001QNames [] = {
    Constants.string2001QName,
    Constants.int2001QName,
    Constants.int2001QName,
    Constants.decimal2001QName,
    Constants.float2001QName,
    Constants.float2001QName,
    Constants.double2001QName,
    Constants.double2001QName,
    Constants.boolean2001QName,
    Constants.boolean2001QName,
    Constants.long2001QName,
    Constants.long2001QName,
    Constants.short2001QName,
    Constants.short2001QName,
    Constants.byte2001QName,
    Constants.byte2001QName,
    Constants.hex2001QName,
    Constants.qName2001QName,
    Constants.date2001QName,
    Constants.timeInst2001QName,
    Constants.object2001QName,
    Constants.object2001QName,
    Constants.object2001QName,
    Constants.object2001QName,
    Constants.object2001QName,
  };

  private static Class classes [] = {
    String.class,
    Integer.class,
    int.class,
    BigDecimal.class,
    Float.class,
    float.class,
    Double.class,
    double.class,
    Boolean.class,
    boolean.class,
    Long.class,
    long.class,
    Short.class,
    short.class,
    Byte.class,
    byte.class,
    Hex.class,
    QName.class,
    GregorianCalendar.class,
    Date.class,
    javax.mail.internet.MimeBodyPart.class,
    java.io.InputStream.class,
    javax.activation.DataSource.class,
    javax.activation.DataHandler.class,
    Object.class,
  };

  /*
    This serializer runs its content through a mechanism to replace
    the characters {&, ", ', <, >} with their appropriate escape
    sequences.
  */
  private static Serializer cleanSer = new Serializer()
  {
    public void marshall(String inScopeEncStyle, Class javaType, Object src,
                         Object context, Writer sink, NSStack nsStack,
                         XMLJavaMappingRegistry xjmr, SOAPContext ctx)
      throws IllegalArgumentException, IOException {
      nsStack.pushScope();

      if (src == null)
      {
        SoapEncUtils.generateNullStructure(inScopeEncStyle, javaType, context,
                                           sink, nsStack, xjmr);
      }
      else
      {
        SoapEncUtils.generateStructureHeader(inScopeEncStyle,
                                             javaType,
                                             context,
                                             sink,
                                             nsStack,
                                             xjmr);
        
        sink.write(Utils.cleanString(src.toString()) + "</" + context + '>');
      }
      nsStack.popScope();
    }
  };

  /*
    This serializer does not apply escape sequences to its content.
    This serializer should be used for numbers and other things that
    will not have any of the following characters: {&, ", ', <, >}
  */
  private static Serializer ser = new Serializer()
  {
    public void marshall(String inScopeEncStyle, Class javaType, Object src,
                         Object context, Writer sink, NSStack nsStack,
                         XMLJavaMappingRegistry xjmr, SOAPContext ctx)
      throws IllegalArgumentException, IOException {
      nsStack.pushScope();

      SoapEncUtils.generateStructureHeader(inScopeEncStyle,
                                           javaType,
                                           context,
                                           sink,
                                           nsStack,
                                           xjmr);

      sink.write(src + "</" + context + '>');

      nsStack.popScope();
    }
  };

  private static Serializer serializers [] = {
    cleanSer,  // String
    ser,       // Integer
    ser,       // int
    ser,       // BigDecimal
    ser,       // Float
    ser,       // float
    ser,       // Double
    ser,       // double
    ser,       // Boolean
    ser,       // boolean
    ser,       // Long
    ser,       // long
    ser,       // Short
    ser,       // short
    ser,       // Byte
    ser,       // byte
    ser,       // Hex
    qNameSer,  // QName
    calSer,    // GregorianCalendar
    dateSer,   // Date
    partSer,   // MimeBodyPart
    partSer,   // InputStream
    partSer,   // DataSource
    partSer,   // DataHandler
    null,      // Object
  };

  private static Deserializer deserializers [] = {
    stringDeser,
    null,
    intDeser,
    decimalDeser,
    null,
    floatDeser,
    null,
    doubleDeser,
    null,
    booleanDeser,
    null,
    longDeser,
    null,
    shortDeser,
    null,
    byteDeser,
    hexDeser,
    qNameSer,
    calSer,
    dateSer,
    null,
    null,
    null,
    null,
    objDeser,
  };

  /**
   * Create a new SMR. The resulting registry is aware of all
   * pre-defined type mappings.
   */
  public SOAPMappingRegistry()
  {
    this.schemaURI = Constants.NS_URI_CURRENT_SCHEMA_XSD;
    this.parent = getBaseRegistry (Constants.NS_URI_CURRENT_SCHEMA_XSD);
  }

    /**
     * This constructor takes a "parent" registry as a base registry.
     * Lookup requests cascade up to the parent while registration 
     * requests stay here.
     */
    public SOAPMappingRegistry(SOAPMappingRegistry parent)
    {
      this(parent, Constants.NS_URI_CURRENT_SCHEMA_XSD);
    }

    /**
    * This constructor is the base constructor. If parent is null, then this
     * is viewed as a base registry and the registry is initialized with
     * all the default mappings etc.. If it is not-null, the no init
     * is done and the parent is assumed to have the stuff in it already.
     *
     * @param parent the "parent" SMR to delegate lookups to if I can't
     *        find the stuff in my tables. If parent is null, then I get
     *        pre-loaded with all the default type mappings etc. (some
     *        of which are based on the schema URI). If parent is not null,
     *        the default stuff is not put in - the idea is that in that
     *        case the parent already contains the defaults.
     * @param schemaURI the namespace URI of XSD to be used for serializers.
     *        Deserializers for all 3 XSD URIs are always registered.
     */
    public SOAPMappingRegistry(SOAPMappingRegistry parent, String schemaURI)
    {
      this.parent = parent;
      if (parent == null) {
        initializeRegistry(schemaURI);
      }
      this.schemaURI = schemaURI;
    }

  /**
   * Return the singleton registry instance configured for the 
   * indicated schema URI. If the schemaURI is unrecognized, the
   * 2001 base registry is returned.
   */
  public static SOAPMappingRegistry getBaseRegistry (String schemaURI) {
    synchronized (SOAPMappingRegistry.class) {
      if (baseReg1999 == null) {
	baseReg1999 = 
	  new SOAPMappingRegistry (null, Constants.NS_URI_1999_SCHEMA_XSD);
	baseReg2000 = 
	  new SOAPMappingRegistry (null, Constants.NS_URI_2000_SCHEMA_XSD);
	baseReg2001 = 
	  new SOAPMappingRegistry (null, Constants.NS_URI_2001_SCHEMA_XSD);
      }
    }

    if (schemaURI.equals(Constants.NS_URI_1999_SCHEMA_XSD)) {
      return baseReg1999;
    } else if (schemaURI.equals(Constants.NS_URI_2000_SCHEMA_XSD)) {
      return baseReg2000;
    } else {
      return baseReg2001;
    }
  }

  /** Set the default encoding style.  If the query*() calls
   * are invoked with a null encodingStyleURI parameter, we'll
   * use this instead.
   */
  public void setDefaultEncodingStyle(String defEncStyle) {
      super.setDefaultEncodingStyle(defEncStyle);
      if (parent != null)
	      parent.setDefaultEncodingStyle(defEncStyle);
  }

  /**
   * Return the schemaURI that was used to create this registry
   * instance.
   */
  public String getSchemaURI () {
    return schemaURI;
  }

  /**
   * Returns the "parent" registry, if there is one. Otherwise,
   * returns null.
   */
  public SOAPMappingRegistry getParent () {
    return parent;
  }

  /**
   * Initialize myself with all the built-in default stuff. If schemaURI
   * is not recognized, defaults to 2001 schema URI.
   */
  private void initializeRegistry(String schemaURI)
  {
    QName schemaQNames[] = null;

    if (schemaURI.equals(Constants.NS_URI_1999_SCHEMA_XSD)) {
      schemaQNames = schema1999QNames;
      mapSchemaTypes(schema2000QNames, false);
      mapSchemaTypes(schema2001QNames, false);
    } else if (schemaURI.equals(Constants.NS_URI_2000_SCHEMA_XSD)) {
      mapSchemaTypes(schema1999QNames, false);
      schemaQNames = schema2000QNames;
      mapSchemaTypes(schema2001QNames, false);
    } else {
      if (!schemaURI.equals(Constants.NS_URI_2001_SCHEMA_XSD)) {
	System.err.println("WARNING: Unrecognized Schema URI '" + schemaURI +
			   "' specified.  Defaulting to '" + 
			   Constants.NS_URI_2001_SCHEMA_XSD + "'.");
      }
      mapSchemaTypes(schema1999QNames, false);
      mapSchemaTypes(schema2000QNames, false);
      schemaQNames = schema2001QNames;
    }
    
    // map the ones that I want to do read-write with
    mapSchemaTypes(schemaQNames, true);

    // Register parameter serializer for SOAP-ENC encoding style.
    mapTypes(soapEncURI, RPCConstants.Q_ELEM_PARAMETER, Parameter.class,
             paramSer, paramSer);

    // Register array deserializer for SOAP-ENC encoding style.
    mapTypes(soapEncURI, arrayQName, null, null, arraySer);

    // Register parameter serializer for literal xml encoding style.
    mapTypes(Constants.NS_URI_LITERAL_XML, RPCConstants.Q_ELEM_PARAMETER,
             Parameter.class, xmlParamSer, xmlParamSer);

    try {
      Class XMISerializer = 
        Class.forName("org.apache.soap.util.xml.XMISerializer");
      Class XMIParameterSerializer =
        Class.forName("org.apache.soap.encoding.xmi.XMIParameterSerializer");

      // Register default serializers for XMI encoding style.
      mapTypes(Constants.NS_URI_XMI_ENC, null, null,
               (Serializer)XMISerializer.newInstance(),
               (Deserializer)XMIParameterSerializer.newInstance());

      // Register serializer for Parameter class - not deserializer!
      mapTypes(Constants.NS_URI_XMI_ENC, null, Parameter.class,
               (Serializer)XMIParameterSerializer.newInstance(), null);
    } catch (IllegalAccessException iae) {
    } catch (InstantiationException ie) {
    } catch (ClassNotFoundException cnfe) {
    } catch (NoClassDefFoundError ncdfe) {
      // If the class can't be loaded, continue without it...
    }

    /*
      Basic collection types - these should map fine to Perl, Python, C++...
      (but an encoding like this needs to be agreed upon)
    */
    mapTypes(soapEncURI, new QName(Constants.NS_URI_XML_SOAP, "Vector"),
             Vector.class, vectorSer, vectorSer);
    mapTypes(soapEncURI, new QName(Constants.NS_URI_XML_SOAP, "Map"),
             Hashtable.class, hashtableSer, hashtableSer);

    try {
      Class mapClass = Class.forName("java.util.Map");
      MapSerializer mapSer = new MapSerializer();

      mapTypes(soapEncURI, new QName(Constants.NS_URI_XML_SOAP, "Map"),
               mapClass, mapSer, mapSer);
    } catch (ClassNotFoundException cnfe) {
    } catch (NoClassDefFoundError ncdfe) {
      // If the class can't be loaded, continue without it...
    }

    /*
      Map a Java byte array to the SOAP-ENC:base64 subtype.
    */
    Base64Serializer base64Ser = new Base64Serializer();
    QName base64QName = new QName(Constants.NS_URI_2001_SCHEMA_XSD, "base64Binary");
    mapTypes(soapEncURI, base64QName, byte[].class, base64Ser, base64Ser);
    base64QName = new QName(soapEncURI, "base64");
    mapTypes(soapEncURI, base64QName, byte[].class, base64Ser, base64Ser);
  }
  
  /**
   * Map a set of schema types defined in the arrays above.  If
   * the "serialize" arg is set to true, we'll map the serializer
   * side (i.e. when output gets generated it'll be as those QNames),
   * otherwise we just do deserializers.
   */
  private void mapSchemaTypes(QName [] schemaQNames, boolean serialize)
  {    
    for (int i = 0; i < schemaQNames.length; i++) {
      QName qName = schemaQNames[i];
      Class cls = classes[i];
      Serializer ser = serialize ? serializers[i] : null;
      Deserializer dser = deserializers[i];
      
      mapTypes(soapEncURI, qName, cls, ser, dser);
    }
  }

  /**
   * This function overrides the one in XMLJavaMappingRegistry for the sole
   * purpose of returning SOAP-ENC:Array when javaType represents an array.
   * The XMLJavaMappingRegistry will be consulted first, and if no mapping
   * is found, SOAP-ENC:Array is returned. Obviously, this only applies when
   * the encoding style is soap encoding.
   */
  protected QName queryElementType_(Class javaType, String encodingStyleURI)
  {
    QName qn = super.queryElementType_(javaType, encodingStyleURI);
    if (qn != null) {
      return qn;
    }
    if (parent != null) {
      qn = parent.queryElementType_(javaType, encodingStyleURI);
      if (qn != null) {
        return qn;
      }
    }

    if (javaType != null
        && (javaType.isArray())
        && encodingStyleURI != null
        && encodingStyleURI.equals(soapEncURI)) {
      return arrayQName;
    }

    return null;
  }

  /**
   * This function overrides the one in XMLJavaMappingRegistry for the sole
   * purpose of returning an ArraySerializer when javaType represents an
   * array. The XMLJavaMappingRegistry will be consulted first, and if no
   * serializer is found for javaType, ArraySerailizer is returned.
   * Obviously, this only applies when the encoding style is soap encoding.
   */
  protected Serializer querySerializer_(Class javaType,
                                        String encodingStyleURI)
  {
    Serializer s = super.querySerializer_(javaType, encodingStyleURI);
    if (s != null) {
      return s;
    }
    if (parent != null) {
      s = parent.querySerializer_(javaType, encodingStyleURI);
      if (s != null) {
        return s;
      }
    }

    if (javaType != null
        && encodingStyleURI != null
        && encodingStyleURI.equals(soapEncURI)) {
      if (javaType.isArray()) {
        return arraySer;
      }
    }

    return null;
  }

  /**
   * Override the query deserializer to look at the parent too before
   * saying that a deserializer is not available.
   */
  protected Deserializer queryDeserializer_(QName elementType,
                                            String encodingStyleURI)
  {
    Deserializer ds = super.queryDeserializer_(elementType, encodingStyleURI);
    if (ds != null) {
      return ds;
    }
    if (parent != null) {
      ds = parent.queryDeserializer_(elementType, encodingStyleURI);
      if (ds != null) { 
        return ds;
      }
    }
    return null;
  }

  /**
   * Overide the query Javatype to look at the parent too before
   * saying that a Java type is not available.
   */
  protected Class queryJavaType_(QName elementType, String encodingStyleURI)
  {
    Class jt = super.queryJavaType_(elementType, encodingStyleURI);
    if (jt != null) {
      return jt;
    }
    if (parent != null) {
      jt = parent.queryJavaType_(elementType, encodingStyleURI);
      if (jt != null) {
        return jt;
      }
    }
    return null;
  }
}
