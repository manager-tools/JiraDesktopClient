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

package org.apache.soap.transport.smtp;

import java.io.*;
import java.net.*;
import java.util.*;
import org.w3c.dom.*;
import org.apache.soap.util.*;
import org.apache.soap.util.xml.*;
import org.apache.soap.Envelope;
import org.apache.soap.SOAPException;
import org.apache.soap.encoding.*;
import org.apache.soap.transport.*;
import org.apache.soap.rpc.*;

import com.ibm.network.mail.base.*;

/**
 * <code>SOAPSMTPConnection</code> is an implementation of the
 * <code>SOAPTransport</code> interface for <em>SMTP</em>.
 *
 * @author Sanjiva Weerawarana (sanjiva@watson.ibm.com)
 */
public class SOAPSMTPConnection implements SOAPTransport {
  private String fromAddr;
  private String subject;
  private Object waitObject = new Object ();
  private long popPollDelay;
  private boolean responseReceived = false;
  private Hashtable responseHeaders = new Hashtable ();
  private BufferedReader responseReader = null;
  com.ibm.network.mail.smtp.protocol.CoreProtocolBean smtpBean =
    new com.ibm.network.mail.smtp.protocol.CoreProtocolBean ();
  com.ibm.network.mail.pop3.protocol.CoreProtocolBean pop3Bean =
    new com.ibm.network.mail.pop3.protocol.CoreProtocolBean ();
  private SOAPContext responseSOAPContext;

  /**
   * Constructor: sets up this SMTP connection for sending and 
   * receiving a SOAP envelope via SMTP and POP3, respectively.
   *
   * @param fromAddr the sender's address
   * @param subject subject of the email message
   * @param smtpServer the SMTP server to use to send
   * @param popPollDelay the number of millis to sleep between response polls
   * @param popServer hostname of POP3 server to receive from
   * @param popLogin login ID to receive mail at
   * @param popPassword password for login
   */
  public SOAPSMTPConnection (String fromAddr, String subject, 
                             String smtpServer, long popPollDelay,
                             String popServer, String popLogin,
                             String popPassword) {
    this.fromAddr = fromAddr;
    this.subject = subject;
    this.popPollDelay = popPollDelay;

    // set up send side
    smtpBean.setSmtpServerHost (smtpServer);
    smtpBean.addStatusListener (
      new com.ibm.network.mail.smtp.event.StatusListener () {
        public void operationComplete (
            com.ibm.network.mail.smtp.event.StatusEvent e) {
          System.err.println ("DONE: " + e.getStatusString ());
          synchronized (waitObject) {
            waitObject.notify ();
          }
        }
        
        public void processStatus (
            com.ibm.network.mail.smtp.event.StatusEvent e) {
          System.err.println ("Status update: " + e.getStatusString ());
        }
      }
    );

    // set up receive side
    responseReader = null;
    pop3Bean.setPOP3ServerHost (popServer);
    pop3Bean.setUserOptions(/* popLoginName */ popLogin,
                            /* password */ popPassword,
                            /* leaveMessagesOnServer */ false,
                            /* rememberPassword */ false);
    pop3Bean.addMessageListener (
      new com.ibm.network.mail.pop3.event.MessageListener () {
        public void messageReceived (
            com.ibm.network.mail.pop3.event.MessageEvent me) {
          /* extract the stuff from the POP message received */
          MimeMessage msg = me.getMessage ();
          String subj = msg.getHeader (SMTPConstants.SMTP_HEADER_SUBJECT);
          if (subj != null) {
            responseHeaders.put (SMTPConstants.SMTP_HEADER_SUBJECT, subj);
          }
          String soapAction = 
            msg.getHeader (org.apache.soap.Constants.HEADER_SOAP_ACTION);
          if (soapAction != null) {
            responseHeaders.put (org.apache.soap.Constants.HEADER_SOAP_ACTION,
                                 soapAction);
          }
          String to = msg.getHeader (SMTPConstants.SMTP_HEADER_TO);
          if (to != null) {
            responseHeaders.put (SMTPConstants.SMTP_HEADER_TO, to);
          }
          String from = msg.getHeader (SMTPConstants.SMTP_HEADER_FROM);
          if (from != null) {
            responseHeaders.put (SMTPConstants.SMTP_HEADER_FROM, from);
          }
          MimeBodyPart mbp = (MimeBodyPart) msg.getContent ();
          byte[] ba = (byte[]) mbp.getContent ();
          String baStr = new String (ba);
          try {
              responseSOAPContext = new SOAPContext();
              responseSOAPContext.setRootPart(baStr, "text/xml");
          } catch(Exception ioe) {
              ioe.printStackTrace();
          }
          responseReader = new BufferedReader (new StringReader (baStr));
          responseReceived = true;
        }
      }
    );
    pop3Bean.addStatusListener (new POP3StatusListener ());
  }

