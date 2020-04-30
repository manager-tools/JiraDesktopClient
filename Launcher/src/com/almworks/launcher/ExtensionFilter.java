package com.almworks.launcher;

import java.io.File;
import java.io.FileFilter;
import java.util.Locale;

/**
 * :todoc:
 *
 * @author sereda
 */
class ExtensionFilter implements FileFilter {
  private final String myExtension;

  public ExtensionFilter(String extension) {
    extension = extension.toUpperCase(Locale.US);
    if (!extension.startsWith("."))
      extension = "." + extension;
    myExtension = extension;
  }

  public boolean accept(File pathname) {
    return pathname != null && pathname.isFile() && pathname.getName().toUpperCase(Locale.US).endsWith(myExtension);
  }
}
