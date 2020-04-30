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
import org.apache.soap.* ;
import org.apache.soap.rpc.* ;
import org.apache.soap.server.* ;
import org.apache.soap.util.* ;
import java.lang.Math;

public class RPCProvider implements Provider
{
  private DeploymentDescriptor dd ;
  private Envelope             envelope ;
  private Call                 call ;
  private String               methodName ;
  private String               targetObjectURI ;
  private HttpServlet          servlet ;
  private HttpSession          session ;
  private byte[] vp= null;
  private String progid= null;
  private ServletContext sc= null;
  private String threadingModel= null;
  private static boolean initLog= false;

  public void locate( DeploymentDescriptor dd,
                      Envelope env,
                      Call call,
                      String methodName,
                      String targetObjectURI,
                      SOAPContext reqContext)
      throws SOAPException
  {
    HttpServlet servlet = (HttpServlet) reqContext.getProperty( Constants.BAG_HTTPSERVLET );
    HttpSession session = (HttpSession) reqContext.getProperty( Constants.BAG_HTTPSESSION );
    // sc= servlet.getServletConfig().getServletContext();
    if(!initLog)
    { 
      Log.init(servlet);
      initlog(Log.getLevel());
      initLog=true;
    }

    Log.msg(Log.SUCCESS, "msg.comprovider.inlocate", getClass().getName(), targetObjectURI, methodName );
    if( null != dllLoadException)
    {
      Log.msg(Log.ERROR, "msg.comprovider.dllfail", dllLoadException );
      throw dllLoadException;
     }
     
    //This validates that the method name is listed in the deployment descriptor.
    if (!MessageRouter.validMessage (dd, methodName)) {
      String msg=
      Log.msg(Log.ERROR, "msg.comprovider.badMethod", targetObjectURI, methodName);
        throw new SOAPException (Constants.FAULT_CODE_SERVER, msg);
      }

    Vector parms= call.getParams();
    int noParms= 0;
    if(parms != null)
    {
      vp= new byte[sizeOfVariant  * parms.size()];
      noParms= parms.size();
    }

    for(int i=0; i < noParms; ++i)
    {
     Log.msg(Log.INFORMATION, "msg.comprovider.info.parms", new Integer(i), parms.elementAt(i).getClass().getName(), parms.elementAt(i).toString());
     //Yes. Prameters are reversed here.
     objectToVariant( ((Parameter)(parms.elementAt(noParms - i -1))).getValue(), vp, i * sizeOfVariant);
    }

    Hashtable    props = dd.getProps();

    progid=  (String) props.get("progid"); 
    if(null== progid)
    {
     if(targetObjectURI.startsWith("urn:"))
     {
       progid= targetObjectURI.substring(4);
     }
     else
       progid= targetObjectURI;
    }

    threadingModel=  (String) props.get("threadmodel"); 
    if( null == threadingModel) threadingModel= "MULTITHREADED";
    Log.msg(Log.INFORMATION, "msg.comprovider.info.cominf", progid, threadingModel );

    this.dd              = dd ;
    this.envelope        = env ;
    this.call            = call ;
    this.methodName      = methodName ;
    this.targetObjectURI = targetObjectURI ;
    this.servlet         = servlet ;
    this.session         = session ;


    // Add logic to locate/load the service here
  } //Endof of locate


