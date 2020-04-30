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

/**
 * FPDeserUtil: Utilities to help with deserialization of
 * floating point special values.
 *
 * Supplements Java.lang deserialization of float, Float, double, and
 * Double by handling the special values INF, -INF, and NaN, something
 * that Java doesn't do (i.e. new Float("INF") will cause a
 * NumberFormatException).
 *
 * These special values specify infinity, negative infinity,
 * and Not-a-Number in the XML Schema - Datatypes specification
 * for the primitive datatypes "float" (Section 3.2.4.1) and
 * "double" (Section 3.2.5.1).
 * 
 * Also accepts "Infinity" for "INF" and "-Infinity" for "-INF"
 * (case-insensitive) because that's what Java generates for
 * toString() of a Float or Double.
 *
 * See: <a href="http://www.w3.org/TR/xmlschema-2/">XML Schema
 * Part 2: Datatypes (W3C PR 16 March 2001)</a>
 *
 * @author Jim Stearns (Jim_Stearns@hp.com)
 */
public class FPDeserUtil
{
    // Static methods only, so disable constructor
    private FPDeserUtil()
    {
    }

    // Deserialize INF, -INF, and NaN, too.
    public static Float newFloat(String value)
    {
        Float res = null;
        try {
            res = new Float(value);
        } catch (NumberFormatException e) {
            if (value.equals("INF")
            ||  value.toLowerCase().equals("infinity")) {
                return new Float(Float.POSITIVE_INFINITY);
            } else if (value.equals("-INF")
            ||  value.toLowerCase().equals("-infinity")) {
                return new Float(Float.NEGATIVE_INFINITY);
            } else if (value.equals("NaN")) {
                return new Float(Float.NaN);
            }
            throw e;
        }
        return res;
    }

    public static Double newDouble(String value)
    {
        Double res = null;
        try {
            res = new Double(value);
        } catch (NumberFormatException e) {
            if (value.equals("INF")
            ||  value.toLowerCase().equals("infinity")) {
                return new Double(Double.POSITIVE_INFINITY);
            } else if (value.equals("-INF") 
            ||  value.toLowerCase().equals("-infinity")) {
                return new Double(Double.NEGATIVE_INFINITY);
            } else if (value.equals("NaN")) {
                return new Double(Double.NaN);
            }
            throw e;
        }
        return res;
    }
}
