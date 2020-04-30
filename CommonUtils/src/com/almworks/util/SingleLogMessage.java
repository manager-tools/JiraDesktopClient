package com.almworks.util;

import org.almworks.util.Collections15;

import java.util.Set;
import java.util.logging.Level;

public class SingleLogMessage {
  private final Level myLevel;
  private final String myMessage;
  private final Set<Trio<String, String, String>> myLogged = Collections15.hashSet();

  public SingleLogMessage(Level level, String message) {
    myLevel = level;
    myMessage = message;
  }

  public void log(Object obj1, Object obj2, Object obj3) {
    Trio<String, String, String> logKey = Trio.create(objToString(obj1), objToString(obj2), objToString(obj3));
    synchronized (myLogged) {
      if (myLogged.contains(logKey)) return;
      myLogged.add(logKey);
    }
    LogHelper.log(myLevel, myMessage, obj1, obj2, obj3);
  }

  private String objToString(Object object) {
    return object != null ? object.toString() : null;
  }
}
