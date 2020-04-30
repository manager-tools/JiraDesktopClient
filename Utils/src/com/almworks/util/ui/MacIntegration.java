package com.almworks.util.ui;

import com.almworks.util.Env;
import com.almworks.util.ExceptionHandler;
import com.almworks.util.commons.Procedure;
import com.almworks.util.ui.macosx.*;
import org.almworks.util.Log;
import org.almworks.util.detach.Detach;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.concurrent.*;

/**
 * Utility class for integrating the application into Mac OS X.
 * Uses Apple extensions, but makes all calls reflectively.
 */
public class MacIntegration {
  /**
   * Install a Mac-specific handler that calls event-specific
   * handler {@code Runnable}s. and {@code Procedure}s.
   * @see #setQuitHandler(Runnable)
   * @see #setAboutHandler(Runnable)
   * @see #setReopenHandler(Runnable)
   * @see #setOpenUriHandler(Procedure)
   */
  public static void installMacHandlers() {
    BasicHandlersFeature feature = AppIntegrationFeature.getFeature(BasicHandlersFeature.class);
    if (feature != null) {
      feature.installMacHandlers();
    }
  }

  /**
   * Set a {@code Runnable} to be called when the user selects
   * "Quit ApplicationName" from Mac OS X Application menu or
   * presses Command-Q.
   * @param quitHandler The {@code Runnable} to call.
   */
  public static void setQuitHandler(Runnable quitHandler) {
    BasicHandlersFeature feature = AppIntegrationFeature.getFeature(BasicHandlersFeature.class);
    if (feature != null) {
      feature.setQuitHandler(quitHandler);
    }
  }

  /**
   * Set a {@code Runnable} to be called when the user selects
   * "About ApplicationName" from Mac OS X Application menu.
   * @param aboutHandler The {@code Runnable} to call.
   */
  public static void setAboutHandler(Runnable aboutHandler) {
    BasicHandlersFeature feature = AppIntegrationFeature.getFeature(BasicHandlersFeature.class);
    if (feature != null) {
      feature.setAboutHandler(aboutHandler);
    }
  }

  /**
   * Set a {@code Runnable} to be called when the user reopens
   * the application (double clicks a running app in the Finder
   * or clicks its icon in the Dock).
   * @param reopenHandler The {@code Runnable} to call.
   */
  public static void setReopenHandler(Runnable reopenHandler) {
    BasicHandlersFeature feature = AppIntegrationFeature.getFeature(BasicHandlersFeature.class);
    if (feature != null) {
      feature.setReopenHandler(reopenHandler);
    }
  }

  /**
   * Set a {@code Procedure} to be called when the application
   * is asked to open an URI by some other Mac OS X application.
   * @param uriHandler The {@code Procedure} to call.
   */
  public static void setOpenUriHandler(Procedure<URI> uriHandler) {
    OpenUriHandlerFeature feature = AppIntegrationFeature.getFeature(OpenUriHandlerFeature.class);
    if (feature != null) {
      feature.setOpenUriHandler(uriHandler);
    }
  }

  /**
   * @return whether a handler can be installed for Mac OS X system-wide "open URI" events.
   */
  public static boolean isOpenUriHandlerSupported() {
    return AppIntegrationFeature.getFeature(OpenUriHandlerFeature.class) != null;
  }

  /**
   * Set a menu bar to use when no frames are active.
   * @param menuBar The menu bar.
   */
  public static void setDefaultMenuBar(JMenuBar menuBar) {
    DefaultMenuBarFeature feature = AppIntegrationFeature.getFeature(DefaultMenuBarFeature.class);
    if (feature != null) {
      feature.setDefaultMenuBar(menuBar);
    }
  }

  /**
   * @return whether full-screen application mode is supported on the current version of Mac OS X and Java.
   */
  public static boolean isFullScreenSupported() {
    return AppIntegrationFeature.getFeature(FullScreenFeature.class) != null;
  }

  /**
   * Marks the given window as able to switch to full-screen mode on Mac OS X Lion.
   * @param window The window.
   */
  public static void makeWindowFullScreenable(Window window) {
    FullScreenFeature feature = AppIntegrationFeature.getFeature(FullScreenFeature.class);
    if (feature != null) {
      feature.makeWindowFullScreenable(window);
    }
  }

