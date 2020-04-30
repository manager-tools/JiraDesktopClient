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
import org.apache.soap.*;
import org.apache.soap.rpc.*;

/**
 * A <code>MapSerializer</code> can be used to serialize and
 * deserialize Maps using the <code>SOAP-ENC</code>
 * encoding style. This is a quick implementation that simply
 * delegates to the HashtableSerializer (which used to be
 * named the MapSerializer).
 *
 * @author Matthew J. Duftler (duftler@us.ibm.com)
 */
public class MapSerializer implements Serializer, Deserializer
{
  private final HashtableSerializer hashtableSer = new HashtableSerializer();

  public void marshall(String inScopeEncStyle,
                       Class javaType,
                       Object src,
                       Object context,
                       Writer sink,
                       NSStack nsStack,
                       XMLJavaMappingRegistry xjmr,
                       SOAPContext ctx)
    throws IllegalArgumentException, IOException
  {
    Hashtable hashtable = null;

    if (src instanceof Hashtable)
    {
      hashtable = (Hashtable)src;
    }
    else if (src instanceof Map)
    {
      hashtable = new Hashtable();
      hashtable.putAll((Map)src);
    }
    else
    {
      throw new IllegalArgumentException("Tried to pass a '" +
                                         src.getClass().toString() +
                                         "' to MapSerializer");
    }

    hashtableSer.marshall(inScopeEncStyle,
                          Hashtable.class,
                          hashtable,
                          context,
                          sink,
                          nsStack,
                          xjmr,
                          ctx);
  }

  public Bean unmarshall(String inScopeEncStyle,
                         QName elementType,
                         Node src,
                         XMLJavaMappingRegistry xjmr,
                         SOAPContext ctx)
    throws IllegalArgumentException
  {
    return hashtableSer.unmarshall(inScopeEncStyle,
                                   elementType,
                                   src,
                                   xjmr,
                                   ctx);
  }
}
