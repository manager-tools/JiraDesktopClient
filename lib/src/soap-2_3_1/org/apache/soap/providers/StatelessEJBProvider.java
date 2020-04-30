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

package org.apache.soap.providers;

import java.io.* ;
import java.util.* ;
import javax.servlet.* ;
import javax.servlet.http.* ;
import org.apache.soap.* ;
import org.apache.soap.rpc.* ;
import org.apache.soap.server.* ;
import org.apache.soap.util.* ;

import java.lang.reflect.*;
import javax.rmi.*;
import javax.ejb.*;
import javax.naming.*;
import javax.naming.Context.*;

public class StatelessEJBProvider implements org.apache.soap.util.Provider {

        private DeploymentDescriptor dd ;
        private Envelope             envelope ;
        private Call                 call ;
        private String               methodName ;
        private String               targetObjectURI ;
        private HttpServlet          servlet ;
        private HttpSession          session ;

        private javax.naming.Context contxt = null;
        private EJBObject remoteObjRef = null;
        public static java.lang.String CNTXT_PROVIDER_URL = "iiop://localhost:900";
        public static java.lang.String CNTXT_FACTORY_NAME = "com.ibm.ejs.ns.jndi.CNInitialContextFactory";
        private Vector methodParameters = null;
        private String respEncStyle = null;
/**
 * StatelessEJBProvider constructor comment.
 */
public StatelessEJBProvider() {
        super();
}

private void initialize() throws SOAPException {

        if(contxt == null) {

                java.util.Properties properties = new java.util.Properties();
                properties.put(javax.naming.Context.PROVIDER_URL, CNTXT_PROVIDER_URL);
                properties.put(javax.naming.Context.INITIAL_CONTEXT_FACTORY, CNTXT_FACTORY_NAME);
                try {
                        contxt = new javax.naming.InitialContext(properties);
                } catch (NamingException ne) {
                        // ErrorListener?
                        System.out.println("Naming Exception caught during InitialContext creation @ " + CNTXT_PROVIDER_URL);
                        throw new SOAPException(Constants.FAULT_CODE_SERVER, "Unable to initialize context" );
                }
        }

}

