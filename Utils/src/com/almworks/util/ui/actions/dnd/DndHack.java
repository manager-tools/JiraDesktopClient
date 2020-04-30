package com.almworks.util.ui.actions.dnd;

import org.almworks.util.Log;

import java.awt.datatransfer.DataFlavor;
import java.io.File;
import java.util.Collection;

public class DndHack {
  // got this from http://forums.java.net/jive/thread.jspa?forumID=74&threadID=8509
  public static final DataFlavor uriListFlavor = createUrlListFlavor();

  private static DataFlavor createUrlListFlavor() {
    try {
      return new DataFlavor("text/uri-list;class=java.lang.String");
    } catch (ClassNotFoundException e) {
      // can't happen
      Log.error(e);
      return DataFlavor.javaFileListFlavor;
    }
  }

  public static Object getUriListObject(Collection<File> collection) {
    // refer to RFC 2483 for the text/uri-list format
    StringBuffer sb = new StringBuffer();
    for (File file : collection) {
      sb.append(file.toURI()).append("\r\n");
    }
    return sb.toString();
  }
}
