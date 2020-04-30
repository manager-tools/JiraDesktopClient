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
import org.w3c.dom.*;
import org.apache.soap.*;
import org.apache.soap.rpc.*;
import org.apache.soap.server.*;
import org.apache.soap.encoding.*;
import org.apache.soap.util.*;
import org.apache.soap.util.xml.*;
import org.apache.soap.transport.*;

/**
 * This servlet routes messages to the appropriate listener method.
 *
 * @author Sanjiva Weerawarana (sanjiva@watson.ibm.com)
 * @author Matthew J. Duftler (duftler@us.ibm.com)
 * @author Bill Nagy (nagy@watson.ibm.com)
 */
public class MessageRouterServlet extends HttpServlet {
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
      <servlet-name>MessageRouterServlet</servlet-name>
      <servlet-class>org.apache.soap.server.http.MessageRouterServlet</servlet-class>
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
    String envelopeEditorFactoryClassName =
      servletConfig.getInitParameter(Constants.ENVELOPE_EDITOR_FACTORY);

    // Is there an envelope editory factory?
    if (envelopeEditorFactoryClassName != null) {
      EnvelopeEditorFactory factory =
        (EnvelopeEditorFactory)createObject(envelopeEditorFactoryClassName,
                                            servletClassLoader);

      if (factory != null) {
        try {
          Properties props = new Properties();
          Enumeration enum = servletConfig.getInitParameterNames();

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

    // Is there a user-specified config filename?
    if (tempStr != null) {
      configFilename = tempStr;
    }

    tempStr = servletConfig.getInitParameter(Constants.XML_PARSER);

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

    out.println("<html><head><title>SOAP Message Router</title></head>");
    out.println ("<body><h1>SOAP Message Router</h1>");
    out.println ("<p>Sorry, I don't speak via HTTP GET- you have to use");
    out.println ("HTTP POST to talk to me.</p></body></html>");
  }

  public void doPost (HttpServletRequest req, HttpServletResponse res)
    throws ServletException, IOException
  {
    ServletConfig config = getServletConfig();
    ServletContext context = config.getServletContext ();
    HttpSession session = req.getSession ();
    ServiceManager serviceManager =
      ServerHTTPUtils.getServiceManagerFromContext (context, configFilename);
    int status;
    DeploymentDescriptor dd = null;
    TransportMessage sres;

    SOAPContext reqCtx = new SOAPContext() ;
    SOAPContext resCtx = new SOAPContext() ;

    reqCtx.setClassLoader( ServerHTTPUtils.
                             getServletClassLoaderFromContext(context) );

    try {
      try {
        reqCtx.setProperty( Constants.BAG_HTTPSERVLET, this );
        reqCtx.setProperty( Constants.BAG_HTTPSESSION, session );
        reqCtx.setProperty( Constants.BAG_HTTPSERVLETREQUEST, req );
        reqCtx.setProperty( Constants.BAG_HTTPSERVLETRESPONSE, res );

        // get the envelope and request SOAPContext
  
        // Generate Envelope after the incoming message is translated by
        // EnvelopeEditor
        // Note: XMLParser that is specified by init-param isused in
        // this process.
        DocumentBuilder xdb = XMLParserUtils.getXMLDocBuilder();
        Envelope msgEnv =
          ServerHTTPUtils.readEnvelopeFromRequest(xdb,
                                                  req.getContentType(),
                                                  req.getContentLength(),
                                                  req.getInputStream(),
                                                  editor, res, reqCtx);
        if (msgEnv == null)
            return;
  
        // get the namespace URI and and localpart of the first child
        // element of the body of the envelope
        Body body = msgEnv.getBody ();
        Element e = (Element) body.getBodyEntries().elementAt (0);
        if (e == null) {
          throw new SOAPException (Constants.FAULT_CODE_CLIENT,
                                   "Message envelope's body is empty!");
        }
        String targetID = e.getNamespaceURI ();
        String messageName = e.getLocalName ();
  
        // is this a valid message?
        dd = serviceManager.query (targetID);
        reqCtx.setProperty( Constants.BAG_DEPLOYMENTDESCRIPTOR, dd );
  
        Provider provider;
        if ( dd.getProviderType() == DeploymentDescriptor.PROVIDER_JAVA ) {
          // Handle Java based services
          provider = new org.apache.soap.providers.MsgJavaProvider();
        } else if (dd.getProviderType() ==
                   DeploymentDescriptor.PROVIDER_USER_DEFINED) {
          // Handle user-defined providers
          provider = ServerUtils.loadProvider(dd, reqCtx);
        } else {
          // Handle scripts
          provider = new org.apache.soap.providers.MsgJavaProvider();
        }

        provider.locate( dd, msgEnv, null, messageName, targetID, reqCtx );
        provider.invoke( reqCtx, resCtx );
  
        sres = new TransportMessage(null, resCtx, null);
  
        // apply transport hook (if root part of response is text)
        sres.editOutgoing(editor);
  
        // set the response status to 200 OK and put the contents of the
        // out stream out
        status = res.SC_OK;
      } catch (Throwable t) {
        // note that we catch runtime errors too with the above .. the
        // idea is to do a SOAP fault for everything that goes out so
        // that if the recepient is expecting to read some XML they'll
        // get it. If not, it doesn't hurt.
  
        SOAPException e = null;
        if (t instanceof SOAPException)
          e = (SOAPException)t;
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
  
        resCtx = new SOAPContext(); // get rid of old one
        Response resp =  new Response(null,
                                     null,
                                     fault,
                                     null,
                                     null,
                                     Constants.NS_URI_SOAP_ENC,
                                     resCtx);

        Envelope env = resp.buildEnvelope();
        StringWriter sw = new StringWriter();
        env.marshall(sw, ServerHTTPUtils.getSMRFromContext (context),
		     resp.getSOAPContext());
        String envelopeString = sw.toString();
        sres = new TransportMessage(envelopeString, resCtx, null);
      }

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
