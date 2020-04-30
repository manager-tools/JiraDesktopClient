package com.almworks.util.i18n;



public abstract class LText2<T1, T2> extends LTextBase{
  protected LText2(String key, String defaultFormat) {
    super(key, defaultFormat);
  }

  public String format(T1 param1, T2 param2) {
    return formatString(new Object[] {param1, param2});
  }
}
