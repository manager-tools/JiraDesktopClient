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
import java.util.*;
import org.apache.soap.util.*;
import org.apache.soap.*;
import org.apache.soap.encoding.*;
import org.apache.soap.server.*;
import org.apache.soap.util.mime.*;
import javax.mail.*;
import javax.mail.internet.*;
import javax.activation.*;

/**
 * SOAP context for a message. Encapsulates:
 * <ul>
 *  <li>MIME multipart
 * </ul>
 *
 * @author Wouter Cloetens (wcloeten@raleigh.ibm.com)
 */
public class SOAPContext {
    protected MimeMultipart parts;
    protected Hashtable     bag    = new Hashtable();
    protected ClassLoader   loader = null ;

    /**
     * This flag indicates if setRootPart() was called, so we can distinguish
     * default root part resolution from deliberate root part indication.
     * Effect: calling setRootPart() twice will <strong>replace</strong> the
     * original root part.
     */
    protected boolean rootPartSet = false;

    /**
     * List of MIME headers to strip from JavaMail-generated header.
     */
    private static final String[] ignoreHeaders =
        {"Message-ID", "Mime-Version"};

    private static final String DEFAULT_BASEURI = "thismessage:/";

    /**
     * Constructor.
     */
    public SOAPContext() {
        parts = null;
    }

    /**
     * Initialise MIME multipart object from a data source.
     *
     * @param ds        a DataSource object to read from
     */
    public void readMultipart(DataSource ds) throws MessagingException {
        parts = new MimeMultipart(ds);
    }

    /**
     * Get the specified Part.  Parts are numbered starting at 0.
     *
     * @param index     the index of the desired Part
     * @return          the Part, or null if no such part exists.
     */
    public MimeBodyPart getBodyPart(int index) {
      /* Actually, this method never throws a MessagingException. In case a
       * future implementation does, catch it and throw an
       * IndexOutOfBoundsException
       */
        if (parts == null) {
            return null;
        }
        try {
            return (MimeBodyPart)parts.getBodyPart(index);
        }
        catch (MessagingException me) {
            throw new IndexOutOfBoundsException(me.getMessage());
        }
        catch (IndexOutOfBoundsException iobe) {
            return null;
        }
        catch (NullPointerException npe) {
            return null;
        }
    }

    /**
     * Get the Mimepart referred to by the given ContentID (CID).
     * Returns null if the part is not found.
     *
     * @param  CID      the ContentID of the desired part
     * @return          the Part, or null if no such part exists.
     */
    public MimeBodyPart getBodyPart(String CID) {
        if (parts == null) {
            return null;
        }
        try {
            return (MimeBodyPart)parts.getBodyPart(CID);
        }
        catch (MessagingException me) {
            return null;
        }
        catch (NullPointerException npe) {
            return null;
        }
        catch (IndexOutOfBoundsException iobe) {
            return null;
        }
    }

    /**
     * Find the Mimepart referred to by the given URI. This can be:<ul>
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
        if (parts == null || uri == null) {
            return null;
        }
        try {
            if (uri.length() > 4 &&
               uri.substring(0, 4).equalsIgnoreCase("cid:")) {
                // It's a Content-ID. URLDecode and lookup.
                String cid = MimeUtils.decode(uri.substring(4));
                // References are not supposed to be inside brackets, but be
                // tolerant.
                if (cid.charAt(0) != '<' || cid.charAt(cid.length()) != '>')
                    cid = '<' + cid + '>';

                try {
                    return (MimeBodyPart)parts.getBodyPart(cid);
                } catch(NullPointerException npe) {
                }
            } else {
                // It's a URI.
                return findPartByLocation(uri);
            }

        } catch (MessagingException me) {
        } catch (NullPointerException npe) {
        }
        return null;
    }

    /**
     * Determine the document's base URI, according to the following scheme:
     * <ol>
     * <li>The Content-Location header of the root (SOAP) part.
     * <li>The Content-Location header of the multipart. (not implemented)
     * <li>"thismessage:/"
     * </ol>
     */
    public String getBaseURI() {
        String baseUri = null;
        try {
            baseUri = getRootPart().getHeader(
                Constants.HEADER_CONTENT_LOCATION, null);
        } catch(MessagingException me) {
        }
        if (baseUri == null)
            baseUri = DEFAULT_BASEURI;
        return baseUri;
    }

