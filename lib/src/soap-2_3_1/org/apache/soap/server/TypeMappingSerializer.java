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

package org.apache.soap.server;

import java.util.*;
import java.io.*;
import org.w3c.dom.*;
import org.apache.soap.util.Bean;
import org.apache.soap.util.StringUtils;
import org.apache.soap.util.xml.*;
import org.apache.soap.Constants;
import org.apache.soap.rpc.SOAPContext;
import org.apache.soap.encoding.*;
import org.apache.soap.encoding.soapenc.SoapEncUtils;

/**
 * Serialize and deserialize type mappings according to SOAP-Enc.
 *
 * @author Sanjiva Weerawarana (sanjiva@watson.ibm.com)
 * @author Matthew J. Duftler (duftler@us.ibm.com)
 */
public class TypeMappingSerializer implements Serializer, Deserializer {
  /**
   * This is the Serializer interface .. called to serialize an instance
   * of a TypeMapping (the src arg)
   * @param inScopeEncStyle the encoding style currently in scope
   * @param javaType should be org.apache.soap.server.TypeMapping.class
   * @param src instance to serialize as XML
   * @param context accessor name
   * @param sink the writer to write XML into
   * @param nsStack namespace stack
   * @param xjmr unused
   */
  public void marshall (String inScopeEncStyle, Class javaType, Object src,
                        Object context, Writer sink, NSStack nsStack,
                        XMLJavaMappingRegistry xjmr, SOAPContext ctx)
       throws IllegalArgumentException, IOException {
    TypeMapping tm = (TypeMapping) src;

    nsStack.pushScope ();
    SoapEncUtils.generateStructureHeader (inScopeEncStyle, javaType, context, 
                                          sink, nsStack, xjmr);
    sink.write (StringUtils.lineSeparator);

    // these namespaces being defined by the envelope stuff
    String xsiPrefix = nsStack.getPrefixFromURI (Constants.NS_URI_CURRENT_SCHEMA_XSI);
    String xsdPrefix = nsStack.getPrefixFromURI (Constants.NS_URI_CURRENT_SCHEMA_XSD);
    if ((xsiPrefix == null) || (xsdPrefix == null)) {
      throw new IllegalArgumentException ("required namespace names '" +
                                          Constants.NS_URI_CURRENT_SCHEMA_XSI + 
                                          "' and/or '" +
                                          Constants.NS_URI_CURRENT_SCHEMA_XSD + 
                                          "' is not defined.");
    }

    if (tm.encodingStyle != null) {
      sink.write ("<encodingStyle " + xsiPrefix + ":type=\"" + xsdPrefix +
                  ":string\">" + tm.encodingStyle + "</encodingStyle>");
      sink.write (StringUtils.lineSeparator);
    }

    if (tm.elementType != null) {
      sink.write ("<elementType-ns " + xsiPrefix + ":type=\"" + xsdPrefix + 
                  ":string\">" + tm.elementType.getNamespaceURI () +
                  "</elementType-ns>");
      sink.write (StringUtils.lineSeparator);
      
      sink.write ("<elementType-lp " + xsiPrefix + ":type=\"" + xsdPrefix + 
                  ":string\">" + tm.elementType.getLocalPart () +
                  "</elementType-lp>");
      sink.write (StringUtils.lineSeparator);
    }

    if (tm.javaType != null) {
      sink.write ("<javaType " + xsiPrefix + ":type=\"" + xsdPrefix + 
                  ":string\">" + tm.javaType + "</javaType>");
      sink.write (StringUtils.lineSeparator);
    }

    if (tm.xml2JavaClassName != null) {
      sink.write ("<xml2JavaClassName " + xsiPrefix + ":type=\"" + xsdPrefix + 
                  ":string\">" + tm.xml2JavaClassName + 
                  "</xml2JavaClassName>");
      sink.write (StringUtils.lineSeparator);
    }

    if (tm.java2XMLClassName != null) {
      sink.write ("<java2XMLClassName " + xsiPrefix + ":type=\"" + xsdPrefix + 
                  ":string\">" + tm.java2XMLClassName + 
                  "</java2XMLClassName>");
      sink.write (StringUtils.lineSeparator);
    }

    sink.write ("</" + context + '>');
    nsStack.popScope ();
  }


  /**
   * The deserializer interface.
   */
  public Bean unmarshall (String inScopeEncStyle, QName elementType, Node src,
                          XMLJavaMappingRegistry xjmr, SOAPContext ctx)
       throws IllegalArgumentException {
    NodeList nl = src.getChildNodes ();
    int nKids = nl.getLength ();

    // info for the type mapping object
    String encodingStyle = null;
    String elTypeNS = null;
    String elTypeLP = null;
    QName qname = null;
    String javaType = null;
    String java2XMLClassName = null;
    String xml2JavaClassName = null;

    for (int i = 0; i < nKids; i++) {
      Node n = nl.item (i);
      if (n.getNodeType () != Node.ELEMENT_NODE) {
        continue;
      }
      Element e = (Element) n;
      String tagName = e.getTagName ();
      String elData = DOMUtils.getChildCharacterData (e);
      if (tagName.equals ("encodingStyle")) {
        encodingStyle = elData;
      } else if (tagName.equals ("elementType-ns")) {
        elTypeNS = elData;
      } else if (tagName.equals ("elementType-lp")) {
        elTypeLP = elData;
      } else if (tagName.equals ("javaType")) {
        javaType = elData;
      } else if (tagName.equals ("java2XMLClassName")) {
        java2XMLClassName = elData;
      } else if (tagName.equals ("xml2JavaClassName")) {
        xml2JavaClassName = elData;
      } else {
        throw new IllegalArgumentException ("unknown element '" +
                                            tagName + "' while " + 
                                            "unmarshalling a TypeMapping");
      }
    }

    if (elTypeNS != null && elTypeLP != null) {
      qname = new QName (elTypeNS, elTypeLP);
    }

    return new Bean (TypeMapping.class,
                     new TypeMapping (encodingStyle,
                                      qname,
                                      javaType,
                                      java2XMLClassName, xml2JavaClassName));
  }
}
