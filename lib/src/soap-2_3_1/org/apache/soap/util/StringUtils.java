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

package org.apache.soap.util;

import java.io.*;
import java.util.*;
import java.net.URL;
import java.net.MalformedURLException;
import java.beans.Introspector;

/**
 * Deals with strings (probably need to elaborate some more).
 *
 * @author Matthew J. Duftler
 */
public class StringUtils
{
  public static final String lineSeparator =
    System.getProperty("line.separator", "\n");
  public static String URI_SEPARATION_CHAR = "@";

  /*
    This method will return the correct name for a class object representing
    a primitive, a single instance of a class, as well as n-dimensional arrays
    of primitives or instances. This logic is needed to handle the string returned
    from Class.getName(). If the class object represents a single instance (or
    a primitive), Class.getName() returns the fully-qualified name of the class
    and no further work is needed. However, if the class object represents an
    array (of n dimensions), Class.getName() returns a Descriptor (the Descriptor
    grammar is defined in section 4.3 of the Java VM Spec). This method will
    parse the Descriptor if necessary.
  */
  public static String getClassName(Class targetClass)
  {
    String className = targetClass.getName();

    return targetClass.isArray() ? parseDescriptor(className) : className;
  }

  /*
    See the comment above for getClassName(targetClass)...
  */
  private static String parseDescriptor(String className)
  {
    char[] classNameChars = className.toCharArray();
    int    arrayDim       = 0;
    int    i              = 0;

    while (classNameChars[i] == '[')
    {
      arrayDim++;
      i++;
    }

    StringBuffer classNameBuf = new StringBuffer();

    switch (classNameChars[i++])
    {
      case 'B' : classNameBuf.append("byte");
                 break;
      case 'C' : classNameBuf.append("char");
                 break;
      case 'D' : classNameBuf.append("double");
                 break;
      case 'F' : classNameBuf.append("float");
                 break;
      case 'I' : classNameBuf.append("int");
                 break;
      case 'J' : classNameBuf.append("long");
                 break;
      case 'S' : classNameBuf.append("short");
                 break;
      case 'Z' : classNameBuf.append("boolean");
                 break;
      case 'L' : classNameBuf.append(classNameChars,
                                     i, classNameChars.length - i - 1);
                 break;
    }

    for (i = 0; i < arrayDim; i++)
      classNameBuf.append("[]");

    return classNameBuf.toString();
  }

  /*
    The recursiveDepth argument is used to insure that the algorithm gives up
    after hunting 2 levels up in the contextURL's path.
  */
  private static URL getURL(URL contextURL, String spec, int recursiveDepth)
                                                  throws MalformedURLException
  {
    URL url = null;

    try
    {
      url = new URL(contextURL, spec);

      try
      {
        url.openStream();
      }
      catch (IOException ioe1)
      {
        throw new MalformedURLException("This file was not found: " + url);
      }
    }
    catch (MalformedURLException e1)
    {
      url = new URL("file", "", spec);

      try
      {
        url.openStream();
      }
      catch (IOException ioe2)
      {
        if (contextURL != null)
        {
          String contextFileName = contextURL.getFile();
          String parentName      = new File(contextFileName).getParent();

          if (parentName != null && recursiveDepth < 3)
          {
            return getURL(new URL("file", "", parentName + '/'),
                          spec,
                          recursiveDepth + 1);
          }
        }

        throw new MalformedURLException("This file was not found: " + url);
      }
    }

    return url;
  }

  /*
  */
  public static URL getURL(URL contextURL, String spec) throws MalformedURLException
  {
    return getURL(contextURL, spec, 1);
  }

  /*
    Returns a Reader for reading from the specified resource, if the resource
    points to a stream.
  */
  public static Reader getContentAsReader(URL url) throws SecurityException,
                                                          IllegalArgumentException,
                                                          IOException
  {
    if (url == null)
    {
      throw new IllegalArgumentException("URL cannot be null.");
    }

    try
    {
      Object content = url.getContent();

      if (content == null)
      {
        throw new IllegalArgumentException("No content.");
      }

      if (content instanceof InputStream)
      {
        Reader in = new InputStreamReader((InputStream)content);

        if (in.ready())
        {
          return in;
        }
        else
        {
          throw new FileNotFoundException();
        }
      }
      else
      {
        throw new IllegalArgumentException((content instanceof String)
                                           ? (String)content
                                           : "This URL points to a: " +
                                             StringUtils.getClassName(content.getClass()));
      }
    }
    catch (SecurityException e)
    {
      throw new SecurityException("Your JVM's SecurityManager has disallowed this.");
    }
    catch (FileNotFoundException e)
    {
      throw new FileNotFoundException("This file was not found: " + url);
    }
  }

  /*
    Shorthand for: IOUtils.getStringFromReader(getContentAsReader(url)).
  */
  public static String getContentAsString(URL url) throws SecurityException,
                                                          IllegalArgumentException,
                                                          IOException
  {
    return IOUtils.getStringFromReader(getContentAsReader(url));
  }

  /**
  *  This method will perform the splicing of a full URI. It is currently 
  *  the only place where the delimiting character in the URI that triggers the 
  *  splicing operation is specified. (This character should later be specified
  *  as a constant...
  *
  * Creation date: (10/23/00 2:54:33 PM)
  * @return java.lang.String
  * @param fullTargetObjectURI java.lang.String
  */
  public static String parseFullTargetObjectURI(String fullTargetObjectURI) {
         if ( fullTargetObjectURI == null ) return null ;
	 int delimIndex = fullTargetObjectURI.indexOf(URI_SEPARATION_CHAR);
	 if ( (fullTargetObjectURI != null) && (delimIndex != -1) )
		 return fullTargetObjectURI.substring(0,delimIndex);
	 else
		 return fullTargetObjectURI;
 	
  }
 

}
