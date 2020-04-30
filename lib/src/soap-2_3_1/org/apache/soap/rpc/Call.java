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

package org.apache.soap.rpc;

import java.io.*;
import java.net.*;
import java.util.*;
import java.lang.reflect.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;
import org.xml.sax.*;
import org.apache.soap.util.xml.*;
import org.apache.soap.util.IOUtils;
import org.apache.soap.*;
import org.apache.soap.encoding.*;
import org.apache.soap.transport.*;
import org.apache.soap.transport.http.*;
import org.apache.soap.server.*;
import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;

/**
 * A <code>Call</code> object represents an <em>RPC</em> call. Both the
 * client and the server use <code>Call</code> objects to invoke the
 * method.
 *
 * @author Matthew J. Duftler (duftler@us.ibm.com)
 * @author Sanjiva Weerawarana (sanjiva@watson.ibm.com)
 */
public class Call extends RPCMessage
{
  private DocumentBuilder     xdb = XMLParserUtils.getXMLDocBuilder();
  private SOAPMappingRegistry smr = null;
  private SOAPTransport       st  = null;;
  private int                 to  = 0;

  public Call()
  {
    this(null, null, null, null, null);
  }

  public Call(String targetObjectURI, String methodName, Vector params,
              Header header, String encodingStyleURI)
  {
    this(targetObjectURI, methodName, params, header, encodingStyleURI,
         new SOAPContext());
  }

  public Call(String targetObjectURI, String methodName, Vector params,
              Header header, String encodingStyleURI, SOAPContext ctx)
  {
    super(targetObjectURI, methodName, params, header, encodingStyleURI, ctx);
  }

  public void setSOAPMappingRegistry(SOAPMappingRegistry smr)
  {
    this.smr = smr;
  }

  public SOAPMappingRegistry getSOAPMappingRegistry()
  {
    // if the smr hasn't been created yet, do it now
    if (smr == null)
    {
      smr = new SOAPMappingRegistry();
    }

    return smr;
  }

  public void setSOAPTransport(SOAPTransport st)
  {
    this.st = st;
  }
   
  public SOAPTransport getSOAPTransport()
  {
    return st;
  }

  /**
   * Set timeout in our MessageContext.
   * 
   * @param value the maximum amount of time, in milliseconds
   */
  public void setTimeout (int value) {
    to = value;
  }
    
  /**
   * Get timeout from our MessageContext.
   * 
   * @return value the maximum amount of time, in milliseconds
   */
  public int getTimeout () {
    return to;
  }
    
  /**
   * Add a MIME BodyPart.
   *
   * @param  part  The Part to be appended
   * @exception    MessagingException
   */
  public void addBodyPart(MimeBodyPart part) throws MessagingException
  {
    ctx.addBodyPart(part);
  }

  /**
   * Remove a MIME BodyPart.
   */
  public void removeBodyPart(MimeBodyPart part) throws MessagingException
  {
    ctx.removeBodyPart(part);
  }

  public Envelope buildEnvelope()
  {
    return super.buildEnvelope(false);
  }

  public static Call extractFromEnvelope(Envelope env, ServiceManager svcMgr,
                                         SOAPContext ctx)
    throws IllegalArgumentException
  {
    return (Call)RPCMessage.extractFromEnvelope(env, svcMgr, false, null, ctx);
  }

  /**
   * Check if response root part is text/xml and return it as a String.
   * Temporarily placing this method here - I think it will be moved to
   * SOAPContext after a redesign of SOAPTransport interaction.
   *
   * @author Wouter Cloetens <wcloeten@raleigh.ibm.com>
   */
  public static String getEnvelopeString(SOAPTransport st)
    throws SOAPException, MessagingException, IOException {
    SOAPContext respCtx = st.getResponseSOAPContext();
    BufferedReader in = null;
    String payloadStr = null;

    MimeBodyPart rootPart = respCtx.getRootPart();
    if (rootPart.isMimeType("text/*")) {
      // Get the input stream to read the response envelope from.
      in = st.receive();
      payloadStr = IOUtils.getStringFromReader(in);
    }

    // Check Content-Type of root part of response to see if it's
    // consistent with a SOAP envelope (text/xml).
    if (!rootPart.isMimeType(Constants.HEADERVAL_CONTENT_TYPE)) {
      throw new SOAPException(Constants.FAULT_CODE_PROTOCOL,
        "Unsupported response content type \"" +
        rootPart.getContentType() + "\", must be: \"" +
        Constants.HEADERVAL_CONTENT_TYPE + "\"." +
        (payloadStr == null ? "" : " Response was:\n" + payloadStr));
    }

    return payloadStr;
  }

  /**
   * Invoke this call at the specified URL. Valid only on the client side.
   */
  public Response invoke(URL url, String SOAPActionURI) throws SOAPException
  {
    if (SOAPActionURI == null)
    {
      SOAPActionURI = "";
    }

    // if the smr hasn't been created yet, do it now
    if (smr == null)
    {
      smr = new SOAPMappingRegistry();
    }

    try
    {
      // Build an envelope containing the call.
      Envelope callEnv = buildEnvelope();

      // Construct default HTTP transport if not specified.
      if (st == null)
        st = new SOAPHTTPConnection();

      // set the timeout
      if (to != 0 && st instanceof SOAPHTTPConnection) 
        ((SOAPHTTPConnection)st).setTimeout(to);

      // Post the call envelope.
      st.send(url, SOAPActionURI, null, callEnv, smr, ctx);

      // Get the response context.
      SOAPContext respCtx = st.getResponseSOAPContext();

      // Pre-read the response root part as a String to be able to
      // log it in an exception if parsing fails.
      String payloadStr = getEnvelopeString(st);

      // Parse the incoming response stream.
      Document respDoc =
        xdb.parse(new InputSource(new StringReader(payloadStr)));
      Element payload = null;

      if (respDoc != null)
      {
        payload = respDoc.getDocumentElement();
      }
      else  //probably does not happen
      {
        throw new SOAPException (Constants.FAULT_CODE_CLIENT,
          "Parsing error, response was:\n" + payloadStr);
      }

      // Unmarshall the response envelope.
      Envelope respEnv = Envelope.unmarshall(payload, respCtx);

      // Extract the response from the response envelope.
      Response resp = Response.extractFromEnvelope(respEnv, smr, respCtx);

      // Reset the targetObjectURI in case it was changed by the server
      String fullTargetObjectURI = resp.getFullTargetObjectURI();

      if (fullTargetObjectURI != null)
      {
        setTargetObjectURI(fullTargetObjectURI);
      }

      return resp;
    }
    catch (MessagingException me)
    {
      throw new SOAPException(Constants.FAULT_CODE_CLIENT, me.getMessage(), me);
    }
    catch (IllegalArgumentException e)
    {
      throw new SOAPException(Constants.FAULT_CODE_CLIENT, e.getMessage(), e);
    }
    catch (SAXException e)
    {
      throw new SOAPException(Constants.FAULT_CODE_CLIENT,
                              "Parsing error, response was:\n" +e.getMessage(),
			      e);
    }
    catch (IOException e)
    {
      throw new SOAPException(Constants.FAULT_CODE_PROTOCOL,
                              e.getMessage(), e);
    }
  }
}