  public void invoke(SOAPContext reqContext, SOAPContext resContext)
                      throws SOAPException
  {
    if( null != dllLoadException)
    {
      Log.msg(Log.ERROR, "msg.comprovider.dllfail", dllLoadException );
      throw dllLoadException;
     }
     if( null == progid)
     {
       String msg= Log.msg(Log.ERROR, "msg.comprovider.error.nullprog", methodName, targetObjectURI);
       throw new SOAPException( Constants.FAULT_CODE_SERVER, msg);
     }
     if( null == methodName)
     {
       String msg= Log.msg(Log.ERROR, "msg.comprovider.error.nullmname", targetObjectURI);
       throw new SOAPException( Constants.FAULT_CODE_SERVER, msg);
     }



    // If you want the client to use a new targetObjectURI for subsequence
    // calls place it here (targetObjectURI on 'new Response').
    //   i.e. we added an 'instance-id' to the urn
    Object ret= null;
         try{
          ret=     invoke( threadingModel, progid, methodName, vp); 
          }catch( Exception e)
          {
            String msg= Log.msg(Log.ERROR, "msg.comprovider.error.nativeError", e.toString());
            throw new SOAPException( Constants.FAULT_CODE_SERVER, msg);
          }
          try {
          Parameter pret= null;
          if(ret != null) pret= new Parameter(RPCConstants.ELEM_RETURN , ret.getClass(), ret, null);
              vp=null; //dereference.
          Response resp = new Response( targetObjectURI,   // URI
                           call.getMethodName(),  // Method
                           pret,      // ReturnValue
                           null,                  // Params
                           null,                  // Header
                           call.getEncodingStyleURI (),                  // encoding
                           resContext );          // response soapcontext - not supported yet

          Envelope env = resp.buildEnvelope();
          StringWriter  sw = new StringWriter();
          env.marshall( sw, call.getSOAPMappingRegistry(), resContext );
          resContext.setRootPart( sw.toString(), Constants.HEADERVAL_CONTENT_TYPE_UTF8);
          }
          catch( Exception e ) {
            String msg= Log.msg(Log.ERROR, "msg.comprovider.error.exp", e.toString());
           if ( e instanceof SOAPException ) throw (SOAPException ) e ;
            throw new SOAPException( Constants.FAULT_CODE_SERVER, msg );
          }

   Log.msg(Log.SUCCESS, "msg.comprovider.ok", ret == null ? "*null*" : ret.toString() );
  } //invoke

  /* JNI Code ---------------------------------------------------------------------------------------------------------------------------------------- */
  static SOAPException dllLoadException= null; //Can hold an exception on from the loading of the c++ dll.
  static final String libName= "COMProvider";  //C++ dll name.
  protected void logit( int level, String msg) //Called by CPP
  {
    Log.msg(level, msg);
  }
  
  
  static final String pname= "org.apache.soap.providers.com";
  static final String cname= pname + ".RPCProvider";
  static 
  {
    try
    {
      System.loadLibrary (libName);
    }
    catch(java.lang.SecurityException e)
    {
      dllLoadException= new SOAPException(Constants.FAULT_CODE_SERVER, "SecurityException from " + cname + " loading library:" + libName + " " + e.getMessage(),e); 
    }
    catch(java.lang.UnsatisfiedLinkError e)
    {
      dllLoadException= new SOAPException(Constants.FAULT_CODE_SERVER, "UnsatisfiedLinkError from " + cname + " loading library:" + libName + " " + e.getMessage(),e); 
    } 
  } //Endof static

  private static SOAPException getSOAPException( String msg)
  {
    return  new SOAPException(Constants.FAULT_CODE_SERVER, msg); 
  }

  private native Object invoke(String threadingModel, String progId, String methodName,  byte[] parms) throws SOAPException;
  private static native void initlog(short level);
  private static native  byte[] nativeConvertToBString( String  o );

  /* Java to CPP Marshalling code. ---------------------------------------------------------------------------------------------------------------------------------------- */

  private static final int sizeOfVariant= 16;


