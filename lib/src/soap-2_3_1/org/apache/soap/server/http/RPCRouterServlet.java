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

package org.apache.soap.server.http;

import java.io.*;
import java.util.*;
import java.lang.reflect.*;
import javax.servlet.*;
import javax.servlet.http.*;
import javax.xml.parsers.*;
import org.w3c.dom.* ;
import org.apache.soap.*;
import org.apache.soap.rpc.*;
import org.apache.soap.server.*;
import org.apache.soap.encoding.*;
import org.apache.soap.transport.*;
import org.apache.soap.util.*;
import org.apache.soap.util.xml.*;
import org.apache.soap.transport.EnvelopeEditor;
import org.apache.soap.transport.EnvelopeEditorFactory;

/**
 * This servlet routes RPC requests to the intended method of
 * the intended object.
 *
 * @author Sanjiva Weerawarana (sanjiva@watson.ibm.com)
 * @author Matthew J. Duftler (duftler@us.ibm.com)
 * @author Steven McDowall (sjm@aptest.com)
 * @author Eric M. Dashofy (edashofy@ics.uci.edu)
 * @author Kevin J. Mitchell (kevin.mitchell@xmls.com)
 * @author Wouter Cloetens (wcloeten@raleigh.ibm.com)
 * @author Bill Nagy (nagy@watson.ibm.com)
 */
public class RPCRouterServlet extends HttpServlet {
  /*
    EnvelopeEditorFactory, XMLParser, and ConfigFile are
    all server-side parameters which can be set using Servlet
    init-parameters or WebApp context-parameters.
    For example, you may add the following
    description to web.xml when using Tomcat:

    <context-param>    
        <param-name>EnvelopeEditorFactory</param-name>
        <param-value>MyEnvelopeEditorFactory</param-value>
    </context-param>    
    <context-param>    
        <param-name>XMLParser</param-name>
        <param-value>SampleXMLDocumentBuilderFactory</param-value>
    </context-param>    
    <context-param>    
        <param-name>ConfigFile</param-name>
        <param-value>myconfig.xml</param-value>
    </context-param>    
    <servlet>
      <servlet-name>RPCRouterServlet</servlet-name>
      <servlet-class>org.apache.soap.server.http.RPCRouterServlet</servlet-class>
      <init-param>
        <param-name>EnvelopeEditorFactory</param-name>
        <param-value>MyEnvelopeEditorFactory</param-value>
      </init-param>
      <init-param>
        <param-name>XMLParser</param-name>
        <param-value>SampleXMLDocumentBuilderFactory</param-value>
      </init-param>
      <init-param>
        <param-name>ConfigFile</param-name>
        <param-value>myconfig.xml</param-value>
      </init-param>
    </servlet>

    The servlet init-parameter values will override those of the
    context-parameters.
  */
  private EnvelopeEditor editor = null;
  private String configFilename = null;

