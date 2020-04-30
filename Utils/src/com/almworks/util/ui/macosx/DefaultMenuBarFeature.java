package com.almworks.util.ui.macosx;

import com.almworks.util.Env;
import com.almworks.util.ui.actions.CantPerformException;
import org.almworks.util.Log;

import javax.swing.*;

public class DefaultMenuBarFeature extends AppIntegrationFeature {
  private static final String SET_DEFAULT_MENU_BAR = "setDefaultMenuBar";
  private static final Class<?>[] JMENUBAR = { JMenuBar.class };

  static DefaultMenuBarFeature create() {
    if (Env.isMac() && methodExists(EAWT_APPLICATION, SET_DEFAULT_MENU_BAR, JMENUBAR)) {
      return new DefaultMenuBarFeature();
    }
    return null;
  }

  private DefaultMenuBarFeature() {}

  public void setDefaultMenuBar(JMenuBar menuBar) {
    try {
      reflectivelyCall(null, getEawtApplication(), "setDefaultMenuBar", JMENUBAR, menuBar);
    } catch (CantPerformException e) {
      if (Env.isMac()) {
        Log.warn(e.getCause());
      }
    }
  }
}
