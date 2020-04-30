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

/*
 * Begin Transport-Hook-Extension
 */
package org.apache.soap.transport;

import java.io.*;
import java.net.*;
import java.util.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;
import org.xml.sax.*;
import org.apache.soap.util.net.*;
import org.apache.soap.util.xml.*;
import org.apache.soap.*;
import org.apache.soap.rpc.SOAPContext;
import org.apache.soap.encoding.*;
import org.apache.soap.transport.*;

/**
 * <code>SOAPHTTPConnection</code> is an implementation of the
 * <code>SOAPTransport</code> interface for <em>HTTP</em>.
 *
 * @author Ryo Neyama (neyama@jp.ibm.com)
 */
public class FilterTransport implements SOAPTransport {
  private EnvelopeEditor editor;
  private SOAPTransport transport;

  public FilterTransport(EnvelopeEditor editor, SOAPTransport transport) {
    this.editor = editor;
    this.transport = transport;
  }

  /**
   * This method is used to request that an envelope be posted to the
   * given URL. The response (if any) must be gotten by calling the
   * receive() function.
   *
   * @param sendTo the URL to send the envelope to
   * @param action the SOAPAction header field value
   * @param headers any other header fields to go to as protocol headers
   * @param env the envelope to send
   * @param smr the XML<->Java type mapping registry (passed on)
   * @param ctx the request SOAPContext
   *
   * @exception SOAPException with appropriate reason code if problem
   */
  public void send(URL sendTo,
                   String action,
                   Hashtable headers,
                   Envelope env,
                   SOAPMappingRegistry
                   smr,
                   SOAPContext ctx) throws SOAPException
  {
    try {
      StringWriter sout = new StringWriter();
      env.marshall (sout, smr, ctx);

      StringReader sin = new StringReader(sout.getBuffer().toString());
      if (editor != null) {
        sout = new StringWriter();
        editor.editOutgoing(sin, sout);
        sout.flush();
        sin = new StringReader(sout.getBuffer().toString());
      }

      DocumentBuilder xdb = XMLParserUtils.getXMLDocBuilder();
      Element docElem = xdb.parse(new InputSource(sin)).getDocumentElement();

      Envelope env2 = Envelope.unmarshall(docElem);
      transport.send(sendTo, action, headers, env2, smr, ctx);
    } catch (IllegalArgumentException e) {
      throw new SOAPException (Constants.FAULT_CODE_CLIENT,
                               e.getMessage(),
                               e);
    } catch (SAXException e) {
      throw new SOAPException (Constants.FAULT_CODE_CLIENT,
                               e.getMessage(),
                               e);
    } catch (IOException e) {
      throw new SOAPException (Constants.FAULT_CODE_CLIENT,
                               e.getMessage(),
                               e);
    }
  }

  /**
   * Return a buffered reader to receive back the response to whatever
   * was sent to whatever.
   *
   * @return a reader to read the results from or null if that's not
   *         possible.
   */
  public BufferedReader receive () {
    try {
      BufferedReader in = transport.receive();
      if (editor == null || in == null)
        return in;
      else {
        StringWriter sout = new StringWriter();
        editor.editIncoming(in, sout);
        sout.flush();
        StringReader sin = new StringReader(sout.getBuffer().toString());
        return new BufferedReader(sin);
      }
    } catch (SOAPException e) {
      e.printStackTrace();
      // This exception should be thrown to the application.
      return null;
    }
  }

  /**
   * Return access to headers generated by the protocol.
   * 
   * @return a hashtable containing all the headers
   */
  public Hashtable getHeaders () {
    return transport.getHeaders();
  }

  /**
   * Return the SOAPContext associated with the response.
   *
   * @return response SOAPContext
   */
  public SOAPContext getResponseSOAPContext () {
      return transport.getResponseSOAPContext();
  }
}
/*
 * End Transport-Hook-Extension
 */
