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

package org.apache.soap.server ;

import java.util.* ;
import java.io.* ;
import javax.servlet.* ;
import javax.servlet.http.* ;
import org.apache.soap.* ;
import org.apache.soap.rpc.* ;
import org.apache.soap.server.* ;
import org.apache.soap.server.http.* ;
import org.apache.soap.util.* ;

/**
 * An <code>XMLConfigManager</code> ...
 *
 * @author Dug (dug@us.ibm.com)
 * @author <a href="mailto:magnus@handpoint.com">Magnus Thor Torfason</a>
 *
 * This class should be almost identical in function to the
 * DefaultConfigManager.
 *
 * <p>
 *
 * The only notable difference is that while
 * the DefaultConfigManager uses java readObject() and writeObject()
 * methods, this class uses the XML routines of DeploymentDescriptor
 * to save and load the DeploymentDescriptors from the underlying
 * registry file (typically <tt>DeployedServices.xml</tt>).
 *
 */
public abstract class BaseConfigManager implements ConfigManager {
  protected Hashtable      dds      = new Hashtable();
  protected String[]       serviceNamesCache ;
  protected ServletContext context  = null;

  /**
   * Sets the Servlet Context.
   */
  public void setContext(ServletContext context) {
    this.context = context;
  }

  /**
   * The init method loads any services that are defined in
   * the underlying registry file.
   */
  public void init() throws SOAPException {
    loadRegistry();
  }

  /**
   * Used to deploy a service using a specified DeploymentDescriptor.
   *
   * <p>
   *
   * Note that this method will save the currently deployed services
   * to the underlying registry file, overriding the old configuration.
   */
  public void deploy( DeploymentDescriptor dd ) throws SOAPException {
    String  id = dd.getID();
    dds.put( id, dd );
    saveRegistry();
    serviceNamesCache = null ;
  }

  /**
   * Undeploy previously deployed services.
   *
   * <p>
   *
   * Note that this method will save the currently deployed services
   * to the underlying registry file, overriding the old configuration.
   */
  public DeploymentDescriptor undeploy( String id ) throws SOAPException {
    DeploymentDescriptor dd = (DeploymentDescriptor) dds.remove( id );
    if ( dd != null ) {
      saveRegistry();
      serviceNamesCache = null ;
    } else {
      throw new SOAPException( Constants.FAULT_CODE_SERVER,
        "Service '" + id + "' unknown" );
    }
    return( dd );
  }

  /**
   * Returns a list of all currently deployed services.
   */
  public String[] list() throws SOAPException {
    if (serviceNamesCache != null) {
      return serviceNamesCache;
    }
    Enumeration e = dds.keys ();
    int count = dds.size ();
    serviceNamesCache = new String[count];
    for (int i = 0; i < count; i++) {
      serviceNamesCache[i] = (String) e.nextElement ();
    }
    return serviceNamesCache;
  }

  /**
   * Returns a DeploymentDescriptor from the ConfigManager.
   *
   * <p>
   *
   * This method will simply return null if there is no descriptor
   * corresponding to the id (should it throw a SOAPException ?).
   */
  public DeploymentDescriptor query(String id) throws SOAPException {
    DeploymentDescriptor dd = (DeploymentDescriptor) dds.get (id);
    return( dd );
  }

  /**
   * The loadRegistry() method must be implemented in non-abstract
   * subclasses of BaseConfigManager.
   */
  public abstract void loadRegistry() throws SOAPException;

  /**
   * The saveRegistry() method must be implemented in non-abstract
   * subclasses of BaseConfigManager.
   */
  public abstract void saveRegistry() throws SOAPException;
}
