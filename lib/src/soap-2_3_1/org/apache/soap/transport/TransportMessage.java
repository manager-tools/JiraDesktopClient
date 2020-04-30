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

package org.apache.soap.transport;

import java.io.*;
import java.util.*;
import javax.activation.*;
import javax.mail.*;
import javax.mail.internet.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;
import org.xml.sax.*;
import org.apache.soap.*;
import org.apache.soap.encoding.*;
import org.apache.soap.rpc.*;
import org.apache.soap.util.*;
import org.apache.soap.util.xml.*;
import org.apache.soap.util.mime.*;
import org.apache.soap.transport.EnvelopeEditor;

/**
 * Transport type-independent encapsulation of SOAP message content.
 *
 * @author Wouter Cloetens
 */
public class TransportMessage implements Serializable {
    protected String contentType = null;
    protected int offset = 0;
    protected byte[] bytes = null;
    protected String envelope = null;
    protected Hashtable headers = null;
    protected SOAPContext ctx = null;

    /**
     * No-argument constructor.
     */
    public TransportMessage() {
    }

    /**
     * Create a message from an already built envelope and/or
     * SOAPContext. The envelope argument may be null.
     * Call save() to generate the byte array.
     */
    public TransportMessage(String envelope, SOAPContext ctx,
                            Hashtable headers)
        throws IllegalArgumentException, MessagingException,
               IOException, SOAPException {
        this.envelope = envelope;
        this.ctx = ctx;
        if (headers != null)
            this.headers = headers;
        else
            this.headers = new Hashtable();
    }

    /**
     * Create a message from an InputStream. This reads the InputStream and
     * stores it in a byte array.
     * Call read() to extract the SOAPContext and SOAP
     * envelope from the byte array.
     */
    public TransportMessage(InputStream is, int contentLength,
                            String contentType, SOAPContext ctx,
                            Hashtable headers)
        throws IllegalArgumentException, MessagingException,
               IOException, SOAPException {
        if (headers != null)
            this.headers = headers;
        else
            this.headers = new Hashtable();
        this.ctx = ctx;
        this.contentType = contentType;

        if (contentLength < 0)
            throw new SOAPException (Constants.FAULT_CODE_PROTOCOL,
                                     "Content length must be specified.");

        bytes = new byte[contentLength];
        int offset = 0;
        int bytesRead = 0;

        // We're done reading when we get all the content OR when the stream
        // returns a -1.
        while ((offset < contentLength) && (bytesRead >= 0)) {
            bytesRead = is.read(bytes, offset, contentLength - offset);
            offset += bytesRead;
        }
        if (offset < contentLength)
            throw new SOAPException (Constants.FAULT_CODE_PROTOCOL,
                       "Premature end of stream. Data is truncated. Read "
                       + offset + " bytes successfully, expected "
                       + contentLength);
    }

    /**
     * Apply envelope/root part editor on inbound message.
     * Nothing will be done if the root part is not text.
     * If it is, the Content-Type of the root part is preserved.
     */
    public void editIncoming(EnvelopeEditor editor)
        throws SOAPException, IOException, MessagingException {
        editEnvelope(editor, true);
    }

    /**
     * Apply envelope/root part editor on outgoing message.
     * Nothing will be done if the root part is not text.
     * If it is, the Content-Type of the root part is preserved.
     */
    public void editOutgoing(EnvelopeEditor editor)
        throws SOAPException, IOException, MessagingException {
        editEnvelope(editor, false);
    }

    /*
     * Implementation of envelope editor.
     */
    protected void editEnvelope(EnvelopeEditor editor, boolean isIncoming)
        throws SOAPException, IOException, MessagingException {
        if (editor != null) {
            // Do nothing if the root part is not text.
            if (getEnvelope() == null)
                return;
            // Apply filter.
            StringWriter tout = new StringWriter();
            if (isIncoming)
                editor.editIncoming(getEnvelopeReader(), tout);
            else
                editor.editOutgoing(getEnvelopeReader(), tout);
            tout.flush();
            envelope = tout.toString();
        }
    }