  /**
   * This method is used to request that an envelope be sent.
   * Note: sending SOAPContext not implemented yet.
   *
   * @param sendTo the URL to send the envelope to ("mailto:xxx@yyy")
   * @param action the SOAPAction header field value
   * @param headers any other header fields to go to as protocol headers
   *        [IGNORED right now]
   * @param env the envelope to send
   * @param smr the XML<->Java type mapping registry (passed on)
   * @param ctx the request SOAPContext
   *
   * @exception SOAPException with appropriate reason code if problem
   */
  public void send (URL toAddr, String actionURI, Hashtable headers,
                    Envelope env,
                    SOAPMappingRegistry smr, SOAPContext ctx)
    throws SOAPException {
    MimeMessage msg = new MimeMessage();

    MimeBodyPart mbp = new MimeBodyPart ();
    StringWriter sw = new StringWriter();
    try {
      env.marshall (sw, smr);
    } catch (IOException e) {
      e.printStackTrace ();
    }
    mbp.setContent (sw.toString (), "text/xml");
    mbp.setEncoding (MimePart.SEVENBIT);
    mbp.setDisposition (MimePart.INLINE);
    msg.setContent (mbp, "");

    msg.addHeader ("Subject", subject);
    msg.addHeader ("SOAPAction", actionURI);
    msg.addHeader ("From", fromAddr);
    msg.addRecipients (MimeMessage.TO, 
                       new InternetAddress[] {
                         new InternetAddress (toAddr.getFile ())
                       });

    smtpBean.sendMessage (msg);

    try {
      synchronized (waitObject) {
        waitObject.wait ();
      }
    } catch (Exception e) {
      e.printStackTrace ();
    }
  }

  /**
   * Return a buffered reader to receive back the response to whatever
   * was sent to whatever. This does a blocking wait until a response
   * is received .. don't do it lightly. Should take into account
   * timeouts and such in the future.
   *
   * @return a reader to read the results from or null if that's not
   *         possible.
   */
  public synchronized BufferedReader receive () {
    if (responseReader == null) {
      responseReceived = false;
      while (!responseReceived) {
        try {
          if (pop3Bean.isReady ()) {
            System.err.println ("SOAPSMTPConnection: Polling for response ..");
            pop3Bean.receiveMessage ();
            if (responseReceived) {
              break;
            }
          }
          Thread.sleep (popPollDelay);
        } catch (Exception e) {
          e.printStackTrace ();
        }
      }
    }
    return responseReader;
  }

  /**
   * Return access to headers generated by the protocol.
   * 
   * @return a hashtable containing all the headers
   */
  public synchronized Hashtable getHeaders () {
    if (responseReader == null)
      receive();
    return responseHeaders;
  }

  /**
   * Return the SOAPContext associated with the response.
   * Not implemented yet!
   *
   * @return response SOAPContext
   */
  public synchronized SOAPContext getResponseSOAPContext () {
    if (responseReader == null)
      receive();
      return responseSOAPContext;
  }
}
