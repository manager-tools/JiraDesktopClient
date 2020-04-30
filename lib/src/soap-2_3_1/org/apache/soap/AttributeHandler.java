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
import org.apache.soap.util.xml.*;
import org.apache.soap.rpc.SOAPContext;

/**
 * An <code>AttributeHandler</code> maintains attributes and namespace
 * declarations for the other <em>SOAP</em> classes.
 *
 * @author Matthew J. Duftler (duftler@us.ibm.com)
 */
class AttributeHandler
{
  private Hashtable attributes             = new Hashtable();
  private Hashtable namespaceURIs2Prefixes = new Hashtable();
  private int       nsPrefixIndex          = 0;

  public AttributeHandler()
  {
    // Make sure to use "xmlns" as the prefix for any namespace declarations.
    namespaceURIs2Prefixes.put(Constants.NS_URI_XMLNS,
                               Constants.NS_PRE_XMLNS);
  }

  public void setAttribute(QName attrQName, String value)
  {
    attributes.put(attrQName, value);

    // If this is a namespace declaration, register the new prefix.
    if (attrQName.getNamespaceURI().equals(Constants.NS_URI_XMLNS))
    {
      namespaceURIs2Prefixes.put(value, attrQName.getLocalPart());
    }
  }

  public String getAttribute(QName attrQName)
  {
    return (String)attributes.get(attrQName);
  }

  public void removeAttribute(QName attrQName)
  {
    attributes.remove(attrQName);
  }

  private Enumeration getAttributeQNames()
  {
    generateNSDeclarations();

    return attributes.keys();
  }

  public void declareNamespace(String nsPrefix, String namespaceURI)
  {
    setAttribute(new QName(Constants.NS_URI_XMLNS, nsPrefix), namespaceURI);
  }

  private void generateNSDeclarations()
  {
    Enumeration keys = attributes.keys();

    while (keys.hasMoreElements())
    {
      QName qname = (QName)keys.nextElement();

      // Ensure that a prefix has been associated with this namespace URI.
      getPrefixFromURI(qname.getNamespaceURI());
    }
  }

  private String getPrefixFromURI(String namespaceURI)
  {
    if ("".equals(namespaceURI)) return null ;

    String nsPrefix = (String)namespaceURIs2Prefixes.get(namespaceURI);

    if (nsPrefix == null)
    {
      nsPrefix = "ns" + nsPrefixIndex++;

      setAttribute(new QName(Constants.NS_URI_XMLNS, nsPrefix), namespaceURI);
    }

    return nsPrefix;
  }

  public void populateNSStack(NSStack nsStack)
  {
    generateNSDeclarations();

    nsStack.pushScope();

    Enumeration e = namespaceURIs2Prefixes.keys();

    while (e.hasMoreElements())
    {
      String namespaceURI = (String)e.nextElement();
      String namespacePrefix = getPrefixFromURI(namespaceURI);

      if (namespacePrefix != null)
        nsStack.addNSDeclaration(namespacePrefix, namespaceURI);
    }
  }

  public String getUniquePrefixFromURI(String namespaceURI,
                                       String preferredPrefix,
                                       NSStack nsStack)
  {
    String retPrefix = nsStack.getPrefixFromURI(namespaceURI);

    if (retPrefix == null)
    {
      int prefixCount = 0;

      if (preferredPrefix == null)
      {
        preferredPrefix = "ns";
        prefixCount++;
      }

      while (retPrefix == null)
      {
        String newPrefix = preferredPrefix + (prefixCount > 0
                                              ? prefixCount + ""
                                              : "");

        // Is this prefix free?
        if (nsStack.getURIFromPrefix(newPrefix) == null)
        {
          // Have to declare the namespace, and update the namespace stack.
          nsStack.popScope();
          declareNamespace(newPrefix, namespaceURI);
          populateNSStack(nsStack);
          retPrefix = nsStack.getPrefixFromURI(namespaceURI);
        }
        else
        {
          prefixCount++;
        }
      }
    }

    return retPrefix;
  }

  public void marshall(Writer sink, SOAPContext ctx)
      throws IllegalArgumentException, IOException
  {
    Enumeration attrQNames = getAttributeQNames();

    while (attrQNames.hasMoreElements())
    {
      QName attrQName = (QName)attrQNames.nextElement();

      sink.write(' ') ;
      String nsPrefix ;
      if ((nsPrefix = getPrefixFromURI(attrQName.getNamespaceURI())) != null)
        sink.write(nsPrefix + ':') ;
      sink.write(attrQName.getLocalPart() + "=\"" +
                 getAttribute(attrQName) + '\"');
    }
  }

  public static AttributeHandler unmarshall(Node src, SOAPContext ctx)
    throws IllegalArgumentException
  {
    NamedNodeMap attrs = src.getAttributes();
    AttributeHandler attrHandler = new AttributeHandler();
    int size = attrs.getLength();

    for (int i = 0; i < size; i++)
    {
      Attr attr = (Attr)attrs.item(i);
      String namespaceURI = attr.getNamespaceURI();
      String localName = attr.getLocalName();
      String value = attr.getValue();

      attrHandler.setAttribute(new QName(namespaceURI, localName), value);
    }

    return attrHandler;
  }

  public String toString()
  {
    StringWriter sw = new StringWriter();

    try
    {
      sw.write("{");
      marshall(sw, new SOAPContext());
      sw.write("}");
    }
    catch (Exception e)
    {
    }

    return sw.toString();
  }
}
