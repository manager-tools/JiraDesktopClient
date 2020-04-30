package com.almworks.util;

import com.almworks.util.text.TextUtil;
import org.almworks.util.*;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class LogHelper {
  private static final ThreadLocal<List<Throwable>> invocationContext = new ThreadLocal<List<Throwable>>();
  private static final ThreadLocal<List<String>> contextHints = new ThreadLocal<>();

  /** @return true so that this method can be used only when assertions are on */
  public static boolean assertError(boolean condition, Object ... messages) {
    if (!condition) error(messages);
    return true;
  }
  
  /** @return {@code condition} */
  public static boolean checkError(boolean condition, Object ... messages) {
    if (!condition) error(messages);
    return condition;
  }

  /** @return true so that this method can be used only when assertions are on */
  public static boolean assertWarning(boolean condition, Object ... messages) {
    if (!condition) warning(messages);
    return true;
  }

  /** @return {@code condition} */
  public static boolean checkWarn(boolean condition, Object ... messages) {
    if (!condition) warning(messages);
    return condition;
  }

  public static <T> T assertNotNull(@Nullable T object, Object ... messages) {
    assertError(object != null, messages);
    return object;
  }

  public static void error(Object ... messages) {
    log(Level.SEVERE, messages);
  }

  public static void warning(Object ... message) {
    log(Level.WARNING, message);
  }

  public static void debug(Object ... message) {
    log(Level.INFO, message);
  }

  public static void log(Level level, Object ... message) {
    Logger logger = Log.getApplicationLogger();
    if (!logger.isLoggable(level)) return;
    if (message == null) message = Const.EMPTY_OBJECTS;
    StringBuilder builder = new StringBuilder();
    appendHint(builder);
    Throwable t = buildMessage(builder, message);
    String textMessage = builder.toString();
    if (t == null && level.intValue() > Level.WARNING.intValue()) t = getMark(textMessage);
    LogRecord record = new LogRecord(level, textMessage);
    record.setThrown(t);
    record.setSourceClassName("");
    record.setSourceMethodName("");
    logger.log(record);
  }

  public static boolean isDebugLoggable() {
    return isLoggable(Level.INFO);
  }

  public static boolean isLoggable(Level level) {
    return Log.getApplicationLogger().isLoggable(level);
  }

  @Nullable
  public static Throwable buildMessage(StringBuilder builder, Object ... message) {
    Throwable t = null;
    for (Object o : message) {
      if (t == null) {
        //noinspection ThrowableResultOfMethodCallIgnored
        t = Util.castNullable(Throwable.class, o);
        if (t != null) continue;
      }
      if (builder.length() > 0) builder.append(' ');
      objToStrting(builder, o);
    }
    return t;
  }

  private static void objToStrting(StringBuilder builder, Object o) {
    if (o == null) builder.append("<null>");
    else if (o.getClass().isArray()) ArrayUtil.anyArrayToString(builder, o);
    else builder.append(o);
  }

  public static void pushContext(Throwable mark) {
    List<Throwable> context = invocationContext.get();
    if (context == null) {
      context = Collections15.arrayList();
      invocationContext.set(context);
    }
    context.add(mark);
  }

  public static void popContext(Throwable mark) {
    List<Throwable> context = invocationContext.get();
    if (context == null) return;
    int index = context.lastIndexOf(mark);
    if (index < 0) return;
    while (context.size() > index) context.remove(context.size() - 1);
    if (context.isEmpty()) invocationContext.set(null);
  }

  public static Throwable getMark() {
    return getMark("");
  }

  public static Throwable getMark(String message) {
    final List<Throwable> context = invocationContext.get();
    final Throwable prev = context != null && !context.isEmpty() ? context.get(context.size() - 1) : null;
    final Throwable mark = new Throwable(message, prev);

    final String logHelper = LogHelper.class.getName();
    final StackTraceElement[] trace = mark.getStackTrace();
    int junk;
    for(junk = 0; logHelper.equals(trace[junk].getClassName()); junk++);

    mark.setStackTrace(Arrays.copyOfRange(trace, junk, trace.length));
    return mark;
  }

  private static void appendHint(StringBuilder builder) {
    List<String> hints = contextHints.get();
    if (hints == null || hints.isEmpty()) return;
    builder.append("(Hints:").append(TextUtil.separateToString(hints, ", ")).append(") ");
  }

  /**
   * Appends additional info to every log message logged by the operation.
   * @param hint the additional message
   * @param operation the operation to run with additional log info
   * @return the result of the operation run
   */
  public static <T> T withHint(String hint, Supplier<T> operation) {
    List<String> hints = contextHints.get();
    if (hints == null) {
      hints = new ArrayList<>();
      contextHints.set(hints);
    }
    int initialSize = hints.size();
    hints.add(hint);
    try {
      return operation.get();
    } finally {
      while (hints.size() > initialSize) hints.remove(hints.size() - 1);
    }
  }
}
