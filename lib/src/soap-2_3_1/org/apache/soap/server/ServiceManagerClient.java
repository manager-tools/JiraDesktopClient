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

import java.net.URL;
import java.io.*;
import java.util.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;
import org.xml.sax.*;
import org.apache.soap.util.xml.*;
import org.apache.soap.*;
import org.apache.soap.encoding.SOAPMappingRegistry;
import org.apache.soap.transport.http.SOAPHTTPConnection;
import org.apache.soap.rpc.*;

/**
 * This is a client to talk to an Apache SOAP ServiceManager to manage services
 * deployed on the server.
 *
 * @author Sanjiva Weerawarana (sanjiva@watson.ibm.com)
 */
public class ServiceManagerClient {
  URL routerURL;
  Vector params = new Vector ();
  Call call = new Call ();
  String userName;
  String password;

  public ServiceManagerClient (URL routerURL) {
    Serializer bs = new org.apache.soap.encoding.soapenc.BeanSerializer ();

    this.routerURL = routerURL;
    SOAPMappingRegistry smr = call.getSOAPMappingRegistry ();

    // register serializer/deserializer for DeploymentDescriptor.class
    // and TypeMapping.class
    smr.mapTypes (Constants.NS_URI_SOAP_ENC,
                  new QName (Constants.NS_URI_XML_SOAP,
                             "DeploymentDescriptor"),
                  DeploymentDescriptor.class, bs, (Deserializer) bs);
    bs = new TypeMappingSerializer ();
    smr.mapTypes (Constants.NS_URI_SOAP_ENC,
                  new QName (Constants.NS_URI_XML_SOAP, "TypeMapping"),
                  TypeMapping.class, bs, (Deserializer) bs);
  }

  public void setUserName (String userName) {
    this.userName = userName;
  }

  public void setPassword (String password) {
    this.password = password;
  }

  private Response invokeMethod (String methodName, Parameter param) 
       throws SOAPException {
    call.setTargetObjectURI (ServerConstants.SERVICE_MANAGER_SERVICE_NAME);
    call.setMethodName (methodName);
    call.setEncodingStyleURI (Constants.NS_URI_SOAP_ENC);
    if (userName != null) {
      SOAPHTTPConnection hc = new SOAPHTTPConnection ();
      hc.setUserName (userName);
      hc.setPassword (password);
      call.setSOAPTransport (hc);
    }
    if (param != null) {
      params.removeAllElements ();
      params.addElement (param);
      call.setParams (params);
    } else {
      call.setParams (null);
    }
    Response resp = call.invoke (routerURL, "");
    if (resp.generatedFault ()) {
      Fault fault = resp.getFault ();
      System.out.println ("Ouch, the call failed: ");
      System.out.println ("  Fault Code   = " + fault.getFaultCode ());  
      System.out.println ("  Fault String = " + fault.getFaultString ());
    }  
    return resp;
  }

  public void deploy (DeploymentDescriptor dd) throws SOAPException {
    Parameter p1 = new Parameter ("descriptor", DeploymentDescriptor.class,
                                  dd, null);
    invokeMethod ("deploy", p1);
  }

  public void undeploy (String serviceName) throws SOAPException {
    Parameter p1 = new Parameter ("name", String.class, serviceName, null);
    invokeMethod ("undeploy", p1);
  }
  
  public String[] list () throws SOAPException {
    Response resp = invokeMethod ("list", null);
    if (!resp.generatedFault ()) {
      Parameter result = resp.getReturnValue ();
      return (String[]) result.getValue ();
    } else {
      return null;
    }
  }

  public DeploymentDescriptor query (String serviceName) throws SOAPException {
    Parameter p1 = new Parameter ("name", String.class, serviceName, null);
    Response resp = invokeMethod ("query", p1);
    if (!resp.generatedFault ()) {
      Parameter result = resp.getReturnValue ();
      return (DeploymentDescriptor) result.getValue ();
    } else {
      return null;
    }
  }

  private static void badUsage () {
    System.err.println ("Usage: java " +
                        ServiceManagerClient.class.getName () +
                        " [-auth username:password] url operation arguments");
    System.err.println ("where");
    System.err.println ("\tusername and password is the HTTP Basic" +
			" authentication info");
    System.err.println ("\turl is the Apache SOAP router's URL whose" +
                        " services are managed");
    System.err.println ("\toperation and arguments are:");
    System.err.println ("\t\tdeploy deployment-descriptor-file.xml");
    System.err.println ("\t\tlist");
    System.err.println ("\t\tquery service-name");
    System.err.println ("\t\tundeploy service-name");
    System.exit (1);
  }

  /**
   * Command-line app for managing services on an Apache SOAP server.
   */
  public static void main (String[] args) throws Exception {
    URL routerURL;
    String op;
    String userName = null;
    String password = null;

    if (args.length < 2) {
      badUsage ();
    }

    int base = 0;
    if (args[0].equals ("-auth")) {
      if (args.length < 4) { // -auth user:pass + url + op is minimal
	badUsage ();
      }
      StringTokenizer st = new StringTokenizer (args[1], ":");
      if (st.countTokens () != 2) {
	badUsage ();
      }
      userName = st.nextToken ();
      password = st.nextToken ();
      base = 2;
    }

    ServiceManagerClient smc = 
      new ServiceManagerClient (new URL (args[base]));
    if (base == 2) {
      smc.setUserName (userName);
      smc.setPassword (password);
    }

    op = args[base+1];
    if (op.equals ("deploy")) {
      if (args.length != base+3) {
        badUsage ();
      }
      FileReader fr = new FileReader (args[base+2]);
      DocumentBuilder xdb = XMLParserUtils.getXMLDocBuilder();
      Document doc = xdb.parse(new InputSource(fr));
      smc.deploy (DeploymentDescriptor.fromXML (doc.getDocumentElement ()));
    } else if (op.equals ("undeploy")) {
      if (args.length != base+3) {
        badUsage ();
      }
      smc.undeploy (args[base+2]);
    } else if (op.equals ("list")) {
      String[] sms = smc.list ();
      if (sms != null) {
        System.out.println ("Deployed Services:");
        for (int i = 0; i < sms.length; i++) {
          System.out.println ("\t" + sms[i]);
        }
      }
    } else if (op.equals ("query")) {
      if (args.length != base+3) {
        badUsage ();
      }
      DeploymentDescriptor dd = smc.query (args[base+2]);
      if (dd != null) {
        dd.toXML (new OutputStreamWriter (System.out));
      }
    } else {
      badUsage ();
    }
  }
}
