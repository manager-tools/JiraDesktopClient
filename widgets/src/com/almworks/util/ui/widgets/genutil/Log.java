package com.almworks.util.ui.widgets.genutil;

import com.almworks.util.text.TextUtil;

import java.util.Arrays;

public class Log<T> {
  private static final Log INSTANCE = new Log();

  public static <T> Log<T> get(Class<T> eventManagerClass) {
    return INSTANCE;
  }

  public void error(T obj, Object ... message) {
    org.almworks.util.Log.error(createLogMessage(obj, message));
  }

  public void warn(T obj, Object ... message) {
    org.almworks.util.Log.warn(createLogMessage(obj, message));
  }

  private String createLogMessage(T obj, Object... message) {
    StringBuilder builder = new StringBuilder();
    String strMessage = TextUtil.separateToString(Arrays.asList(message), " ");
    return obj != null ? strMessage + " from " + String.valueOf(obj) : strMessage;
  }

  public void errorStatic(Object ... message) {
    error(null, message);
  }
}
