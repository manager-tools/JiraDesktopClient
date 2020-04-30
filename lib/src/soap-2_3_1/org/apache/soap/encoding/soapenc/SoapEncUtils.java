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

package org.apache.soap.encoding.soapenc;

import java.io.*;
import org.w3c.dom.*;
import org.apache.soap.util.xml.*;
import org.apache.soap.*;

/**
 * These static methods can be used to do much of the repetitive and
 * mechanical work that is required when generating structures using
 * the <code>SOAP-ENC</code> encoding style.
 *
 * @author Matthew J. Duftler (duftler@us.ibm.com)
 */
public class SoapEncUtils
{
  public static void generateNullStructure(String inScopeEncStyle,
                                           Class javaType, Object context,
                                           Writer sink, NSStack nsStack,
                                           XMLJavaMappingRegistry xjmr)
    throws IllegalArgumentException, IOException
  {
    generateStructureHeader(inScopeEncStyle, javaType, context, sink,
                            nsStack, xjmr, null, null, true);
  }

  public static void generateNullArray(String inScopeEncStyle,
                                       Class javaType, Object context,
                                       Writer sink, NSStack nsStack,
                                       XMLJavaMappingRegistry xjmr,
                                       QName arrayElementType,
                                       String arrayLengthStr)
    throws IllegalArgumentException, IOException
  {
    generateStructureHeader(inScopeEncStyle, javaType, context, sink,
                            nsStack, xjmr, arrayElementType, arrayLengthStr,
                            true);
  }

  public static void generateArrayHeader(String inScopeEncStyle,
                                         Class javaType, Object context,
                                         Writer sink, NSStack nsStack,
                                         XMLJavaMappingRegistry xjmr,
                                         QName arrayElementType,
                                         String arrayLengthStr)
    throws IllegalArgumentException, IOException
  {
    generateStructureHeader(inScopeEncStyle, javaType, context, sink,
                            nsStack, xjmr, arrayElementType, arrayLengthStr,
                            false);
  }

  public static void generateStructureHeader(String inScopeEncStyle,
                                             Class javaType, Object context,
                                             Writer sink, NSStack nsStack,
                                             XMLJavaMappingRegistry xjmr)
    throws IllegalArgumentException, IOException
  {
    generateStructureHeader(inScopeEncStyle, javaType, context, sink,
                            nsStack, xjmr, null, null, false);
  }

  private static void generateStructureHeader(String inScopeEncStyle,
                                              Class javaType, Object context,
                                              Writer sink, NSStack nsStack,
                                              XMLJavaMappingRegistry xjmr,
                                              QName arrayElementType,
                                              String arrayLengthStr,
                                              boolean isNull)
    throws IllegalArgumentException, IOException
  {
    QName elementType = xjmr.queryElementType(javaType,
                                              Constants.NS_URI_SOAP_ENC);
    String namespaceDecl = "";

    if (context instanceof PrefixedName)
    {
      PrefixedName pname = (PrefixedName)context;
      QName qname = pname.getQName();

      if (qname != null)
      {
        String namespaceURI = qname.getNamespaceURI();

        if (namespaceURI != null && !namespaceURI.equals(""))
        {
          if (pname.getPrefix() == null)
          {
            String prefix = nsStack.getPrefixFromURI(namespaceURI);

            if (prefix == null)
            {
              prefix = nsStack.addNSDeclaration(namespaceURI);
              namespaceDecl = " xmlns:" + prefix + "=\"" + namespaceURI + '\"';
            }

            pname.setPrefix(prefix);
          }
        }
      }
    }

    sink.write('<' + context.toString() + namespaceDecl);

    // Get prefixes for the needed namespaces.
    String elementTypeNS = elementType.getNamespaceURI();
    String xsiNamespaceURI = Constants.NS_URI_CURRENT_SCHEMA_XSI;

    if (elementTypeNS.startsWith("http://www.w3.org/")
        && elementTypeNS.endsWith("/XMLSchema"))
    {
      xsiNamespaceURI = elementTypeNS + "-instance";
    }

    String xsiNSPrefix = nsStack.getPrefixFromURI(xsiNamespaceURI, sink);
    String elementTypeNSPrefix = nsStack.getPrefixFromURI(elementTypeNS, sink);

    sink.write(' ' + xsiNSPrefix + ':' + Constants.ATTR_TYPE + "=\"" +
               elementTypeNSPrefix + ':' +
               elementType.getLocalPart() + '\"');

    if (inScopeEncStyle == null
        || !inScopeEncStyle.equals(Constants.NS_URI_SOAP_ENC))
    {
      // Determine the prefix associated with the NS_URI_SOAP_ENV
      // namespace URI.
      String soapEnvNSPrefix = nsStack.getPrefixFromURI(
        Constants.NS_URI_SOAP_ENV, sink);

      sink.write(' ' + soapEnvNSPrefix + ':' +
                 Constants.ATTR_ENCODING_STYLE + "=\"" +
                 Constants.NS_URI_SOAP_ENC + '\"');
    }

    if (arrayElementType != null)
    {
      String arrayElementTypeNSPrefix = nsStack.getPrefixFromURI(
        arrayElementType.getNamespaceURI(), sink);
      String arrayTypeValue = arrayElementTypeNSPrefix + ':' +
                              arrayElementType.getLocalPart() +
                              '[' + arrayLengthStr + ']';
      String soapEncNSPrefix = nsStack.getPrefixFromURI(
        Constants.NS_URI_SOAP_ENC, sink);

      sink.write(' ' + soapEncNSPrefix + ':' +
                 Constants.ATTR_ARRAY_TYPE + "=\"" + arrayTypeValue + '\"');
    }

    if (isNull)
    {
      sink.write(' ' + xsiNSPrefix + ':' + nilName(xsiNamespaceURI) + "=\"" +
                 Constants.ATTRVAL_TRUE + "\"/");
    }

    sink.write('>');
  }

