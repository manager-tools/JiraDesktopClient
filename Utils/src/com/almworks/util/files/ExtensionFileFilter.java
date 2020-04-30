package com.almworks.util.files;

import com.almworks.util.Env;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

public class ExtensionFileFilter extends javax.swing.filechooser.FileFilter implements java.io.FileFilter {
  private final boolean myWindows2000orEarlier = Env.isWindows2000orEarlier();

  private final String myExtension;
  private final String myFilterName;
  private final Object myUserData;
  private final String myUpperExtension;
  private final boolean myAcceptAllDirectories;

  public ExtensionFileFilter(@NotNull String extension, @NotNull String filterName, @Nullable Object userData,
    boolean acceptAllDirectories)
  {
    myAcceptAllDirectories = acceptAllDirectories;
    assert extension != null;
    assert extension.length() > 0;
    myExtension = extension;
    myFilterName = filterName;
    myUserData = userData;
    if (extension.charAt(0) != '.') {
      extension = '.' + extension;
    }
    extension = Util.upper(extension);
    myUpperExtension = extension;
  }

  public ExtensionFileFilter(String extension, String filterName, boolean acceptAllDirectories) {
    this(extension, filterName, null, acceptAllDirectories);
  }

  public ExtensionFileFilter(String extension, boolean acceptAllDirectories) {
    this(extension, extension + " files", acceptAllDirectories);
  }

  public boolean accept(File f) {
    if (f == null)
      return false;
    if (myWindows2000orEarlier && isWindowsFloppy(f)) {
      // win2k safeguard
      return true;
    }
    if (!f.isFile())
      return myAcceptAllDirectories;
    String name = f.getName();
    if (name == null)
      return true;
    return Util.upper(name).endsWith(myUpperExtension);
  }

  public String getDescription() {
    return myFilterName + " (*" + Util.lower(myUpperExtension) + ")";
  }

  public Object getUserData() {
    return myUserData;
  }

  public String getExtension() {
    return myExtension;
  }

  public static boolean isWindowsFloppy(File file) {
    String name = file.getName();
    if (name == null)
      return false;
    return "a:".equalsIgnoreCase(name) || "b:".equalsIgnoreCase(name);
  }
}
