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

/**
 * This class is a transport independent SOAP message router. However you
 * do it, if you get a SOAP envelope to me, I will deliver that to the
 * right method of the object you give me to work on.
 *
 * @author Sanjiva Weerawarana <sanjiva@watson.ibm.com>
 */
public class MessageRouter {
  /**
   * Check whether the message is valid - does the service publish it?
   */
  public static boolean validMessage (DeploymentDescriptor dd,
                                      String messageName) {
    String[] pubMessages = dd.getMethods ();
    for (int i = 0; i < pubMessages.length; i++) {
      if (messageName.equals (pubMessages[i])) {
        return true;
      }
    }
    return false;
  }

  /**
   * Deliver the message to the appropriate method on the given target
   * object.
   */

  public static void invoke (DeploymentDescriptor dd, Envelope env,
                             Object targetObject, String messageName,
                             SOAPContext reqCtx, SOAPContext resCtx)
       throws SOAPException {
    byte providerType = dd.getProviderType ();

    try {
      // Class[] argTypes = new Class[] { Envelope.class, PrintWriter.class };
      // Object[] args = new Object[] { env, out };
      Class[] argTypes = new Class[] { Envelope.class, SOAPContext.class,
                                                       SOAPContext.class };
      Object[] args = new Object[] { env, reqCtx, resCtx };

      if (providerType == DeploymentDescriptor.PROVIDER_JAVA) {
        Method m = MethodUtils.getMethod (targetObject, messageName,
                                          argTypes);
        Object resObj = m.invoke (targetObject, args);
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
        m.invoke (null, new Object[] {dd, targetObject, messageName, args});
      }
    } catch (InvocationTargetException e) {
      Throwable t = e.getTargetException();

      if (t instanceof SOAPException) {
        throw (SOAPException)t;
      } else {
        throw new SOAPException(Constants.FAULT_CODE_SERVER,
                                "Exception from service object: " +
                                t.getMessage (), t);
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
  }
}
