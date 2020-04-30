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

package org.apache.soap;

import org.apache.soap.util.xml.QName;

/**
 * <em>SOAP</em> constants.
 *
 * @author Sanjiva Weerawarana (sanjiva@watson.ibm.com)
 * @author Matthew J. Duftler (duftler@us.ibm.com)
 */
public class Constants
{
  // Namespace prefixes.
  public static final String NS_PRE_XMLNS = "xmlns";
  public static final String NS_PRE_SOAP = "SOAP";
  public static final String NS_PRE_SOAP_ENV = NS_PRE_SOAP + "-ENV";
  public static final String NS_PRE_SOAP_ENC = NS_PRE_SOAP + "-ENC";
  public static final String NS_PRE_SCHEMA_XSI = "xsi";
  public static final String NS_PRE_SCHEMA_XSD = "xsd";

  // Namespace URIs.
  public static final String NS_URI_XMLNS =
    "http://www.w3.org/2000/xmlns/";
  public static final String NS_URI_SOAP_ENV =
    "http://schemas.xmlsoap.org/soap/envelope/";
  public static final String NS_URI_SOAP_ENC =
    "http://schemas.xmlsoap.org/soap/encoding/";
  
  public static final String NS_URI_1999_SCHEMA_XSI =
    "http://www.w3.org/1999/XMLSchema-instance";
  public static final String NS_URI_1999_SCHEMA_XSD =
    "http://www.w3.org/1999/XMLSchema";
  public static final String NS_URI_2000_SCHEMA_XSI =
    "http://www.w3.org/2000/10/XMLSchema-instance";
  public static final String NS_URI_2000_SCHEMA_XSD =
    "http://www.w3.org/2000/10/XMLSchema";
  public static final String NS_URI_2001_SCHEMA_XSI =
    "http://www.w3.org/2001/XMLSchema-instance";
  public static final String NS_URI_2001_SCHEMA_XSD =
    "http://www.w3.org/2001/XMLSchema";
  public static final String NS_URI_CURRENT_SCHEMA_XSI =
    NS_URI_2001_SCHEMA_XSI;
  public static final String NS_URI_CURRENT_SCHEMA_XSD =
    NS_URI_2001_SCHEMA_XSD;

  public static final String NS_URI_XML_SOAP =
    "http://xml.apache.org/xml-soap";
  public static final String NS_URI_XML_SOAP_DEPLOYMENT =
    "http://xml.apache.org/xml-soap/deployment";
  public static final String NS_URI_LITERAL_XML =
    "http://xml.apache.org/xml-soap/literalxml";
  public static final String NS_URI_XMI_ENC =
    "http://www.ibm.com/namespaces/xmi";

  // HTTP header field names.
  public static final String HEADER_POST = "POST";
  public static final String HEADER_HOST = "Host";
  public static final String HEADER_CONTENT_TYPE = "Content-Type";
  public static final String HEADER_CONTENT_TYPE_JMS = "ContentType";
  public static final String HEADER_CONTENT_LENGTH = "Content-Length";
  public static final String HEADER_CONTENT_LOCATION = "Content-Location";
  public static final String HEADER_CONTENT_ID = "Content-ID";
  public static final String HEADER_SOAP_ACTION = "SOAPAction";
  public static final String HEADER_AUTHORIZATION = "Authorization";
  public static final String HEADER_PROXY_AUTHORIZATION =
    "Proxy-Authorization";

  // HTTP header field values.
  public static final String HEADERVAL_DEFAULT_CHARSET = "iso-8859-1";
  public static final String HEADERVAL_CHARSET_UTF8 = "utf-8";
  public static final String HEADERVAL_CONTENT_TYPE = "text/xml";
  public static final String HEADERVAL_CONTENT_TYPE_UTF8 =
    HEADERVAL_CONTENT_TYPE + ";charset=" + HEADERVAL_CHARSET_UTF8;
  public static final String HEADERVAL_CONTENT_TYPE_MULTIPART_PRIMARY =
    "multipart";
  public static final String HEADERVAL_MULTIPART_CONTENT_SUBTYPE = "related";
  public static final String HEADERVAL_CONTENT_TYPE_MULTIPART =
    HEADERVAL_CONTENT_TYPE_MULTIPART_PRIMARY + '/' +
    HEADERVAL_MULTIPART_CONTENT_SUBTYPE;

  // XML Declaration string
  public static final String XML_DECL = 
    "<?xml version='1.0' encoding='UTF-8'?>\r\n";

