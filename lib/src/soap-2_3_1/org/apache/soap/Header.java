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

import java.io.*;
import java.util.*;
import org.w3c.dom.*;
import org.apache.soap.util.*;
import org.apache.soap.util.xml.*;
import org.apache.soap.encoding.*;
import org.apache.soap.rpc.SOAPContext;

/**
 * A <code>Header</code> object represents the contents and semantics
 * of a <code>&lt;SOAP-ENV:Header&gt;</code> element.
 *
 * @author Matthew J. Duftler (duftler@us.ibm.com)
 * @author Sanjiva Weerawarana (sanjiva@watson.ibm.com)
 */
public class Header
{
  private Vector           headerEntries = null;
  private AttributeHandler attrHandler   = new AttributeHandler();

  public void setAttribute(QName attrQName, String value)
  {
    attrHandler.setAttribute(attrQName, value);
  }

  public String getAttribute(QName attrQName)
  {
    return attrHandler.getAttribute(attrQName);
  }

  public void removeAttribute(QName attrQName)
  {
    attrHandler.removeAttribute(attrQName);
  }

  public void declareNamespace(String nsPrefix, String namespaceURI)
  {
    attrHandler.declareNamespace(nsPrefix, namespaceURI);
  }

  public void setHeaderEntries(Vector headerEntries)
  {
    this.headerEntries = headerEntries;
  }

  public Vector getHeaderEntries()
  {
    return headerEntries;
  }

  public void marshall(Writer sink, NSStack nsStack,
                       XMLJavaMappingRegistry xjmr, SOAPContext ctx)
    throws IllegalArgumentException, IOException
  {
    attrHandler.populateNSStack(nsStack);

    // Determine the prefix associated with the NS_URI_SOAP_ENV namespace URI.
    String soapEnvNSPrefix = attrHandler.getUniquePrefixFromURI(
      Constants.NS_URI_SOAP_ENV, Constants.NS_PRE_SOAP_ENV, nsStack);

    // If there are header entries, generate the <SOAP-ENV:Header> element,
    // and serialize the header entries.
    if (headerEntries != null)
    {
      sink.write('<' + soapEnvNSPrefix + ':' + Constants.ELEM_HEADER);

      // Serialize any header attributes.
      attrHandler.marshall(sink, ctx);

      sink.write('>' + StringUtils.lineSeparator);

      // Serialize the header entries within the <SOAP-ENV:Header> element.
      for (Enumeration e = headerEntries.elements(); e.hasMoreElements();)
      {
        Element headerEntryEl = (Element)e.nextElement();

        Utils.marshallNode(headerEntryEl, sink);

        sink.write(StringUtils.lineSeparator);
      }

      sink.write("</" + soapEnvNSPrefix + ':' + Constants.ELEM_HEADER +
                 '>' + StringUtils.lineSeparator);
    }

    nsStack.popScope();
  }

  public static Header unmarshall(Node src, SOAPContext ctx)
    throws IllegalArgumentException
  {
    Element root          = (Element)src;
    Header  header        = new Header();
    Vector  headerEntries = new Vector();

    // Deserialize any header attributes.
    header.attrHandler = AttributeHandler.unmarshall(root, ctx);

    for (Element el = DOMUtils.getFirstChildElement(root);
                 el != null;
                 el = DOMUtils.getNextSiblingElement(el))
    {
      headerEntries.addElement(el);
    }

    header.setHeaderEntries(headerEntries);

    return header;
  }

  public String toString()
  {
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);

    pw.print("[Attributes=" + attrHandler + "] " +
             "[HeaderEntries={");

    if (headerEntries != null)
    {
      for (int i = 0; i < headerEntries.size(); i++)
      {
        if (i > 0)
        {
          pw.print(", ");
        }

        pw.print("[" + headerEntries.elementAt(i) + "]");
      }
    }

    pw.print("}]");

    return sw.toString();
  }
}
