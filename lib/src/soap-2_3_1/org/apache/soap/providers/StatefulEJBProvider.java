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

public class StatefulEJBProvider implements Provider {
  public static String CNTXT_PROVIDER_URL = "iiop://localhost:900";
  public static String CNTXT_FACTORY_NAME =
                          "com.ibm.ejs.ns.jndi.CNInitialContextFactory";
  protected boolean isCreate = false;
  protected String ejbKey = null;
  public static String DELIM_CHAR = "@";

  private DeploymentDescriptor dd ;
  private Envelope             envelope ;
  private Call                 call ;
  private String               targetObjectURI ;
  private HttpServlet          servlet ;
  private HttpSession          session ;

  private Context contxt = null;
  private Remote remoteObjRef = null;
  private String methodName = null;
  private Vector methodParameters = null;
  private String respEncStyle = null;

  /**
   * StatefulEJBProvider constructor comment.
   */
  public StatefulEJBProvider() {
    super();
  }

  public EJBObject deSerialize(String ejbKey) throws SOAPException {
    try {
      byte[] ejbKeyBytes = Base64.decode(ejbKey);
      ByteArrayInputStream byteStream = new ByteArrayInputStream(ejbKeyBytes);
      ObjectInputStream objStream = new ObjectInputStream(byteStream);
      Handle ejbHandle = (Handle)objStream.readObject();
      objStream.close();
      return ejbHandle.getEJBObject();
    } catch (Exception e) {
      throw new SOAPException(Constants.FAULT_CODE_SERVER, e.toString()) ;
    }
  }

  public Call getCall() {
    return call;
  }

  public Context getContxt() {
    return contxt;
  }

  public DeploymentDescriptor getDd() {
    return dd;
  }

  public String getMethodName() {
    return methodName;
  }

  public Vector getMethodParameters() {
    return methodParameters;
  }

  public Remote getRemoteObjRef() {
    return remoteObjRef;
  }

  public String getRespEncStyle() {
    return respEncStyle;
  }

  public HttpServlet getServlet() {
    return servlet;
  }

  public HttpSession getSession() {
    return session;
  }

  public String getTargetObjectURI() {
    return targetObjectURI;
  }

  public static String getUniqueId(String fullURI) {
    int occurance = fullURI.indexOf(DELIM_CHAR);
    if (occurance > 0) return fullURI.substring(occurance + 1);
    return null;
  }

  protected void initialize(String url, String factory)
    throws SOAPException
  {
    if (contxt == null)
    {
      Properties properties = new Properties();

      if ((url != null) && (!url.trim().equals("")))
      {
	properties.put(Context.PROVIDER_URL, url);
      }
      if ((factory != null) && (!factory.trim().equals("")))
      {
	properties.put(Context.INITIAL_CONTEXT_FACTORY, factory);
      }

      try
      {
        contxt = new InitialContext(properties);
      }
      catch (NamingException ne)
      {
        // ErrorListener?
	System.out.println("Naming Exception caught during InitialContext creation @ " + url);
        throw new SOAPException(Constants.FAULT_CODE_SERVER,
                                "Unable to initialize context");
      }
    }
  }

  public void invoke(SOAPContext reqContext, SOAPContext resContext)
                      throws SOAPException {
    Parameter ret = null;
    Object[] args = null;
    Class[] argTypes = null;

    respEncStyle = call.getEncodingStyleURI();

    if (methodParameters != null) {
      int parametersCount = methodParameters.size ();
      args = new Object[parametersCount];
      argTypes = new Class[parametersCount];

      for (int i = 0; i < parametersCount; i++) {
        Parameter param = (Parameter) methodParameters.elementAt (i);
        args[i] = param.getValue ();
        argTypes[i] = param.getType ();

        if (respEncStyle == null) {
          respEncStyle = param.getEncodingStyleURI ();
        }
      }
    }

    if (respEncStyle == null) {
      respEncStyle = Constants.NS_URI_SOAP_ENC;
    }

    try {
      Method m = MethodUtils.getMethod (remoteObjRef, methodName, argTypes);
      Bean result = new Bean (m.getReturnType (),
                              m.invoke (remoteObjRef, args));

      if (result.type != void.class) {
        ret = new Parameter (RPCConstants.ELEM_RETURN, result.type,
                             result.value, null);
      }
    } catch (InvocationTargetException e) {
      Throwable t = e.getTargetException ();
      throw new SOAPException (Constants.FAULT_CODE_SERVER, t.getMessage(), t);
    } catch (Throwable t) {
      throw new SOAPException (Constants.FAULT_CODE_SERVER, t.getMessage(), t);
    }

    // once the super.invoke() is done, and no error occurs, the handle must be
    // serialized and stored.
    if (isCreate) {
      // The return parameter is an EJBObject (for a create method)
      //  or an Enumeration (for a find method)
      // IF this is an enumeration, we are only going to return the first
      // element
      try{
        remoteObjRef = (Remote) ret.getValue();
      } catch (ClassCastException cce) {
        // Try to cast to an enumeration:
        Enumeration enum = (Enumeration) ret.getValue();
        remoteObjRef = (Remote) enum.nextElement();
      }
      // Set the return value to null, so that the remote object is not
      // included in the response destined for the client.
      ret = null;
    }

    if (ejbKey == null) {
      serialize();
    }

    try {
      Response res = new Response( targetObjectURI,            // URI
                                   call.getMethodName(),       // Method
                                   (Parameter) ret,            // ReturnValue
                                   null,                       // Params
                                   null,                       // Header
                                   respEncStyle,               // encoding
                                   resContext );        // response soapcontext - not supported yet
      res.setFullTargetObjectURI(targetObjectURI +
                                 StatefulEJBProvider.DELIM_CHAR + ejbKey);
      Envelope env = res.buildEnvelope();
      StringWriter  sw = new StringWriter();
      env.marshall( sw, call.getSOAPMappingRegistry(), resContext );
      resContext.setRootPart( sw.toString(), Constants.HEADERVAL_CONTENT_TYPE_UTF8);
    }
    catch( Exception e ) {
      if ( e instanceof SOAPException ) throw (SOAPException ) e ;
      throw new SOAPException( Constants.FAULT_CODE_SERVER, e.toString() );
    }
  }