  public void init() throws ServletException {
    ClassLoader servletClassLoader =
      Thread.currentThread().getContextClassLoader();
    
    try
    {
      /*Make sure that we got a useful classloader; if we can not
	even load ourselves, then at least use the classloader that
	loaded us.*/
      servletClassLoader.loadClass(this.getClass().getName());
    }
    catch(ClassNotFoundException e)
    {
      servletClassLoader = getClass().getClassLoader();
    }
    
    if (servletClassLoader == null)
    {
      /*This is needed because some containers use hashtable to store
	servlet attributes and therefore they can't be null.
	If Class.forName is passed in null as the classloader, then it
	seems to default to the system class loader, so this should be ok.*/
      servletClassLoader = ClassLoader.getSystemClassLoader();
    }
    ServletConfig servletConfig = getServletConfig();
    ServletContext servletContext = servletConfig.getServletContext();
    /*We're going to check for init parameters in the servletContext first
      and then allow people to override them in the servletConfig.  This
      will allow for backwards compatability with the old way of setting
      config parameters.*/
    String envelopeEditorFactoryClassName =
      servletConfig.getInitParameter(Constants.ENVELOPE_EDITOR_FACTORY);
    if (envelopeEditorFactoryClassName == null)
      envelopeEditorFactoryClassName
	= servletContext.getInitParameter(Constants.ENVELOPE_EDITOR_FACTORY);


    // Is there an envelope editory factory?
    if (envelopeEditorFactoryClassName != null) {
      EnvelopeEditorFactory factory =
        (EnvelopeEditorFactory)createObject(envelopeEditorFactoryClassName,
                                            servletClassLoader);

      if (factory != null) {
        try {
          Properties props = new Properties();
	  /*First we put in the servletContext parameters, and then
	    overwrite them with the servletConfig parameters if 
	    they are present.*/
          Enumeration enum = servletContext.getInitParameterNames();
	  
          while (enum.hasMoreElements()) {
            String name = (String)enum.nextElement();

            if (!Constants.ENVELOPE_EDITOR_FACTORY.equals(name)
                && !Constants.XML_PARSER.equals(name)) {
              props.put(name, servletContext.getInitParameter(name));
            }
          }

          enum = servletConfig.getInitParameterNames();

          while (enum.hasMoreElements()) {
            String name = (String)enum.nextElement();

            if (!Constants.ENVELOPE_EDITOR_FACTORY.equals(name)
                && !Constants.XML_PARSER.equals(name)) {
              props.put(name, servletConfig.getInitParameter(name));
            }
          }

          // Put the real path into the properties, if it can be found.
          String servletContextPath = servletContext.getRealPath("");

          if (servletContextPath != null) {
            props.put("SOAPServerContextPath", servletContextPath);
          }

          // Create an editor by calling the factory.
          editor = factory.create(props);
        } catch (SOAPException e) {
          throw new ServletException("Can't create editor", e);
        }
      }
    }


    String tempStr = servletConfig.getInitParameter(Constants.CONFIGFILENAME);
    if (tempStr == null)
      tempStr = servletContext.getInitParameter(Constants.CONFIGFILENAME);

    // Is there a user-specified config filename?
    if (tempStr != null) {
      configFilename = tempStr;
    }

    tempStr = servletConfig.getInitParameter(Constants.XML_PARSER);
    if (tempStr == null)
      tempStr = servletContext.getInitParameter(Constants.XML_PARSER);

    // Is there a user-specified JAXP implementation?
    if (tempStr != null) {
      XMLParserUtils.refreshDocumentBuilderFactory(tempStr,
                                                   true,  // namespaceAware
                                                   false);// validating
    }

    ServerHTTPUtils.setServletClassLoaderIntoContext(servletContext,
                                                     servletClassLoader);
  }

  private Object createObject(String className, ClassLoader classLoader)
    throws ServletException
  {
    try {
      return classLoader.loadClass(className).newInstance();
    } catch (ClassNotFoundException e) {
      throw new ServletException("Can't find class named '" + className +
                                 "'.");
    } catch (InstantiationException e) {
      throw new ServletException("Can't instantiate class '" + className +
                                 "'.");
    } catch (IllegalAccessException e) {
      throw new ServletException("WARNING: Can't access the constructor " +
                                 "of the class '" + className + "'.");
    }
  }

  public void doGet (HttpServletRequest req, HttpServletResponse res)
    throws ServletException, IOException {
    res.setContentType("text/html");

    PrintWriter out = res.getWriter ();

    out.println("<html><head><title>SOAP RPC Router</title></head>");
    out.println ("<body><h1>SOAP RPC Router</h1>");
    out.println ("<p>Sorry, I don't speak via HTTP GET- you have to use");
    out.println ("HTTP POST to talk to me.</p></body></html>");
  }

