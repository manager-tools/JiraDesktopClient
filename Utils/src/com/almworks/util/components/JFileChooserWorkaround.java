package com.almworks.util.components;

import org.almworks.util.Util;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.io.File;

public class JFileChooserWorkaround extends JFileChooser {
  private final boolean myActive = isWindows2000orEarlier();

  public JFileChooserWorkaround(String currentDirectoryPath, FileSystemView fsv) {
    super(currentDirectoryPath, fsv);
  }

  public JFileChooserWorkaround(File currentDirectory, FileSystemView fsv) {
    super(currentDirectory, fsv);
  }

  public JFileChooserWorkaround(FileSystemView fsv) {
    super(fsv);
  }

  public JFileChooserWorkaround(String currentDirectoryPath) {
    super(currentDirectoryPath);
  }

  public JFileChooserWorkaround(File currentDirectory) {
    super(currentDirectory);
  }

  public JFileChooserWorkaround() {
  }

  public Icon getIcon(File f) {
    if (myActive && isFloppyDrive(f))
      return UIManager.getIcon("FileView.floppyDriveIcon");
    return super.getIcon(f);
  }

  public String getName(File f) {
    if (myActive && isFloppyDrive(f))
      return "Floppy Drive";
    return super.getName(f);
  }

  public String getTypeDescription(File f) {
    if (myActive && isFloppyDrive(f))
      return "Floppy Drive";
    return super.getTypeDescription(f);
  }

  public boolean isTraversable(File f) {
    if (myActive && isFloppyDrive(f))
      return true;
    return super.isTraversable(f);
  }

  private static boolean isFloppyDrive(File f) {
    String path = f.getAbsolutePath();
    if (path == null)
      return false;
    if (path.charAt(path.length() - 1) != '\\')
      path = path + "\\";
    return path.equalsIgnoreCase("A:\\") || path.equalsIgnoreCase("B:\\");
  }

  private static boolean isWindows2000orEarlier() {
    String osName = System.getProperty("os.name");
    if (Util.upper(osName).indexOf("WINDOWS") < 0)
      return false;
    String osVersion = System.getProperty("os.version");
    if (osVersion == null) {
      // return safer value if not known
      return true;
    }
    int k = osVersion.indexOf('.');
    String major = k > 0 ? osVersion.substring(0, k) : osVersion;
    String minor = k > 0 ? osVersion.substring(k + 1) : null;
    int majorNumber;
    int minorNumber;
    try {
      majorNumber = Integer.parseInt(major.trim());
      minorNumber = minor == null ? 0 : Integer.parseInt(minor.trim());
    } catch (NumberFormatException e) {
      // strange
      return true;
    }
    // Windows 2000 is version 5.0. Windows XP is version 5.1.
    return (majorNumber < 5) || (majorNumber == 5 && minorNumber == 0);
  }
}
