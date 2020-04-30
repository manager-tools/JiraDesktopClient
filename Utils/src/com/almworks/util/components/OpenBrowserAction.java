package com.almworks.util.components;

import com.almworks.util.files.ExternalBrowser;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * @author : Dyoma
 */
public class OpenBrowserAction extends AbstractAction {
  private final String myUrl;
  private final boolean myEncoded;

  public OpenBrowserAction(String url, boolean encoded, String text) {
    myUrl = url;
    myEncoded = encoded;
    if (text != null)
      putValue(NAME, text);
  }

  public Object getValue(String key) {
    if (NAME.equals(key)) {
      String nameValue = (String) super.getValue(key);
      return nameValue == null || nameValue.trim().length() == 0 ? myUrl : nameValue;
    } else
      return super.getValue(key);
  }

  public void actionPerformed(ActionEvent e) {
    ExternalBrowser.openURL(myUrl, myEncoded);
  }
}
