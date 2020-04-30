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

package org.apache.soap.server;

import java.util.*;
import java.io.*;
import javax.servlet.*;
import javax.xml.parsers.*;
import org.apache.soap.*;
import org.apache.soap.server.http.*;
import org.apache.soap.util.* ;
import org.apache.soap.util.xml.*;
import org.apache.soap.rpc.SOAPContext ;
import org.w3c.dom.*;
import org.xml.sax.*;

/**
 * A <code>ServiceManager</code> manages services and their associated
 * <code>DeploymentDescriptors</code>.
 *
 * @author Sanjiva Weerawarana (sanjiva@watson.ibm.com)
 */
public class ServiceManager {
  protected String               configFilename = "soap.xml";
  protected DeploymentDescriptor smsdd;  // Svc Mgmnt Svc's Dep Descriptor
  protected ConfigManager        configMgr = null;
  protected ServletContext       context = null;
  protected DocumentBuilder      xdb = XMLParserUtils.getXMLDocBuilder();
  protected boolean              soapInterfaceEnabled = true;

  public ServiceManager (ServletContext context, String configFilename) {
    this.context = context;

    if (configFilename != null && !configFilename.equals(""))
    {
      this.configFilename = configFilename;
    }

    readConfigFile ();

    /*If the SOAP Interface for the ServiceManager has been disabled,
      don't create the DeploymentDescriptor -WAN*/
    if (soapInterfaceEnabled)
    {
      smsdd = new DeploymentDescriptor ();
      smsdd.setID (ServerConstants.SERVICE_MANAGER_SERVICE_NAME);
      String[] svcs = new String[] { "deploy", "undeploy", "list",  "query" };
      smsdd.setMethods (svcs);
      smsdd.setScope (DeploymentDescriptor.SCOPE_APPLICATION);
      smsdd.setProviderType (DeploymentDescriptor.PROVIDER_JAVA);
      smsdd.setProviderClass ("org.apache.soap.server.ServiceManager");
      smsdd.setIsStatic (false);
      
      // set up mappings to send/recv DeploymentDescriptor and TypeMapping
      // objects
      smsdd.setMappings (new TypeMapping[] {
	new TypeMapping (Constants.NS_URI_SOAP_ENC,
			 new QName (Constants.NS_URI_XML_SOAP,
				    "DeploymentDescriptor"),
			 "org.apache.soap.server.DeploymentDescriptor",
			 "org.apache.soap.encoding.soapenc.BeanSerializer",
			 "org.apache.soap.encoding.soapenc.BeanSerializer"),
	new TypeMapping (Constants.NS_URI_SOAP_ENC,
			 new QName (Constants.NS_URI_XML_SOAP,
				    "TypeMapping"),
			 "org.apache.soap.server.TypeMapping",
			 "org.apache.soap.server.TypeMappingSerializer",
			 "org.apache.soap.server.TypeMappingSerializer")});
    }
  }

  public void setConfigFilename (String configFilename) {
    if (configFilename == null || configFilename.equals(""))
    {
      return;
    }
    else
    {
      this.configFilename = configFilename;
      readConfigFile();
    }
  }

