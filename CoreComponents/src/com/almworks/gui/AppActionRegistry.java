package com.almworks.gui;

import com.almworks.appinit.AWTEventPreprocessor;
import com.almworks.appinit.EventQueueReplacement;
import com.almworks.util.config.ConfigurationException;
import com.almworks.util.config.JDOMConfigurator;
import com.almworks.util.ui.actions.ActionRegistryImpl;
import com.almworks.util.ui.actions.KeyMapLoader;
import org.almworks.util.Log;
import org.picocontainer.Startable;

import java.awt.*;
import java.awt.event.KeyEvent;

/**
 * @author dyoma
 */
public class AppActionRegistry extends ActionRegistryImpl implements Startable {
  private final AWTEventPreprocessor myPreprocessor =
    new AWTEventPreprocessor() {
      public boolean preprocess(AWTEvent event, boolean alreadyConsumed) {
        if (alreadyConsumed || !(event instanceof KeyEvent))
          return false;
        return processKeyEvent((KeyEvent) event);
      }

      @Override
      public boolean postProcess(AWTEvent event, boolean alreadyConsumed) {
        return false;
      }
    };

  public void start() {
    KeyMapLoader loader =
      new KeyMapLoader(JDOMConfigurator.parse(getClass().getClassLoader(), "com/almworks/gui/KeyMap.xml"));
    try {
      loader.registerKeyMap(this);
    } catch (ConfigurationException e) {
      Log.error(e);
    }
    synchronized (this) {
      EventQueueReplacement.ensureInstalled().addPreprocessor(myPreprocessor);
    }
  }

  public void stop() {
    synchronized (this) {
      EventQueueReplacement.detachPreprocessor(myPreprocessor);
      clearActions();
      clearKeyMap();
    }
  }
}