    /**
     * Interpret byte array and extract SOAPContext and SOAP envelope (as
     * a String). Make sure content type is set before calling this.
     * Note that in the Messaging scenario, the root type is not necessarily
     * a SOAP envelope, or even text. If it is text, the text is returned and
     * it is up to the invoker to check the root part's Content-Type
     */
    public String read()
        throws IllegalArgumentException, MessagingException,
        IOException, SOAPException {

        // Parse and validate content type.
        ContentType cType = null;
        try {
            // Hack since WebSphere puts ;; instead of just ;
            int pos = contentType.indexOf( ";;" );
            if ( pos != -1 )
              contentType = contentType.substring(0,pos) +
                            contentType.substring(pos+1) ;
            cType = new ContentType(contentType);
        } catch(ParseException pe) {
        }
        if (cType == null)
            throw new SOAPException(Constants.FAULT_CODE_PROTOCOL,
                                    "Missing content type.");
        MimeBodyPart rootPart;
        ContentType rootContentType;
        byte[] rootBytes;
        if (cType.match(Constants.HEADERVAL_CONTENT_TYPE_MULTIPART_PRIMARY +
                        "/*")) {
            // Parse multipart request.
            ByteArrayDataSource ds = new ByteArrayDataSource(bytes,
                                                             contentType);

            // Parse multipart mime into parts.
            ctx.readMultipart(ds);

            // Find root part.
            rootPart = ctx.getRootPart();
            rootContentType = new ContentType(rootPart.getContentType());
            ByteArrayDataSource bads = new ByteArrayDataSource(
                rootPart.getInputStream(), null);
            rootBytes = bads.toByteArray();
        } else {
            rootBytes = bytes;
            rootContentType = cType;
            // Set the single part as the root part of SOAPContext.
            ByteArrayDataSource ds = new ByteArrayDataSource(bytes,
                                                             contentType);
            DataHandler dh = new DataHandler(ds);
            rootPart = new MimeBodyPart();
            rootPart.setDataHandler(dh);
            ctx.addBodyPart(rootPart);
        }

        // If the root part is text, extract it as a String.
        // Note that we could use JAF's help to do this (see getEnvelope())
        // but implementing it ourselves is safer and faster.
        if (rootContentType.match("text/*")) {
            String charset = rootContentType.getParameter("charset");
            // Hmm, risky, the default charset is transport-specific...
            if (charset == null || charset.equals(""))
                charset = Constants.HEADERVAL_DEFAULT_CHARSET;
            envelope = new String(rootBytes, MimeUtility.javaCharset(charset));
        }

        return envelope;
    }

    /**
     * Parse envelope.
     */
    public Envelope unmarshall(DocumentBuilder xdb)
        throws SOAPException {
        Document doc;
        try {
            doc = xdb.parse(new InputSource(getEnvelopeReader()));
        } catch(Exception e) {
            throw new SOAPException(Constants.FAULT_CODE_CLIENT,
                                    "parsing error: " + e);
        }
        if (doc == null) {
            throw new SOAPException(Constants.FAULT_CODE_CLIENT,
                                    "parsing error: received empty document");
        }

        return Envelope.unmarshall(doc.getDocumentElement());
    }

