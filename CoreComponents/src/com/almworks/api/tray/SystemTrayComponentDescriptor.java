package com.almworks.api.tray;

import com.almworks.api.exec.ApplicationManager;
import com.almworks.api.gui.MainMenu;
import com.almworks.api.gui.MainWindowManager;
import com.almworks.util.config.Configuration;
import com.almworks.util.ui.actions.ActionRegistry;
import org.almworks.util.Const;
import org.almworks.util.Log;
import org.jetbrains.annotations.Nullable;
import org.picocontainer.Startable;
import util.concurrent.SynchronizedBoolean;

import java.awt.*;
import java.lang.reflect.Method;

public class SystemTrayComponentDescriptor implements Startable, TrayIconService {
  private final ActionRegistry myRegistry;
  private final ApplicationManager myApplicationManager;
  private final SystemTrayApplicationView myView;
  private final SynchronizedBoolean myStarted = new SynchronizedBoolean(false);

  public SystemTrayComponentDescriptor(ActionRegistry registry, Configuration configuration, MainWindowManager windowManager, ApplicationManager applicationManager) {
    myRegistry = registry;
    myApplicationManager = applicationManager;
    myView = new SystemTrayApplicationView(configuration, windowManager);
  }

  public void start() {
    if (!isTrayIconSupported())
      return;
    myView.start();
    myRegistry.registerAction(MainMenu.File.USE_SYSTEM_TRAY, new UseSystemTrayAction(myView));
    myApplicationManager.addListener(new ApplicationManager.Adapter() {
      public void onBeforeExit() {
        myView.stop();
      }
    });
    myStarted.set(true);
  }

  public void displayMessage(String caption, String text) {
    if (!myStarted.get())
      return;
    myView.displayMessage(caption, text);
  }

  public void setTrayImage(@Nullable Image icon) {
    if (!myStarted.get())
      return;
    myView.setIcon(icon);
  }

  @SuppressWarnings({"CatchGenericClass"})
  private static boolean isTrayIconSupported() {
//    if (Env.isMac() || Env.isLinux())
//      return false;
    try {
      Class<?> cls_SystemTray = Class.forName("java.awt.SystemTray");
      Method m_isSupported = cls_SystemTray.getMethod("isSupported", Const.EMPTY_CLASSES);
      Object r = m_isSupported.invoke(null, Const.EMPTY_OBJECTS);
      return Boolean.TRUE.equals(r);
    } catch (Exception e) {
      Log.debug("system tray is not supported", e);
      return false;
    }
  }

  public void stop() {
  }
}
