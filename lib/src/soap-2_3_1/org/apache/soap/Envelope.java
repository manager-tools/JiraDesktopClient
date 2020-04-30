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
 * An <code>Envelope</code> object represents the contents and semantics
 * of an <code>&lt;SOAP-ENV:Envelope&gt;</code> element.
 *
 * @author Matthew J. Duftler (duftler@us.ibm.com)
 * @author Sanjiva Weerawarana (sanjiva@watson.ibm.com)
 */
public class Envelope
{
  private Header           header          = null;
  private Body             body            = null;
  private Vector           envelopeEntries = null;
  private AttributeHandler attrHandler     = new AttributeHandler();

  public Envelope()
  {
    // Declare the "SOAP-ENV" namespace.
    declareNamespace(Constants.NS_PRE_SOAP_ENV,
                     Constants.NS_URI_SOAP_ENV);

    // Declare the "xsi" namespace.
    declareNamespace(Constants.NS_PRE_SCHEMA_XSI,
                     Constants.NS_URI_CURRENT_SCHEMA_XSI);

    // Declare the "xsd" namespace.
    declareNamespace(Constants.NS_PRE_SCHEMA_XSD,
                     Constants.NS_URI_CURRENT_SCHEMA_XSD);
  }

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

  public void setHeader(Header header)
  {
    this.header = header;
  }

  public Header getHeader()
  {
    return header;
  }

  public void setBody(Body body)
  {
    this.body = body;
  }

  public Body getBody()
  {
    return body;
  }

  public void setEnvelopeEntries(Vector envelopeEntries)
  {
    this.envelopeEntries = envelopeEntries;
  }

  public Vector getEnvelopeEntries()
  {
    return envelopeEntries;
  }

  /**
   * Supported for backward compatibility.
   *
   * @see #marshall(java.io.Writer, org.apache.soap.util.xml.XMLJavaMappingRegistry, org.apache.soap.rpc.SOAPContext)
   */
  public void marshall(Writer sink, XMLJavaMappingRegistry xjmr)
    throws IllegalArgumentException, IOException {
    marshall(sink, xjmr, new SOAPContext());
  }

  public void marshall(Writer sink, XMLJavaMappingRegistry xjmr,
                       SOAPContext ctx)
    throws IllegalArgumentException, IOException
  {
    // Initialize the namespace stack.
    NSStack nsStack = new NSStack();

    attrHandler.populateNSStack(nsStack);

    Header header          = getHeader();
    Body   body            = getBody();
    Vector envelopeEntries = getEnvelopeEntries();
    String declEncStyle    = getAttribute(new QName(
      Constants.NS_URI_SOAP_ENV, Constants.ATTR_ENCODING_STYLE));

    // Determine the prefix associated with the NS_URI_SOAP_ENV namespace URI.
    String soapEnvNSPrefix = attrHandler.getUniquePrefixFromURI(
      Constants.NS_URI_SOAP_ENV, Constants.NS_PRE_SOAP_ENV, nsStack);

    // Write XML declaration
    sink.write(Constants.XML_DECL);

    sink.write('<' + soapEnvNSPrefix + ':' + Constants.ELEM_ENVELOPE);

    // Serialize any envelope attributes.
    attrHandler.marshall(sink, ctx);

    sink.write('>' + StringUtils.lineSeparator);

    // If there is a header, serialize it.
    if (header != null)
    {
      header.marshall(sink, nsStack, xjmr, ctx);
    }

    // There must always be a <SOAP-ENV:Body>.
    if (body != null)
    {
      body.marshall(declEncStyle, sink, nsStack, xjmr, ctx);
    }
    else
    {
      throw new IllegalArgumentException("An '" + Constants.Q_ELEM_ENVELOPE +
                                         "' must contain a: '" +
                                         Constants.Q_ELEM_BODY + "'.");
    }

    // Serialize any envelope entries (in addition to <SOAP-ENV:Body>).
    if (envelopeEntries != null)
    {
      for (Enumeration e = envelopeEntries.elements(); e.hasMoreElements();)
      {
        Element envelopeEntryEl = (Element)e.nextElement();

        Utils.marshallNode(envelopeEntryEl, sink);

        sink.write(StringUtils.lineSeparator);
      }
    }

    sink.write("</" + soapEnvNSPrefix + ':' + Constants.ELEM_ENVELOPE + 
               '>' + StringUtils.lineSeparator);
  }

  /**
   * Supported for backward compatibility.
   *
   * @see #unmarshall(org.w3c.dom.Node, org.apache.soap.rpc.SOAPContext)
   */
  public static Envelope unmarshall(Node src)
    throws IllegalArgumentException {
      return unmarshall(src, new SOAPContext());
  }

  public static Envelope unmarshall(Node src, SOAPContext ctx)
    throws IllegalArgumentException
  {
    Element  root = (Element)src;
    Envelope env  = new Envelope();

    if (Constants.Q_ELEM_ENVELOPE.matches(root))
    {
      // Deserialize any envelope attributes.
      env.attrHandler = AttributeHandler.unmarshall(root, ctx);

      // Examine the subelements of the envelope.
      Element headerEl = null;
      Element bodyEl   = null;
      Element tempEl   = DOMUtils.getFirstChildElement(root);

      if (Constants.Q_ELEM_HEADER.matches(tempEl))
      {
        headerEl = tempEl;
        tempEl = DOMUtils.getNextSiblingElement(tempEl);
      }

      if (Constants.Q_ELEM_BODY.matches(tempEl))
      {
        bodyEl = tempEl;
        tempEl = DOMUtils.getNextSiblingElement(tempEl);
      }

      // Deserialize any header entries.
      if (headerEl != null)
      {
        Header header = Header.unmarshall(headerEl, ctx);

        env.setHeader(header);
      }

      // Deserialize any body entries.
      if (bodyEl != null)
      {
        Body body = Body.unmarshall(bodyEl, ctx);

        env.setBody(body);
      }
      else
      {
        throw new IllegalArgumentException("An '" + Constants.Q_ELEM_ENVELOPE +
                                           "' element must contain a: '" +
                                           Constants.Q_ELEM_BODY +
                                           "' element.");
      }

      // Deserialize any envelope entries (in addition to <SOAP-ENV:Body>).
      if (tempEl != null)
      {
        Vector envelopeEntries = new Vector();

        while (tempEl != null)
        {
          envelopeEntries.addElement(tempEl);
          tempEl = DOMUtils.getNextSiblingElement(tempEl);
        }

        env.setEnvelopeEntries(envelopeEntries);
      }
    }
    else
    {
      String localName = root.getLocalName();

      if (localName != null && localName.equals(Constants.ELEM_ENVELOPE))
      {
        throw new IllegalArgumentException(Constants.ERR_MSG_VERSION_MISMATCH);
      }
      else
      {
        throw new IllegalArgumentException("Root element of a SOAP message " +
                                           "must be: '" +
                                           Constants.Q_ELEM_ENVELOPE + "'.");
      }
    }

    return env;
  }

  public String toString()
  {
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);

    pw.print("[Attributes=" + attrHandler + "] " +
             "[Header=" + header + "] " +
             "[Body=" + body + "] " +
             "[EnvelopeEntries=");

    if (envelopeEntries != null)
    {
      pw.println();

      for (int i = 0; i < envelopeEntries.size(); i++)
      {
        pw.println("[(" + i + ")=" +
             DOM2Writer.nodeToString((Element)envelopeEntries.elementAt(i)) +
             "]");
      }
    }

    pw.print("]");

    return sw.toString();
  }
}