    /**
     * Write message to byte array. Override this method for
     * transport types that encode in a non-standard way.
     */
    public void save()
        throws IllegalArgumentException, MessagingException, IOException {
        /* If an envelope was provided as a string, set it as the root part.
         * Otherwise, assume that the SOAPContext already has a root part.
         * If there was already a root part, preserve its content-type.
         */
        String rootContentType = null;
        if (ctx.isRootPartSet()) {
            MimeBodyPart rootPart = ctx.getRootPart();
            if (rootPart != null)
                // Note: don't call MimeBodyPart.getContent() because it will
                // default to "text/plain" if the Content-Type header isn't
                // set.
                rootContentType = rootPart.getHeader(
                    Constants.HEADER_CONTENT_TYPE, null);
        }
        if (rootContentType == null)
            rootContentType = Constants.HEADERVAL_CONTENT_TYPE_UTF8;
        if (getEnvelope() != null)
            ctx.setRootPart(envelope, rootContentType);

        // Print the whole response to a byte array.
        ByteArrayOutputStream payload =
            new ByteArrayOutputStream();
        ctx.writeTo(payload);
        bytes = payload.toByteArray();

        // Now strip off the headers. (Grmbl, get rid of JavaMail
        // for MIME support). Just intercept the Content-Type
        // header. We don't want any of the MIME headers, and we know the
        // overall Content-Length anyway.
        StringBuffer namebuf = new StringBuffer();
        StringBuffer valuebuf = new StringBuffer();
        boolean parsingName = true;
        for (offset = 0; offset < bytes.length; offset++) {
            if (bytes[offset] == '\n') {
                // JavaMail sometimes word-wraps header parameters to the
                // next line. Throttle through that.
                if ((bytes[offset + 1] == ' ')
                    || (bytes[offset + 1] == '\t')) {
                    while ((bytes[(++offset) + 1] == ' ')
                           || (bytes[offset + 1] == '\t'));
                    continue;
                }
                if (namebuf.length() == 0) {
                    offset++;
                    break;
                }
                String name = namebuf.toString();
                // For multipart, append type and start parameters to
                // Content-Type header.
                if (name.equals(Constants.HEADER_CONTENT_TYPE)) {
                    contentType = valuebuf.toString();
                    if (ctx.getCount() > 1) {
                        String rootCID = ctx.getRootPart().getContentID();
                        // Strip < and > off Content-ID.
                        rootCID = rootCID.substring(1, rootCID.length() - 1);
                        contentType += "; type=\""
                            + Constants.HEADERVAL_CONTENT_TYPE
                            + "\"; start=\"" + rootCID + '"';
                    }
                }
                namebuf = new StringBuffer();
                valuebuf = new StringBuffer();
                parsingName = true;
                }
            else if (bytes[offset] != '\r') {
                if (parsingName) {
                    if (bytes[offset] == ':') {
                        parsingName = false;
                        offset++;
                    }
                    else
                        namebuf.append((char)bytes[offset]);
                }
                else
                        valuebuf.append((char)bytes[offset]);
            }
        }
    }

    /**
     * Get SOAPContext.
     */
    public SOAPContext getSOAPContext() {
        return ctx;
    }

    /**
     * Get SOAP Envelope/root part as a String.
     * This method will extract the root part from the SOAPContext as a String
     * if there is no SOAP Envelope.
     */
    public String getEnvelope() throws MessagingException, IOException {
        if (envelope == null) {
            MimeBodyPart rootPart = ctx.getRootPart();
            if (rootPart != null)
                if (rootPart.isMimeType("text/*")) {
                    ByteArrayDataSource ds = new ByteArrayDataSource(
                        rootPart.getInputStream(), rootPart.getContentType());
                    envelope = ds.getText();
            }
        }
        return envelope;
    }

    /**
     * Get SOAP Envelope/root part as a Reader. Returns null if the root part
     * is not text.
     */
    public Reader getEnvelopeReader() throws MessagingException, IOException {
        if (getEnvelope() == null)
            return null;
        else
            return new StringReader(envelope);
    }

    /**
     * Set SOAP Envelope.
     */
    public void setEnvelope(String envelope) {
        this.envelope = envelope;
    }

    /**
     * Get Content-Type.
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * Set Content-Type as String.
     */
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    /**
     * Get size of response content in bytes.
     */
    public int getContentLength() {
        return bytes.length - offset;
    }

    /**
     * Set a transport header.
     */
    public void setHeader(String name, String value) {
        headers.put(name, value);
    }

    /**
     * Get a transport header.
     */
    public String getHeader(String name) {
        return (String)headers.get(name);
    }

    /**
     * Get transport header names.
     */
    public Enumeration getHeaderNames() {
        return headers.keys();
    }

    /**
     * Get the complete header hashtable.
     */
    public Hashtable getHeaders() {
        return headers;
    }

    /**
     * Write content.
     */
    public void writeTo(OutputStream outStream) throws IOException {
        outStream.write(bytes, offset, bytes.length - offset);
        outStream.flush();
    }

    /**
     * Set the byte array of the response.
     */
    public void setBytes(byte[] data) {
        offset = 0;
        bytes = data;
    }

    /**
     * Set the byte array of the response.
     */
    public void readFully(InputStream is) throws IOException {
        offset = 0;
        ByteArrayDataSource bads = new ByteArrayDataSource(is, null);
        bytes = bads.toByteArray();
    }

    /**
     * Get the response byte array.
     */
    public byte[] getBytes() {
        // The offset trick is an efficiency hack. Eliminate that here.
        if (offset != 0) {
            byte[] data = new byte[bytes.length - offset];
            System.arraycopy(bytes, offset, data, 0, data.length);
            bytes = data;
            offset = 0;
        }
        return bytes;
    }
}
