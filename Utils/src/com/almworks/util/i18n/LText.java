package com.almworks.util.i18n;



public abstract class LText extends LTextBase {
  protected LText(String key, String defaultFormat) {
    super(key, defaultFormat);
  }

  public String format() {
    return loadString();
  }
}
