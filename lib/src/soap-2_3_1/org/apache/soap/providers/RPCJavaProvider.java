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

package org.apache.soap.providers ;

import java.io.* ;
import javax.servlet.* ;
import javax.servlet.http.* ;
import org.apache.soap.* ;
import org.apache.soap.rpc.* ;
import org.apache.soap.server.* ;
import org.apache.soap.server.http.* ;
import org.apache.soap.util.* ;

public class RPCJavaProvider implements Provider {
    protected DeploymentDescriptor dd ;
    protected Envelope             envelope ;
    protected Call                 call ;
    protected String               methodName ;
    protected String               targetObjectURI ;
    protected HttpServlet          servlet ;
    protected HttpSession          session ;

    protected Object               targetObject ;

    public void locate( DeploymentDescriptor dd,
                        Envelope env,
                        Call call,
                        String methodName,
                        String targetObjectURI,
                        SOAPContext reqContext )
        throws SOAPException {
      
      HttpServlet servlet = (HttpServlet) reqContext.getProperty( Constants.BAG_HTTPSERVLET );
      HttpSession session = (HttpSession) reqContext.getProperty( Constants.BAG_HTTPSESSION );

      this.dd              = dd ;
      this.envelope        = env ;
      this.call            = call ;
      this.methodName      = methodName ;
      this.targetObjectURI = targetObjectURI ;
      this.servlet         = servlet ;
      this.session         = session ;

      ServletConfig  config  = null ;
      ServletContext context = null ;

      if ( servlet != null ) config  = servlet.getServletConfig();
      if ( config != null ) context = config.getServletContext ();

      ServiceManager serviceManager =
        ServerHTTPUtils.getServiceManagerFromContext (context);

      // Default processing for 'java' and 'script' providers
      // call on a valid method name?
      if (!RPCRouter.validCall (dd, call)) {
        throw new SOAPException (Constants.FAULT_CODE_SERVER,
                                 "Method '" + call.getMethodName () +
                                 "' is not supported.");
      }

      // get at the target object
      targetObject = ServerHTTPUtils.getTargetObject (serviceManager,
                                                      dd, targetObjectURI,
                                                      servlet, session,
                                                      reqContext,
                                                      context);
    };


    public void invoke(SOAPContext reqContext, SOAPContext resContext)
               throws SOAPException {
      // invoke the method on the target object
      try {
        Response resp = RPCRouter.invoke( dd, call, targetObject, 
                                          reqContext, resContext );
        Envelope env = resp.buildEnvelope();
        StringWriter  sw = new StringWriter();
        env.marshall( sw, call.getSOAPMappingRegistry(), resContext );
        resContext.setRootPart( sw.toString(), Constants.HEADERVAL_CONTENT_TYPE_UTF8);
      }
      catch( Exception e ) {
        if ( e instanceof SOAPException ) throw (SOAPException ) e ;
        throw new SOAPException( Constants.FAULT_CODE_SERVER, e.toString() );
      }
    };
};
