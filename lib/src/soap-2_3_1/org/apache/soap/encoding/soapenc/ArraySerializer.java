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

import java.beans.*;
import java.io.*;
import java.util.*;
import java.lang.reflect.*;
import org.w3c.dom.*;
import org.apache.soap.util.*;
import org.apache.soap.util.xml.*;
import org.apache.soap.*;
import org.apache.soap.rpc.*;

/**
 * A <code>ArraySerializer</code> can be used to serialize and deserialize
 * arrays using the <code>SOAP-ENC</code> encoding style.
 *
 * @author Matthew J. Duftler (duftler@us.ibm.com)
 */
public class ArraySerializer implements Serializer, Deserializer
{
  public void marshall(String inScopeEncStyle, Class javaType, Object src,
                       Object context, Writer sink, NSStack nsStack,
                       XMLJavaMappingRegistry xjmr, SOAPContext ctx)
    throws IllegalArgumentException, IOException
  {
    nsStack.pushScope();

    String lengthStr = src != null
                       ? Array.getLength(src) + ""
                       : "";
    Class componentType = javaType.getComponentType();
    QName elementType = xjmr.queryElementType(componentType,
                                              Constants.NS_URI_SOAP_ENC);

    if (src == null)
    {
      SoapEncUtils.generateNullArray(inScopeEncStyle,
                                     javaType,
                                     context,
                                     sink,
                                     nsStack,
                                     xjmr,
                                     elementType,
                                     lengthStr);
    }
    else
    {
      SoapEncUtils.generateArrayHeader(inScopeEncStyle,
                                       javaType,
                                       context,
                                       sink,
                                       nsStack,
                                       xjmr,
                                       elementType,
                                       lengthStr);

      sink.write(StringUtils.lineSeparator);

      int length = Array.getLength(src);

      for (int i = 0; i < length; i++)
      {
        nsStack.pushScope();

        Object value = Array.get(src, i);

        if (value == null)
        {
          SoapEncUtils.generateNullStructure(inScopeEncStyle, componentType,
                                             "item", sink, nsStack, xjmr);
        }
        else
        {
          Class actualComponentType = value.getClass();

          xjmr.marshall(Constants.NS_URI_SOAP_ENC, actualComponentType, value, "item",
                        sink, nsStack, ctx);
        }

        sink.write(StringUtils.lineSeparator);
        nsStack.popScope();
      }

      sink.write("</" + context + '>');
    }

    nsStack.popScope();
  }

  public Bean unmarshall(String inScopeEncStyle, QName elementType, Node src,
                         XMLJavaMappingRegistry xjmr, SOAPContext ctx)
    throws IllegalArgumentException
  {
    Element root = (Element)src;
    String name = root.getTagName();
    QName arrayItemType = new QName("", "");
    Object array = getNewArray(inScopeEncStyle, root, arrayItemType, xjmr);

    if (SoapEncUtils.isNull(root))
    {
      return new Bean(array.getClass(), null);
    }

    Element tempEl = DOMUtils.getFirstChildElement(root);
    int length = Array.getLength(array);

    for (int i = 0; i < length; i++)
    {
      String declEncStyle = DOMUtils.getAttributeNS(tempEl,
        Constants.NS_URI_SOAP_ENV, Constants.ATTR_ENCODING_STYLE);
      String actualEncStyle = declEncStyle != null
                              ? declEncStyle
                              : inScopeEncStyle;
      QName declItemType = SoapEncUtils.getTypeQName(tempEl);
      QName actualItemType = declItemType != null
                             ? declItemType
                             : arrayItemType;

      // If it's a local reference, follow it.
      String href = tempEl.getAttribute(Constants.ATTR_REFERENCE);
      Element actualEl = tempEl;

      if(href != null && !href.equals("") && (href.charAt(0) == '#'))
      {
        href = href.substring(1);
        actualEl = DOMUtils.getElementByID(src.getOwnerDocument().getDocumentElement(),href);
        if (actualEl == null) {
          throw new IllegalArgumentException("No such ID '" + href + "'");
        }
      }

      if (!SoapEncUtils.isNull(actualEl))
      {
        Bean itemBean = xjmr.unmarshall(actualEncStyle,
                                        actualItemType,
                                        actualEl,
                                        ctx);

        Array.set(array, i, itemBean.value);
      }

      tempEl = DOMUtils.getNextSiblingElement(tempEl);
    }

    return new Bean(array.getClass(), array);
  }

  public static Object getNewArray(String inScopeEncStyle, Element arrayEl,
                                   QName arrayItemType,
                                   XMLJavaMappingRegistry xjmr)
    throws IllegalArgumentException
  {
    QName arrayTypeValue = SoapEncUtils.getAttributeValue(arrayEl,
      Constants.NS_URI_SOAP_ENC, Constants.ATTR_ARRAY_TYPE, "array", true);
    String arrayTypeValueNamespaceURI = arrayTypeValue.getNamespaceURI();
    String arrayTypeValueLocalPart = arrayTypeValue.getLocalPart();
    int leftBracketIndex = arrayTypeValueLocalPart.lastIndexOf('[');
    int rightBracketIndex = arrayTypeValueLocalPart.lastIndexOf(']');

    if (leftBracketIndex == -1
        || rightBracketIndex == -1
        || rightBracketIndex < leftBracketIndex)
    {
      throw new IllegalArgumentException("Malformed arrayTypeValue '" +
                                         arrayTypeValue + "'.");
    }

    String componentTypeName =
      arrayTypeValueLocalPart.substring(0, leftBracketIndex);

    if (componentTypeName.endsWith("]"))
    {
      throw new IllegalArgumentException("Arrays of arrays are not " +
                                         "supported '" + arrayTypeValue +
                                         "'.");
    }

    arrayItemType.setNamespaceURI(arrayTypeValueNamespaceURI);
    arrayItemType.setLocalPart(componentTypeName);

    int length = DOMUtils.countKids(arrayEl, Node.ELEMENT_NODE);
    String lengthStr =
      arrayTypeValueLocalPart.substring(leftBracketIndex + 1,
                                        rightBracketIndex);

    if (lengthStr.length() > 0)
    {
      if (lengthStr.indexOf(',') != -1)
      {
        throw new IllegalArgumentException("Multi-dimensional arrays are " +
                                           "not supported '" +
                                           lengthStr + "'.");
      }

      try
      {
        int explicitLength = Integer.parseInt(lengthStr);

        if (length != explicitLength)
        {
          throw new IllegalArgumentException("Explicit array length is " +
                                             "not equal to the number of " +
                                             "items '" + explicitLength +
                                             " != " + length + "'.");
        }
      }
      catch (NumberFormatException e)
      {
        throw new IllegalArgumentException("Explicit array length is not a " +
                                           "valid integer '" + lengthStr +
                                           "'.");
      }
    }

    Class componentType = xjmr.queryJavaType(arrayItemType, inScopeEncStyle);

    return Array.newInstance(componentType, length);
  }
}