    /**
     * Find the Mimepart referred to by the given URI. This can be:<ul>
     * <li>An absolute URI, in which case a part is located with a
     *     corresponding Content-Location header.
     * <li>A relative URI, in which case an absolute URI is constructed
     *     relative to the Content-Location header of the root SOAP part
     *     or the multipart and the previous rule is then applied.
     * </ul>Returns null if the part is not found.
     * <p>Note: relative URIs not entirely implemented yet. We also can't pick
     * up the base URI from the multipart (transport) headers yet, only from
     * the root (SOAP) part.
     *
     * @param  uri      the URI
     * @return          the Part or null if not found
     */
    public MimeBodyPart findPartByLocation(String uri) {
        try {
            String baseUri = getBaseURI();
            uri = normalizeURI(uri, baseUri);
            if (uri == null)
                return null;
            for (int i = 0; i < parts.getCount(); i++) {
                MimeBodyPart part = getBodyPart(i);
                if (part != null) {
                    String partUri = part.getHeader(
                        Constants.HEADER_CONTENT_LOCATION, null);
                    if (uri.equals(normalizeURI(partUri, baseUri)))
                        return part;
                }
            }
        } catch(MessagingException me) {
        }
        return null;
    }

    /**
     * Normalise URI to an absolute URI according to rules in RFC2396.
     * <p>Note: resolution for relative URIs is not completely implemented
     * yet. For now, we just concatenate the relative URI to the base URI.
     */
    private String normalizeURI(String uri, String baseUri) {
        // Check if it is an absolute URI.
        int p1 = uri.indexOf(':');
        if (p1 >= 0) {
            String scheme = uri.substring(0, p1);
            if (scheme.indexOf('/') == -1 &&
                scheme.indexOf('?') == -1 &&
                scheme.indexOf('#') == -1)
                // Yes, it's absolute! Return unchanged.
                return uri;
        }
        // It's a relative URI. Normalise to an absolute URI.
        return baseUri + uri;
    }

    /**
     * Adds a Part.  The BodyPart is appended to
     * the list of existing Parts.
     *
     * @param  part  The Part to be appended
     * @exception    MessagingException
     * @exception    IllegalWriteException if the underlying
     *               implementation does not support modification
     *               of existing values
     */
    public void addBodyPart(MimeBodyPart part) throws MessagingException {
        // Implemented as addBodyPart(part, index=-1) below.
        addBodyPart(part, -1);
    }

