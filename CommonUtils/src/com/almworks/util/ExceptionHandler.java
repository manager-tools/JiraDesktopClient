package com.almworks.util;

import org.almworks.util.Log;

import java.lang.reflect.InvocationTargetException;

public interface ExceptionHandler {
  Object handle(Exception e);

  ExceptionHandler RUNTIME = new ExceptionHandler() {
    @Override
    public Object handle(Exception e) {
      if(e instanceof InvocationTargetException) {
        throw new RuntimeException(e.getCause());
      } else if(e instanceof RuntimeException) {
        throw (RuntimeException)e;
      } else {
        throw new RuntimeException(e);
      }
    }
  };

  ExceptionHandler NULL = new ExceptionHandler() {
    @Override
    public Object handle(Exception e) {
      return null;
    }
  };

  ExceptionHandler WARN = new ExceptionHandler() {
    @Override
    public Object handle(Exception e) {
      if(e instanceof InvocationTargetException) {
        Log.warn(e.getCause());
      } else {
        Log.warn(e);
      }
      return null;
    }
  };
}
