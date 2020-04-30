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

package org.apache.soap.util;

import java.util.* ;
import javax.servlet.* ;
import javax.servlet.http.* ;
import org.apache.soap.* ;
import org.apache.soap.rpc.* ;
import org.apache.soap.server.* ;

/**
 * A <code>ConfigManager</code> ...
 *
 * @author Dug (dug@us.ibm.com)
 *
 * With the introduction of a ConfigManager, the notion of a SOAP
 * configuration file was also introduced.  The SOAP server will now
 * look for a 'soap.xml' file - or whatever filename was specified
 * by the 'ConfigFile' init parameter of the Servlet.
 * The soap.xml file should look like:
 * <soapServer>
 *  <!-- This section defines the same thing you get if you don't -->
 *  <!-- specify anything at all - aka the default                -->
 *  <configManager value="org.apache.soap.server.DefaultConfigManager" >
 *    <option name="filename" value="DS.ds" />
 *  </configManager>
 * </soapServer>
 *
 * See 'DefaultConfigManager' for more info.
 *
 * Classes that implement these APIs are responsible for saving and
 * loading the list of deployed services.  The SOAP server will just
 * call deploy/undeploy... and its up to the class that implements
 * these API to handling the persistence of the list.  See the
 * DefaultConfigManager to see an example of how to do this.
 *
 * Init will be called after the 'setOptions' call allowing the ConfigMgr
 * to do any setup.
 * 
 */
public interface ConfigManager
{
  public void      setContext(ServletContext context);
  public void      setOptions( Hashtable options ) throws SOAPException ;
  public void      init() throws SOAPException ;
  public void      deploy( DeploymentDescriptor dd ) throws SOAPException ;
  public String[]  list() throws SOAPException ;

  public DeploymentDescriptor undeploy( String id ) throws SOAPException ;
  public DeploymentDescriptor query(String id) throws SOAPException ;
}
