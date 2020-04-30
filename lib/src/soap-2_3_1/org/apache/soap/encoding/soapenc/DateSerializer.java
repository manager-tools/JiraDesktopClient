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

import org.apache.soap.util.xml.*;
import org.apache.soap.util.*;
import org.apache.soap.rpc.SOAPContext;
import java.io.*;
import org.w3c.dom.*;
import java.io.*;
import java.util.*;
import java.text.*;

/**
*
* This class de/serializes instances of java.util.Date from/to
* the XML-Schema type 'timeInstant'. For details see the
* <A HREF="http://www.w3.org/TR/xmlschema-2/#timeInstant">XML-Schema specification</A>
*
* @author Phil Mork
* @author Glen Daniels (gdaniels@allaire.com)
* @author Matthew J. Duftler (duftler@us.ibm.com)
* @author Sam Ruby (rubys@us.ibm.com)
* @see Serializer
* @see Deserializer
*/
public class DateSerializer implements Serializer, Deserializer
{
  SimpleDateFormat sdf;

  public DateSerializer()
  {
    sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
                            //  0123456789 0 123456789
    sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
  }

  public void marshall(String inScopeEncStyle, Class javaType, Object src,
                       Object context, Writer sink, NSStack nsStack,
                       XMLJavaMappingRegistry xjmr, SOAPContext ctx)
                        throws IllegalArgumentException, IOException
  {
    if(!javaType.equals(java.util.Date.class))
    {
      throw new IllegalArgumentException("Can only serialize java.util.Date instances");
    }
    nsStack.pushScope();
    if(src!=null)
    {
      SoapEncUtils.generateStructureHeader(inScopeEncStyle,
                                           javaType,
                                           context,
                                           sink,
                                           nsStack,xjmr);

      String fdate=null;

      synchronized(sdf)
      {
        fdate=sdf.format((Date)src);
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
      }
      catch (ParseException pe)
      {
        try
        {
          // validate fixed portion of format
          if (value.length() < 19) 
            throw new ParseException("",0);

          if (value.charAt(4) != '-' || value.charAt(7) != '-' ||
              value.charAt(10) != 'T')
            throw new ParseException("",0);

          if (value.charAt(13) != ':' || value.charAt(16) != ':')
            throw new ParseException("",0);

          // convert what we have validated so far
          try
          {
            synchronized(sdf)
            {
              date=sdf.parse(value.substring(0,19)+".000Z"); 
            }
          }
          catch (Exception e)
          {
            throw new ParseException("",0);
          }

          int pos = 19;

          // parse optional milliseconds
          if (pos < value.length() && value.charAt(pos)=='.') {
            int milliseconds = 0;
            int start = ++pos;
            while (pos<value.length() && Character.isDigit(value.charAt(pos)))
              pos++;

            String decimal=value.substring(start,pos);
            if (decimal.length()==3) {
              milliseconds=Integer.parseInt(decimal);
            } else if (decimal.length() < 3) {
              milliseconds=Integer.parseInt((decimal+"000").substring(0,3));
            } else {
              milliseconds=Integer.parseInt(decimal.substring(0,3));
              if (decimal.charAt(3)>='5') ++milliseconds;
            }

            // add milliseconds to the current result
            date.setTime(date.getTime()+milliseconds);
          }

          // parse optional timezone
          if (pos+5 < value.length() &&
               (value.charAt(pos)=='+' || (value.charAt(pos)=='-')))
          {
            if (!Character.isDigit(value.charAt(pos+1)) || 
                !Character.isDigit(value.charAt(pos+2)) || 
                value.charAt(pos+3) != ':'              || 
                !Character.isDigit(value.charAt(pos+4)) || 
                !Character.isDigit(value.charAt(pos+5)))
               throw new ParseException("",0);

            int hours = (value.charAt(pos+1)-'0')*10+value.charAt(pos+2)-'0';
            int mins  = (value.charAt(pos+4)-'0')*10+value.charAt(pos+5)-'0';
            int milliseconds = (hours*60+mins)*60*1000;

            // subtract milliseconds from the current date to obtain GMT
            if (value.charAt(pos)=='+') milliseconds=-milliseconds;
            date.setTime(date.getTime()+milliseconds);
            pos+=6;  
          }

          if (pos < value.length() && value.charAt(pos)=='Z') pos++;

          if (pos < value.length())
            throw new ParseException("",0);
        }
        catch (ParseException pe2)
        {
          synchronized(sdf)
          {
            throw new IllegalArgumentException("String represents no valid " +
                                             "Date for this Deserializer; " +
                                             "try " + sdf.toPattern() + ".");
          }
        }
      }
    }
    return new Bean(java.util.Date.class,date);
  }
}
