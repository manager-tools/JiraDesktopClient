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

package org.apache.soap.server ;

// Core java classes
import java.util.* ;
import java.io.* ;

// Java Servlet Classes
import javax.servlet.* ;
import javax.servlet.http.* ;

// XML Classes
import javax.xml.parsers.* ;
import org.w3c.dom.* ;
import org.xml.sax.* ;

// SOAP Classes
import org.apache.soap.* ;
import org.apache.soap.rpc.* ;
import org.apache.soap.server.* ;
import org.apache.soap.server.http.* ;
import org.apache.soap.util.* ;
import org.apache.soap.util.xml.* ;

/**
 * An <code>XMLConfigManager</code> ...
 *
 * @author <a href="mailto:magnus@handpoint.com">Magnus Thor Torfason</a>
 *
 * This class should be almost identical in function to the
 * DefaultConfigManager.
 *
 * <p>
 *
 * The only notable difference is that while
 * the DefaultConfigManager uses java readObject() and writeObject()
 * methods, this class uses the XML routines of DeploymentDescriptor
 * to save and load the DeploymentDescriptors from the underlying
 * registry file (typically <tt>DeployedServices.xml</tt>).
 *
 */
public class XMLConfigManager extends BaseConfigManager
          implements ConfigManager {
  protected DocumentBuilder xdb = XMLParserUtils.getXMLDocBuilder();

  /** The name of the deployment file. */
  protected String filename = "DeployedServices.xml";

  /**
   * This method sets the configuration options (usually
   * read from the config file).
   */
  public void setOptions( Hashtable options ) {
    if ( options == null ) return ;

    String value = (String) options.get( "filename" );
    if ( value != null && !"".equals(value) ) {
      filename = value ;
    }
  }

  /**
   * Loads the descriptors from the underlying registry file, which
   * should be represented as a list of deployment descriptor elements.
   */
  public void loadRegistry() throws SOAPException {
    dds = null ;
    try {
      File file = ServerHTTPUtils.getFileFromNameAndContext(filename,
        context);
      FileReader rd = new FileReader (file);
      Document doc = null;
      Element root = null;

      try {
        doc  = xdb.parse(new InputSource(rd));
        root = doc.getDocumentElement();
      } catch (Exception e) {
        e.printStackTrace();
        throw new SOAPException(Constants.FAULT_CODE_SERVER,e.getMessage());
      }

      NodeList deploymentElements = root.getElementsByTagNameNS(
              Constants.NS_URI_XML_SOAP_DEPLOYMENT, "service");

      int count = deploymentElements.getLength();
      dds = new Hashtable();
      for( int i=0; i<count; i++ ) {
        Element deploymentElement = (Element)deploymentElements.item(i);
        DeploymentDescriptor dd = DeploymentDescriptor.fromXML(
                deploymentElement);
        String  id = dd.getID();
        dds.put( id, dd );
      }
    } catch(Exception e) {
      dds = new Hashtable ();
      System.err.println ("SOAP Service Manager: Unable to read '" +
        filename +  "': assuming fresh start");
    }
  }

  /**
   * Saves currently deployed descriptors to the underlying registry file,
   * in XML format.
   */
  public void saveRegistry() throws SOAPException {
    try {
      File file = ServerHTTPUtils.getFileFromNameAndContext(filename,
        context);
      PrintWriter pw = new PrintWriter(new FileWriter (file));
      Enumeration e = dds.elements();

      pw.println("<deployedServices>");
      pw.println();
      while ( e.hasMoreElements() ) {
        DeploymentDescriptor dd = (DeploymentDescriptor)e.nextElement();
        dd.toXML(pw);
        pw.println();
      }
      pw.println("</deployedServices>");

      pw.close ();
    } catch (Exception e) {
      throw new SOAPException (Constants.FAULT_CODE_SERVER,
        "Error saving services registry: " +
        e.getMessage ());
    }
  }
}

