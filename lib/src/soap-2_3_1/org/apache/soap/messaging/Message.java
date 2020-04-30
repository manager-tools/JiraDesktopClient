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

package org.apache.soap.messaging;

import java.io.*;
import java.util.*;
import java.net.URL;
import javax.xml.parsers.*;
import org.w3c.dom.*;
import org.xml.sax.*;
import org.apache.soap.util.*;
import org.apache.soap.util.xml.*;
import org.apache.soap.rpc.SOAPContext;
import org.apache.soap.rpc.Call;
import org.apache.soap.*;
import org.apache.soap.transport.*;
import org.apache.soap.transport.http.*;
import javax.mail.*;
import javax.mail.internet.*;
import javax.activation.*;

/**
 * A <code>Message</code> is the class whose instances represent
 * one-way messages in SOAP. While messages are one-way, they are
 * sometimes carried on two-way transports such as HTTP. To accomodate
 * that, the API supported here has a "receive" method as well - that
 * is only applicable if the transport that is being used supports
 * the receive() method (@see org.apache.soap.transport.SOAPTransport)
 * so that this API can get at the 'response' envelope.
 *
 * @author Sanjiva Weerawarana (sanjiva@watson.ibm.com)
 */
public class Message {
  SOAPTransport st;
  SOAPContext reqCtx = new SOAPContext (), resCtx = null;
  DocumentBuilder xdb = XMLParserUtils.getXMLDocBuilder();

  public Message () {
  }

  public void setSOAPTransport (SOAPTransport st) {
    this.st = st;
  }
   
  public SOAPTransport getSOAPTransport () {
    return st;
  }

  /**
   * Send an envelope to the given URL via the SOAPTransport that has
   * been configured for this instance (or SOAPHTTPConnection by default).
   * The envelope is sent exactly as-is.
   * 
   * @param url the url to send to
   * @param actionURI the value of the SOAPAction header
   * @param env envelope to send
   *
   * @exception SOAPException if something goes wrong.
   */
  public void send (URL url, String actionURI, Envelope env)
       throws SOAPException {
    // Construct default HTTP transport if not specified.
    if (st == null) {
      st = new SOAPHTTPConnection ();
    }

    // Send request.
    st.send (url, actionURI, null, env, null, reqCtx);
  }

  /**
   * Receive an envelope from the given transport. Only applicable
   * where the transport supports the receive method.
   * If the (root part of) the response does not have text/xml as the
   * Content-Type, null will be returned. If the response is not a
   * SOAP envelope, receive() should be used instead.
   * @return the envelope received
   * @exception SOAPException if something goes wrong
   * @see #receive()
   */
  public Envelope receiveEnvelope () throws SOAPException {
    if (st == null) {
      throw new SOAPException (Constants.FAULT_CODE_CLIENT,
                               "Unable to receive without sending on an " +
                               "appropriate transport.");
    }
    try {
      resCtx = st.getResponseSOAPContext ();
      String payloadStr = Call.getEnvelopeString (st);

      Document doc = 
          xdb.parse(new InputSource(new StringReader(payloadStr)));

      if (doc == null) {
        throw new SOAPException (Constants.FAULT_CODE_CLIENT,
                                 "Parsing error, response was:\n" +
                                  payloadStr);
      }

      return Envelope.unmarshall (doc.getDocumentElement (), resCtx);
    } catch (MessagingException me) {
      throw new SOAPException (Constants.FAULT_CODE_CLIENT, me.getMessage (),
                              me);
    } catch (SAXException ioe) {
      throw new SOAPException (Constants.FAULT_CODE_CLIENT, 
                               "Parsing error, response was:\n" + 
                               ioe.getMessage(), ioe);
    } catch (IOException ioe) {
      throw new SOAPException (Constants.FAULT_CODE_CLIENT, ioe.getMessage (),
                               ioe);
    } catch (IllegalArgumentException e) {
      throw new SOAPException (Constants.FAULT_CODE_CLIENT, e.getMessage (),
                               e);
    }
  }

  /**
   * Receive a response from the given transport. Only applicable
   * where the transport supports the receive method.
   * @return the root part of the response as a DataHandler
   * @exception SOAPException if something goes wrong
   * @see #receive()
   */
  public DataHandler receive() throws SOAPException, MessagingException {
    if (st == null) {
      throw new SOAPException(Constants.FAULT_CODE_CLIENT,
                              "Unable to receive without sending on an " +
                              "appropriate transport.");
    }
    st.receive();
    resCtx = st.getResponseSOAPContext();
    return resCtx.getRootPart().getDataHandler();
  }

  /**
   * Get the request SOAPContext.
   * @return SOAPContext
   */
  public SOAPContext getRequestSOAPContext() {
    return reqCtx;
  }

  /**
   * Add a MIME BodyPart to the request MIME envelope.
   *
   * @param  part  The Part to be appended
   * @exception    MessagingException
   */
  public void addBodyPart(MimeBodyPart part) throws MessagingException {
    reqCtx.addBodyPart(part);
  }

  /**
   * Get the response SOAPContext.
   * @return SOAPContext
   */
  public SOAPContext getResponseSOAPContext() {
    return resCtx;
  }

  /**
   * Find the MIME part referred to by the given URI in the response MIME
   * envelope. This can be:<ul>
   * <li>An absolute URI, in which case a part is located with a
   *     corresponding Content-Location header.
   * <li>A relative URI, in which case an absolute URI is constructed
   *     relative to the Content-Location header of the root SOAP part
   *     or the multipart and the previous rule is then applied.
   * <li>A URI of the format "cid:xxx"> in which case a part is located
   *     with a matching Content-ID header.
   * </ul>Returns null if the part is not found.
   * <p>Note: relative URIs not entirely implemented yet.
   *
   * @param  uri      the URI
   * @return          the Part or null if not found
   */
  public MimeBodyPart findBodyPart(String uri) {
    if (resCtx == null)
      return null;
    else
      return resCtx.findBodyPart(uri);
  }

  /**
   * Return the number of enclosed BodyPart objects in the response MIME
   * envelope.
   *
   * @return          number of parts
   * @exception       MessagingException
   */
  public int getPartCount() throws MessagingException {
    if (resCtx == null)
      return 0;
    else
      return resCtx.getCount();
  }

  /**
   * Get the specified Part in the reponse MIME envelope by its index.
   * Parts are numbered starting at 0.
   *
   * @param index     the index of the desired Part
   * @return          the Part
   * @exception       IndexOutOfBoundsException if no such Part exists
   */
  public MimeBodyPart getBodyPart(int index) throws IndexOutOfBoundsException {
    if (resCtx == null)
      return null;
    else
      return resCtx.getBodyPart(index);
  }

  /**
   * Get the root Part of the reponse MIME envelope, or the only part if the
   * response wasn't multipart.
   *
   * @return          the Part
   */
  public MimeBodyPart getRootPart() throws MessagingException {
    if (resCtx == null)
      return null;
    else
      return resCtx.getRootPart();
  }
}
