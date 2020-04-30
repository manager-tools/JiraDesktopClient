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
import java.util.*;
import javax.net.ssl.*;
import java.security.*;

/**
 * A bunch of utility stuff for doing SSL things.
 *
 * @author Chris Nelson (cnelson@synchrony.net)
 */
public class SSLUtils {
        static String tunnelHost;
        static int tunnelPort;
	
	/** This method builds an SSL socket, after auto-starting SSL */
	public static Socket buildSSLSocket(String host, int port, String httpProxyHost,
                                            int httpProxyPort)
		throws IOException, UnknownHostException
	{
           SSLSocket sslSocket =  null;
           SSLSocketFactory factory =
              (SSLSocketFactory)SSLSocketFactory.getDefault();

           // Determine if a proxy should be used. Use system properties if set
           // Otherwise use http proxy. If neither is set, dont use a proxy
           tunnelHost = System.getProperty("https.proxyHost");
           tunnelPort = Integer.getInteger("https.proxyPort", 80).intValue();

           if (tunnelHost==null) {
              // Try to use http proxy instead
              tunnelHost = httpProxyHost;
              tunnelPort = httpProxyPort;
           }

           /*
           System.out.println("https proxyHost=" + tunnelHost +
                              " proxyPort=" + tunnelPort +
                              " host=" + host +
                              " port=" + port);
           */

           /*                         
            * If a proxy has been set...
            * Set up a socket to do tunneling through the proxy.
            * Start it off as a regular socket, then layer SSL
            * over the top of it.
            */
           if (tunnelHost==null) {
              sslSocket = (SSLSocket)factory.createSocket(host, port);
           } else {
              Socket tunnel = new Socket(tunnelHost, tunnelPort);
              doTunnelHandshake(tunnel, host, port);

              // Overlay tunnel socket with SSL
              sslSocket = (SSLSocket)factory.createSocket(tunnel, host, port, true);
           }

           /*
            * Handshaking is started manually in this example because
            * PrintWriter catches all IOExceptions (including
            * SSLExceptions), sets an internal error flag, and then
            * returns without rethrowing the exception.
            *
            * Unfortunately, this means any error messages are lost,
            * which caused lots of confusion for others using this
            * code.  The only way to tell there was an error is to call
            * PrintWriter.checkError().
            */
           sslSocket.startHandshake();   

           return  sslSocket;  
	    
	}

        static private void doTunnelHandshake(Socket tunnel, String host, int port)
         throws IOException
        {
             OutputStream out = tunnel.getOutputStream();
             String msg = "CONNECT " + host + ":" + port + " HTTP/1.0\n"
                          + "User-Agent: "
                          + sun.net.www.protocol.http.HttpURLConnection.userAgent
                          + "\r\n\r\n";
             byte b[];
             try {
                 /*
                  * We really do want ASCII7 -- the http protocol doesn't change
                  * with locale.
                  */
                 b = msg.getBytes("ASCII7");
             } catch (UnsupportedEncodingException ignored) {
                 /*
                  * If ASCII7 isn't there, something serious is wrong, but
                  * Paranoia Is Good (tm)
                  */
                 b = msg.getBytes();
             }
             out.write(b);
             out.flush();

             /*
              * We need to store the reply so we can create a detailed
              * error message to the user.
              */
             byte		reply[] = new byte[200];
             int		replyLen = 0;
             int		newlinesSeen = 0;
             boolean		headerDone = false;	/* Done on first newline */

             InputStream	in = tunnel.getInputStream();
             boolean		error = false;

             while (newlinesSeen < 2) {
                 int i = in.read();
                 if (i < 0) {
                     throw new IOException("Unexpected EOF from proxy");
                 }
                 if (i == '\n') {
                     headerDone = true;
                     ++newlinesSeen;
                 } else if (i != '\r') {
                     newlinesSeen = 0;
                     if (!headerDone && replyLen < reply.length) {
                         reply[replyLen++] = (byte) i;
                     }
                 }
             }

             /*
              * Converting the byte array to a string is slightly wasteful
              * in the case where the connection was successful, but it's
              * insignificant compared to the network overhead.
              */
             String replyStr;
             try {
                 replyStr = new String(reply, 0, replyLen, "ASCII7");
             } catch (UnsupportedEncodingException ignored) {
                 replyStr = new String(reply, 0, replyLen);
             }

             // Parse response, check for status code
             StringTokenizer st = new StringTokenizer(replyStr);
             st.nextToken(); // ignore version part
             if (!st.nextToken().startsWith("200")) {
                throw new IOException("Unable to tunnel through "
                        + tunnelHost + ":" + tunnelPort
                        + ".  Proxy returns \"" + replyStr + "\"");
             }

             /* tunneling Handshake was successful! */
         }
}
