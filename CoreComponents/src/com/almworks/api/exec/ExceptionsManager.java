package com.almworks.api.exec;

import com.almworks.util.collections.Convertor;
import com.almworks.util.properties.Role;
import org.almworks.util.ExceptionUtil;
import org.almworks.util.Util;
import org.almworks.util.detach.Detach;

import java.util.Date;

/**
 * @author : Dyoma
 */
public interface ExceptionsManager {
  Role<ExceptionsManager> ROLE = Role.role("ExceptionManager");
  Detach addListener(Listener listener);

  boolean isAnyExceptionOccured();

  public interface Listener {
    void onException(ExceptionEvent event);
  }

  static class ExceptionEvent {
    public static final String EXCEPTION = "Exception";
    public static final String ERROR = "Error";
    public static final String WARNING = "Warning";
    public static final String DIAGNOSTIC = "Diagnostic";

    public static final Convertor<ExceptionEvent, String> GET_STACK_TRACE = new Convertor<ExceptionEvent, String>() {
      public String convert(ExceptionEvent value) {
        return value.getStacktrace();
      }
    };

    private final Thread myThread;
    private final Throwable myThrowable;
    private final String myType;
    private final String myMessage;
    private final long myWhen;

    public ExceptionEvent(Thread thread, Throwable throwable, String type) {
      this(thread, throwable, type, null);
    }

    public ExceptionEvent(Thread thread, Throwable throwable, String type, String message) {
      myThread = thread;
      myThrowable = throwable;
      myType = type;
      myMessage = message;
      myWhen = System.currentTimeMillis();
    }

    public Thread getThread() {
      return myThread;
    }

    public Throwable getThrowable() {
      return myThrowable;
    }

    public String getType() {
      return myType;
    }

    public String getMessage() {
      return myMessage;
    }

    public String getStacktrace() {
      return ExceptionUtil.getStacktrace(getThrowable());
    }

    public Date getWhen() {
      return new Date(myWhen);
    }

    public ExceptionHash createRecord() {
      if(myThrowable != null) {
        return ExceptionHash.createHash(myThrowable);
      }
      return ExceptionHash.createHash(Util.NN(myMessage, ":no-throwable:"));
    }
  }
}
