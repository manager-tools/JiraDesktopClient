package com.almworks.util.i18n;



public abstract class LText1<T1> extends LTextBase {
  protected LText1(String key, String defaultFormat) {
    super(key, defaultFormat);
  }

  public String format(T1 param1) {
    return formatString(new Object[] {param1});
  }
}
