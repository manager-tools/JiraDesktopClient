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
 * A <code>DefaultConfigManager</code> ...
 *
 * @author Dug (dug@us.ibm.com)
 * @author <a href="mailto:magnus@handpoint.com">Magnus Thor Torfason</a>
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
 * If not specified, the DefaultConfigManager will be used.  By
 * default this will write the list of deployed services out to a file
 * called 'DeployedServices.ds' in the current directory.  To change
 * this simply use the 'filename' option in the 'configManager' section
 * of the soap.xml configuration file.
 *
 */
public class DefaultConfigManager extends BaseConfigManager
          implements ConfigManager {

  /** The name of the deployment file. */
  protected String filename = "DeployedServices.ds";


  /**
   * This method sets the configuration options (usually
   * read from the config file).
   */
  public void setOptions( Hashtable options ) {
    if ( options == null ) return ;

    String value = (String) options.get( "filename" );
    if ( value != null && !"".equals(value) )
      filename = value ;
  }

  /**
   * Loads the descriptors from the underlying registry file, which
   * contains a serialized Hashtable.
   */
  public void loadRegistry() throws SOAPException {
    // load in a serialized thing
    dds = null ;
    try {
      File file = ServerHTTPUtils.getFileFromNameAndContext(filename,
                                                            context);
      FileInputStream fis = new FileInputStream (file);
      ObjectInputStream is = new ObjectInputStream (fis);

      dds = (Hashtable) is.readObject ();
      is.close ();
    } catch(Exception e) {
      dds = new Hashtable ();
      System.err.println ("SOAP Service Manager: Unable to read '" +
                          filename +  "': assuming fresh start");
    }
  }

  /**
   * Saves currently deployed descriptors to the underlying registry file,
   * as a serialized Hashtable.
   */
  public void saveRegistry() throws SOAPException {
    try {
      File file = ServerHTTPUtils.getFileFromNameAndContext(filename,
                                                            context);
      FileOutputStream fos = new FileOutputStream (file);
      ObjectOutputStream os = new ObjectOutputStream (fos);

      os.writeObject (dds);
      os.close ();
    } catch (Exception e) {
      throw new SOAPException (Constants.FAULT_CODE_SERVER,
                               "Error saving services registry: " +
                               e.getMessage ());
    };
  }
};
