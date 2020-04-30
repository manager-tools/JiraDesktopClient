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

import org.apache.soap.*;
import org.apache.soap.rpc.*;
import org.apache.soap.transport.*;
import org.apache.soap.util.mime.*;
import javax.mail.*;
import javax.mail.internet.*;
import javax.activation.*;
import java.lang.reflect.*;

/**
 * A bunch of utility stuff for doing HTTP things.
 * <p>
 * 2000/07/30 W. Cloetens     added Multipart Mime support
 *
 * @author Sanjiva Weerawarana (sanjiva@watson.ibm.com)
 * @author Matthew J. Duftler (duftler@us.ibm.com)
 * @author Wouter Cloetens (wcloeten@raleigh.ibm.com)
 * @author Scott Nichol (snichol@computer.org)
 */
public class HTTPUtils {
  private static final String HTTP_VERSION = "1.0";
  private static final int    HTTP_DEFAULT_PORT = 80;
  private static final int    HTTPS_DEFAULT_PORT = 443;

  public  static final int    DEFAULT_OUTPUT_BUFFER_SIZE = 512;

  /**
   * This method either creates a socket or calls SSLUtils to
   * create an SSLSocket.  It uses reflection to avoid a compile time
   * dependency on SSL.
   *
   * @author Chris Nelson
   */
  private static Socket buildSocket(URL url, int targetPort,
                                    String httpProxyHost, int httpProxyPort,
				    Boolean tcpNoDelay)
     throws Exception {
      Socket s = null;
      String host = null;
      int port = targetPort;
      host = url.getHost();

      if (url.getProtocol().equalsIgnoreCase("HTTPS")) {
          // Using reflection to avoid compile time dependencies
          Class SSLUtilsClass =
              Class.forName("org.apache.soap.util.net.SSLUtils");
          Class[] paramTypes = new Class[] {String.class, int.class, String.class, int.class};
          Method buildSSLSocket = SSLUtilsClass.getMethod(
              "buildSSLSocket", paramTypes);
          Object[] params = new Object[] {host, new Integer(port),
              httpProxyHost, new Integer(httpProxyPort)};
          s = (Socket)buildSSLSocket.invoke(null, params);
      } else {
          if (httpProxyHost != null) {
              host = httpProxyHost;
              port = httpProxyPort;
          }
          s = new Socket(host, port);
      }
      
      if (tcpNoDelay != null)
      {
	if (s != null) 
	  s.setTcpNoDelay(tcpNoDelay.booleanValue());
      }

      return s;
   }

  /**
   * Utility function to determine port number from URL object.
   *
   * @param url URL object from which to determine port number
   * @return port number
   */
  private static int getPort(URL url) throws IOException {
      int port = url.getPort();
      if (port < 0)  // No port given, use HTTP or HTTPS default
          if (url.getProtocol().equalsIgnoreCase("HTTPS"))
              port = HTTPS_DEFAULT_PORT;
          else
              port = HTTP_DEFAULT_PORT;
      return port;
  }

  /**
   * POST something to the given URL. The headers are put in as
   * HTTP headers, the content length is calculated and the content
   * byte array is sent as the POST content.
   *
   * @param url the url to post to
   * @param request the message
   * @param timeout the amount of time, in ms, to block on reading data
   * @param httpProxyHost the HTTP proxy host or null if no proxy
   * @param httpProxyPort the HTTP proxy port, if the proxy host is not null
   * @return the response message
   */
  public static TransportMessage post(URL url, TransportMessage request,
                                      int timeout,
                                      String httpProxyHost, int httpProxyPort)
      throws IllegalArgumentException, IOException, SOAPException {
    return post(url,
                request,
                timeout,
                httpProxyHost,
                httpProxyPort,
                DEFAULT_OUTPUT_BUFFER_SIZE,
		null);
  }

  /**
   * POST something to the given URL. The headers are put in as
   * HTTP headers, the content length is calculated and the content
   * byte array is sent as the POST content.
   *
   * @param url the url to post to
   * @param request the message
   * @param timeout the amount of time, in ms, to block on reading data
   * @param httpProxyHost the HTTP proxy host or null if no proxy
   * @param httpProxyPort the HTTP proxy port, if the proxy host is not null
   * @param outputBufferSize the size of the output buffer on the HTTP stream
   * @return the response message
   */
  public static TransportMessage post(URL url, TransportMessage request,
                                      int timeout,
                                      String httpProxyHost, int httpProxyPort,
                                      int outputBufferSize)
      throws IllegalArgumentException, IOException, SOAPException {
    return post(url,
                request,
                timeout,
                httpProxyHost,
                httpProxyPort,
                outputBufferSize,
		null);
  }