  // Element names.
  public static final String ELEM_ENVELOPE = "Envelope";
  public static final String ELEM_BODY = "Body";
  public static final String ELEM_HEADER = "Header";
  public static final String ELEM_FAULT = "Fault";
  public static final String ELEM_FAULT_CODE = "faultcode";
  public static final String ELEM_FAULT_STRING = "faultstring";
  public static final String ELEM_FAULT_ACTOR = "faultactor";
  public static final String ELEM_DETAIL = "detail";
  public static final String ELEM_FAULT_DETAIL_ENTRY = "detailEntry";

  // Qualified element names.
  public static QName Q_ELEM_ENVELOPE =
    new QName(NS_URI_SOAP_ENV, ELEM_ENVELOPE);
  public static QName Q_ELEM_HEADER =
    new QName(NS_URI_SOAP_ENV, ELEM_HEADER);
  public static QName Q_ELEM_BODY =
    new QName(NS_URI_SOAP_ENV, ELEM_BODY);
  public static QName Q_ELEM_FAULT =
    new QName(NS_URI_SOAP_ENV, ELEM_FAULT);

  // Attribute names.
  public static final String ATTR_ENCODING_STYLE = "encodingStyle";
  public static final String ATTR_MUST_UNDERSTAND = "mustUnderstand";
  public static final String ATTR_TYPE = "type";
  public static final String ATTR_NULL = "null";
  public static final String ATTR_NIL  = "nil";
  public static final String ATTR_ARRAY_TYPE = "arrayType";
  public static final String ATTR_REFERENCE = "href";
  public static final String ATTR_ID = "id";

  // Qualified attribute names.
  public static QName Q_ATTR_MUST_UNDERSTAND =
    new QName(NS_URI_SOAP_ENV, ATTR_MUST_UNDERSTAND);

  // Attribute values.
  public static String ATTRVAL_TRUE = "true";

  // SOAP defined fault codes.
  public static String FAULT_CODE_VERSION_MISMATCH =
    NS_PRE_SOAP_ENV + ":VersionMismatch";
  public static String FAULT_CODE_MUST_UNDERSTAND =
    NS_PRE_SOAP_ENV + ":MustUnderstand";
  public static String FAULT_CODE_CLIENT = NS_PRE_SOAP_ENV + ":Client";
  public static String FAULT_CODE_SERVER = NS_PRE_SOAP_ENV + ":Server";
  public static String FAULT_CODE_PROTOCOL = NS_PRE_SOAP_ENV + ":Protocol";

  // XML-SOAP implementation defined fault codes.
  public static String FAULT_CODE_SERVER_BAD_TARGET_OBJECT_URI =
      Constants.FAULT_CODE_SERVER + ".BadTargetObjectURI";

  // Error messages.
  public static String ERR_MSG_VERSION_MISMATCH =
      FAULT_CODE_VERSION_MISMATCH +
      ": Envelope element must " +
      "be associated with " +
      "the '" +
      Constants.NS_URI_SOAP_ENV +
      "' namespace.";

  // Well-defined names for the 'bag' in SOAPContext
  public static String BAG_HTTPSERVLET = "HttpServlet" ;
  public static String BAG_HTTPSESSION = "HttpSession" ;
  public static String BAG_HTTPSERVLETREQUEST = "HttpServletRequest" ;
  public static String BAG_HTTPSERVLETRESPONSE = "HttpServletResponse" ;
  public static String BAG_DEPLOYMENTDESCRIPTOR = "DeploymentDescriptor" ;

  // Servlet init-parameter names.
  public static final String ENVELOPE_EDITOR_FACTORY = "EnvelopeEditorFactory";
  public static final String XML_PARSER = "XMLParser";
  public static final String CONFIGFILENAME = "ConfigFile";