  /**
   * @param window The window.
   * @return Whether the window is able to switch to full-screen mode on Mac OS X Lion.
   */
  public static boolean isWindowFullScreenable(Window window) {
    FullScreenFeature feature = AppIntegrationFeature.getFeature(FullScreenFeature.class);
    return feature != null && feature.isWindowFullScreenable(window);
  }

  /**
   * Toggle the given window's full-screen state.
   * @param window The window.
   */
  public static void toggleFullScreen(Window window) {
    FullScreenFeature feature = AppIntegrationFeature.getFeature(FullScreenFeature.class);
    if (feature != null) {
      feature.toggleFullScreen(window);
    }
  }

  /**
   * @param window The window.
   * @return Whether the window is in full-screen mode.
   */
  public static boolean isWindowInFullScreen(Window window) {
    FullScreenFeature feature = AppIntegrationFeature.getFeature(FullScreenFeature.class);
    return feature != null && feature.isWindowInFullScreen(window);
  }

  /**
   * Adds a listener that will be called whenever the given window's full-screen state changes.
   * @param window The window.
   * @param listener The listener.
   * @return The detach that removes the listener.
   */
  public static Detach addFullScreenListener(Window window, FullScreenEvent.Listener listener) {
    FullScreenFeature feature = AppIntegrationFeature.getFeature(FullScreenFeature.class);
    if (feature != null) {
      return feature.addFullScreenListener(window, listener);
    }
    return Detach.NOTHING;
  }

  public static class EventCancelled extends RuntimeException {}

  public static void cancelEvent() {
    throw new EventCancelled();
  }

  /* AppleScript support. */

  private static final ExecutorService myScriptExecutor;
  private static final Object myScriptLock;
  private static ScriptEngine myScriptEngine;

  static {
    if(Env.isMac()) {
      myScriptLock = new Object();
      myScriptExecutor = new ThreadPoolExecutor(0, 1, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
      myScriptExecutor.submit(new Runnable() {
        @Override
        public void run() {
          final ScriptEngine appleScript = new ScriptEngineManager().getEngineByName("AppleScript");
          if(appleScript == null) {
            Log.warn("MacIntegration: AppleScript engine is not available.");
            return;
          }
          synchronized(myScriptLock) {
            myScriptEngine = appleScript;
          }
        }
      });
    } else {
      myScriptLock = null;
      myScriptExecutor = null;
    }
  }

  public static Future<Boolean> submitAppleScript(final String code) {
    if(!Env.isMac()) {
      return null;
    }

    assert myScriptLock != null;
    assert myScriptExecutor != null;

    synchronized(myScriptLock) {
      if(myScriptEngine == null) {
        Log.warn("MacIntegration: AppleScript engine is not available.");
        return null;
      }
    }

    return myScriptExecutor.submit(new Callable<Boolean>() {
      @Override
      public Boolean call() throws Exception {
        synchronized(myScriptLock) {
          assert myScriptEngine != null;
          myScriptEngine.eval(code);
          return true;
        }
      }
    });
  }

  public static boolean evalAppleScript(String code, long timeout, TimeUnit unit) {
    final Future<Boolean> future = submitAppleScript(code);
    if(future == null) {
      return false;
    }

    try {
      return timeout < 0L ? future.get() : future.get(timeout, unit);
    } catch(TimeoutException te) {
      Log.warn(te);
    } catch(ExecutionException ee) {
      Log.warn(ee);
    } catch(InterruptedException ie) {
      Thread.currentThread().interrupt();
    }
    return false;
  }

  public static boolean sendActivate() {
    return evalAppleScript("activate", 1L, TimeUnit.SECONDS);
  }

  /* "Reveal in Finder" support. */

  private static final Method myRevealInFinder = getRevealInFinderMethod();

  private static Method getRevealInFinderMethod() {
    if(!Env.isMac()) {
      return null;
    }
    try {
      return Class.forName("com.apple.eio.FileManager").getMethod("revealInFinder", File.class);
    } catch(Exception e) {
      Log.debug("MacIntegration: revealInFinder() unavailable");
      return null;
    }
  }

  public static boolean isRevealInFinderSupported() {
    return myRevealInFinder != null;
  }

  public static void revealInFinder(File file) {
    revealInFinder(file, ExceptionHandler.WARN);
  }

  public static void revealInFinder(File file, ExceptionHandler handler) {
    try {
      myRevealInFinder.invoke(null, file);
    } catch(Exception e) {
      handler.handle(e);
    }
  }
}

