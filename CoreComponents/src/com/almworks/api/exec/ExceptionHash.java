package com.almworks.api.exec;

import com.almworks.util.io.IOUtils;
import org.almworks.util.Log;
import org.jetbrains.annotations.NotNull;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;

public class ExceptionHash {
  private final String myTraceHash;

  public ExceptionHash(@NotNull String traceHash) {
    assert traceHash != null;
    myTraceHash = traceHash;
  }

  @NotNull
  public String getTraceHash() {
    return myTraceHash;
  }

  public static ExceptionHash createHash(String traceHash) {
    return new ExceptionHash(traceHash);
  }

  public static ExceptionHash createHash(Throwable e) {
    final StringBuilder h = new StringBuilder();
    appendExceptionChain(e, h);
    return new ExceptionHash(getHash(h.toString()));
  }

  private static void appendExceptionChain(Throwable e, StringBuilder h) {
    if(e != null) {
      h.append(e.getClass().getName());
      appendStackTrace(e, h);
      appendExceptionChain(e.getCause(), h);
    }
  }

  private static void appendStackTrace(Throwable e, StringBuilder h) {
    for(final StackTraceElement ste : e.getStackTrace()) {
      if(!isVolatile(ste)) {
        h.append(ste.toString());
      }
    }
  }

  private static boolean isVolatile(StackTraceElement ste) {
    final String className = ste.getClassName();
    return className.startsWith("sun.reflect.");
  }

  private static String getHash(String trace) {
    try {
      return IOUtils.md5sum(trace);
    } catch (NoSuchAlgorithmException e) {
      Log.warn(e);
    } catch (UnsupportedEncodingException e) {
      Log.warn(e);
    }
    return trace.substring(0, Math.min(100, trace.length()));
  }

  public static boolean sameException(ExceptionHash e1, ExceptionHash e2) {
    return e1.getTraceHash().equals(e2.getTraceHash());
  }
}
