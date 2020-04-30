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
import javax.servlet.* ;
import javax.servlet.http.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;
import org.apache.soap.*;
import org.apache.soap.server.*;
import org.apache.soap.util.*;
import org.apache.soap.util.xml.*;
import org.apache.soap.rpc.SOAPContext;
import org.apache.soap.encoding.SOAPMappingRegistry;
import javax.activation.*;
import javax.mail.*;
import javax.mail.internet.*;
import org.apache.soap.transport.EnvelopeEditor;

/**
 * Any utility stuff for HTTP SOAP stuff. 
 *
 * @author Sanjiva Weerawarana
 */
public class ServerHTTPUtils {
  private static final String SERVICE_MANAGER_ID = "serviceManager";
  private static final String SCRIPT_CLASS = "com.ibm.bsf.BSFManager";
  private static final String SCRIPT_INVOKER = 
    "org.apache.soap.server.InvokeBSF";
  private static final String SERVLET_CLASSLOADER = "servletClassLoader";

  /**
   * Look up the service manager or create it from the context. NOTE:
   * this approach may not work if this servlet is put in a webapp
   * that is marked distributed .. the servlet context is unique only
   * per JVM. In that case we'll have to use an external database
   * to store this webapp-global attribute.
   */
  public static ServiceManager getServiceManagerFromContext(ServletContext context,
                                                            String configFilename) {
    Object o;
    if ( context != null ) {
      synchronized (context) {
        o = context.getAttribute(SERVICE_MANAGER_ID);
        if (o == null) {
          o = new ServiceManager(context, configFilename);
          context.setAttribute (SERVICE_MANAGER_ID, o);
        }
      }
    }
    else {
      o = new ServiceManager( null, configFilename );
    }
    return (ServiceManager) o;
  }

  /**
   * Equivalent to: getServiceManagerFromContext(context, null)
   */
  public static ServiceManager getServiceManagerFromContext(ServletContext context) {
    return getServiceManagerFromContext(context, null);
  }

  /**
   * Retrieves the ClassLoader from the ServletContext, if one is registered.
   */
  public static ClassLoader getServletClassLoaderFromContext(ServletContext context) {
    if (context != null) {
      Object o;

      synchronized (context) {
        o = context.getAttribute(SERVLET_CLASSLOADER);
      }
      
      return (ClassLoader) o;
    } else {
      return null;
    }
  }

  /**
   * Registers the ClassLoader into the ServletContext.
   */
  public static void setServletClassLoaderIntoContext(ServletContext context,
						                                           ClassLoader cl) {
    synchronized (context) {
      context.setAttribute(SERVLET_CLASSLOADER, cl);
    }
  }

  /**
   * If the fileName is absolute, a file representing it is returned.
   * Otherwise, a File is returned which represents the file relative
   * to the servlet's docBase. If ServletContext.getRealPath(fileName)
   * returns null, a File is returned which represents the file
   * relative to the current working directory.
   * Note: Uses ServletContext.getRealPath(fileName).
   */
  public static File getFileFromNameAndContext(String fileName,
                                               ServletContext context) {
    File file = new File(fileName);

    if (!file.isAbsolute())
    {
      if (context != null)
      {
        String realFileName = context.getRealPath(fileName);

        if (realFileName != null)
        {
          file = new File(realFileName);
        }
      }
    }

    return file;
  }

  /**
   * Read in stuff from the HTTP request stream and return the envelope.
   * Returns null (and sets the error on the response stream) if a
   * transport level thing is wrong and throws a SOAPException if a
   * SOAP level thing is wrong.
   * 
   * @return Envelope containing the SOAP envelope found in the request
   *
   * @exception SOAPException if a SOAP level thing goes wrong
   * @exception IOException if something fails while sending an
   *            error response
   */
  public static Envelope readEnvelopeFromRequest (DocumentBuilder xdb,
                                                  String contentType,
                                                  int contentLength,
                                                  InputStream requestStream,
                                                  EnvelopeEditor editor,
                                                  HttpServletResponse res,
                                                  SOAPContext ctx)
       throws SOAPException, IOException {
    try {
        return ServerUtils.readEnvelopeFromInputStream (xdb, requestStream,
                                                        contentLength,
                                                        contentType, editor,
                                                        ctx);
    } catch (IllegalArgumentException e) {
      String msg = e.getMessage ();
      res.sendError (res.SC_BAD_REQUEST, "Error unmarshalling envelope: " +
                     msg);
      return null;
    } catch (MessagingException me) {
      res.sendError (res.SC_BAD_REQUEST, "Error unmarshalling envelope: " +
                     me);
      return null;
    }
  }

