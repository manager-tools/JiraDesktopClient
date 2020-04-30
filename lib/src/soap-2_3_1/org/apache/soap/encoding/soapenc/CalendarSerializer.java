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

/*
* Initial revision - based on code from Apache serializer classes
*/

package org.apache.soap.encoding.soapenc;

import org.apache.soap.util.xml.*;
import org.apache.soap.util.*;
import org.apache.soap.encoding.soapenc.SoapEncUtils;
import org.apache.soap.rpc.SOAPContext;
import java.io.*;
import org.w3c.dom.*;
import java.io.*;
import java.util.*;
import java.text.*;

/**
*
* This class de/serializes instances of java.util.GregorianCalendar from/to
* the XML-Schema type 'date'. For details see the
* <A HREF="http://www.w3.org/TR/xmlschema-2/#date">XML-Schema specification</A>
*
* @author Phil Mork
* @author Glen Daniels (gdaniels@allaire.com)
* @see Serializer
* @see Deserializer
*/
public class CalendarSerializer implements Serializer, Deserializer
{
  SimpleDateFormat sdf;  

  public CalendarSerializer()
  {    
    sdf=new SimpleDateFormat("yyyy-MM-dd");
    //For now just use the default locale timezone  - could set to GMT if needed
    //sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
  }

  public void marshall(String inScopeEncStyle, Class javaType, Object src,
                       Object context, Writer sink, NSStack nsStack,
                       XMLJavaMappingRegistry xjmr, SOAPContext ctx)
                        throws IllegalArgumentException, IOException
  {
    if(!javaType.equals(java.util.GregorianCalendar.class))
    {
      throw new IllegalArgumentException("Can only serialize GregorianCalendar instances");
    }
    nsStack.pushScope();
    if(src!=null)
    {
      SoapEncUtils.generateStructureHeader(inScopeEncStyle,
                                           javaType,
                                           context,
                                           sink,
                                           nsStack,xjmr);

      Date calDate = ((GregorianCalendar)src).getTime();
      String fdate = null;

      synchronized(sdf)
      {
        fdate=sdf.format(calDate);
      }

      sink.write(fdate);
      sink.write("</" + context + '>');
    }
    else
    {
      SoapEncUtils.generateNullStructure(inScopeEncStyle,
                                           javaType,
                                           context,
                                           sink,
                                           nsStack,xjmr);
    }
    nsStack.popScope();
  }

  public Bean unmarshall(String inScopeEncStyle, QName elementType, Node src,
                         XMLJavaMappingRegistry xjmr, SOAPContext ctx)
                          throws IllegalArgumentException
  {
    Date date=null;
    Calendar calDate = new GregorianCalendar();
    Element root = (Element)src;
    String value = DOMUtils.getChildCharacterData(root);
    if(value!=null && !((value=value.trim()).equals("")))
    {
      try
      {
        synchronized(sdf)
        {
          date=sdf.parse(value);
        }
        calDate.setTime(date);
      }
      catch (ParseException pe)
      {
        throw new IllegalArgumentException("String represents no valid Date for this Deserializer");
      }
    }
    return new Bean(java.util.GregorianCalendar.class, calDate);
  }
}
