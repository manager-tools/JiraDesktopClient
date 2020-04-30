package com.almworks.util.ui;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * @author Vasya
 */
public interface StringChecker {
  public boolean isTextValid(String text);

  public static StringChecker IS_NOT_EMPTY = new StringChecker() {
    public boolean isTextValid(String text) {
      return text.length() > 0;
    }
  };

  public static StringChecker IS_URL = new StringChecker() {
    public boolean isTextValid(String text) {
      try {
        new URI(new URL(text).toString());
        return true;
      } catch (MalformedURLException e) {
      } catch (URISyntaxException e) {
      }
      return false;
    }
  };
}
