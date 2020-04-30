package org.apache.soap.providers;

import java.io.* ;
import java.util.* ;
import java.rmi.Remote;
import javax.servlet.* ;
import javax.servlet.http.* ;
import org.apache.soap.* ;
import org.apache.soap.rpc.* ;
import org.apache.soap.server.* ;
import org.apache.soap.util.* ;
import org.apache.soap.encoding.soapenc.Base64;

import java.lang.reflect.*;
import javax.rmi.*;
import javax.ejb.*;
import javax.naming.*;
import javax.naming.Context.*;

public class EntityEJBProvider extends StatefulEJBProvider {
  public EntityEJBProvider() {
    super();
  }

  /**
   * locate method comment.
   */
  public void locate(DeploymentDescriptor depDesc, 
                     Envelope env,
                     Call origCall, 
                     String methodName,
                     String targetObjectURI, 
                     SOAPContext reqContext) throws SOAPException {

    HttpServlet servletRef = (HttpServlet) reqContext.getProperty( Constants.BAG_HTTPSERVLET );
    HttpSession sessObj    = (HttpSession) reqContext.getProperty( Constants.BAG_HTTPSESSION );

    setDd(depDesc);
    setCall(origCall);
    setTargetObjectURI(origCall.getTargetObjectURI()) ;
    setServlet(servletRef);
    setSession(sessObj);
    setMethodName(origCall.getMethodName());
    setMethodParameters(origCall.getParams());

    // Check if there is a key appended to the URI;
    String fullURI = origCall.getFullTargetObjectURI();
    ejbKey = StatefulEJBProvider.getUniqueId(fullURI);

    if (ejbKey != null) {
      setRemoteObjRef(deSerialize(ejbKey));
      // We have obtained the necessary object.
      return;
    }

    Hashtable props = depDesc.getProps();

    String ContxtProviderURL = (String) props.get("ContextProviderURL");
    String ContxtFactoryName = (String) props.get("FullContextFactoryName");

    if ((ContxtProviderURL != null) || (ContxtFactoryName != null))
      initialize(ContxtProviderURL, ContxtFactoryName);
    else
      initialize(CNTXT_PROVIDER_URL, CNTXT_FACTORY_NAME);

    String homeInterfaceName = (String) props.get("FullHomeInterfaceName");
    if (homeInterfaceName == null)
      throw new SOAPException(Constants.FAULT_CODE_SERVER,
                   "Error in Deployment Descriptor Property Settings");

    // From the Deployment Descriptor get the JNDI lookup name that is inside
    // the "java" element...
    String jndiName = (String) props.get("JNDIName");
    if ( jndiName == null ) jndiName = depDesc.getProviderClass();

    if ((jndiName != null) && (getContxt() != null)) {
      try {
        // Use service name to locate EJB home object via the contxt
        EJBHome home = (EJBHome) PortableRemoteObject.narrow(
                                   getContxt().lookup(jndiName),
                                   Class.forName(homeInterfaceName));

        // Must check to see if the create method has any parameters associated
        // with it.
        // There are 2 scenarios:
        //   1) if the method name in the call is create or find, then it will
        //      have parameters.
        //   2) if the method name in the call is not create or find, and
        //      because no key is associated with this call, then we assume
        //      that we can call create without any additional parameters,
        //      and prepare to invoke the given method.

        if ( getMethodName().equals("create") ||
             getMethodName().startsWith("find") ) {
          // The method is create, so let the invoke method deal with it;
          //  just give it the home interface.
          setRemoteObjRef(home);
          isCreate = true;
          return;
        } else {
          // Method name is something other than create or find - so, the
          // invoke command has another directive to process; we just perform
          //  a basic 'create' then.
          Method createMethod = home.getClass().getMethod("create",
                                                          new Class[0]);
          setRemoteObjRef((EJBObject) createMethod.invoke((Object) home,
                                                          new Object[0]));
        }
      } catch (Exception e) {
        throw new SOAPException(Constants.FAULT_CODE_SERVER,
                                "Error in connecting to EJB", e);
      }
    }
  }
}