  //////////////////////////////////////////////////////
  // Type QNames for the various schemas
  public static final QName string1999QName =
    new QName(Constants.NS_URI_1999_SCHEMA_XSD, "string");
  public static final QName int1999QName =
    new QName(Constants.NS_URI_1999_SCHEMA_XSD, "int");
  public static final QName decimal1999QName =
    new QName(Constants.NS_URI_1999_SCHEMA_XSD, "decimal");
  public static final QName float1999QName =
    new QName(Constants.NS_URI_1999_SCHEMA_XSD, "float");
  public static final QName double1999QName =
    new QName(Constants.NS_URI_1999_SCHEMA_XSD, "double");
  public static final QName date1999QName =
    new QName(Constants.NS_URI_1999_SCHEMA_XSD, "date");
  public static final QName boolean1999QName =
    new QName(Constants.NS_URI_1999_SCHEMA_XSD, "boolean");
  public static final QName long1999QName =
    new QName(Constants.NS_URI_1999_SCHEMA_XSD, "long");
  public static final QName short1999QName =
    new QName(Constants.NS_URI_1999_SCHEMA_XSD, "short");
  public static final QName byte1999QName =
    new QName(Constants.NS_URI_1999_SCHEMA_XSD, "byte");
  public static final QName hex1999QName =
    new QName(Constants.NS_URI_1999_SCHEMA_XSD, "hex");
  public static final QName qName1999QName =
    new QName(Constants.NS_URI_1999_SCHEMA_XSD, "QName");
  public static final QName timeInst1999QName =
    new QName(Constants.NS_URI_1999_SCHEMA_XSD, "timeInstant");
  public static final QName object1999QName =
    new QName(Constants.NS_URI_1999_SCHEMA_XSD, "ur-type");

  public static final QName string2000QName =
    new QName(Constants.NS_URI_2000_SCHEMA_XSD, "string");
  public static final QName int2000QName =
    new QName(Constants.NS_URI_2000_SCHEMA_XSD, "int");
  public static final QName decimal2000QName =
    new QName(Constants.NS_URI_2000_SCHEMA_XSD, "decimal");
  public static final QName float2000QName =
    new QName(Constants.NS_URI_2000_SCHEMA_XSD, "float");
  public static final QName double2000QName =
    new QName(Constants.NS_URI_2000_SCHEMA_XSD, "double");
  public static final QName date2000QName =
    new QName(Constants.NS_URI_2000_SCHEMA_XSD, "date");
  public static final QName boolean2000QName =
    new QName(Constants.NS_URI_2000_SCHEMA_XSD, "boolean");
  public static final QName long2000QName =
    new QName(Constants.NS_URI_2000_SCHEMA_XSD, "long");
  public static final QName short2000QName =
    new QName(Constants.NS_URI_2000_SCHEMA_XSD, "short");
  public static final QName byte2000QName =
    new QName(Constants.NS_URI_2000_SCHEMA_XSD, "byte");
  public static final QName hex2000QName =
    new QName(Constants.NS_URI_2000_SCHEMA_XSD, "hex");
  public static final QName qName2000QName =
    new QName(Constants.NS_URI_2000_SCHEMA_XSD, "QName");
  public static final QName timeInst2000QName =
    new QName(Constants.NS_URI_2000_SCHEMA_XSD, "timeInstant");
  public static final QName object2000QName =
    new QName(Constants.NS_URI_2000_SCHEMA_XSD, "anyType");

  public static final QName string2001QName =
    new QName(Constants.NS_URI_2001_SCHEMA_XSD, "string");
  public static final QName int2001QName =
    new QName(Constants.NS_URI_2001_SCHEMA_XSD, "int");
  public static final QName decimal2001QName =
    new QName(Constants.NS_URI_2001_SCHEMA_XSD, "decimal");
  public static final QName float2001QName =
    new QName(Constants.NS_URI_2001_SCHEMA_XSD, "float");
  public static final QName double2001QName =
    new QName(Constants.NS_URI_2001_SCHEMA_XSD, "double");
  public static final QName date2001QName =
    new QName(Constants.NS_URI_2001_SCHEMA_XSD, "date");
  public static final QName boolean2001QName =
    new QName(Constants.NS_URI_2001_SCHEMA_XSD, "boolean");
  public static final QName long2001QName =
    new QName(Constants.NS_URI_2001_SCHEMA_XSD, "long");
  public static final QName short2001QName =
    new QName(Constants.NS_URI_2001_SCHEMA_XSD, "short");
  public static final QName byte2001QName =
    new QName(Constants.NS_URI_2001_SCHEMA_XSD, "byte");
  public static final QName hex2001QName =
    new QName(Constants.NS_URI_2001_SCHEMA_XSD, "hexBinary");
  public static final QName qName2001QName =
    new QName(Constants.NS_URI_2001_SCHEMA_XSD, "QName");
  public static final QName timeInst2001QName =
    new QName(Constants.NS_URI_2001_SCHEMA_XSD, "dateTime");
  public static final QName object2001QName =
    new QName(Constants.NS_URI_2001_SCHEMA_XSD, "anyType");
}
