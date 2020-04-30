package com.almworks.util.components.plaf;

import org.almworks.util.Util;

import javax.swing.*;
import java.awt.*;

/**
 * Supports pluggable extensions to look and feel.
 *
 * @author sereda
 */
public abstract class LAFExtension {
  protected static final String BACKUP = "patch-backup.";

  /**
   * If returns true, then install() method will be called to install values into UIDefaults
   * when this certain look-and-feel is selected.
   */
  public boolean isExtendingLookAndFeel(LookAndFeel laf) {
    return true;
  }

  /**
   * Should install additional values into UIDefaults.
   */
  public abstract void install(LookAndFeel laf);

  public void uninstall(LookAndFeel laf) {
  }

  /**
   * @return {@code true} if we're on Windows XP, Vista, or 7, and a "modern"
   * (as opposed to "classic") UI theme is used.
   */
  public static boolean isWindowsModernStyle() {
    return isStyleXP(UIManager.getLookAndFeel());
  }

  protected static boolean isWindowsLAF(LookAndFeel laf) {
    String lafClassName = laf.getClass().getName();
    return Util.upper(lafClassName.substring(lafClassName.lastIndexOf('.') + 1)).indexOf("WINDOWS") >= 0;
  }

  /**
   * Note: To check that we're on Windows Vista/7 with standard theme, we may also try this:
   *   String dllName = laf.getDesktopProperty("win.xpstyle.dllName"));
   *   standardTheme = dllName.contains("Aero");
   */
  protected static boolean isStyleXP(LookAndFeel laf) {
    if (!isWindowsLAF(laf))
      return false;
    Toolkit toolkit = Toolkit.getDefaultToolkit();
    Boolean xp = (Boolean)toolkit.getDesktopProperty("win.xpstyle.themeActive");
    return xp != null && xp.booleanValue();
  }

  protected static UIDefaults defaults() {
    return UIManager.getDefaults();
  }

  protected void save(String key) {
    defaults().put(backupKey(key), defaults().get(key));
  }

  private String backupKey(String key) {
    return BACKUP + getClass().getName() + "." + key;
  }

  protected void restore(String key) {
    String backupKey = backupKey(key);
    if (defaults().contains(backupKey)) {
      defaults().put(key, defaults().get(backupKey));
      defaults().remove(backupKey);
    }
  }
}
