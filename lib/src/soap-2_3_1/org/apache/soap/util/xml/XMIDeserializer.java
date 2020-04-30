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

package org.apache.soap.util.xml;

import com.ibm.xmi.job.*;
import java.io.*;
import java.util.*;
import org.apache.soap.util.Bean;
import org.w3c.dom.*;
import org.apache.soap.rpc.SOAPContext;

/**
 * An <code>XMIDeserializer</code> ...
 *
 * @author Francisco Curbera (curbera@us.ibm.com)
 */
public class XMIDeserializer implements Deserializer
{

  static String XMIheader = "<?xml version='1.0' encoding='UTF-8'?><XMI xmi.version='1.1' timestamp='timestamp temporarily ommitted'><XMI.header><XMI.documentation><XMI.exporter>Java Object Bridge (JOB)</XMI.exporter><XMI.exporterVersion>0.9</XMI.exporterVersion></XMI.documentation></XMI.header><XMI.content>";

  static String XMIend = "</XMI.content></XMI>";

  public Bean unmarshall(String inScopeEncStyle, QName elementType, Node src,
                         XMLJavaMappingRegistry xjmr, SOAPContext ctx)
       throws IllegalArgumentException

  {
    // needs no mapping registry
    // no QName
    // assumes encoding style is XMI

    // need to serialize the node into an output stream
          
    String xmlString = DOMWriter.nodeToString(src);
    String document = XMIheader + xmlString + XMIend;
    byte[] bytes = document.getBytes();
    ByteArrayInputStream bs = new  ByteArrayInputStream(bytes);

    com.ibm.xmi.framework.WriterFactory.setInline(true);
    Collection objects = Job.readObjects(bs);

    try{
      bs.close();
    }catch(IOException e){
      e.printStackTrace(System.err);
    }
        
    
    Iterator it = objects.iterator();

    Object o;
    // assumethat there was one object encoded at most
    if( it.hasNext() )
      return new Bean((o=it.next()).getClass(), o);
    else
      throw new IllegalArgumentException("Unable to unmarshall XMI-encoded " +
                                         "object.");
                            
  } 
  
}
