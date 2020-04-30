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

package org.apache.soap.util.xml;

// JAXP packages
import javax.xml.parsers.*;
import org.xml.sax.*;
import org.xml.sax.helpers.*;
import org.w3c.dom.*;
import org.apache.soap.Constants;

/**
 * XML Parser Utilities
 * Provides methods to set and use JAXP compatible XML parsers.
 *
 * @author Ruth Bergman (ruth@alum.mit.edu)
 * @author Matthew J. Duftler (duftler@us.ibm.com)
 */
public class XMLParserUtils {
  private static DocumentBuilderFactory dbf = null;

  static {
    // Create a default instance.
    refreshDocumentBuilderFactory(null, true, false);
  }

  /**
   * Causes the private DocumentBuilderFactory reference to point
   * to a new instance of a DocumentBuilderFactory. This method
   * only needs to be called if you want to specify a different
   * DocumentBuilderFactory implementation then that specified
   * prior to this class being initialized. Or, if you want to
   * specify different configuration options.
   *
   * @param factoryClassName the fully-qualified name of a class
   * that implemements DocumentBuilderFactory. If this argument
   * is null, the default (platform-specific) implementation is
   * used. Basically, if this argument is not null, the 
   * javax.xml.parsers.DocumentBuilderFactory system property
   * is set (with the specified value) before the
   * DocumentBuilderFactory.newInstance() method is invoked.
   * @param namespaceAware configure the new DocumentBuilderFactory
   * to produce namespace aware parsers (i.e. DocumentBuilders)
   * @param validating configure the new DocumentBuilderFactory to
   * produce validating parsers (i.e. DocumentBuilders)
   */
  synchronized public static void refreshDocumentBuilderFactory(
                                            String factoryClassName,
                                            boolean namespaceAware,
                                            boolean validating) {
    if (factoryClassName != null) {
      System.setProperty("javax.xml.parsers.DocumentBuilderFactory",
                         factoryClassName);
    }

    // Step 1: create a DocumentBuilderFactory and configure it
    dbf = DocumentBuilderFactory.newInstance();

    // Optional: set various configuration options
    dbf.setNamespaceAware(namespaceAware);
    dbf.setValidating(validating);

    /*
      At this point the DocumentBuilderFactory instance can be saved
      and reused to create any number of DocumentBuilder instances
      with the same configuration options.
    */
  }

  /**
   * Use this method to get a JAXP document builder.  
   * This method creates a namespace aware, nonvalidating 
   * instance of the XML parser.
   *
   * @return DocumentBuilder an instance of a document builder, 
   * or null if a ParserConfigurationException was thrown.
   */
  synchronized public static DocumentBuilder getXMLDocBuilder()
    throws IllegalArgumentException {
    // Step 2: create a DocumentBuilder that satisfies the constraints
    // specified by the DocumentBuilderFactory
    try {
      return dbf.newDocumentBuilder();
    } catch (ParserConfigurationException pce) {
      throw new IllegalArgumentException(pce.toString());
    }
  }
}
