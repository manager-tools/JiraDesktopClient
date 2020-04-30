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
 * A <code>HashtableSerializer</code> can be used to serialize and
 * deserialize Hashtables using the <code>SOAP-ENC</code>
 * encoding style.<p>
 * 
 * TODO: This should eventually deal with Maps, but doesn't yet.
 * 
 * @author Glen Daniels (gdaniels@allaire.com)
 */
public class HashtableSerializer implements Serializer, Deserializer
{
  private static final String STR_KEY = "key";
  private static final String STR_ITEM = "item";
  private static final String STR_VALUE = "value";
  
  public void marshall(String inScopeEncStyle, Class javaType, Object src,
                       Object context, Writer sink, NSStack nsStack,
                       XMLJavaMappingRegistry xjmr, SOAPContext ctx)
    throws IllegalArgumentException, IOException
  {
    if (src == null)
    {
      SoapEncUtils.generateNullStructure(inScopeEncStyle,
                                         javaType,
                                         context,
                                         sink,
                                         nsStack,
                                         xjmr);
    }
    else if (src instanceof Hashtable)
    {
      SoapEncUtils.generateStructureHeader(inScopeEncStyle,
                                           javaType,
                                           context,
                                           sink,
                                           nsStack,
                                           xjmr);

      sink.write(StringUtils.lineSeparator);

      Hashtable hash = (Hashtable)src;

      for (Enumeration e = hash.keys(); e.hasMoreElements(); )
      {
        Object key = e.nextElement();
        Object value = hash.get(key);

        sink.write("<" + STR_ITEM + ">");
        sink.write(StringUtils.lineSeparator);

        // ??? Deal with null keys?
        xjmr.marshall(Constants.NS_URI_SOAP_ENC, key.getClass(), key, STR_KEY,
                      sink, nsStack, ctx);
        sink.write(StringUtils.lineSeparator);

        if (value == null)
        {
          SoapEncUtils.generateNullStructure(Constants.NS_URI_SOAP_ENC,
                                             Object.class, STR_VALUE, sink,
                                             nsStack, xjmr);
        }
        else
        {
          Class actualComponentType = value.getClass();

          xjmr.marshall(Constants.NS_URI_SOAP_ENC, actualComponentType, value,
                        STR_VALUE, sink, nsStack, ctx);
        }

        sink.write(StringUtils.lineSeparator);
        sink.write("</" + STR_ITEM + ">");
        sink.write(StringUtils.lineSeparator);
      }

      sink.write("</" + context + '>');
    }
    else
    {
      throw new IllegalArgumentException("Tried to pass a '" +
                                         src.getClass().toString() +
                                         "' to HashtableSerializer");
    }
  }

  public Bean unmarshall(String inScopeEncStyle, QName elementType, Node src,
                         XMLJavaMappingRegistry xjmr, SOAPContext ctx)
    throws IllegalArgumentException
  {
    Element root = (Element)src;
    String name = root.getTagName();

    if (SoapEncUtils.isNull(root))
    {
      return new Bean(Hashtable.class, null);
    }

    Hashtable hash = new Hashtable();
    Element tempEl = DOMUtils.getFirstChildElement(root);

    while (tempEl != null) {
      // got an item
      Element keyEl = DOMUtils.getFirstChildElement(tempEl);
      String tagName = keyEl.getTagName();

      if (!tagName.equalsIgnoreCase(STR_KEY))
      {
        throw new IllegalArgumentException("Got <" + tagName +
                                           "> tag when expecting <" +
                                           STR_KEY + ">");
      }

      Element valEl = DOMUtils.getNextSiblingElement(keyEl);

      tagName = valEl.getTagName();

      if (!tagName.equalsIgnoreCase("value"))
      {
        throw new IllegalArgumentException("Got <" + tagName + 
                                           "> tag when expecting <" +
                                           STR_VALUE + ">");
      }

      Bean keyBean = unmarshallEl(inScopeEncStyle, xjmr, keyEl, ctx);
      Bean valBean = unmarshallEl(inScopeEncStyle, xjmr, valEl, ctx);

      hash.put(keyBean.value, valBean.value);

      tempEl = DOMUtils.getNextSiblingElement(tempEl);
    }

    return new Bean(Hashtable.class, hash);
  }

  private Bean unmarshallEl(String inScopeEncStyle,
                            XMLJavaMappingRegistry xjmr,
                            Element targetEl, SOAPContext ctx)
  {
    String declEncStyle = DOMUtils.getAttributeNS(targetEl,
        Constants.NS_URI_SOAP_ENV, Constants.ATTR_ENCODING_STYLE);
    String actualEncStyle = (declEncStyle != null)
                            ? declEncStyle
                            : inScopeEncStyle;
    QName declItemType = SoapEncUtils.getTypeQName(targetEl);

    return xjmr.unmarshall(actualEncStyle, declItemType, targetEl, ctx);
  }
}
