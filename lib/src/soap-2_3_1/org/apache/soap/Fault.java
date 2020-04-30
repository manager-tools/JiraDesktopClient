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
import org.apache.soap.rpc.Parameter;
import org.apache.soap.rpc.RPCConstants;
import org.apache.soap.rpc.SOAPContext;

/**
 * A <code>Fault</code> object represents the contents and semantics
 * of a <code>&lt;SOAP-ENV:Fault&gt;</code> element.
 *
 * @author Matthew J. Duftler (duftler@us.ibm.com)
 * @author Sanjiva Weerawarana (sanjiva@watson.ibm.com)
 * @author Eric M. Dashofy (edashofy@ics.uci.edu)
 * @author Kevin J. Mitchell (kevin.mitchell@xmls.com)
 */
public class Fault
{
  private String           faultCode     = null;
  private String           faultString   = null;
  private String           faultActorURI = null;
  private Vector           detailEntries = null;
  private Vector           faultEntries  = null;
  private AttributeHandler attrHandler   = new AttributeHandler();

  public Fault() {}

  public Fault(SOAPException _soapException)
  {
    faultCode = _soapException.getFaultCode();
    faultString = _soapException.getMessage();
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

  public void setFaultCode(String faultCode)
  {
    this.faultCode = faultCode;
  }

  public String getFaultCode()
  {
    return faultCode;
  }

  public void setFaultString(String faultString)
  {
    this.faultString = faultString;
  }

  public String getFaultString()
  {
    return faultString;
  }

  public void setFaultActorURI(String faultActorURI)
  {
    this.faultActorURI = faultActorURI;
  }

  public String getFaultActorURI()
  {
    return faultActorURI;
  }

  public void setDetailEntries(Vector detailEntries)
  {
    this.detailEntries = detailEntries;
  }

  public Vector getDetailEntries()
  {
    return detailEntries;
  }

  public void setFaultEntries(Vector faultEntries)
  {
    this.faultEntries = faultEntries;
  }

  public Vector getFaultEntries()
  {
    return faultEntries;
  }

  public void marshall(String inScopeEncStyle, Writer sink, NSStack nsStack,
                       XMLJavaMappingRegistry xjmr, SOAPContext ctx)
    throws IllegalArgumentException, IOException
  {

    attrHandler.populateNSStack(nsStack);

    String faultCode      = getFaultCode();
    String faultString    = getFaultString();
    String faultActorURI  = getFaultActorURI();
    Vector detailEntries  = getDetailEntries();
    Vector faultEntries   = getFaultEntries();

    // Determine the prefix associated with the NS_URI_SOAP_ENV namespace URI.
    String soapEnvNSPrefix = attrHandler.getUniquePrefixFromURI(
      Constants.NS_URI_SOAP_ENV, Constants.NS_PRE_SOAP_ENV, nsStack);

    // Generate the required <SOAP-ENV:Fault>, <faultcode> and
    // <faultstring> elements.
    sink.write('<' + soapEnvNSPrefix + ':' + Constants.ELEM_FAULT);

    // Serialize any fault attributes.
    attrHandler.marshall(sink, ctx);

    sink.write('>' + StringUtils.lineSeparator +
               '<' + Constants.ELEM_FAULT_CODE + '>' + faultCode +
               "</" + Constants.ELEM_FAULT_CODE + '>' +
               StringUtils.lineSeparator +
               '<' + Constants.ELEM_FAULT_STRING + '>' + faultString +
               "</" + Constants.ELEM_FAULT_STRING + '>' +
               StringUtils.lineSeparator);

    // Generate the <faultactor> element if a value is present.
    if (faultActorURI != null)
    {
      sink.write('<' + Constants.ELEM_FAULT_ACTOR + '>' + faultActorURI +
                 "</" + Constants.ELEM_FAULT_ACTOR + '>' +
                 StringUtils.lineSeparator);
    }

    // If there are detail entries, generate the <detail> element,
    // and serialize the detail entries.
    if (detailEntries != null)
    {
      sink.write('<' + Constants.ELEM_DETAIL + '>' +
                 StringUtils.lineSeparator);

      // Serialize the detail entries within the <detail> element.
      for (Enumeration e = detailEntries.elements(); e.hasMoreElements();)
      {
        Object detailEntry = e.nextElement();

        // If the detail entry is an Element, just write it out.
        if (detailEntry instanceof Element)
        {
          Element detailEntryEl = (Element)detailEntry;

          Utils.marshallNode(detailEntryEl, sink);
          sink.write(StringUtils.lineSeparator);
        }
        /*
          If the detail entry is a Parameter, try to find a serializer.
          If there is an error, write nothing.
        */
        else if (detailEntry instanceof Parameter)
        {
          try
          {
            Parameter detailEntryParameter = (Parameter)detailEntry;
            Serializer s = xjmr.querySerializer(Parameter.class, inScopeEncStyle);

            if (s != null)
            {
              s.marshall(null,
                         Parameter.class,
                         detailEntryParameter,
                         Constants.ELEM_FAULT_DETAIL_ENTRY,
                         sink,
                         nsStack,
                         xjmr,
                         ctx);
              sink.write(StringUtils.lineSeparator);
            }
            else
            {
              throw new IllegalArgumentException("Could not find Parameter " +
                                                 "serializer.");
            }
          }
          catch (IllegalArgumentException iae)
          {
          }
        }
      }

      sink.write("</" + Constants.ELEM_DETAIL + '>' +
                 StringUtils.lineSeparator);
    }

    // Serialize any fault entries (in addition to <faultcode>, <faultstring>,
    // <faultactor>, and <detail>).
    if (faultEntries != null)
    {
      for (Enumeration e = faultEntries.elements(); e.hasMoreElements();)
      {
        Element faultEntryEl = (Element)e.nextElement();

        Utils.marshallNode(faultEntryEl, sink);

        sink.write(StringUtils.lineSeparator);
      }
    }

    sink.write("</" + soapEnvNSPrefix + ':' + Constants.ELEM_FAULT + '>' +
               StringUtils.lineSeparator);

    nsStack.popScope();
  }

  public static Fault unmarshall(String inScopeEncStyle, Node src,
                                 XMLJavaMappingRegistry xjmr, SOAPContext ctx)
    throws IllegalArgumentException
  {
    Element root  = (Element)src;
    Fault   fault = new Fault();


    if (Constants.Q_ELEM_FAULT.matches(root))
    {
      Element faultCodeEl   = null;
      Element faultStringEl = null;
      Element faultActorEl  = null;
      Element detailEl      = null;
      Vector  faultEntries  = new Vector();
      Element tempEl        = DOMUtils.getFirstChildElement(root);

      // Deserialize any fault attributes.
      fault.attrHandler = AttributeHandler.unmarshall(root, ctx);

      // Examine the subelements of the fault.
      while (tempEl != null)
      {
        String namespaceURI = tempEl.getNamespaceURI();
        String localPart    = tempEl.getLocalName();

        if (localPart == null)
        {
          localPart = tempEl.getTagName();
        }

        // SOAP-ENV namespace is ok, as is no namespace at all.
        if (namespaceURI == null
            || namespaceURI.equals(Constants.NS_URI_SOAP_ENV))
        {
          if (localPart.equals(Constants.ELEM_FAULT_CODE))
          {
            faultCodeEl = tempEl;
          }
          else if (localPart.equals(Constants.ELEM_FAULT_STRING))
          {
            faultStringEl = tempEl;
          }
          else if (localPart.equals(Constants.ELEM_FAULT_ACTOR))
          {
            faultActorEl = tempEl;
          }
          else if (localPart.equals(Constants.ELEM_DETAIL))
          {
            detailEl = tempEl;
          }
          else
          {
            // This must be an additional fault entry.
            faultEntries.addElement(tempEl);
          }
        }
        else
        {
          // This must be an additional fault entry.
          faultEntries.addElement(tempEl);
        }

        tempEl = DOMUtils.getNextSiblingElement(tempEl);
      }

      // Deserialize the required <faultcode> element.
      if (faultCodeEl != null)
      {
        String faultCode = DOMUtils.getChildCharacterData(faultCodeEl);

        fault.setFaultCode(faultCode);
      }
      else
      {
        throw new IllegalArgumentException("A '" + Constants.Q_ELEM_FAULT +
                                           "' element must contain a: '" +
                                           Constants.ELEM_FAULT_CODE +
                                           "' element.");
      }

      // Deserialize the required <faultstring> element.
      if (faultStringEl != null)
      {
        String faultString = DOMUtils.getChildCharacterData(faultStringEl);

        fault.setFaultString(faultString);
      }
      else
      {
        throw new IllegalArgumentException("A '" + Constants.Q_ELEM_FAULT +
                                           "' element must contain a: '" +
                                           Constants.ELEM_FAULT_STRING +
                                           "' element.");
      }

      // Deserialize the <faultactor> element, if present.
      if (faultActorEl != null)
      {
        String faultActorURI = DOMUtils.getChildCharacterData(faultActorEl);

        fault.setFaultActorURI(faultActorURI);
      }

      // Deserialize any detail entries.
      if (detailEl != null)
      {
        Vector detailEntries = new Vector();

        for (Element el = DOMUtils.getFirstChildElement(detailEl);
                     el != null;
                     el = DOMUtils.getNextSiblingElement(el))
        {
          // Try to deserialize. If it fails, just add element to list.
          try
          {
            String declEncStyle =
              DOMUtils.getAttributeNS(el,
                                      Constants.NS_URI_SOAP_ENV,
                                      Constants.ATTR_ENCODING_STYLE);
            String actualEncStyle = declEncStyle != null
                                    ? declEncStyle
                                    : inScopeEncStyle;
            Bean paramBean = xjmr.unmarshall(declEncStyle,
                                             RPCConstants.Q_ELEM_PARAMETER,
                                             el,
                                             ctx);
            Parameter param = (Parameter)paramBean.value;

            detailEntries.addElement(param);
          }
          catch (Exception e)
          {
            detailEntries.addElement(el);
          }
        }

        fault.setDetailEntries(detailEntries);
      }

      // Set the faultEntries property, if any additional fault entries
      // were encountered.
      if (faultEntries.size() > 0)
      {
        fault.setFaultEntries(faultEntries);
      }
    }
    else
    {
      throw new IllegalArgumentException("Root element of a SOAP Fault " +
                                         "must be: '" +
                                         Constants.Q_ELEM_FAULT + "'.");
    }

    return fault;
  }

  public String toString()
  {
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);

    pw.print("[Attributes=" + attrHandler + "] " +
             "[faultCode=" + faultCode + "] " +
             "[faultString=" + faultString + "] " +
             "[faultActorURI=" + faultActorURI + "] " +
             "[DetailEntries=");

    if (detailEntries != null)
    {
      pw.println();

      for (int i = 0; i < detailEntries.size(); i++)
      {
        Object detailEl = detailEntries.elementAt(i);

        if (detailEl instanceof Parameter)
        {
          Parameter param = (Parameter)detailEl;

          pw.println("[(" + i + ")=" + param +"]");
        }
        else
        {
          pw.println("[(" + i + ")=" +
                     DOM2Writer.nodeToString((Element)detailEl) + "]");
        }
      }
    }

    pw.print("] [FaultEntries=");

    if (faultEntries != null)
    {
      pw.println();

      for (int i = 0; i < faultEntries.size(); i++)
      {
        pw.println("[(" + i + ")=" +
                DOM2Writer.nodeToString((Element)faultEntries.elementAt(i)) +
                "]");
      }
    }

    pw.print("]");

    return sw.toString();
  }
}