  /**
   * Return the target object that services the service with the
   * given ID. Depending on the deployment information of the
   * service, the object's lifecycle is also managed here.
   */
  public static Object getTargetObject (ServiceManager serviceManager,
                                 DeploymentDescriptor dd,
                                 String targetID,
                                 HttpServlet thisServlet,
                                 HttpSession session,
                                 SOAPContext ctxt,
                                 ServletContext context)
       throws SOAPException {
    int scope = dd.getScope ();
    byte providerType = dd.getProviderType ();
    String className;
    Object targetObject = null;
    if (providerType == DeploymentDescriptor.PROVIDER_JAVA ||
        providerType == DeploymentDescriptor.PROVIDER_USER_DEFINED) {
      className = dd.getProviderClass ();
    } else {
      // for scripts, we need a new BSF manager basically
      className = SCRIPT_CLASS;
    }
      
    // determine the scope and lock object to use to manage the lifecycle
    // of the service providing object
    Object scopeLock = null;
    if (scope == DeploymentDescriptor.SCOPE_REQUEST) {
      scopeLock = thisServlet; // no need to register .. create, use and dink
    } else if (scope == DeploymentDescriptor.SCOPE_SESSION) {
      scopeLock = session;
    } else if (scope == DeploymentDescriptor.SCOPE_APPLICATION) {
      scopeLock = context;
    } else {
      throw new SOAPException (Constants.FAULT_CODE_SERVER,
                               "Service uses deprecated object scope " +
                               "'page': inform provider of error");
    }

    // create the object if necessary
    boolean freshObject = false;
    
    // find the target object on which the requested method should
    // be invoked
    if (targetID.equals (ServerConstants.SERVICE_MANAGER_SERVICE_NAME)) {
      targetObject = serviceManager;
    } else {
      // locate (or create) the target object and invoke the method
      if ( scopeLock == null ) scopeLock = className ; // Just pick something
      synchronized (scopeLock) {
        if (scopeLock == session) {
          // targetObject = session.getAttribute (targetID);
          targetObject = session.getValue (targetID);
        } else if (scopeLock == context) {
          targetObject = context.getAttribute (targetID);
        } else {
          targetObject = null;
        }
        if (targetObject == null) {
          try {
            Class c = ctxt.loadClass(className);

            if (dd.getIsStatic ()) {
              targetObject = c;
            } else {
              targetObject = c.newInstance ();
            }
            freshObject = true;

            // remember the created instance if the scope is not REQUEST;
            // in that case the object is to be thrown away after handling
            // the request
            if (scopeLock == session) {
              session.putValue (targetID, targetObject);
              // session.setAttribute (targetID, targetObject);
            } else if (scopeLock == context) {
              context.setAttribute (targetID, targetObject);
            }
          } catch (Exception e) {
            String msg;
            if (providerType == DeploymentDescriptor.PROVIDER_JAVA ||
                providerType == DeploymentDescriptor.PROVIDER_USER_DEFINED) {
              msg = "Unable to resolve target object: " + e.getMessage ();
            } else {
              msg = "Unable to load BSF: script services not available " +
                "without BSF: " + e.getMessage ();
            }
            throw new SOAPException (
              Constants.FAULT_CODE_SERVER_BAD_TARGET_OBJECT_URI, msg, e);
          }
        }
      }
    }

    // if script provider type and first time to it, then load and
    // exec the script
    if ((providerType != DeploymentDescriptor.PROVIDER_JAVA &&
         providerType != DeploymentDescriptor.PROVIDER_USER_DEFINED)
        && freshObject) {
      // find the class that provides the BSF services (done
      // this way via reflection to avoid a static dependency on BSF)
      Class bc = null;
      try {
        bc = ctxt.loadClass(SCRIPT_INVOKER);
      } catch (Exception e) {
        String msg = "Unable to load BSF invoker (" + SCRIPT_INVOKER + ")" +
          ": script services not available without BSF: " + e.getMessage ();
        throw new SOAPException (Constants.FAULT_CODE_SERVER, msg, e);
      }

        // get the script string to exec
      String script = dd.getScriptFilenameOrString ();
      if (providerType == DeploymentDescriptor.PROVIDER_SCRIPT_FILE) {
        String fileName = context.getRealPath (script);
        try {
          script = IOUtils.getStringFromReader (new FileReader (fileName));
        } catch (Exception e) {
          String msg = "Unable to load script file (" + fileName + ")" +
            ": " + e.getMessage ();
          throw new SOAPException (Constants.FAULT_CODE_SERVER, msg, e);
        }
      }

      // exec it
      Class[] sig = {DeploymentDescriptor.class,
                     Object.class,
                     String.class};
      try {
        Method m = MethodUtils.getMethod (bc, "init", sig, true);
        m.invoke (null, new Object[] {dd, targetObject, script});
      } catch (InvocationTargetException ite) {
        Throwable te = ite.getTargetException();
        if (te instanceof SOAPException)
          throw (SOAPException)te;
        String msg = "Unable to invoke init method of script invoker: " + te;
        throw new SOAPException (Constants.FAULT_CODE_SERVER, msg, te);
      } catch (Exception e) {
        String msg = "Unable to invoke init method of script invoker: " + e;
        throw new SOAPException (Constants.FAULT_CODE_SERVER, msg, e);
      }
    }

    return targetObject;
  }

  /**
   * Return the soap mapping registry instance from the servlet context.
   * If one isn't there, then create one and store it in there and then
   * return it.
   */
  public static SOAPMappingRegistry 
       getSMRFromContext (ServletContext context) {
    SOAPMappingRegistry smr = null;
    synchronized (context) {
      smr = 
	(SOAPMappingRegistry) context.getAttribute ("__cached_servlet_SMR__");
      if (smr == null) {
	smr = new SOAPMappingRegistry ();
	context.setAttribute ("__cached_servlet_SMR__", smr);
      }
    }
    return smr;
  }
}