  /**
   * POST something to the given URL. The headers are put in as
   * HTTP headers, the content length is calculated and the content
   * byte array is sent as the POST content.
   *
   * @param url the url to post to
   * @param request the message
   * @param timeout the amount of time, in ms, to block on reading data
   * @param httpProxyHost the HTTP proxy host or null if no proxy
   * @param httpProxyPort the HTTP proxy port, if the proxy host is not null
   * @param outputBufferSize the size of the output buffer on the HTTP stream
   * @param tcpNoDelay the tcpNoDelay setting for the socket
   * @return the response message
   */
  public static TransportMessage post(URL url, TransportMessage request,
                                      int timeout,
                                      String httpProxyHost, int httpProxyPort,
                                      int outputBufferSize,
				      Boolean tcpNoDelay)
      throws IllegalArgumentException, IOException, SOAPException {
      /* Open the connection */
      OutputStream outStream = null;
      InputStream inStream = null;
      BufferedReader in = null;
      int port;
      Socket s;
      try {
          port = getPort(url);

          s = buildSocket(url, port, httpProxyHost, httpProxyPort, tcpNoDelay);
          if (url.getProtocol().equalsIgnoreCase("HTTPS")) {
             // Ignore proxy from now on. Buildsocket takes handles it
             httpProxyHost = null;
          }

          if (timeout > 0)  // Should be redundant but not every JVM likes this
              s.setSoTimeout(timeout);

          outStream = s.getOutputStream ();
          inStream = s.getInputStream ();
      }
      catch (Exception e) {
        Throwable t = e;

        if (t instanceof InvocationTargetException) {
          t = ((InvocationTargetException)t).getTargetException();
        }

        throw new IllegalArgumentException("Error opening socket: " + t);
      }

      /* Compute the Request URI */
      String URI = (httpProxyHost == null ? url.getFile() : url.toString());
      if (URI.length() == 0) URI = "/";

      /* Construct the HTTP header. */
      StringBuffer headerbuf = new StringBuffer();
      headerbuf.append(Constants.HEADER_POST).append(' ').append(URI)
          .append(" HTTP/").append(HTTP_VERSION).append("\r\n")
          .append(Constants.HEADER_HOST).append(": ").append(url.getHost())
          .append(':').append(port)
          .append("\r\n")
          .append(Constants.HEADER_CONTENT_TYPE).append(": ")
          .append(request.getContentType()).append("\r\n")
          .append(Constants.HEADER_CONTENT_LENGTH).append(": ")
          .append(request.getContentLength()).append("\r\n");
      for (Enumeration e = request.getHeaderNames(); e.hasMoreElements(); ) {
          Object key = e.nextElement();
          headerbuf.append(key).append(": ")
              .append(request.getHeader((String)key)).append("\r\n");
      }
      headerbuf.append("\r\n");

      /* Send the request. */
      BufferedOutputStream bOutStream = new BufferedOutputStream(outStream, outputBufferSize);
      bOutStream.write(
          headerbuf.toString().getBytes(Constants.HEADERVAL_DEFAULT_CHARSET));
      request.writeTo(bOutStream);
      //bOutStream.write('\r'); bOutStream.write('\n');
      //bOutStream.write('\r'); bOutStream.write('\n');
      bOutStream.flush();
      outStream.flush();

      BufferedInputStream bInStream = new BufferedInputStream(inStream);
      /* Read the response status line. */
      int statusCode = 0;
      String statusString = null;
      StringBuffer linebuf = new StringBuffer();
      int b = 0;
      while (b != '\n' && b != -1) {
          b = bInStream.read();
          if (b != '\n' && b != '\r' && b != -1)
              linebuf.append((char)b);
      }
      String line = linebuf.toString();
      try {
          StringTokenizer st = new StringTokenizer(line);
          st.nextToken(); // ignore version part
          statusCode = Integer.parseInt (st.nextToken());
          StringBuffer sb = new StringBuffer();
          while (st.hasMoreTokens()) {
              sb.append (st.nextToken());
              if (st.hasMoreTokens()) {
                  sb.append(" ");
              }
          }
          statusString = sb.toString();
      }
      catch (Exception e) {
          throw new IllegalArgumentException(
              "Error parsing HTTP status line \"" + line + "\": " + e);
      }

      /* Read the entire response (following the status line)
       * into a byte array. */
      ByteArrayDataSource ds = new ByteArrayDataSource(bInStream,
                          Constants.HEADERVAL_DEFAULT_CHARSET);

      /* Extract the headers, content type and content length. */
      byte[] bytes = ds.toByteArray();
      Hashtable respHeaders = new Hashtable();
      int respContentLength = -1;
      String respContentType = null;
      StringBuffer namebuf = new StringBuffer();
      StringBuffer valuebuf = new StringBuffer();
      boolean parsingName = true;
      int offset;
      for (offset = 0; offset < bytes.length; offset++) {
          if (bytes[offset] == '\n') {
              if (namebuf.length() == 0)
                  break;
              String name = namebuf.toString();

              // Remove trailing ; to prevent ContextType from throwing exception
              int valueLen = valuebuf.length();

              if (valueLen > 0 && valuebuf.charAt(valueLen - 1) == ';') {
                  valuebuf.deleteCharAt(valueLen - 1);
              }

              String value = valuebuf.toString();
              if (name.equalsIgnoreCase(Constants.HEADER_CONTENT_LENGTH))
                  respContentLength = Integer.parseInt(value);
              else if (name.equalsIgnoreCase(Constants.HEADER_CONTENT_TYPE))
                  respContentType = value;
              else
                  respHeaders.put(name, value);
              namebuf = new StringBuffer();
              valuebuf = new StringBuffer();
              parsingName = true;
          }
          else if (bytes[offset] != '\r') {
              if (parsingName) {
                  if (bytes[offset] == ':') {
                      parsingName = false;
                      if ((offset != bytes.length-1) &&
                          bytes[offset+1] == ' ')
                        offset++;
                  }
                  else
                      namebuf.append((char)bytes[offset]);
              }
              else
                  valuebuf.append((char)bytes[offset]);
          }
      }
      InputStream is = ds.getInputStream();
      is.skip(offset + 1);
      if (respContentLength < 0)
          respContentLength = ds.getSize() - offset - 1;

      /* Construct the response object. */
      SOAPContext ctx;
      TransportMessage response;
      try {
          // Create response SOAPContext.
          ctx = new SOAPContext();
          // Read content.
          response = new TransportMessage(is, respContentLength,
                                          respContentType, ctx, respHeaders);
          // Extract envelope and SOAPContext
          response.read();
      } catch (MessagingException me) {
          throw new IllegalArgumentException("Error parsing response: " + me);
      }

      /* All done here! */
      bOutStream.close();
      outStream.close();
      bInStream.close();
      inStream.close();
      s.close();
      return response;
  }
}
