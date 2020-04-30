package com.almworks.util.components.plaf;

import com.almworks.util.Env;
import com.almworks.util.GlobalProperties;
import com.almworks.util.components.plaf.linux.LinuxPatches;
import com.almworks.util.components.plaf.patches.*;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.ui.TextImproveUtil;
import com.almworks.util.ui.UIUtil;
import com.almworks.util.ui.UndoUtil;
import org.almworks.util.Log;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.AWTEventListener;
import java.awt.event.ContainerEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

public class LAFUtil {
  private LAFUtil() {
  }

  public static void initializeLookAndFeel() {
    setupLAFProperties();
    setSystemDefaultLAF();
    installPatches();
    installExtensions();
    setupToolkit();
    installTextImprovementsSupport();
    installTextFocusTraversal();

    if (shouldInstallTextMenus()) {
      LinuxPatches.installTextMenus();
    }
  }

  private static boolean shouldInstallTextMenus() {
    return (Env.isWindows() && !isWinLafAllowed()) || Env.isMac() || Env.isLinux();
  }

  private static void installTextImprovementsSupport() {
    PropertyChangeListener listener = new PropertyChangeListener() {
      public void propertyChange(PropertyChangeEvent evt) {
        Object o = evt.getNewValue();
        installUndoSupport(o);
        installCtrlBackspaceSupport(o);
      }
    };
    KeyboardFocusManager.getCurrentKeyboardFocusManager().addPropertyChangeListener("focusOwner", listener);
    KeyboardFocusManager.getCurrentKeyboardFocusManager().addPropertyChangeListener("permanentFocusOwner", listener);
  }

  private static void installCtrlBackspaceSupport(Object o) {
    if (o instanceof JTextComponent) {
      TextImproveUtil.improveTextOverMaps(((JTextComponent) o));
    }
  }

  private static void installUndoSupport(Object o) {
    if ((o instanceof JTextComponent) && (!UndoUtil.isUndoInstalled((JComponent) o)))
      UndoUtil.addUndoSupport((JComponent) o);
  }

  private static void installTextFocusTraversal() {
    Toolkit.getDefaultToolkit().addAWTEventListener(new AWTEventListener() {
      private final JTextField source = new JTextField();
      @Override
      public void eventDispatched(AWTEvent event) {
        if(event.getID() == ContainerEvent.COMPONENT_ADDED && event instanceof ContainerEvent) {
          final Component child = ((ContainerEvent)event).getChild();
          if(child instanceof JTextArea || child instanceof JTextPane) {
            UIUtil.copyFocusTraversalKeys(source, child);
          }
        }
      }
    }, AWTEvent.CONTAINER_EVENT_MASK);
  }

  private static void setupLAFProperties() {
    if (Env.isLinux()) {
      if (!Env.getBoolean("no.metal")) {
        System.setProperty("use.metal", "true");
      }
    }
  }

  private static void setupToolkit() {
    Toolkit.getDefaultToolkit().setDynamicLayout(true);
  }

  public static void installPatches() {
    LAFExtensionManager.installExtension(new TabbedPanePatches());
    LAFExtensionManager.installExtension(new TextAreaPatches());
    LAFExtensionManager.installExtension(new TablePatches());
    LAFExtensionManager.installExtension(new FontSizeSettingPatch());
    LAFExtensionManager.installExtension(new EasternCharsetsOnWindowsPatch());
    LAFExtensionManager.installExtension(new RowHeightPatch());
  }

  public static void installExtensions() {
    LAFExtensionManager.installExtension(new StatusBarExtensions());
    LAFExtensionManager.installExtension(new ASwingExtension());
    LAFExtensionManager.installExtension(new AdditionalColorExtension());
    LAFExtensionManager.installExtension(new Aero());
  }

  private static void setSystemDefaultLAF() {
    ThreadGate.AWT_IMMEDIATE.execute(new Runnable() {
      public void run() {
        LookAndFeel laf = null;
        try {
          laf = getLookAndFeel();
          if (laf != null) {
            Log.debug("using laf " + laf);

// undocumented swing peculiarity. needed to be set sot that UIDefaults may load
// UI classes.
            UIManager.put("ClassLoader", LAFUtil.class.getClassLoader());

// another swing f**king undocumented peculiarity
            UIManager.put("Application.useSystemFontSettings", Boolean.TRUE);

            UIManager.setLookAndFeel(laf);
          } else {
            Log.debug("not using laf");
          }
        } catch (Throwable e) {
          Log.warn("failed to set look and feel " + laf, e);
        }
      }
    });
  }

  private static LookAndFeel getLookAndFeel() {
    LookAndFeel overrideLookAndFeel = getOverrideLookAndFeel();
    if (overrideLookAndFeel != null)
      return overrideLookAndFeel;
    String lafClassName = UIManager.getSystemLookAndFeelClassName();
    if (isWinLafAllowed() && lafClassName.endsWith("WindowsLookAndFeel") &&
      !Env.getBoolean(GlobalProperties.USE_METAL))
    {

      // Check for availability of "winlaf" lib, http://www.winlaf.org/
      String winlaf = "net.java.plaf.windows.WindowsLookAndFeel";
      LookAndFeel laf = instantiateLaf(winlaf);
      if (laf != null)
        return laf;
    }
    if (Env.getBoolean(GlobalProperties.USE_METAL))
      lafClassName = "javax.swing.plaf.metal.MetalLookAndFeel";
    return instantiateLaf(lafClassName);
  }

  private static boolean isWinLafAllowed() {
    if (Env.getBoolean(GlobalProperties.NO_WINLAF))
      return false;
    if (Env.getBoolean(GlobalProperties.USE_WINLAF))
      return true;
    if (Env.isWindowsVistaOrLater())
      return false; // WinLaF makes all menus drawn with black foreground on black background
    return Env.getJavaSpecificationVersion() < 6; // Windows l&f from Java 1.6 is good enough
  }

  private static LookAndFeel getOverrideLookAndFeel() {
    String className = Env.getString("swing.defaultlaf");
    if (className == null)
      return null;
    return instantiateLaf(className);
  }

  private static LookAndFeel instantiateLaf(String lafName) {
    try {
      Class lafClass = Class.forName(lafName);
      return (LookAndFeel) lafClass.newInstance();
    } catch (Exception e) {
      Log.warn("failed to load " + lafName, e);
    }
    return null;
  }

  public static boolean isAcceleratorShown() {
    Boolean b = (Boolean) UIManager.get("ToolTip.hideAccelerator");
    return b != null && !b;
  }
}
