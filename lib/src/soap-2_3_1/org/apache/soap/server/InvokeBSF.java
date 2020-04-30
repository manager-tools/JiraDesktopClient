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

import org.apache.soap.util.Bean;
import org.apache.soap.SOAPException;
import org.apache.soap.Constants;
import org.apache.soap.server.DeploymentDescriptor;
import com.ibm.bsf.*;


/**
 * This class provides a static method to invoke services implemented
 * via BSF. This code used to live in rpcrouter.jsp, but it was moved
 * here to keep rpcrouter.jsp independent of BSF (didn't want to prereq
 * BSF). The code in rpcrouter.jsp invokes these methods using reflection
 * to avoid being dependent on them ..
 *
 * @author Sanjiva Weerawarana (sanjiva@watson.ibm.com)
 * @author Matthew J. Duftler (duftler@us.ibm.com)
 */
public class InvokeBSF {

  /**
   * This method is invoked to exec the script implementing the service into
   * the scripting engine so that later service calls can work.
   *
   * @param dd the deployment descriptor of the service
   * @param scriptStr the string containing function definitions to be exec'ed
   *
   * @exception SOAPException if something goes wrong
   */
  public static void init (DeploymentDescriptor dd, Object target,
                           String scriptStr) throws SOAPException {
    try {
      BSFManager mgr = (BSFManager) target;
      mgr.exec (dd.getScriptLanguage (), "service script for '" + 
                dd.getID () + "'", 0, 0, scriptStr);
    } catch (BSFException e) {
      throw new SOAPException (Constants.FAULT_CODE_SERVER,
                               "BSF Error: " + e.getMessage (), e);
    }
  }

  /**
   * This method is invoked when a method is to be called.
   *
   * @param dd the deployment descriptor of the service
   * @param target the BSFManager object which is used to exec/call
   * @param methodName name of function in the script to call
   * @param args arguments to the function or null if none
   *
   * @return a Bean object containing the result
   *
   * @exception SOAPException if something goes wrong
   */
  public static Bean service (DeploymentDescriptor dd, Object target,
                              String methodName, Object[] args) 
       throws SOAPException {
    try {
      BSFManager mgr = (BSFManager) target;
      BSFEngine eng = mgr.loadScriptingEngine (dd.getScriptLanguage ());
      Object result = eng.call (null, methodName, args);
      return new Bean ((result != null) ? result.getClass () : Object.class,
                       result);
    } catch (BSFException e) {
      throw new SOAPException (Constants.FAULT_CODE_SERVER,
                               "BSF Error: " + e.getMessage (), e);
    }
  }
}
