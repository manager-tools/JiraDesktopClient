package org.apache.soap.server;

import java.io.*;
import java.util.*;
import java.lang.reflect.*;
import org.w3c.dom.*;
import org.apache.soap.util.Bean;
import org.apache.soap.util.MethodUtils;
import org.apache.soap.util.IOUtils;
import org.apache.soap.*;
import org.apache.soap.rpc.*;
import org.apache.soap.util.StringUtils;

/**
 * This class is a transport independent SOAP RPC router. However you
 * do it, if you get a SOAP envelope to me, I will build a Call object
 * out of it, check whether its valid and finally do it for you (on
 * an object you give me). 
 *
 * @author Sanjiva Weerawarana <sanjiva@watson.ibm.com>
 */
public class RPCRouter {
  public static Call extractCallFromEnvelope (ServiceManager serviceManager,
                                              Envelope callEnv,
                                              SOAPContext ctx)
                                                throws SOAPException {
    // determine target object URI by looking in the envelope
    Vector bodyEntries = callEnv.getBody ().getBodyEntries ();
    Element mainEntry = (Element) bodyEntries.elementAt (0);
    String targetObjectURI = StringUtils.parseFullTargetObjectURI(mainEntry.getNamespaceURI ());
    
    // call on known entity?
    DeploymentDescriptor dd = serviceManager.query (targetObjectURI);
    if (dd == null) {
      throw new SOAPException (
        Constants.FAULT_CODE_SERVER_BAD_TARGET_OBJECT_URI,
        "Unable to determine object id from call: is the method element " +
        "namespaced?");
    }

    // If I'm supposed to check mustUnderstands, do it now.
    if (dd.getCheckMustUnderstands()) {
      Header header = callEnv.getHeader();

      if (header != null) {
        Utils.checkMustUnderstands(header);
      }
    }

    // now extract the call from the call envelope with the given
    // deployment information about the object. Note that I still
    // pass in the service manager because I don't want to mess
    // with that code right now. I should actually be passing in
    // the soap mapping registry for this service.
    return Call.extractFromEnvelope (callEnv, serviceManager, ctx);
  }

  /**
   * Check whether the call is valid w.r.t. being on a method that's
   * published by the service. The call is assumed to have been built
   * by the above method; hence the call is on a known service.
   */
  public static boolean validCall (DeploymentDescriptor dd, Call call) {
    String callMethodName = call.getMethodName ();
    String[] pubMethods = dd.getMethods ();
    for (int i = 0; i < pubMethods.length; i++) {
      if (callMethodName.equals (pubMethods[i])) {
        return true;
      }
    }
    return false;
  }

  /**
   * Do the call on the given target object.
   */
  public static Response invoke (DeploymentDescriptor dd,
                                 Call call, Object targetObject,
                                 SOAPContext reqCtx, SOAPContext resCtx ) 
       throws SOAPException {
    byte providerType = dd.getProviderType ();

    // build the args and determine response encoding style
    Vector params = call.getParams ();
    String respEncStyle = call.getEncodingStyleURI ();
    Object[] args = null;
    Class[] argTypes = null;
    if (params != null) {
      int paramsCount = params.size ();
      args = new Object[paramsCount];
      argTypes = new Class[paramsCount];
      for (int i = 0; i < paramsCount; i++) {
        Parameter param = (Parameter) params.elementAt (i);
        args[i] = param.getValue ();
        argTypes[i] = param.getType ();
        if (respEncStyle == null) {
          respEncStyle = param.getEncodingStyleURI ();
        }
      }
    }
    if (respEncStyle == null) {
      // need to set a default encoding to be used for the response
      respEncStyle = Constants.NS_URI_SOAP_ENC;
    }
    
    // invoke the method (directly for Java and via InvokeBSF for script
    // methods)
    Bean result = null;
    try {
      if (providerType == DeploymentDescriptor.PROVIDER_JAVA ||
          providerType == DeploymentDescriptor.PROVIDER_USER_DEFINED) {
        Method m = null ;
        try {
          m = MethodUtils.getMethod (targetObject,
                                     call.getMethodName(),
                                     argTypes);
        } catch (NoSuchMethodException e) {
          try {
            int paramsCount = 0 ;
            if ( params != null ) paramsCount = params.size();
            Class[]   tmpArgTypes = new Class[paramsCount+1];
            Object[]  tmpArgs     = new Object[paramsCount+1];
            for ( int i = 0 ; i < paramsCount ; i++ )
              tmpArgTypes[i+1] = argTypes[i] ;
            argTypes = tmpArgTypes ;
            argTypes[0] = SOAPContext.class ;
            m = MethodUtils.getMethod (targetObject,call.getMethodName(),
                                       argTypes);
            for ( int i = 0 ; i < paramsCount ; i++ )
              tmpArgs[i+1] = args[i] ;
            tmpArgs[0] = reqCtx ;
            args = tmpArgs ;
          } catch (NoSuchMethodException e2) {
            /*
              Don't want the "No Signature Match" error message to include
              the SOAPContext argument.
            */
            throw e;
          } catch (Exception e2) {
            throw e2;
          }
        } catch (Exception e) {
          throw e;
        }

        result = new Bean (m.getReturnType (), m.invoke (targetObject, args));
      } else {
        // find the class that provides the BSF services (done
        // this way via reflection to avoid a compile-time dependency on BSF)
        Class bc = Class.forName ("org.apache.soap.server.InvokeBSF");

        // now invoke the service
        Class[] sig = {DeploymentDescriptor.class,
                       Object.class,
                       String.class,
                       Object[].class};
        Method m = MethodUtils.getMethod (bc, "service", sig, true);
        result = (Bean) m.invoke (null, new Object[] {dd, targetObject, 
                                                      call.getMethodName (),
                                                      args});
      }
    } catch (InvocationTargetException e) {
      Throwable t = e.getTargetException();

      if (t instanceof SOAPException) {
        throw (SOAPException)t;
      } else {
        throw new SOAPException(Constants.FAULT_CODE_SERVER,
                                "Exception from service object: " +
                                t.getMessage(), t);
      }
    } catch (ClassNotFoundException e) {
      throw new SOAPException (Constants.FAULT_CODE_SERVER,
                               "Unable to load BSF: script services " +
                               "unsupported without BSF", e);
    } catch (Throwable t) {
      throw new SOAPException (Constants.FAULT_CODE_SERVER, 
                               "Exception while handling service request: " +
                               t.getMessage(), t);
    }
   
    // build the response object
    Parameter ret = null;
    if (result.type != void.class) {
      ret = new Parameter (RPCConstants.ELEM_RETURN, result.type, 
                           result.value, null);
    }
    return new Response (call.getTargetObjectURI (), call.getMethodName (),
                         ret, null, null, respEncStyle, resCtx);
  }
}
