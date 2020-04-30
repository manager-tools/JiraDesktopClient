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

package org.apache.soap.util.net;

import java.net.*;
import java.io.*;

/**
 * A <code>TcpTunnel</code> object listens on the given port,
 * and once <code>Start</code> is pressed, will forward all bytes
 * to the given host and port.
 *
 * @author Sanjiva Weerawarana (sanjiva@watson.ibm.com)
 */
public class TcpTunnel {
  public static void main (String args[]) throws IOException {
    if (args.length != 3) {
      System.err.println ("Usage: java TcpTunnel listenport tunnelhost tunnelport");
      System.exit (1);
    }

    int listenport = Integer.parseInt (args[0]);
    String tunnelhost = args[1];
    int tunnelport = Integer.parseInt (args[2]);

    System.out.println ("TcpTunnel: ready to rock and roll on port " + 
                        listenport);

    ServerSocket ss = new ServerSocket (listenport);
    while (true) {
      // accept the connection from my client
      Socket sc = ss.accept ();
      
      // connect to the thing I'm tunnelling for
      Socket st = new Socket (tunnelhost, tunnelport);
      
      System.out.println ("TcpTunnel: tunnelling port " + listenport +
                          " to port " + tunnelport + " on host " + 
                          tunnelhost);

      // relay the stuff thru
      new Relay (sc.getInputStream(), st.getOutputStream(), null).start ();
      new Relay (st.getInputStream(), sc.getOutputStream(), null).start ();
      
      // that's it .. they're off; now I go back to my stuff.
    }
  }
}
