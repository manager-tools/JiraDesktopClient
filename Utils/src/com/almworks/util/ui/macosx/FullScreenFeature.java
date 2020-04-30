package com.almworks.util.ui.macosx;

import com.almworks.util.Env;
import com.almworks.util.events.FireEventSupport;
import com.almworks.util.ui.actions.CantPerformException;
import org.almworks.util.Util;
import org.almworks.util.detach.Detach;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class FullScreenFeature extends AppIntegrationFeature {
  private static final String FULL_SCREEN_UTILITIES = "com.apple.eawt.FullScreenUtilities";

  private static final String SET_WINDOW_CAN_FULL_SCREEN = "setWindowCanFullScreen";
  private static final Class<?>[] WINDOW_BOOLEAN = { Window.class, boolean.class };
  private static final String ADD_FULL_SCREEN_LISTENER_TO = "addFullScreenListenerTo";
  private static final String REQUEST_TOGGLE_FULL_SCREEN = "requestToggleFullScreen";
  private static final Class<?>[] WINDOW = { Window.class };
  
  private static final String FULL_SCREEN_LISTENER = "com.apple.eawt.FullScreenListener";
  private static final String WINDOW_ENTERED_FULL_SCREEN = "windowEnteredFullScreen";
  private static final String WINDOW_ENTERING_FULL_SCREEN = "windowEnteringFullScreen";
  private static final String WINDOW_EXITED_FULL_SCREEN = "windowExitedFullScreen";
  private static final String WINDOW_EXITING_FULL_SCREEN = "windowExitingFullScreen";

  private static final Object FULL_SCREENABLE_KEY = new Object();
  private static final Object FULL_SCREEN_STATE_KEY = new Object();
  private static final Object FULL_SCREEN_HANDLER_KEY = new Object();

  private static final Map<String, FullScreenEvent.Type> EVENT_TYPES;
  static {
    Map<String, FullScreenEvent.Type> map = new HashMap<String, FullScreenEvent.Type>();
    map.put(WINDOW_ENTERED_FULL_SCREEN, FullScreenEvent.Type.ENTERED);
    map.put(WINDOW_ENTERING_FULL_SCREEN, FullScreenEvent.Type.ENTERING);
    map.put(WINDOW_EXITED_FULL_SCREEN, FullScreenEvent.Type.EXITED);
    map.put(WINDOW_EXITING_FULL_SCREEN, FullScreenEvent.Type.EXITING);
    EVENT_TYPES = Collections.unmodifiableMap(map);
  }

  static FullScreenFeature create() {
    if (Env.isMacLionOrNewer()
      && methodExists(FULL_SCREEN_UTILITIES, SET_WINDOW_CAN_FULL_SCREEN, WINDOW_BOOLEAN)
      && methodExists(EAWT_APPLICATION, REQUEST_TOGGLE_FULL_SCREEN, WINDOW)
      && classExists(FULL_SCREEN_LISTENER))
    {
      return new FullScreenFeature();
    }
    return null;
  }

  private FullScreenFeature() {}

  public void makeWindowFullScreenable(Window window) {
    if (!isWindowFullScreenable(window)) {
      try {
        reflectivelyCall(FULL_SCREEN_UTILITIES, null, SET_WINDOW_CAN_FULL_SCREEN, WINDOW_BOOLEAN, window, true);
        putProperty(window, FULL_SCREENABLE_KEY, Boolean.TRUE);
        installFullScreenHandler(window);
      } catch (CantPerformException e) {
        warn(e);
      }
    }
  }

  private void installFullScreenHandler(Window window) throws CantPerformException {
    Class<?>[] listenerIntf = { getClass(FULL_SCREEN_LISTENER) };
    Class<?>[] argClasses = { Window.class, listenerIntf[0] };
    FullScreenHandler handler = new FullScreenHandler(window);
    Object proxy = Proxy.newProxyInstance(listenerIntf[0].getClassLoader(), listenerIntf, handler);
    reflectivelyCall(FULL_SCREEN_UTILITIES, null, ADD_FULL_SCREEN_LISTENER_TO, argClasses, window, proxy);
    putProperty(window, FULL_SCREEN_HANDLER_KEY, handler);
  }

  public boolean isWindowFullScreenable(Window window) {
    return Boolean.TRUE.equals(getProperty(window, FULL_SCREENABLE_KEY, Boolean.class));
  }

  public boolean isWindowInFullScreen(Window window) {
    return isWindowFullScreenable(window)
      && FullScreenEvent.Type.ENTERED.equals(getProperty(window, FULL_SCREEN_STATE_KEY, FullScreenEvent.Type.class));
  }

  public void toggleFullScreen(Window window) {
    try {
      reflectivelyCall(null, getEawtApplication(), REQUEST_TOGGLE_FULL_SCREEN, WINDOW, window);
    } catch (CantPerformException e) {
      warn(e);
    }
  }

  public Detach addFullScreenListener(Window window, FullScreenEvent.Listener listener) {
    if (isWindowFullScreenable(window)) {
      FullScreenHandler handler = getProperty(window, FULL_SCREEN_HANDLER_KEY, FullScreenHandler.class);
      if (handler != null) {
        return handler.getEventSupport().addAWTListener(listener);
      }
    }
    return Detach.NOTHING;
  }

  private static void putProperty(Window window, Object key, Object value) {
    if (window instanceof RootPaneContainer) {
      ((RootPaneContainer) window).getRootPane().putClientProperty(key, value);
    }
  }

  private static <T> T getProperty(Window window, Object key, Class<T> clazz) {
    if (window instanceof RootPaneContainer) {
      return Util.castNullable(clazz, ((RootPaneContainer) window).getRootPane().getClientProperty(key));
    }
    return null;
  }

  private static class FullScreenHandler implements InvocationHandler {
    private final Window myTarget;
    private final FireEventSupport<FullScreenEvent.Listener> myEventSupport = FireEventSupport.create(FullScreenEvent.Listener.class);

    public FullScreenHandler(Window target) {
      myTarget = target;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {
      FullScreenEvent.Type type = EVENT_TYPES.get(method.getName());
      if (type != null && args.length == 1) {
        try {
          Window window = Util.castNullable(Window.class, reflectivelyCall(null, args[0], "getWindow", NO_ARGS));
          if (window == myTarget) {
            putState(window, type);
            fixBottom(window, type);
            fireEvent(window, type);
          }
        } catch (CantPerformException e) {
          warn(e);
        }
      }
      return null;
    }

    private void putState(Window window, FullScreenEvent.Type type) {
      putProperty(window, FULL_SCREEN_STATE_KEY, type);
    }

    private void fixBottom(final Window window, FullScreenEvent.Type type) {
      if (type == FullScreenEvent.Type.ENTERED) {
        SwingUtilities.invokeLater(new Runnable() {
          @Override
          public void run() {
            window.setSize(window.getWidth(), window.getHeight() + window.getInsets().top);
          }
        });
      }
    }

    private void fireEvent(Window window, FullScreenEvent.Type type) {
      myEventSupport.getDispatcher().onFullScreenEvent(new FullScreenEvent(window, type));
    }

    public FireEventSupport<FullScreenEvent.Listener> getEventSupport() {
      return myEventSupport;
    }
  }
}
