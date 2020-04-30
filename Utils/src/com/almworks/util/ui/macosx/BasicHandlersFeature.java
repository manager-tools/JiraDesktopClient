package com.almworks.util.ui.macosx;

import com.almworks.util.Env;
import com.almworks.util.ui.MacIntegration;
import com.almworks.util.ui.actions.CantPerformException;
import org.almworks.util.Log;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class BasicHandlersFeature extends AppIntegrationFeature {
  /** Whether the ApplicationHandler had already been installed or not. */
  private volatile boolean myHandlerInstalled = false;

  /** A map from method names in com.apple.eawt.ApplicationListener to Runnables that get called. */
  private final Map<String, Runnable> myHandlerMap = Collections.synchronizedMap(new HashMap<String, Runnable>());

  static BasicHandlersFeature create() {
    if (Env.isMac() && classExists(EAWT_APPLICATION)) {
      return new BasicHandlersFeature();
    }
    return null;
  }

  private BasicHandlersFeature() {}

  public void installMacHandlers() {
    if (myHandlerInstalled) {
      return;
    }

    try {
      final Class<?>[] appListenerIntf = { getClass("com.apple.eawt.ApplicationListener") };

      final Object listenerProxy = Proxy.newProxyInstance(appListenerIntf[0].getClassLoader(), appListenerIntf,
        new InvocationHandler() {
          public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            final Runnable handler = myHandlerMap.get(method.getName());

            boolean handled = false;
            if (handler != null) {
              try {
                handler.run();
                handled = true;
              } catch (MacIntegration.EventCancelled e) {
                handled = false;
              }
            }

            try {
              reflectivelyCall(null, args[0], "setHandled", new Class<?>[] {Boolean.TYPE}, handled);
            } catch (CantPerformException e) {
              if (Env.isMac()) {
                Log.debug(e.getCause());
              }
            }
            return null;
          }
        });

      reflectivelyCall(null, getEawtApplication(), "addApplicationListener", appListenerIntf, listenerProxy);
    } catch(CantPerformException e) {
      if (Env.isMac()) {
        Log.warn(e.getCause());
      }
    } finally {
      myHandlerInstalled = true;
    }
  }

  public void setQuitHandler(Runnable quitHandler) {
    myHandlerMap.put("handleQuit", quitHandler);
  }

  public void setAboutHandler(Runnable aboutHandler) {
    myHandlerMap.put("handleAbout", aboutHandler);
  }

  public void setReopenHandler(Runnable reopenHandler) {
    myHandlerMap.put("handleReOpenApplication", reopenHandler);
  }
}