    /**
     * Adds a MimeBodyPart at position <code>index</code>.
     * If <code>index</code> is not the last one in the list,
     * the subsequent parts are shifted up. If <code>index</code>
     * is larger than the number of parts present, the
     * MimeBodyPart is appended to the end.
     *
     * @param           part  The BodyPart to be inserted
     * @param           index Location where to insert the part
     * @exception       MessagingException
     * @exception       IllegalWriteException if the underlying
     *                  implementation does not support modification
     *                  of existing values
     * @exception       IllegalArgumentException if the part cannot be made
     *                  into an attachment for some reason
     */
    public void addBodyPart(MimeBodyPart part, int index)
        throws MessagingException, IllegalArgumentException {
        if (parts == null)
            parts = new MimeMultipart(
                Constants.HEADERVAL_MULTIPART_CONTENT_SUBTYPE);
        // set some Mime headers. Assume that a passed MimeBodyPart
        // already has these set to appropriate values
        DataHandler dh = part.getDataHandler();
        try {
            MimeType ctype = new MimeType(dh.getContentType());
            part.setHeader(Constants.HEADER_CONTENT_TYPE,
                           ctype.toString());
            if (dh.getDataSource() instanceof ByteArrayDataSource)
                part.setHeader(Constants.HEADER_CONTENT_LENGTH,
                             String.valueOf(((ByteArrayDataSource)dh
                                             .getDataSource())
                                            .getSize()));
            /* NOTE. The following part will do some guesswork to determine if
             * 8-bit encoding should be used. Even then, JavaMail may still
             * use base64 encoding for binary types that aren't in the list
             * below, or quoted-printable for text. Not all SOAP MIME
             * implemenations may support base64 or quoted-printable, so this
             * should be replaced with code that always uses 8-bit encoding
             * exception for text/*, which should be UTF-8 encoded.
             * To do: find a way to do the latter...
             */
            if (ctype.match("application/octet-stream") ||
                ctype.match("image/*") ||
                ctype.match("audio/*") ||
                ctype.match("video/*"))
                part.setHeader("Content-Transfer-Encoding", "8bit");
        } catch(MessagingException me) {
            throw new IllegalArgumentException(
                "Invalid InputStream/DataSource/DataHandler metadata: "
                + me);
        } catch(MimeTypeParseException mtpe) {
            throw new IllegalArgumentException("Invalid Mime type \""
                                               + dh.getContentType()
                                               + "\": " + mtpe);
        }
        if (index == -1)
            parts.addBodyPart(part);
        else
            parts.addBodyPart(part, index);
    }

    /**
     * Remove a body part.
     */
    public void removeBodyPart(MimeBodyPart part) throws MessagingException {
      parts.removeBodyPart(part);
    }

    /**
     * Adds the root BodyPart. Add it in the first position, create a
     * unique Content ID and set the "start" Content-Type header modifier
     * that that.<p>
     * Calling this method twice will <strong>replace</strong> the previous
     * root part with the new one.
     *
     * @param           part  The BodyPart to be inserted
     * @exception       MessagingException
     */
    public void setRootPart(MimeBodyPart part) throws MessagingException {
        String rootCid = '<' + MimeUtils.getUniqueValue() + '>';
        part.setHeader(Constants.HEADER_CONTENT_ID, rootCid);
        if (rootPartSet)
            parts.removeBodyPart(getRootPart());
        addBodyPart(part, 0);
        rootPartSet = true;
    }

    /**
     * Set the root part to a provided string (usually the SOAP envelope).
     * The string will be UTF-8 encoded.
     *
     * @param           s  The String to be inserted
     * @param           contentType the Content-Type of the root part
     * @exception       MessagingException
     */
    public void setRootPart(String s, String contentType)
        throws MessagingException, IOException {
        setRootPart(s.getBytes("UTF8"), contentType);
    }

    /**
     * Set the root part to a provided string (usually the SOAP envelope).
     * The string will be UTF-8 encoded.
     *
     * @param           b  The String to be inserted
     * @param           contentType the Content-Type of the root part
     * @return          "start" Content-Type header modifier
     * @exception       MessagingException
     */
    public void setRootPart(byte[] b, String contentType)
        throws MessagingException {
        ByteArrayDataSource ds = new ByteArrayDataSource(b, contentType);
        DataHandler dh = new DataHandler(ds);
        MimeBodyPart bp = new MimeBodyPart();
        bp.setDataHandler(dh);
        bp.setHeader(Constants.HEADER_CONTENT_LENGTH,
                     String.valueOf(ds.getSize()));

         // Avoid letting JavaMail determine a transfer-encoding of
        // quoted-printable or base64... Force 8-bit encoding.
        bp.setHeader("Content-Transfer-Encoding", "8bit");

        setRootPart(bp);
    }

