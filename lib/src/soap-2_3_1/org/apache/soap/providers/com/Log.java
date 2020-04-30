/*
 * The Apache Software License, Version 1.1
 *
 *
 * Copyright (c) 2001 The Apache Software Foundation.  All rights 
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
 * originally based on software copyright (c) 2001, International
 * Business Machines, Inc., http://www.apache.org.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */
package org.apache.soap.providers.com;

import java.io.* ;
import java.util.* ;
import java.text.MessageFormat;
import javax.servlet.* ;
import javax.servlet.http.* ;
import org.apache.soap.util.* ;
import java.lang.Math;

public class Log 
{
  private ServletContext sc= null;
  static Log log= null; 
  public static final short INFORMATION = 0x1000;
  public static final short SUCCESS= 0x100;
  public static final short WARNING = 0x10;
  public static final short ERROR = 1;
  public static final short NOINIT = -1;
  static short currentLevel=NOINIT;
  static final String pname= "org.apache.soap.providers.com";
  static final String cname= pname + ".COMProvider";
  protected static final String msgFile= cname; 
  protected java.io.PrintStream ls= null;
  protected static ResourceBundle rb=null;
  public static synchronized void  init( Servlet servlet) //Specialize for HTTP.
  {
    if(log != null) return;
    String params=  null;
    if(servlet != null) params= servlet.getServletConfig().getInitParameter("comprovider.msglvl");
   
    java.io.PrintStream ls= null;
    String propInit= params == null ? findMessageString("loginit") : params;
    if(propInit != null)
    {
      StringTokenizer tok= new StringTokenizer(propInit);
      String f1= tok.hasMoreTokens() ? tok.nextToken() : "";
      if( f1.length() != 0)
      {
        
        if( 0 == f1.compareTo("="))
        {
         String f2= null;
          try{
            f2= tok.hasMoreTokens() ? tok.nextToken() : "";
            
            Class cl= Class.forName(f2);          
            Object o= cl.newInstance();
            if( o instanceof Log)
            {
              ((Log)o).init( servlet, params);
            }
            else
            {
             ls= System.err;
             System.err.println("Log handler:" + f2 + " not an instance of Log."); //Just keep going.
             log= new Log(null, "-file -" ); //Instance allows for subclassing.
            }
          }
          catch ( Exception e)
          {
            log= new Log(null, "-file -" ); 
            System.err.println("Error in loading log handler=" + f2 + "exception "  + e); //Just keep going.
          }
          return; //done.
        }
      }
    }

    log= new Log(servlet, params); //Instance allows for subclassing.
    
  }

  public  void init( Servlet servlet, String params){/* Do nothing */};
  
  public Log( Servlet servlet,  String params)
  {
    currentLevel= ERROR;
      
    if(null != params)  
    for( StringTokenizer tok= new StringTokenizer(params);tok.hasMoreTokens();)
    {
      String f1=  tok.nextToken();
      if( 0 == f1.compareTo("-noservletlog"))
      {
          servlet= null;
      }
      else if( 0 == f1.compareTo("-file"))
      {
          if(tok.hasMoreTokens())
          {
            String f2=  tok.nextToken();
            if( 0 == f2.compareTo("+") ) ls= System.out;
            else if( 0 == f2.compareTo("-")) ls= System.err;
            else
            {
              try
              {
                ls= new PrintStream(new BufferedOutputStream( new FileOutputStream( f2, true)), true);
              }catch( java.io.FileNotFoundException e)
              {
                ls= System.err;
              }
             }
           }
      }
      else
      {

        try{
            if(0==f1.compareToIgnoreCase("INFORMATION")) currentLevel= INFORMATION | SUCCESS | WARNING | ERROR;
            else if(0==f1.compareToIgnoreCase("SUCCESS")) currentLevel= SUCCESS | WARNING | ERROR;
            else if(0==f1.compareToIgnoreCase("WARNING")) currentLevel= WARNING | ERROR;
            else if(0==f1.compareToIgnoreCase("ERROR"));
            else  currentLevel=  Short.parseShort(params);
        }catch(Exception e) { currentLevel= ERROR;}
      }
    }//endfor

    if(null != servlet) sc= servlet.getServletConfig().getServletContext();



  }

  public static void init()
  { 
     init( null);
  }
  public static String getMessage(String msgId, Object[] args)
  {
     String ret=  findMessageString(msgId, args);
     if(ret == null)
          ret= "Missing msg id \""+ msgId +"\" in \"" + msgFile +"\".";

     return ret;
  }
  public static String findMessageString(String msgId)
  {
    return findMessageString(msgId, null);
  }
  public static String findMessageString(String msgId, Object[] args)
  {
     String ret= null;

       try
       { 
        if( rb == null)  rb = ResourceBundle.getBundle (msgFile, Locale.getDefault());
         String formatString = rb.getString(msgId);
         if (args == null) ret= formatString;
         else 
         {
          MessageFormat formatter = new MessageFormat(formatString);
          ret= formatter.format(args);
         }
       } catch (java.util.MissingResourceException mre) {
       }

     return ret;
  }
  public String logit(final int level, String s) //Could be over ride. called by CPP!!!
  {
    if( s == null) s= "null";
    if(ls != null) ls.println(s);
    if( null != sc) sc.log(s);
    return s;
  }

  public String logit(final int level, final String msgId, final Object[] args) //Could be over ride.
  {
    String s= getMessage(msgId, args);
    logit(level, s);
    return s;
  }
  public static final String msg(final int level, final String msg )
  {
   return log.logit(level, msg); 
  }
  public static final String msg(final int level, final String msgId, Object o )
  {
   if( (level & currentLevel) == 0 ) return "" ;
   return msg( level, msgId, new Object[] {o});
  }
  public static final String msg(final int level, final String msgId, Object o, Object o2 )
  {
   if( (level & currentLevel) == 0 ) return "" ;
   return msg( level, msgId, new Object[] {o,o2});
  }
  public static final String msg(final int level, final String msgid, Object o, Object o2, Object o3 )
  {
   if( (level & currentLevel) == 0 ) return "" ;
   return msg( level, msgid, new Object[] {o,o2,o3});
  }
  public static final String msg(final int level, final String msgId, Object o, Object o2, Object o3, Object o4 )
  {
   if( (level & currentLevel) == 0 ) return "" ;
   return msg( level, msgId, new Object[] {o,o2,o3,o4});
  }
  public static final String msg(final int level, final String msgId, final Object[] args)
  {
   if( (level & currentLevel) == 0 ) return "" ;
   return log.logit( level, msgId, args);
  }
  public static final short getLevel()
  {
   return currentLevel;
  }
  public static final boolean willLog(short level)
  {
    return (level & currentLevel) == 0; 
  }


}
