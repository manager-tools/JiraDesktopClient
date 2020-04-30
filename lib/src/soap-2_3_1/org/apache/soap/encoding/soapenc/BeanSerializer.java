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
 * A <code>BeanSerializer</code> can be used to serialize and deserialize
 * <em>JavaBeans</em> using the <code>SOAP-ENC</code> encoding style. The
 * public properties of the bean become named accessors.
 *
 * @author Matthew J. Duftler (duftler@us.ibm.com)
 */
public class BeanSerializer implements Serializer, Deserializer
{
  public void marshall(String inScopeEncStyle, Class javaType, Object src,
                       Object context, Writer sink, NSStack nsStack,
                       XMLJavaMappingRegistry xjmr, SOAPContext ctx)
    throws IllegalArgumentException, IOException
  {
    nsStack.pushScope();

    SoapEncUtils.generateStructureHeader(inScopeEncStyle,
                                         javaType,
                                         context,
                                         sink,
                                         nsStack,
                                         xjmr);

    sink.write(StringUtils.lineSeparator);

    PropertyDescriptor[] properties = getPropertyDescriptors(javaType);

    for (int i = 0; i < properties.length; i++)
    {
      String propName = properties[i].getName();
      Class propType = properties[i].getPropertyType();

      // Serialize every property except the "class" property.
      if (!propType.equals(Class.class))
      {
        Method propReadMethod = properties[i].getReadMethod();

        // Only serialize readable properties.
        if (propReadMethod != null)
        {
          Object propValue = null;

          // Get the property's value.
          try
          {
            if (src != null)
            {
              propValue = propReadMethod.invoke(src, new Object[]{});
            }
          }
          catch (Exception e)
          {
            throw new IllegalArgumentException("Unable to retrieve '" +
                                               propName + "' property " +
                                               "value: " + e.getMessage() +
                                               '.');
          }

          // Serialize the property.
          Parameter param = new Parameter(propName, propType, propValue, null);

          xjmr.marshall(Constants.NS_URI_SOAP_ENC, Parameter.class, param,
                        null, sink, nsStack, ctx);

          sink.write(StringUtils.lineSeparator);
        }
      }
    }

    sink.write("</" + context + '>');

    nsStack.popScope();
  }

  public Bean unmarshall(String inScopeEncStyle, QName elementType, Node src,
                         XMLJavaMappingRegistry xjmr, SOAPContext ctx)
    throws IllegalArgumentException
  {
    Element root = (Element)src;
    Element tempEl = DOMUtils.getFirstChildElement(root);
    Class javaType = xjmr.queryJavaType(elementType, inScopeEncStyle);
    Object bean = instantiateBean(javaType);
    PropertyDescriptor[] properties = getPropertyDescriptors(javaType);

    while (tempEl != null)
    {
      Bean paramBean = xjmr.unmarshall(inScopeEncStyle,
                                       RPCConstants.Q_ELEM_PARAMETER,
                                       tempEl, ctx);
      Parameter param = (Parameter)paramBean.value;
      Method propWriteMethod = getWriteMethod(param.getName(),
                                              properties,
                                              javaType);

      if (propWriteMethod != null)
      {
        // Set the property's value.
        try
        {
          propWriteMethod.invoke(bean, new Object[]{param.getValue()});
        }
        catch (Exception e)
        {
          throw new IllegalArgumentException("Unable to set '" +
                                             param.getName() +
                                             "' property: " +
                                             e.getMessage() + '.');
        }
      }

      tempEl = DOMUtils.getNextSiblingElement(tempEl);
    }

    return new Bean(javaType, bean);
  }

  private PropertyDescriptor[] getPropertyDescriptors(Class javaType)
    throws IllegalArgumentException
  {
    BeanInfo beanInfo = null;

    try
    {
      beanInfo = Introspector.getBeanInfo(javaType);
    }
    catch (IntrospectionException e)
    {
    }

    if (beanInfo != null)
    {
      PropertyDescriptor[] properties = beanInfo.getPropertyDescriptors();

      if (properties != null)
      {
        return properties;
      }
      else
      {
        throw new IllegalArgumentException("Unable to retrieve property " +
                                           "descriptors for '" +
                                           StringUtils.getClassName(javaType) +
                                           "'.");
      }
    }
    else
    {
      throw new IllegalArgumentException("Unable to retrieve BeanInfo " +
                                         "for '" + StringUtils.getClassName(
                                         javaType) + "'.");
    }
  }

  protected Method getWriteMethod(String propertyName,
                                       PropertyDescriptor[] pds,
                                       Class javaType)
  {
    for (int i = 0; i < pds.length; i++)
    {
      if (propertyName.equals(pds[i].getName()))
      {
        return pds[i].getWriteMethod();
      }
    }

    throw new IllegalArgumentException("Unable to retrieve " +
                                       "PropertyDescriptor for property '" +
                                       propertyName + "' of class '" +
                                       javaType + "'.");
  }

  private Object instantiateBean(Class javaType)
    throws IllegalArgumentException
  {
    try
    {
      return javaType.newInstance();
    }
    catch (Throwable t)
    {
      throw new IllegalArgumentException("Unable to instantiate '" +
                                         StringUtils.getClassName(javaType) +
                                         "': " + t.getMessage());
    }
  }
}
