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
import org.apache.soap.util.*;
import org.apache.soap.util.xml.*;
import org.apache.soap.*;
import org.apache.soap.rpc.*;

/**
 * A <code>ParameterSerializer</code> is used to serialize and deserialize
 * parameters using the <code>SOAP-ENC</code> encoding style.
 *
 * @author Matthew J. Duftler (duftler@us.ibm.com)
 * @author Sander Brienen (sander.brienen@capgemini.nl)
 * @author Sanjiva Weerawarana (sanjiva@watson.ibm.com)
 */
public class ParameterSerializer implements Serializer, Deserializer
{
  public void marshall(String inScopeEncStyle, Class javaType, Object src,
                       Object context, Writer sink, NSStack nsStack,
                       XMLJavaMappingRegistry xjmr, SOAPContext ctx)
    throws IllegalArgumentException, IOException
  {
    nsStack.pushScope();

    Parameter param = (Parameter)src;
    Class type = param.getType();
    String name = param.getName();
    Object value = param.getValue();

    if (!(context instanceof PrefixedName))
    {
      context = name;
    }

    if (value == null && !type.isArray())
    {
      SoapEncUtils.generateNullStructure(inScopeEncStyle, type, context,
                                         sink, nsStack, xjmr);
    }
    else
    {
      String declEncStyle = param.getEncodingStyleURI();
      String actualEncStyle = declEncStyle != null
                              ? declEncStyle
                              : inScopeEncStyle;
      Serializer s = xjmr.querySerializer(type, actualEncStyle);

      s.marshall(inScopeEncStyle, type, value, context,
                 sink, nsStack, xjmr, ctx);
    }

    nsStack.popScope();
  }

  public Bean unmarshall(String inScopeEncStyle, QName elementType, Node src,
                         XMLJavaMappingRegistry xjmr, SOAPContext ctx)
    throws IllegalArgumentException
  {
    Element paramEl = (Element)src;
    String name = paramEl.getTagName();
    Bean bean = null;

    /*
      If the element contains an href= parameter, shortcut to
      MimePartSerializer.
    */
    String href = DOMUtils.getAttribute(paramEl, Constants.ATTR_REFERENCE);

    if (href != null)
    {
      // First, check to see if it's a local ref.
      if (href.length() > 0 && href.charAt(0) == '#')
      {
        href = href.substring(1);

        Element el =
          DOMUtils.getElementByID(src.getOwnerDocument().getDocumentElement(),
                                  href);

        if (el == null)
        {
          throw new IllegalArgumentException("No such ID '" + href + "'.");
        }

        QName soapType = SoapEncUtils.getTypeQName(el);

        if (soapType == null)
        {
          /*
            No xsi:type attribute found: determine the type as the 
            qualified name of the parameter element (if the parameter
            element is qualified) or as the qname formed by an empty
            namespace URI and the tag name of the parameter element.
            Is that a legal qname???
          */
          String paramNamespaceURI = paramEl.getNamespaceURI();

          if (paramNamespaceURI != null)
          {
            soapType = new QName(paramNamespaceURI, name);
          }
          else
          {
            soapType = new QName("", name);
          }
        }

        bean = xjmr.unmarshall(inScopeEncStyle, soapType, el, ctx);
      }
      else
      {
        bean = (new MimePartSerializer()).unmarshall(inScopeEncStyle,
                                                     elementType, src,
                                                     xjmr, ctx);
      }
    }
    else
    {
      QName soapType = SoapEncUtils.getTypeQName(paramEl);

      if (soapType == null)
      {
        /*
          No xsi:type attribute found: determine the type as the 
          qualified name of the parameter element (if the parameter
          element is qualified) or as the qname formed by an empty
          namespace URI and the tag name of the parameter element.
          Is that a legal qname???
        */
        String paramNamespaceURI = paramEl.getNamespaceURI();

        if (paramNamespaceURI != null)
        {
          soapType = new QName(paramNamespaceURI, name);
        }
        else
        {
          soapType = new QName("", name);
        }
      }

      bean = (SoapEncUtils.isNull(paramEl)
              && !new QName(Constants.NS_URI_SOAP_ENC,
                            "Array").equals(soapType)
              ? new Bean(xjmr.queryJavaType(soapType, inScopeEncStyle),
                         null)
              : xjmr.unmarshall(inScopeEncStyle, soapType, paramEl, ctx));
    }
    
    Parameter parameter = new Parameter(name, bean.type,
                                        bean.value, null);
    
    return new Bean(Parameter.class, parameter);
  }
}