  private static String nilName(String currentSchemaXSI)
  {
    return (currentSchemaXSI.equals(Constants.NS_URI_2001_SCHEMA_XSI))
           ? Constants.ATTR_NIL
           : Constants.ATTR_NULL;
  }

  public static boolean isNull(Element element)
  {
    String nullValue = DOMUtils.getAttributeNS(element,
                                               Constants.NS_URI_2001_SCHEMA_XSI,
                                               Constants.ATTR_NIL);

    if (nullValue == null)
    {
      nullValue = DOMUtils.getAttributeNS(element,
                                          Constants.NS_URI_2000_SCHEMA_XSI,
                                          Constants.ATTR_NULL);
    }

    if (nullValue == null)
    {
      nullValue = DOMUtils.getAttributeNS(element,
                                          Constants.NS_URI_1999_SCHEMA_XSI,
                                          Constants.ATTR_NULL);
    }

    return nullValue != null && decodeBooleanValue(nullValue);
  }

  public static boolean decodeBooleanValue(String value)
  {
    switch (value.charAt(0))
    {
      case '0': case 'f': case 'F':
        return false;

      case '1': case 't': case 'T':
        return true;

      default:
        throw new IllegalArgumentException("Invalid boolean value: " + value);
    }
  }

  public static QName getAttributeValue(Element el,
                                        String attrNameNamespaceURI,
                                        String attrNameLocalPart,
                                        String elDesc,
                                        boolean isRequired)
    throws IllegalArgumentException
  {
    String attrValue = DOMUtils.getAttributeNS(el,
                                               attrNameNamespaceURI,
                                               attrNameLocalPart);

    if (attrValue != null)
    {
      int index = attrValue.indexOf(':');

      if (index != -1)
      {
        String attrValuePrefix       = attrValue.substring(0, index);
        String attrValueLocalPart    = attrValue.substring(index + 1);
        String attrValueNamespaceURI =
          DOMUtils.getNamespaceURIFromPrefix(el, attrValuePrefix);

        if (attrValueNamespaceURI != null)
        {
          return new QName(attrValueNamespaceURI, attrValueLocalPart);
        }
        else
        {
          throw new IllegalArgumentException("Unable to resolve namespace " +
                                             "URI for '" + attrValuePrefix +
                                             "'.");
        }
      }
      else
      {
        throw new IllegalArgumentException("The value of the '" +
                                           attrNameNamespaceURI + ':' +
                                           attrNameLocalPart +
                                           "' attribute must be " +
                                           "namespace-qualified.");
      }
    }
    else if (isRequired)
    {
      throw new IllegalArgumentException("The '" +
                                         attrNameNamespaceURI + ':' +
                                         attrNameLocalPart +
                                         "' attribute must be " +
                                         "specified for every " +
                                         elDesc + '.');
    }
    else
    {
      return null;
    }
  }

  /**
   * Get the value of the xsi:type attribute, for varying values of
   * the xsi namespace.
   */
  public static QName getTypeQName(Element el)
    throws IllegalArgumentException
  {
    // Try 2001
    QName typeQName = getAttributeValue(el, Constants.NS_URI_2001_SCHEMA_XSI,
                                        Constants.ATTR_TYPE, null, false);

    if (typeQName != null)
      return typeQName;

    // Try 2000
    typeQName = getAttributeValue(el, Constants.NS_URI_2000_SCHEMA_XSI,
                                  Constants.ATTR_TYPE, null, false);

    if (typeQName != null)
      return typeQName;

    // Try 1999
    typeQName = getAttributeValue(el, Constants.NS_URI_1999_SCHEMA_XSI,
                                  Constants.ATTR_TYPE, null, false);

    return typeQName;
  }
}
