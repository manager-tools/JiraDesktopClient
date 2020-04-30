package com.almworks.util.ui.macosx;

import com.almworks.util.Env;
import com.almworks.util.commons.Procedure;
import com.almworks.util.ui.actions.CantPerformException;
import org.almworks.util.Log;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URI;
import java.util.Arrays;

public class OpenUriHandlerFeature extends AppIntegrationFeature {
  private static final String APP_EVENT_LISTENER = "com.apple.eawt.AppEventListener";
  private static final String OPEN_URI_HANDLER = "com.apple.eawt.OpenURIHandler";
  private static final String SET_OPEN_URI_HANDLER = "setOpenURIHandler";
  
  static OpenUriHandlerFeature create() {
    if (Env.isMac() && classExists(APP_EVENT_LISTENER) && classExists(OPEN_URI_HANDLER)) {
      return new OpenUriHandlerFeature();
    }
    return null;
  }
  
  private OpenUriHandlerFeature() {}

  public void setOpenUriHandler(Procedure<URI> uriHandler) {
    try {
      final Class<?>[] handlerIntf = { getClass(OPEN_URI_HANDLER) };
      final Object handlerProxy;
      if(uriHandler == null) {
        handlerProxy = null;
      } else {
        handlerProxy = Proxy.newProxyInstance(
          handlerIntf[0].getClassLoader(), handlerIntf, new UriInvocationHandler(uriHandler));
      }
      reflectivelyCall(null, getEawtApplication(), SET_OPEN_URI_HANDLER, handlerIntf, handlerProxy);
    } catch(CantPerformException e) {
      if (Env.isMac()) {
        Log.warn(e.getCause());
      }
    }
  }

  private static class UriInvocationHandler implements InvocationHandler {
    private final Procedure<URI> myUserCode;

    public UriInvocationHandler(Procedure<URI> userCode) {
      myUserCode = userCode;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      if("openURI".equals(method.getName()) && args.length == 1) {
        try {
          final Object uri = reflectivelyCall(null, args[0], "getURI", NO_ARGS);
          if(uri instanceof URI) {
            myUserCode.invoke((URI)uri);
          } else {
            Log.warn("OUHF: openURI(" + uri + ")");
          }
        } catch(CantPerformException e) {
          Log.warn(e);
        }
      } else {
        Log.warn("OUHF: " + method.getName() + "(" + Arrays.toString(args) + ")");
      }
      return null;
    }
  }
}