  private void initialize(String url, String factory)
    throws SOAPException
  {
    if(contxt == null)
    {
      java.util.Properties properties = new java.util.Properties();
      
      if ((url != null) && (!url.trim().equals("")))
      {
	properties.put(javax.naming.Context.PROVIDER_URL, url);
      }
      if ((factory != null) && (!factory.trim().equals("")))
      {
	properties.put(javax.naming.Context.INITIAL_CONTEXT_FACTORY, factory);
      }
      
      try
      {
	contxt = new javax.naming.InitialContext(properties);
      }
      catch (NamingException ne)
      {
	// ErrorListener?
	System.out.println("Naming Exception caught during InitialContext creation @ " + url);
	throw new SOAPException(Constants.FAULT_CODE_SERVER, "Unable to initialize context");
      }
    }
  }


/**
 * invoke method comment.
 */
public void invoke(SOAPContext reqContext, SOAPContext resContext)
                    throws SOAPException {
                  System.err.println( "=============================================" );
          System.err.println("In TemplateProvider.invoke()" );

        Parameter ret = null;
        Object[] args = null;
        Class[] argTypes = null;

        respEncStyle = call.getEncodingStyleURI();

        if (methodParameters != null) {
                int parametersCount = methodParameters.size ();
                args = new Object[parametersCount];
                argTypes = new Class[parametersCount];

                for (int i = 0; i < parametersCount; i++) {
                        Parameter param = (Parameter) methodParameters.elementAt (i);
                        args[i] = param.getValue ();
                        argTypes[i] = param.getType ();

                        if (respEncStyle == null) {
                                respEncStyle = param.getEncodingStyleURI ();
                        }
                }
        }

        if (respEncStyle == null) {
          respEncStyle = Constants.NS_URI_SOAP_ENC;
        }

        try {

                Method m = MethodUtils.getMethod (remoteObjRef, methodName, argTypes);
                Bean result = new Bean (m.getReturnType (), m.invoke (remoteObjRef, args));


                if (result.type != void.class) {
                  ret = new Parameter (RPCConstants.ELEM_RETURN, result.type,
                                                   result.value, null);
                }


        } catch (InvocationTargetException e) {
                System.err.println("Exception Caught upon method invocation attempt: " + e.getMessage());
                Throwable t = e.getTargetException ();
          throw new SOAPException (Constants.FAULT_CODE_SERVER, t.getMessage(), t);
        }
         catch (Throwable t) {
                System.err.println("Exception Caught upon method invocation attempt: " + t.toString());
                throw new SOAPException (Constants.FAULT_CODE_SERVER, t.getMessage(), t);
        }

        try {
          Response resp = new Response( targetObjectURI,            // URI
                               call.getMethodName(),       // Method
                               (Parameter) ret,            // ReturnValue
                               null,                       // Params
                               null,                       // Header
                               respEncStyle,               // encoding
                               resContext );        // response soapcontext - not supported yet
                  Envelope env = resp.buildEnvelope();
                  StringWriter  sw = new StringWriter();
                  env.marshall( sw, call.getSOAPMappingRegistry(), resContext );
                  resContext.setRootPart( sw.toString(), Constants.HEADERVAL_CONTENT_TYPE_UTF8);
                }
                catch( Exception e ) {
                  if ( e instanceof SOAPException ) throw (SOAPException ) e ;
                  throw new SOAPException( Constants.FAULT_CODE_SERVER, e.toString() );
                }
}

/**
 * locate method comment.
 */
public void locate(DeploymentDescriptor dd, 
                   Envelope env, 
                   Call call, 
                   String methodName, 
                   String targetObjectURI, 
                   SOAPContext reqContext)
              throws org.apache.soap.SOAPException {

          HttpServlet servlet = (HttpServlet) reqContext.getProperty( Constants.BAG_HTTPSERVLET );
          HttpSession session = (HttpSession) reqContext.getProperty( Constants.BAG_HTTPSESSION );

          System.err.println( "=============================================" );
          System.err.println( "In TemplateProvider.locate()" );
          System.err.println( "URI: " + targetObjectURI );
          System.err.println( "DD.ServiceClass: " + dd.getServiceClass() );
          System.err.println( "DD.ProviderClass: " + dd.getProviderClass() );
          System.err.println( "Call.MethodName: " + call.getMethodName() );

          this.dd              = dd ;
          this.envelope        = env ;
          this.call            = call ;
          this.methodName      = methodName ;
          this.targetObjectURI = targetObjectURI ;
          this.servlet         = servlet ;
          this.session         = session ;

          Hashtable props = dd.getProps();


        String ContxtProviderURL = (String) props.get("ContextProviderURL");
        String ContxtFactoryName = (String) props.get("FullContextFactoryName");

        if ((ContxtProviderURL != null) || (ContxtFactoryName != null))
                initialize(ContxtProviderURL, ContxtFactoryName);
        else
                initialize();

        String homeInterfaceName = (String) props.get("FullHomeInterfaceName");
        if (homeInterfaceName == null)
                throw new SOAPException(Constants.FAULT_CODE_SERVER, "Error in Deployment Descriptor Property Settings");

        // From the Deployment Descriptor get the JNDI lookup name that is inside the "java" element...
        String jndiName = (String) props.get("JNDIName");
        if ( jndiName == null ) jndiName = dd.getProviderClass();

        if ((jndiName != null) && (contxt != null)) {

                try {

                        // Use service name to locate EJB home object via the contxt
                        // EJBHome home = (EJBHome) contxt.lookup(jndiName);
                        EJBHome home = (EJBHome) PortableRemoteObject.narrow(contxt.lookup(jndiName), Class.forName(homeInterfaceName));
                        // call the 'create' method on the EJB home object, and store the
                        //   ref to the EJB object.
                        Method createMethod = home.getClass().getMethod("create", new Class[0]);
                        remoteObjRef = (EJBObject) createMethod.invoke((Object) home, new Object[0]);

                } catch (Exception e) {

                        System.out.println("Exception caught: " + e.toString());
                        throw new SOAPException(Constants.FAULT_CODE_SERVER,"Error in connecting to EJB", e);
                }
        }

        // Once previous steps have been successful, then we take the Call Object
        //  and extract the method name from it, and any parameters, and store them.
        methodName = call.getMethodName();
        methodParameters = call.getParams();


}
}
