package com.almworks.api.tray;

import com.almworks.api.exec.ApplicationManager;
import com.almworks.api.gui.MainWindowManager;
import com.almworks.api.install.Setup;
import com.almworks.timetrack.api.TimeTrackerWindow;
import com.almworks.util.Env;
import com.almworks.util.collections.Modifiable;
import com.almworks.util.collections.SimpleModifiable;
import com.almworks.util.config.Configuration;
import com.almworks.util.exec.Context;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.images.Icons;
import com.almworks.util.threads.Threads;
import com.almworks.util.ui.actions.ActionUtil;
import org.almworks.util.Log;
import org.almworks.util.Util;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class SystemTrayApplicationView {
  private static final String SETTING_ACTIVE = "active";

  private final Configuration myConfig;
  private final MainWindowManager myWindowManager;
  private final SimpleModifiable myModifiable = new SimpleModifiable();

  private boolean myActive;
  private Object myTrayIcon;

  private static Class<?> ourClass_TrayIcon;
  private static Class<?> ourClass_SystemTray;
  private Image myImageOverride;
  private Dimension myIconSize;

  public SystemTrayApplicationView(Configuration config, MainWindowManager windowManager) {
    myConfig = config;
    myWindowManager = windowManager;
  }

  public void setActive(boolean active) {
    Threads.assertAWTThread();
    if (active == myActive)
      return;
    try {
      if (active) {
        Object trayIcon = getTrayIcon();
        addTrayIcon(trayIcon);
      } else {
        removeFromTray();
      }
      myWindowManager.setHideOnMinimizeAndClose(active);
      myActive = active;
      myConfig.setSetting(SETTING_ACTIVE, active);
    } catch (CompatibilityException e) {
      Log.debug(e);
    }
    myModifiable.fireChanged();
  }

  private void removeFromTray() throws CompatibilityException {
    Object icon = myTrayIcon;
    if (icon != null) {
      removeTrayIcon(icon);
    }
  }

  private static void addTrayIcon(Object trayIcon) throws CompatibilityException {
    try {
      Class<?> cls_SystemTray = getSystemTrayClass();
      Object tray = cls_SystemTray.getMethod("getSystemTray").invoke(null);
      cls_SystemTray.getMethod("add", getTrayIconClass()).invoke(tray, trayIcon);
    } catch (IllegalAccessException e) {
      throw new CompatibilityException(e);
    } catch (InvocationTargetException e) {
      throw new CompatibilityException(e);
    } catch (NoSuchMethodException e) {
      throw new CompatibilityException(e);
    }
  }

  private static void removeTrayIcon(Object trayIcon) throws CompatibilityException {
    try {
      Class<?> cls_SystemTray = getSystemTrayClass();
      Object tray = cls_SystemTray.getMethod("getSystemTray").invoke(null);
      cls_SystemTray.getMethod("remove", getTrayIconClass()).invoke(tray, trayIcon);
    } catch (IllegalAccessException e) {
      throw new CompatibilityException(e);
    } catch (InvocationTargetException e) {
      throw new CompatibilityException(e);
    } catch (NoSuchMethodException e) {
      throw new CompatibilityException(e);
    }
  }

  private Object getTrayIcon() throws CompatibilityException {
    Threads.assertAWTThread();
    if (myTrayIcon != null)
      return myTrayIcon;

    PopupMenu menu = createTrayMenu();

    Object icon = null;
    try {
      Class<?> cls_SystemTray = getSystemTrayClass();
      Object tray = cls_SystemTray.getMethod("getSystemTray").invoke(null);
      Object dim = cls_SystemTray.getMethod("getTrayIconSize").invoke(tray);
      if (dim != null && !(dim instanceof Dimension))
        throw new CompatibilityException("[" + dim + "]");
      myIconSize = (Dimension) dim;
      Image image = createTrayImage();

      Class<?> cls_TrayIcon = getTrayIconClass();
      Constructor<?> cons_TrayIcon = cls_TrayIcon.getConstructor(Image.class, String.class, PopupMenu.class);
      icon = cons_TrayIcon.newInstance(image, Setup.getProductName(), menu);
//      cls_TrayIcon.getMethod("setImageAutoSize", boolean.class).invoke(icon, true);

      cls_TrayIcon.getMethod("addMouseListener", MouseListener.class).invoke(icon, new MouseAdapter() {
        public void mouseClicked(MouseEvent e) {
          if (e.getButton() == MouseEvent.BUTTON1) {
            if (e.getClickCount() == 1) {
              TimeTrackerWindow ttw = Context.get(TimeTrackerWindow.class);
              if (ttw == null || !ttw.shouldPreferTimeTrackingWindowForTray()) {
                myWindowManager.bringToFront();
              } else {
                ttw.show();
              }
            } else {
              myWindowManager.bringToFront();
            }
          }
        }
      });
    } catch (NoSuchMethodException e) {
      throw new CompatibilityException(e);
    } catch (InstantiationException e) {
      throw new CompatibilityException(e);
    } catch (IllegalAccessException e) {
      throw new CompatibilityException(e);
    } catch (InvocationTargetException e) {
      throw new CompatibilityException(e);
    }

    myTrayIcon = icon;
    return icon;
  }

  private PopupMenu createTrayMenu() {
    Font font = new JMenuItem().getFont();
    PopupMenu menu = new PopupMenu();
    addMenuItem(menu, font, "Open " + Setup.getProductName(), new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        myWindowManager.bringToFront();
      }
    });
    addMenuItem(menu, font, "Open Time Tracker", new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        TimeTrackerWindow window = Context.get(TimeTrackerWindow.class);
        if (window != null)
          window.show();
      }
    });
    menu.addSeparator();
    addMenuItem(menu, font, Env.isMac() ? "Quit" : "Exit", new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        ActionUtil.performSafe(ApplicationManager.EXIT_ACTION, myWindowManager.getMainFrame().getRootPane());
      }
    });
    return menu;
  }

  private static void addMenuItem(PopupMenu menu, Font font, String name, ActionListener action) {
    MenuItem itemShow = new MenuItem(name);
    itemShow.addActionListener(action);
    if (font != null) itemShow.setFont(font);
    menu.add(itemShow);
  }

  private Image createTrayImage() {
    Dimension size = myIconSize;
    if (size == null)
      size = new Dimension(16, 16);
    Image image = myImageOverride == null ? Icons.APPLICATION_LOGO_ICON_SMALL.getImage() : myImageOverride;
    if (size.width != 16 || size.height != 16) {
      image = image.getScaledInstance(size.width, size.height, Image.SCALE_SMOOTH);
      image = new ImageIcon(image).getImage();
    }
    return image;
  }

  private static Class<?> getTrayIconClass() throws CompatibilityException {
    Class<?> cls = ourClass_TrayIcon;
    if (cls == null) {
      try {
        cls = Class.forName("java.awt.TrayIcon");
      } catch (ClassNotFoundException e) {
        throw new CompatibilityException(e);
      }
      ourClass_TrayIcon = cls;
    }
    return cls;
  }

  private static Class<?> getSystemTrayClass() throws CompatibilityException {
    Class<?> cls = ourClass_SystemTray;
    if (cls == null) {
      try {
        cls = Class.forName("java.awt.SystemTray");
      } catch (ClassNotFoundException e) {
        throw new CompatibilityException(e);
      }
      ourClass_SystemTray = cls;
    }
    return cls;
  }

  public void start() {
    ThreadGate.AWT.execute(new Runnable() {
      public void run() {
        setActive(myConfig.getBooleanSetting(SETTING_ACTIVE, false));
      }
    });
  }

  public Modifiable getModifiable() {
    return myModifiable;
  }

  public boolean isActive() {
    return myActive;
  }

  public void stop() {
    try {
      removeFromTray();
    } catch (CompatibilityException e) {
      // ignore
    }
  }

  public void displayMessage(String caption, String text) {
    Threads.assertAWTThread();
    Object icon = myTrayIcon;
    if (icon == null) return;
    try {
      Class<? extends Enum> cls_messageType = getMessageTypeClass();
      Enum t = getInfoMessageType(cls_messageType);
      Method displayMessage = getTrayIconClass().getMethod("displayMessage", String.class, String.class, cls_messageType);
      displayMessage.invoke(icon, caption, text, t);
    } catch (Exception e) {
      Log.warn("cannot display notification", e);
    }
  }

  private Enum getInfoMessageType(Class<? extends Enum> cls_messageType) {
    Enum[] types = cls_messageType.getEnumConstants();
    Enum t = null;
    for (Enum type : types) {
      if ("INFO".equals(type.name())) {
        t = type;
        break;
      }
    }
    return t;
  }

  private static Class<? extends Enum> getMessageTypeClass() throws ClassNotFoundException, CompatibilityException {
    Class<? extends Enum> cls_messageType= (Class<? extends Enum>) Class.forName("java.awt.TrayIcon$MessageType");
    if (!cls_messageType.isEnum()) throw new CompatibilityException();
    return cls_messageType;
  }

  public void setIcon(Image image) {
    Threads.assertAWTThread();
    if (Util.equals(myImageOverride, image)) return;
    myImageOverride = image;
    Object trayIcon = myTrayIcon;
    if (trayIcon == null) return;
    Image updatedImage = createTrayImage();
    try {
      Method method = trayIcon.getClass().getMethod("setImage", Image.class);
      method.invoke(trayIcon, updatedImage);
    } catch (Exception e) {
      Log.warn(e);
    } 
  }
}
