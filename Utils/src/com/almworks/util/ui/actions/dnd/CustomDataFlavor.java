package com.almworks.util.ui.actions.dnd;

import java.awt.datatransfer.DataFlavor;
import java.io.IOException;
import java.io.ObjectInput;

public class CustomDataFlavor extends DataFlavor {
  private String myDataClassName;
  private static final String INFIX = "; class=";

  public CustomDataFlavor(String mimeType, String displayName, Class nonObfuscatedDataClass) {
    super(addClassToMimeType(mimeType, nonObfuscatedDataClass), displayName);
    myDataClassName = nonObfuscatedDataClass.getName();
  }

  private static String addClassToMimeType(String mimeType, Class nonObfuscatedDataClass) {
    if (mimeType.indexOf(';') < 0)
      return mimeType + INFIX + nonObfuscatedDataClass.getName();
    else
      return mimeType;
  }

  public CustomDataFlavor() {
  }

  public String getParameter(String paramName) {
    if ("class".equals(paramName)) {
      if (myDataClassName == null) {
        // hack - it is initialization
        String mimeType = getMimeType();
        int k = mimeType.indexOf(INFIX);
        if (k >= 0) {
          String className = mimeType.substring(k + INFIX.length());
          return className;
        } else {
          assert false : mimeType;
        }
      }
      return myDataClassName;
    } else {
      return super.getParameter(paramName);
    }
  }

  public synchronized void readExternal(ObjectInput is) throws IOException, ClassNotFoundException {
    super.readExternal(is);
    myDataClassName = super.getParameter("class");
  }
}
