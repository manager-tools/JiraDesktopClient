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

package org.apache.soap.encoding.soapenc;

import java.beans.*;
import java.io.*;
import java.util.*;
import java.lang.reflect.*;
import org.w3c.dom.*;
import org.apache.soap.util.*;
import org.apache.soap.util.xml.*;
import org.apache.soap.util.mime.*;
import org.apache.soap.*;
import org.apache.soap.rpc.*;
import javax.activation.*;
import javax.mail.*;
import javax.mail.internet.*;

/**
 * A <code>MimePartSerializer</code> can be used to serialize
 * Java's InputStream, JavaMail's MimeBodyPart and
 * Java Activation Framework's DataSource and DataHandler objects
 * from/to multipart Mime attachments to the SOAP message.<p>
 * Inside the SOAP message body, the reference looks like:<br>
 * <code>&lt;elemName href="cid:foo"&gt;</code><br>
 * where "foo" is the URLEncoded name of the Content-ID of the mime part,
 * or alternatively, an absolute or relative URI referring to the
 * Content-Location of the part.<p>
 * The class always deserializes to a DataHandler, which provides
 * an InputStream, a DataSource with a Content-Type, the content
 * as an object, and allows to write the data to an OutputStream.
 * 
 * @author Wouter Cloetens (wcloeten@raleigh.ibm.com)
 */
public class MimePartSerializer implements Serializer, Deserializer {
    public void marshall(String inScopeEncStyle, Class javaType, Object src,
                       Object context, Writer sink, NSStack nsStack,
                       XMLJavaMappingRegistry xjmr, SOAPContext ctx)
        throws IllegalArgumentException, IOException {
        nsStack.pushScope();

        if ((src != null) &&
            !(src instanceof InputStream) &&
            !(src instanceof DataSource) &&
            !(src instanceof MimeBodyPart) &&
            !(src instanceof DataHandler))
            throw new IllegalArgumentException("Tried to pass a '" +
                 src.getClass().toString() + "' to MimePartSerializer");

        if (src == null) {
            SoapEncUtils.generateNullStructure(inScopeEncStyle, Object.class,
                                               null, sink, nsStack, xjmr);
        } else {
            // get a MimeBodyPart out of the various possible input types
            DataSource ds = null;
            DataHandler dh = null;
            MimeBodyPart bp = null;
            if (src instanceof InputStream)
                ds = new ByteArrayDataSource((InputStream)src,
                                             "application/octet-stream");
            else if (src instanceof DataSource)
                ds = (DataSource)src;
            if (ds != null)
                dh = new DataHandler(ds);
            else if (src instanceof DataHandler)
                dh = (DataHandler)src;
            if (dh != null) {
                bp = new MimeBodyPart();
                try {
                    bp.setDataHandler(dh);
                } catch(MessagingException me) {
                    throw new IllegalArgumentException(
                        "Invalid InputStream/DataSource/DataHandler: " + me);
                }
            } else if (src instanceof MimeBodyPart) {
                bp = (MimeBodyPart)src;
            }
            // by now we must logically have a valid MimeBodyPart

            // set a unique content-ID
            String cid = null;
            try {
                cid = bp.getContentID();
            } catch(MessagingException me) {
            }
            if (cid == null) {
            cid = MimeUtils.getUniqueValue();
            try {
                bp.setHeader(Constants.HEADER_CONTENT_ID, '<' + cid + '>');
            } catch(MessagingException me) {
               throw new IllegalArgumentException("Could not set Content-ID: "
                                                  + me);
            }
            }

            // add the part to the context
            try {
                ctx.addBodyPart(bp);
            } catch(MessagingException me) {
               throw new IllegalArgumentException("Could not add attachment: "
                                                  + me);
            }

            // Now write the XML element.
            sink.write('<' + context.toString());

            // Write the URLEncoded reference.
            sink.write(' ' + Constants.ATTR_REFERENCE + " =\"cid:"
                       + java.net.URLEncoder.encode(cid) + '"');

            sink.write("/>");
        }

        nsStack.popScope();
    }

    public Bean unmarshall(String inScopeEncStyle, QName elementType,
                           Node src, XMLJavaMappingRegistry xjmr,
                           SOAPContext ctx)
        throws IllegalArgumentException  {

        Element paramEl = (Element)src;

        DataHandler dh = null;
        if (!SoapEncUtils.isNull(paramEl)) {
            String uri = paramEl.getAttribute(Constants.ATTR_REFERENCE);

            try {
                MimeBodyPart bp = null;
                try {
                    bp = ctx.findBodyPart(uri);
                } catch(NullPointerException npe) {
                } catch(ClassCastException cce) {
                }
                if (bp == null) {
                    throw new IllegalArgumentException(
                      "Attachment tag \"" + paramEl.getTagName()
                      + "\" refers to a Mime attachment with label \""
                      + uri + "\" which could not be found.");
                } else
                    dh = bp.getDataHandler();
            } catch(MessagingException me) {
                throw new IllegalArgumentException(
                    "Failed to read attachment for tag \""
                    + paramEl.getTagName()
                    + "\" with label \"" + uri + "\": " + me);
            }
        }
        return new Bean(javax.activation.DataHandler.class, dh);
    }
}
