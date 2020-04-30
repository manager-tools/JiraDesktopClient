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

import java.io.*;
import java.net.*;
import java.util.*;

import com.ibm.network.mail.base.*;
import com.ibm.network.mail.smtp.event.*;
import org.apache.soap.util.net.*;
import org.apache.soap.util.*;
import org.apache.soap.rpc.SOAPContext;
import org.apache.soap.Constants;
import org.apache.soap.transport.*;
import org.apache.soap.transport.smtp.*;
import javax.mail.MessagingException;

/**
 * This class can be used as a bridge to relay SOAP messages received via
 * email to an HTTP SOAP listener. This is basically a polling POP3 client
 * that keeps looking for new messages to work on. When it gets one,
 * it forwards it to a SOAP HTTP listener and forwards the response via
 * SMTP to the original requestor (to either the ReplyTo: or From: address).
 *
 * @author Sanjiva Weerawarana (sanjiva@watson.ibm.com)
 */
public class SMTP2HTTPBridge implements Runnable {
  Object waitObject = new Object ();
  long pollDelayMillis;
  com.ibm.network.mail.pop3.protocol.CoreProtocolBean pop3 =
    new com.ibm.network.mail.pop3.protocol.CoreProtocolBean ();
  com.ibm.network.mail.smtp.protocol.CoreProtocolBean smtp =
    new com.ibm.network.mail.smtp.protocol.CoreProtocolBean ();
  URL httpURL;

  /**
   * Constructor: creates a bridge. Call run() to start polling using
   * the current thread. (If you want to use a separate thread then
   * do new Thread (new SMT2HTTPBridge (...)).start ();
   *
   * @param pollDelayMillis number of milli-seconds to sleep b'ween polls
   * @param popServer hostname of POP3 server
   * @param popLoginName login ID
   * @param password password for login
   * @param smtpServer hostname of SMTP server
   */
  public SMTP2HTTPBridge (/* POP3 params */
                          long pollDelayMillis,
                          String popServer, String popLoginName,
                          String password,
                          /* HTTP params */
                          URL httpURL,
                          /* SMTP params */
                          String smtpServer) {
    /* set up the pop3 side */
    this.pollDelayMillis = pollDelayMillis;
    pop3.setPOP3ServerHost (popServer);
    pop3.setUserOptions(/* popLoginName */ popLoginName,
      /* password */ password,
      /* leaveMessagesOnServer */ false,
      /* rememberPassword */ false);
    pop3.addMessageListener (
      new com.ibm.network.mail.pop3.event.MessageListener () {
        public void messageReceived (
            com.ibm.network.mail.pop3.event.MessageEvent me) {
          receiveMessage (me.getMessage ());
        }
      }
    );
    pop3.addStatusListener (new POP3StatusListener ());

    /* set up the http side */
    this.httpURL = httpURL;

    /* set up the smtp side */
    smtp.setSmtpServerHost (smtpServer);
    smtp.addStatusListener (new StatusListener () {
      public void operationComplete (StatusEvent e) {
        System.err.println ("DONE: " + e.getStatusString ());
        synchronized (waitObject) {
          waitObject.notify ();
        }
      }

      public void processStatus (StatusEvent e) {
        System.err.println ("Status update: " + e.getStatusString ());
      }
    });
  }

  /**
   * Poll for messages forever.
   */
  public void run () {
    while (true) {
      if (pop3.isReady ()) {
        System.err.println ("SMTP2HTTPBridge: Polling for messages ..");
        pop3.receiveMessage ();
      }
      try {
        Thread.sleep (pollDelayMillis);
      } catch (Exception e) {
        e.printStackTrace ();
      }
    }
  }

  /**
   * This is called by the POP3 message listener after receiving a 
   * message from the pop server.
   * 
   * @param msg the message that was received
   */
  void receiveMessage (MimeMessage msg) {
    /* extract the stuff from the SMTP message received */
    String subject = msg.getHeader (SMTPConstants.SMTP_HEADER_SUBJECT);
    String actionURI = msg.getHeader (Constants.HEADER_SOAP_ACTION);
    String toAddr = msg.getHeader (SMTPConstants.SMTP_HEADER_TO);
    String fromAddr = msg.getHeader (SMTPConstants.SMTP_HEADER_FROM);
    MimeBodyPart mbp = (MimeBodyPart) msg.getContent ();
    byte[] ba = (byte[]) mbp.getContent ();

    /* forward the content to the HTTP listener as an HTTP POST */
    Hashtable headers = new Hashtable ();
    headers.put (Constants.HEADER_SOAP_ACTION, actionURI);
    TransportMessage response;
    // This is the reponse SOAPContext. Sending it as a reply not supported yet.
    SOAPContext ctx;
    try
    {
        // Note: no support for multipart MIME request yet here...
        TransportMessage tmsg = new TransportMessage(new String (ba),
                                                     new SOAPContext(),
                                                     headers);
        tmsg.save();

        response = HTTPUtils.post (httpURL, tmsg,
                                   30000, null, 0);
        ctx = response.getSOAPContext();
    } catch (Exception e) {
        e.printStackTrace();
	return;
    }
    System.err.println ("HTTP RESPONSE IS: ");
    String payload = null;
    try {
      // read the stream
        payload = IOUtils.getStringFromReader (response.getEnvelopeReader());
    } catch (Exception e) {
      e.printStackTrace ();
    }
    System.err.println ("'" + payload + "'");

    /* forward the response via SMTP to the original sender */
    MimeBodyPart mbpResponse = new MimeBodyPart ();
    mbpResponse.setContent (new String (payload), response.getContentType());
    mbpResponse.setEncoding (MimePart.SEVENBIT);
    mbpResponse.setDisposition (MimePart.INLINE);

    MimeMessage msgResponse = new MimeMessage();
    msgResponse.setContent (mbpResponse, "");
    msgResponse.addHeader (SMTPConstants.SMTP_HEADER_SUBJECT,
                           "Re: " + subject);
    msgResponse.addHeader (Constants.HEADER_SOAP_ACTION, actionURI);
    msgResponse.addHeader (SMTPConstants.SMTP_HEADER_FROM, 
                           SMTPUtils.getAddressFromAddressHeader (toAddr));
    String sendTo = SMTPUtils.getAddressFromAddressHeader (fromAddr);
    msgResponse.addRecipients (MimeMessage.TO, 
                               new InternetAddress[] { 
                                 new InternetAddress (sendTo)
                               });
    smtp.sendMessage (msgResponse);
  }

  public static void main (String[] args) throws Exception {
    if (args.length != 6) {
      System.err.println ("Usage: java " + SMTP2HTTPBridge.class.getName () +
                          " polldelay \\");
      System.err.println ("\t\t pop3host pop3login pop3passwd httpurl " +
                          "smtphostname");
      System.err.println ("  where:");
      System.err.println ("    polldelay    number of millisec to " +
                          "sleep between polls");
      System.err.println ("    pop3host     hostname of the POP3 server");
      System.err.println ("    pop3login    login ID of POP3 account");
      System.err.println ("    pop3passwd   POP3 account password");
      System.err.println ("    routerURL    http URL of SOAP router to " +
                          "bridge to");
      System.err.println ("    smtphostname SMTP server host name");
      System.exit (1);
    }
    SMTP2HTTPBridge sb = new SMTP2HTTPBridge (/* POP3 params */
                                              Integer.parseInt (args[0]),
                                              args[1],
                                              args[2],
                                              args[3],
                                              /* HTTP params */
                                              new URL (args[4]),
                                              /* SMTP params */
                                              args[5]);
    new Thread (sb).start ();
  }
}
