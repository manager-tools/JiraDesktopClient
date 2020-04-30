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
 * A <code>VectorSerializer</code> can be used to serialize (but not
 * deserialize) Vectors and Enumerations using the <code>SOAP-ENC</code>
 * encoding style.<p>
 * 
 * This serializer turns Vectors/Enumerations into SOAP arrays.
 * 
 * @author Glen Daniels (gdaniels@allaire.com)
 */
public class VectorSerializer implements Serializer, Deserializer
{
  public void marshall(String inScopeEncStyle, Class javaType, Object src,
                       Object context, Writer sink, NSStack nsStack,
                       XMLJavaMappingRegistry xjmr, SOAPContext ctx)
    throws IllegalArgumentException, IOException
  {
    nsStack.pushScope();

    if ((src != null) && 
                !(src instanceof Vector) &&
                !(src instanceof Enumeration))
      throw new IllegalArgumentException("Tried to pass a '" +
                        src.getClass().toString() + "' to VectorSerializer");

        String lengthStr;
        Enumeration enum;
        
        if (src instanceof Enumeration) {
                /** TODO: Right now we don't include a length on Enumerations,
                 * due to efficiency concerns.  There should be a way to configure
                 * doing the length calculation (at the cost of traversing the
                 * Enumeration) for a particular installation/service/call.
                 */
                enum = (Enumeration)src;
                lengthStr = "";
        } else {
                Vector v = (Vector)src;
                enum = v.elements();
        
                lengthStr = src != null
                       ? v.size() + ""
                       : "";
        }


    if (src == null)
    {
      SoapEncUtils.generateNullStructure(inScopeEncStyle,
                                     javaType,
                                     context,
                                     sink,
                                     nsStack,
                                     xjmr);
    }
    else
    {
      SoapEncUtils.generateStructureHeader(inScopeEncStyle,
                                       javaType,
                                       context,
                                       sink,
                                       nsStack,
                                       xjmr);

      sink.write(StringUtils.lineSeparator);

      for (Enumeration e = enum; e.hasMoreElements(); )
      {
        nsStack.pushScope();

        Object value = e.nextElement();

        if (value == null)
        {
          SoapEncUtils.generateNullStructure(Constants.NS_URI_SOAP_ENC,
                                             Object.class, "item", sink,
                                             nsStack, xjmr);
        }
        else
        {
          Class actualComponentType = value.getClass();

          xjmr.marshall(Constants.NS_URI_SOAP_ENC, actualComponentType, value,
                        "item", sink, nsStack, ctx);
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
	  if (SoapEncUtils.isNull(root)) {
		  return new Bean(Vector.class, null);
	  }
	  
	  Vector v = new Vector();
	  
	  Element tempEl = DOMUtils.getFirstChildElement(root);
	  while (tempEl != null) {
		  String declEncStyle = DOMUtils.getAttributeNS(tempEl,
														Constants.NS_URI_SOAP_ENV, Constants.ATTR_ENCODING_STYLE);
		  String actualEncStyle = declEncStyle != null
								  ? declEncStyle
									: inScopeEncStyle;

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
									
          QName declItemType = SoapEncUtils.getTypeQName(actualEl);
          QName actualItemType = declItemType;

          Bean itemBean = xjmr.unmarshall(actualEncStyle, actualItemType,
                                                  actualEl, ctx);

		  v.addElement(itemBean.value);

		  tempEl = DOMUtils.getNextSiblingElement(tempEl);
	  }
	  
	  return new Bean(Vector.class, v);
  }

}