  public void locate(DeploymentDescriptor depDesc, 
                     Envelope env,
                     Call origCall, 
                     String methodName ,
                     String targURI, 
                     SOAPContext reqContext)
              throws SOAPException {
   
    HttpServlet servletRef = (HttpServlet) reqContext.getProperty( Constants.BAG_HTTPSERVLET );
    HttpSession sessObj    = (HttpSession) reqContext.getProperty( Constants.BAG_HTTPSESSION );

    dd              = depDesc ;
    envelope        = env ;
    call            = origCall ;
    targetObjectURI = origCall.getTargetObjectURI() ;
    servlet         = servletRef ;
    session         = sessObj ;

    this.methodName = origCall.getMethodName();
    methodParameters = origCall.getParams();

    // Check if there is a key appended to the URI;
    String fullURI = origCall.getFullTargetObjectURI();
    ejbKey = StatefulEJBProvider.getUniqueId(fullURI);

    if (ejbKey != null) {
      remoteObjRef = deSerialize(ejbKey);

      // We have obtained the necessary object.
      return;
    }

    Hashtable props = dd.getProps();

    String ContxtProviderURL = (String) props.get("ContextProviderURL");
    String ContxtFactoryName = (String) props.get("FullContextFactoryName");

    if ((ContxtProviderURL != null) || (ContxtFactoryName != null))
      initialize(ContxtProviderURL, ContxtFactoryName);
    else
      initialize(CNTXT_PROVIDER_URL, CNTXT_FACTORY_NAME);

    String homeInterfaceName = (String) props.get("FullHomeInterfaceName");
    if (homeInterfaceName == null)
      throw new SOAPException(Constants.FAULT_CODE_SERVER, "Error in Deployment Descriptor Property Settings");

    // From the Deployment Descriptor get the JNDI lookup name that is inside
    // the "java" element...
    String jndiName = (String) props.get("JNDIName");
    if ( jndiName == null ) jndiName = dd.getProviderClass();

    if ((jndiName != null) && (contxt != null)) {
      try {
        // Use service name to locate EJB home object via the contxt
        EJBHome home = (EJBHome) PortableRemoteObject.narrow(
                                     contxt.lookup(jndiName),
                                     Class.forName(homeInterfaceName));

        // Must check to see if the create method has any parameters associated
        // with it.
        // There are 2 scenarios:
        //              1) if the method name in the call is create, then it
        //                 will have parameters.
        //              2) if the method name in the call is not create, and
        //                 because no key is associated with this call, then
        //                 we assume that we can call create without any
        //                 additional parameters, and prepare to invoke the
        //                 given method.

        if (methodName.equals("create")) {
          // The method is create, so let the invoke method deal with it;
          //  just give it the home interface.
          remoteObjRef = home;
          isCreate = true;
          return;
        }

        // Method name is something other than create - so, the invoke command
        // has another directive to process; we just perform a basic 'create'
        // then.
        Method createMethod = home.getClass().getMethod("create",
                                                        new Class[0]);
        remoteObjRef = (EJBObject) createMethod.invoke((Object) home,
                                                       new Object[0]);

      } catch (Exception e) {
        throw new SOAPException(Constants.FAULT_CODE_SERVER,
                                "Error in connecting to EJB"+ e.toString());
      }
    }
  }

  public void serialize() throws SOAPException {
    try {
      ByteArrayOutputStream biteStream = new ByteArrayOutputStream();
      ObjectOutputStream objector = new ObjectOutputStream(biteStream);
      objector.writeObject(((EJBObject)remoteObjRef).getHandle());
      objector.flush();
      objector.close();

      ejbKey = Base64.encode(biteStream.toByteArray());
    } catch (Exception e) {
      throw new SOAPException(Constants.FAULT_CODE_SERVER, e.toString()) ;
    }
  }

  public void setCall(Call newCall) {
    call = newCall;
  }

  public void setContxt(Context newContxt) {
    contxt = newContxt;
  }

  public void setDd(DeploymentDescriptor newDd) {
    dd = newDd;
  }

  public void setMethodName(String newMethodName) {
    methodName = newMethodName;
  }

  public void setMethodParameters(Vector newMethodParameters) {
    methodParameters = newMethodParameters;
  }

  public void setRemoteObjRef(Remote newRemoteObjRef) {
    remoteObjRef = newRemoteObjRef;
  }

  public void setRespEncStyle(String newRespEncStyle) {
    respEncStyle = newRespEncStyle;
  }

  public void setServlet(HttpServlet newServlet) {
    servlet = newServlet;
  }

  public void setSession(HttpSession newSession) {
    session = newSession;
  }

  public void setTargetObjectURI(String newTargetObjectURI) {
    targetObjectURI = newTargetObjectURI;
  }
}