  public void doPost (HttpServletRequest req, HttpServletResponse res)
    throws ServletException, IOException {
    ServletConfig config = getServletConfig();
    ServletContext context = config.getServletContext ();
    HttpSession session = req.getSession ();
    ServiceManager serviceManager =
      ServerHTTPUtils.getServiceManagerFromContext (context, configFilename);
    Call call = null;
    Response resp = null;
    String targetID = null;
    String fullTargetID = null;
    int status = res.SC_OK;
    DeploymentDescriptor dd = null;

    SOAPContext reqCtx  = new SOAPContext();
    SOAPContext resCtx  = new SOAPContext();
    Envelope    callEnv = null ;

    reqCtx.setClassLoader( ServerHTTPUtils.
                             getServletClassLoaderFromContext(context) );

    try { // unrecoverable error
      try { // SOAPException
        // extract the call
        try {// Exception extracting the call
          reqCtx.setProperty( Constants.BAG_HTTPSERVLET, this );
          reqCtx.setProperty( Constants.BAG_HTTPSESSION, session );
          reqCtx.setProperty( Constants.BAG_HTTPSERVLETREQUEST, req );
          reqCtx.setProperty( Constants.BAG_HTTPSERVLETRESPONSE, res );
  
          // Carry the request context from the read to the creation of
          // the Call object.
  
          // Generate Envelope after the incoming message is translated by
          // EnvelopeEditor
          // Note: XMLParser that is specified by init-param isused in
          // this process.
          DocumentBuilder xdb = XMLParserUtils.getXMLDocBuilder();

          callEnv =
            ServerHTTPUtils.readEnvelopeFromRequest(xdb,
                                                    req.getContentType(),
                                                    req.getContentLength(),
                                                    req.getInputStream(),
                                                    editor,
                                                    res,
                                                    reqCtx);
          if (callEnv == null)
            return;
          call = RPCRouter.extractCallFromEnvelope(serviceManager, callEnv,
                                                   reqCtx);
          targetID = call.getTargetObjectURI ();
          fullTargetID = call.getFullTargetObjectURI ();
        } catch (IllegalArgumentException e) {
          String msg = e.getMessage ();
          String faultCode =
            (msg != null && msg.equals (Constants.ERR_MSG_VERSION_MISMATCH))
            ? Constants.FAULT_CODE_VERSION_MISMATCH
            : Constants.FAULT_CODE_CLIENT;
          throw new SOAPException (faultCode, msg, e);
        }
  
        // get the deployment descriptor for this service (will except if
        // not known)
        dd = serviceManager.query (targetID);
        reqCtx.setProperty( Constants.BAG_DEPLOYMENTDESCRIPTOR, dd );
  
        Provider provider;
        if ( dd.getProviderType() == DeploymentDescriptor.PROVIDER_JAVA ) {
          // Handle Java based services
          provider = new org.apache.soap.providers.RPCJavaProvider();
        } else if (dd.getProviderType() ==
                   DeploymentDescriptor.PROVIDER_USER_DEFINED) {
          // Handle user-defined providers
          provider = ServerUtils.loadProvider(dd, reqCtx);
        } else {
          // Handle scripts
          provider = new org.apache.soap.providers.RPCJavaProvider();
        }
        
        provider.locate( dd, callEnv, call, call.getMethodName(), fullTargetID, 
                         reqCtx );
        provider.invoke( reqCtx, resCtx );

      } catch (Throwable t) {
        // note that we catch runtime errors too with the above .. the
        // idea is to do a SOAP fault for everything that goes out so
        // that if the recepient is expecting to read some XML they'll
        // get it. If not, it doesn't hurt.
        SOAPException e = null;
        if (t instanceof SOAPException)
          e = (SOAPException)     t;
        else
          e = new SOAPException(Constants.FAULT_CODE_SERVER +
                                ".Exception:", "", t);
  
        Fault fault = new Fault (e);
        fault.setFaultActorURI (req.getRequestURI ());
        if (dd != null)
          dd.buildFaultRouter(reqCtx).notifyListeners(fault, e);
  
        // the status code for faults should always be the internal
        // server error status code (per soap spec)
        status = res.SC_INTERNAL_SERVER_ERROR;
  
        String respEncStyle = null;
        if(call != null)
            respEncStyle = call.getEncodingStyleURI();
        if(respEncStyle == null)
          respEncStyle = Constants.NS_URI_SOAP_ENC;
  
        resCtx = new SOAPContext(); // get rid of old one
        resp = new Response (null, null, fault, null, null, respEncStyle, 
                             resCtx);
        SOAPMappingRegistry smr = 
	  (call != null) ? call.getSOAPMappingRegistry ()
	                 : ServerHTTPUtils.getSMRFromContext (context);
        Envelope env = resp.buildEnvelope();
        StringWriter sw = new StringWriter();
        env.marshall(sw, smr, resp.getSOAPContext());
        resp.getSOAPContext().setRootPart( sw.toString(),
                                           Constants.HEADERVAL_CONTENT_TYPE_UTF8);
      }

      // Generate response.
      TransportMessage sres = new TransportMessage(null, resCtx, null );
      sres.editOutgoing(editor);

      // Generate response byte array.
      sres.save();

      // Write.
      res.setStatus(status);
      res.setContentType(sres.getContentType());
      for (Enumeration headers = sres.getHeaderNames();
           headers.hasMoreElements(); ) {
          String name = (String)headers.nextElement();
          res.setHeader(name, sres.getHeader(name));
      }

      res.setContentLength(sres.getContentLength());
      OutputStream outStream = res.getOutputStream();
      sres.writeTo(outStream);
    }
    catch (Exception e)
    {
        throw new ServletException ("Error building response envelope: " + e);
    }
  }
}