    /**
     * Find the root part. For multipart, search for a "start" Content-Type
     * header modifier, if not present or invalid, assume the first part is
     * the root.
     *
     * @return          document root BodyPart
     * @exception       MessagingException
     */
    public MimeBodyPart getRootPart() throws MessagingException {
        MimeBodyPart rootPart = null;
        if (getCount() > 1) {
            String startCid = new ContentType(
                parts.getContentType()).getParameter("start");
            if (startCid != null)
                rootPart = getBodyPart(MimeUtils.decode(startCid));
        }
        if (rootPart == null)
            rootPart = getBodyPart(0);
        return rootPart;
    }

    /**
     * Set the MultiPart Mime subtype. This method should be invoked only on
     * a new MimeMultipart object created by the client. The default subtype
     * of such a multipart object is "related".<p>
     *
     * @param           subtype         Subtype
     * @exception       MessagingException
     */
    public void setSubType(String subtype) throws MessagingException {
        if (parts == null)
            parts = new MimeMultipart(subtype);
        else
            parts.setSubType(subtype);
    }

    /**
     * Returns true is setRootPart() has been called.
     */
    public boolean isRootPartSet() {
        return rootPartSet;
    }

    /**
     * Return the number of enclosed BodyPart objects.
     *
     * @return          number of parts
     */
    public int getCount() throws MessagingException {
        if (parts == null)
            return 0;
        else
            return parts.getCount();
    }

    /**
     * Return the content-type
     *
     * @return      content type of the Mime multipart if there's more
     *              than one part, or of the root part if there's only
     *              one part
     */
    public String getContentType() throws MessagingException {
        if (parts == null)
            return null;
        else
            if (parts.getCount() == 1)
                return getRootPart().getContentType();
            else
                return parts.getContentType();
    }

    /**
     * Encode the root part or multipart and write to an OutputStream.
     *
     * @param       os        stream to write to
     * @exception   IOException
     * @exception   MessagingException
     */
    public void writeTo(OutputStream os)
        throws IOException, MessagingException {
        int count = getCount();
        if (count == 0)
            throw new IOException("Message is empty!");
        else if (count == 1) {
            getRootPart().writeTo(os);
        }
        else {
            Session session = Session.getDefaultInstance(new Properties(), null);
            MimeMessage msg = new MimeMessage(session);
            msg.setContent(parts);
            msg.saveChanges();
            msg.writeTo(os, ignoreHeaders);
        }
    }

    /**
     * Store something in the hold-all 'bag'
     */
    public void setProperty(String name, Object value) {
      bag.put( name, value );
    }

    /**
     * Look for something in the hold-all 'bag'
     */
    public Object getProperty(String name) {
      return( bag.get(name) );
    }

    /**
     * Remove something from the hold-all 'bag'
     */
    public Object removeProperty(String name) {
      return( bag.remove(name) );
    }

    /**
     * Return the entire list of 'names' in the hold-all 'bag'
     */
    public Enumeration getPropertyNames() {
      return( bag.keys() );
    }

    public void setClassLoader(ClassLoader cl) {
      loader = cl;
    }

    public ClassLoader getClassLoader() {
      return loader ;
    }

    public Class loadClass(String className) throws ClassNotFoundException {
      if ( loader == null )
        return( Class.forName( className ) );
      return( Class.forName( className, true, loader ) );
    }

    /**
     * String representation for debug purposes.
     */
    public String toString()  {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        pw.print("[Parts={");

        if (parts != null) {
            try {
                for (int i = 0; i < getCount(); i++) {
                    if (i > 0) {
                        pw.print(", ");
                    }

                    MimeBodyPart mbp = getBodyPart(i);
                    pw.print("[cid:" + mbp.getContentID()
                             + " type: " + mbp.getContentType()
                             + " enc: " + mbp.getEncoding() + "]");
                }
            }
            catch(MessagingException me) {
                me.printStackTrace();
            }
        }

        pw.print("}]");

        return sw.toString();
    }
}
