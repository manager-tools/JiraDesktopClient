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
import java.util.*;
import java.lang.reflect.*;
import javax.activation.*;
import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.internet.*;
import javax.servlet.*;
import javax.xml.parsers.*;
import org.w3c.dom.* ;
import org.apache.soap.*;
import org.apache.soap.rpc.*;
import org.apache.soap.server.http.*;
import org.apache.soap.util.*;
import org.apache.soap.util.xml.*;
import org.apache.soap.util.mime.*;
import org.apache.soap.transport.EnvelopeEditor;
import org.apache.soap.transport.TransportMessage;

/**
 * Any utility stuff for transport type-independent SOAP stuff.
 *
 * @author Sanjiva Weerawarana
 * @author Wouter Cloetens
 */
public class ServerUtils {
    /**
     * Read in stuff from the request stream and return the envelope.
     * Returns null (and sets the error on the response stream) if a
     * transport level thing is wrong and throws a SOAPException if a
     * SOAP level thing is wrong.
     *
     * @return Envelope containing the SOAP envelope found in the request
     *
     * @exception SOAPException if a SOAP level thing goes wrong
     */
    public static Envelope readEnvelopeFromInputStream (DocumentBuilder xdb,
                                                        InputStream is,
                                                        int contentLength,
                                                        String contentType,
                                                        EnvelopeEditor editor,
                                                        SOAPContext ctx)
        throws SOAPException, IOException,
        IllegalArgumentException, MessagingException {
        // Read input stream.
        TransportMessage reqMsg = new TransportMessage(is, contentLength,
                                                       contentType, ctx,
                                                       null);
        // Extract envelope and SOAPContext
        reqMsg.read();
        // Check Content-Type of root part of request to see if it's
        // consistent with a SOAP envelope (text/xml).
        MimeBodyPart rootPart = ctx.getRootPart();
        if (!rootPart.isMimeType(Constants.HEADERVAL_CONTENT_TYPE))
            throw new SOAPException(Constants.FAULT_CODE_PROTOCOL,
                "Unsupported content type \"" +
                rootPart.getContentType() + "\", must be: \"" +
                Constants.HEADERVAL_CONTENT_TYPE + "\".");
        // Apply Transport-Hook-Extension
        reqMsg.editIncoming(editor);
        // Parse into Envelope.
        return reqMsg.unmarshall(xdb);
    }

    public static Provider loadProvider(DeploymentDescriptor dd,
                                        SOAPContext ctxt)
                                          throws SOAPException {
        String  className ;
        Class   c ;
        Object  newObj ;

        className = dd.getServiceClass();
        if (className == null)
            return null;

        if (className.equals("java"))
            className = "org.apache.soap.providers.RPCJavaProvider";
        else if (className.equals("script"))
            className = "org.apache.soap.providers.RPCJavaProvider";

        try {
          c = ctxt.loadClass( className );
          newObj = c.newInstance();
        } catch(Exception exp) {
            throw new SOAPException(Constants.FAULT_CODE_SERVER,
                                    "Can't load provider '" + className + "'",
                                    exp);
        }

        if (!(newObj instanceof Provider))
            throw new SOAPException(Constants.FAULT_CODE_SERVER,
                                    "'" + className + "' isn't a provider");

        return (Provider)newObj;
    }
}
