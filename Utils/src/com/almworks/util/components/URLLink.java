package com.almworks.util.components;

import javax.swing.*;

public class URLLink extends Link {
  private String myURL;
  private boolean myEncoded;
  private String myURLText;
  private boolean myShowTooltip = true;

  public URLLink() {
    this(null, false, null);
  }

  public URLLink(String url, boolean encoded) {
    this(url, encoded, null);
  }

  public URLLink(String url, boolean encoded, String text) {
    super();
    if (url != null)
      setUrl(url, encoded);
    if (text != null)
      setText(text);
  }

  public String getUrl() {
    return myURL;
  }

  public void setUrl(String URL) {
    setUrl(URL, true);
  }

  public void setUrl(String URL, boolean encoded) {
    myURL = URL;
    myEncoded = encoded;
    updateURL();
  }

  public String getUrlText() {
    return myURLText;
  }

  public void setUrlText(String text) {
    myURLText = text;
    updateURL();
  }

  private void updateURL() {
    if (myURL != null) {
      setAction(new OpenBrowserAction(myURL, myEncoded, myURLText));
      if (myShowTooltip) {
        setToolTipText(myURL);
      }
    } else {
      setAction((Action)null);
      setToolTipText(null);
    }
  }

  public URLLink leftAligned() {
    setHorizontalAlignment(SwingConstants.LEFT);
    return this;
  }

  public boolean isShowTooltip() {
    return myShowTooltip;
  }

  public void setShowTooltip(boolean showTooltip) {
    if (myShowTooltip != showTooltip) {
      myShowTooltip = showTooltip;
      updateURL();
    }
  }
}