  private void readConfigFile() {
    FileReader        reader = null ;
    Document          doc    = null ;
    Element           elem   = null ;
    NodeList          list   = null ;
    int               i, k ;
    Hashtable         options = null ;

    try {
      File configFile =
        ServerHTTPUtils.getFileFromNameAndContext(configFilename, context);

      reader = new FileReader(configFile);
      doc = xdb.parse(new InputSource(reader));
      elem = doc.getDocumentElement();
      if ( !"soapServer".equals(elem.getTagName()) )
        throw new Exception( "Root element must be 'soapServer'" );
      
      list = elem.getChildNodes();
      for ( i = 0 ; list != null && i < list.getLength() ; i++ ) {
        String  name = null ;
        Node    n    = list.item( i );

        if ( n.getNodeType() != Node.ELEMENT_NODE ) continue ;
        elem = (Element) n ;
        name = elem.getTagName();
        if ( name.equals( "configManager" ) ) {
          String className = elem.getAttribute( "value" );

          ClassLoader cl = null ;
          Class       c  = null ;
          
          cl = ServerHTTPUtils.getServletClassLoaderFromContext(context);

          if ( cl == null )
            c = Class.forName( className );
          else
            c = Class.forName( className, true, cl );

          if (!ConfigManager.class.isAssignableFrom(c)) {
            throw new IllegalArgumentException("Class '" + className +
                                               "' isn't a ConfigManager.");
          }
    
          configMgr = (ConfigManager) c.newInstance();

          // Set the servlet context.
          configMgr.setContext(context);
          // Now check for options 
          NodeList  optList = elem.getElementsByTagName( "option" );
          for ( k = 0 ; optList != null && k < optList.getLength() ; k++ ) {
            elem = (Element) optList.item( k );
            name  = elem.getAttribute( "name" );
            String  value = elem.getAttribute( "value" ) ;
            if ( options == null ) options = new Hashtable();
            if ( name == null || value == null ) continue ;
            options.put( name, value );
          }
          // Now set the options
          if ( options != null )
            configMgr.setOptions( options );
        }
	else
	  if (name.equals("serviceManager"))
	  {
          // Now check for options 
	    NodeList  optList = elem.getElementsByTagName( "option" );
	    for ( k = 0 ; optList != null && k < optList.getLength() ; k++ ) {
	      elem = (Element) optList.item( k );
	      name  = elem.getAttribute( "name" );
	      String  value = elem.getAttribute( "value" ) ;
	      if ( name == null || value == null ) continue ;
	      if (name.equalsIgnoreCase("SOAPInterfaceEnabled") && value.equalsIgnoreCase("false"))
		soapInterfaceEnabled = false;
	    }
	  }
      }
      reader.close();
    }
    catch( Throwable e ) {
      // For backwards compatibility - if there was no reader then 
      // we probably failed because the file doesn't exists and in that
      // case just skip the error message - it'll just annoy old users
      if ( reader != null ) {
        System.err.println( "Error processing configuration file (" + 
                            configFilename + ")" );
        System.err.println( "Error was: " + e );
        System.err.println( "Using DefaultConfigManager" );
      }
    }


    // When all is said and done - if there still isn't a configMgr
    // then use the default one.
    if ( configMgr == null ) {
      configMgr = new DefaultConfigManager( );

      // Set the servlet context.
      configMgr.setContext(context);
    }

    try {
      configMgr.init();
    }
    catch( SOAPException e ) {
      e.printStackTrace();
    }
  }

  /**
   * Deploy a service: add the descriptor to the persistent record of
   * what has been deployed. 
   */
  public void deploy (DeploymentDescriptor dd) throws SOAPException {
    String id = dd.getID ();
    if (id.equals (ServerConstants.SERVICE_MANAGER_SERVICE_NAME)) {
      throw new SOAPException (Constants.FAULT_CODE_SERVER,
                               "service management service '" +
                               ServerConstants.SERVICE_MANAGER_SERVICE_NAME +
                               "' cannot be user deployed");
    }
    configMgr.deploy( dd );
  }

  /**
   * Undeploy a service: remove the descriptor from the persistent record
   * of what has been deployed.
   *
   * @id the id of the service I'm undeploying
   * @exception SOAPException if service is not found
   */
  public DeploymentDescriptor undeploy (String id) throws SOAPException {
    return( configMgr.undeploy( id ) );
  }

  /**
   * Return the deployment descriptor for a service. If the id identifies
   * the service management service, then the deployment descriptor of
   * the service management service is returned.
   *
   * @param id the id of the service I'm looking for
   * @exception SOAPException if service is not found
   */
  public DeploymentDescriptor query(String id) throws SOAPException {
    if (id == null) 
      return null;
    else if (id.equals (ServerConstants.SERVICE_MANAGER_SERVICE_NAME))
      return smsdd;
    else {
      DeploymentDescriptor dd = configMgr.query( id );
      if (dd != null) 
        return dd;
      else 
        // Should we really throw an exception ?
        // Why not just return null ?
        throw new SOAPException (Constants.FAULT_CODE_SERVER,
                                 "service '" + id + "' unknown");
    }
  }

  /**
   * Return an array of all the deployed service names. Returns an array
   * of length zero if there are no deployed services.
   *
   * @return array of all service names
   */
  public String[] list () throws SOAPException {
    return( configMgr.list() );
  }
}