  private final byte[] objectToVariant( Object o ) throws SOAPException
  {
   return objectToVariant( o, null, 0);
  }
  private final byte[] objectsToVariants( Object[] o  ) throws SOAPException
  {
    byte[] bo= new byte[ o.length * sizeOfVariant];
    for(int i=0; i < o.length; ++i)
       objectToVariant( o[i], bo, i * sizeOfVariant);
    return bo;
  }
  private final byte[] objectToVariant( Object o,  byte[] bo,  int os) throws SOAPException
  {
    byte[] v= bo;
    if( null == v)
    {
     v= new byte[sizeOfVariant]; //Size of a variant
     os=0;
    }
      
     if( null== o)
     { //to be safe.
      v[os+0] = 1; //VT_NULL
      v[os+1] = 0;
     }
     else if(o  instanceof java.lang.Boolean) //VT_R8
     {
      v[os+0] = 11; //VT_BOOL
      v[os+1] = 0;
      byte x= (byte)( (((Boolean) o).booleanValue()) ? 0xff: 0);
      v[os+8]= x;
      v[os+9]= x;
      v[os+10]= x;
      v[os+11]= x;
     }
     else if(o  instanceof java.lang.Integer) //VT_R8
     {
      v[os+0] = 3; //VT_I4
       v[os+1] = 0;
      int x= ((Integer)o).intValue();
      v[os+8]= (byte)x;
      v[os+9]= (byte)((x>>>8) & 0xff);
      v[os+10]= (byte)((x>>>16) & 0xff);
      v[os+11]= (byte)((x>>>24) & 0xff);
     }
     else if( o instanceof java.lang.String)
     {
      v[os+0] = 8; //VT_BSTR
      v[os+1] = 0;
      byte[] pbs= nativeConvertToBString( (String) o );
      v[os+8]= pbs[0];
      v[os+9]= pbs[1]; 
      v[os+10]= pbs[2]; 
      v[os+11]= pbs[3]; 
     }
     else if(o  instanceof java.lang.Long) //VT_R8
     { //COM has no long type so promote it to double which can contain it.
      v[os+0] = 5; //VT_R8
     	v[os+1] = 0;
      long x= Double.doubleToLongBits((double)(((Long)o).longValue()));
      v[os+8]= (byte)x;
      v[os+9]= (byte)((x>>>8) & 0xff);
      v[os+10]= (byte)((x>>>16) & 0xff);
      v[os+11]= (byte)((x>>>24) & 0xff);
      v[os+12]= (byte)((x>>>32) & 0xff);
      v[os+13]= (byte)((x>>>40) & 0xff);
      v[os+14]= (byte)((x>>>48) & 0xff);
      v[os+15]= (byte)((x>>>56) & 0xff);
     }
     else if(o instanceof java.lang.Short)
     {
      v[os+0] = 2; //VT_I2
      v[os+1] = 0;
      int x= ((Short)o).intValue();
      v[os+8]= (byte)x;
      v[os+9]= (byte)((x>>>8) & 0xff);
      v[os+10]= (byte)((x>>>16) & 0xff);
      v[os+11]= (byte)((x>>>24) & 0xff);
     }
     else if(o instanceof java.lang.Float)
     {
      v[os+0] = 4; //VT_R4
       v[os+1] = 0;
       int x= Float.floatToIntBits(((Float)o).floatValue());
       v[os+8]= (byte)x;
       v[os+9]= (byte)((x>>>8) & 0xff);
       v[os+10]= (byte)((x>>>16) & 0xff);
       v[os+11]= (byte)((x>>>24) & 0xff);
     }
     else if(o  instanceof java.lang.Double) //VT_R8
     {
      v[os+0] = 5; //VT_R8
      v[os+1] = 0;
      long x= Double.doubleToLongBits(((Double)o).doubleValue());
      v[os+8]= (byte)x;
      v[os+9]= (byte)((x>>>8) & 0xff);
      v[os+10]= (byte)((x>>>16) & 0xff);
      v[os+11]= (byte)((x>>>24) & 0xff);
      v[os+12]= (byte)((x>>>32) & 0xff);
      v[os+13]= (byte)((x>>>40) & 0xff);
      v[os+14]= (byte)((x>>>48) & 0xff);
      v[os+15]= (byte)((x>>>56) & 0xff);
     }
     else if(o  instanceof java.lang.Byte) 
     {
      v[os+0] = 17; //VT_UI1
      v[os+1] = 0;
      byte x= ((Byte)o).byteValue();
      v[os+8]= x;
     }
     else if(o  instanceof java.lang.Character) 
     {
      v[os+0] = 17; //VT_UI1
      v[os+1] = 0;
      byte x= (byte) ((Character)o).charValue();
      v[os+8]= x;
     }
     else if(o  instanceof java.lang.Void) 
     {
      v[os+0] = 1; //VT_NULL
      v[os+1] = 0;
     }
     else if( o.getClass().isArray())
     {
      // ArrayInfo ai= new ArrayInfo(o);
      // v= ai.toVariant();
      throw new SOAPException(Constants.FAULT_CODE_SERVER, "Currently arrays are unsupported,  type received:" + o.getClass().getName()); 
     }
     else
     { 
      throw new SOAPException(Constants.FAULT_CODE_SERVER, "Currently unsupported data type received:" + o.getClass().getName()); 
     /*
      // v[os+0] = 9; //VT_DISPATCH for object
      // v[os+1] = 0;

      byte[] cppref= null; 

      if(o  instanceof  ) 
      {
       // System.arraycopy(cppref,0,v,8, cppref.length);
      }
      else if(o  ) //Specific request to return back empty 
      {
        v[os+0] = 0; //VT_EMPTY
        v[os+1] = 0;
      }
      else
      {
        // cppref= nativeObjectToVariant(o); 
        System.arraycopy(cppref,0,v,8, cppref.length);
      }
     */
      
     }
    return v; 
  }
  
} //Endof RPCProvider
