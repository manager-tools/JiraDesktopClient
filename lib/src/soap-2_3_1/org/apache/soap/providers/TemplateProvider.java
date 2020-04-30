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
import java.util.* ;
import javax.servlet.* ;
import javax.servlet.http.* ;
import org.apache.soap.* ;
import org.apache.soap.rpc.* ;
import org.apache.soap.server.* ;
import org.apache.soap.util.* ;

public class TemplateProvider implements Provider {
    private DeploymentDescriptor dd ;
    private Envelope             envelope ;
    private Call                 call ;
    private String               methodName ;
    private String               targetObjectURI ;
    private HttpServlet          servlet ;
    private HttpSession          session ;

    public void locate( DeploymentDescriptor dd,
                        Envelope env,
                        Call call,
                        String methodName,
                        String targetObjectURI,
                        SOAPContext reqContext)
        throws SOAPException {

      HttpServlet servlet = (HttpServlet) reqContext.getProperty( Constants.BAG_HTTPSERVLET );
      HttpSession session = (HttpSession) reqContext.getProperty( Constants.BAG_HTTPSESSION );

      System.err.println( "=============================================" );
      System.err.println( "In TemplateProvider.locate()" );
      System.err.println( "Method: " + methodName );
      System.err.println( "URI: " + targetObjectURI );
      System.err.println( "DD.ServiceClass: " + dd.getServiceClass() );
      System.err.println( "DD.ProviderClass: " + dd.getProviderClass() );
      System.err.println( "Call.MethodName: " + call.getMethodName() );

      Hashtable    props = dd.getProps();
      Enumeration  keys  = props.keys();

      System.err.println( "Options:" );
      while ( keys.hasMoreElements() ) {
          String  key   = (String) keys.nextElement();
          String  value = (String) props.get( key );
          System.err.println( "Key: " + key + "  Value: " + value );
      }

      this.dd              = dd ;
      this.envelope        = env ;
      this.call            = call ;
      this.methodName      = methodName ;
      this.targetObjectURI = targetObjectURI ;
      this.servlet         = servlet ;
      this.session         = session ;

      // Add logic to locate/load the service here
    };


    public void invoke(SOAPContext reqContext, SOAPContext resContext)
                        throws SOAPException {
      System.err.println( "=============================================" );
      System.err.println( "In TemplateProvider.invoke()" );

      // Add logic to invoke the service and get back the result here

      // If you want the client to use a new targetObjectURI for subsequence
      // calls place it here (targetObjectURI on 'new Response').
      //   i.e. we added an 'instance-id' to the urn
      try {
            Response resp = new Response( targetObjectURI,   // URI
                             call.getMethodName(),  // Method
                             (Parameter) null,      // ReturnValue
                             null,                  // Params
                             null,                  // Header
                             null,                  // encoding
                             resContext );          // response soapcontext - not supported yet

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
