package com.almworks.util.events;

import com.almworks.util.Pair;
import com.almworks.util.exec.ThreadGate;
import org.almworks.util.Collections15;

import java.lang.reflect.Method;
import java.util.List;

/**
 * :todoc:
 *
 * @author sereda
 */
public final class EventDebugger {
  public static final EventType DISPATCH_STARTS = new EventType("dispatchStart");
  public static final EventType DISPATCH_BEFORE_GATING = new EventType("dispatchBeforeGating");
  public static final EventType DISPATCH_BEFORE_INVOCATION = new EventType("dispatchBeforeInvocation");
  public static final EventType DISPATCH_AFTER_INVOCATION = new EventType("dispatchAfterInvocation");
  public static final EventType DISPATCH_INVOCATION_EXCEPTION = new EventType("dispatchInvocationException");
  public static final EventType DISPATCH_FINISHED = new EventType("dispatchFinish");

  private static final EventDebugger INSTANCE = new EventDebugger();

  private volatile boolean myEnabled = false;
  private final List<Debugger> myDebuggers = Collections15.arrayList();

  public static boolean isEnabled() {
    return INSTANCE.myEnabled;
  }

  public static void setEnabled(boolean enabled) {
    INSTANCE.myEnabled = enabled;
  }

  public static int logStartDispatch(Pair[] listeners, Method method, Object[] args) {
    DispatchEvent event = new DispatchEvent(-1, DISPATCH_STARTS, method, args, null, null, null, listeners);
    INSTANCE.debug(event);
    return event.getDispatchID();
  }

  public static void addDebugger(Debugger debugger) {
    synchronized (INSTANCE) {
      INSTANCE.myDebuggers.add(debugger);
    }
  }

  public static void removeDebugger(Debugger debugger) {
    synchronized (INSTANCE) {
      INSTANCE.myDebuggers.remove(debugger);
    }
  }

  private void debug(EventDebugEvent event) {
    synchronized (this) {
      if (myDebuggers.size() == 0)
        return;
      Debugger[] debuggers = myDebuggers.toArray(new Debugger[myDebuggers.size()]);
      for (int i = 0; i < debuggers.length; i++) {
        debuggers[i].debug(event);
      }
    }
  }

  public static void logDispatchBeforeGating(int dispatchID, ThreadGate gate, Object listener, Method method,
    Object[] args) {

    INSTANCE.debug(new DispatchEvent(dispatchID, DISPATCH_BEFORE_GATING, method, args, listener, gate, null, null));
  }

  public static void logDispatchInvocationStarts(int dispatchID, ThreadGate gate, Object listener, Method method,
    Object[] args) {

    INSTANCE.debug(new DispatchEvent(dispatchID, DISPATCH_BEFORE_INVOCATION, method, args, listener, gate, null, null));
  }

  public static void logDispatchInvocationException(int dispatchID, Throwable throwable, ThreadGate gate, Object listener,
    Method method, Object[] args) {

    INSTANCE.debug(
      new DispatchEvent(dispatchID, DISPATCH_INVOCATION_EXCEPTION, method, args, listener, gate, throwable, null));
  }

  public static void logDispatchInvocationFinishes(int dispatchID, ThreadGate gate, Object listener, Method method,
    Object[] args) {

    INSTANCE.debug(new DispatchEvent(dispatchID, DISPATCH_AFTER_INVOCATION, method, args, listener, gate, null, null));
  }

  public static void logEndDispatch(int dispatchID, Pair[] listeners, Method method, Object[] args) {
    INSTANCE.debug(new DispatchEvent(dispatchID, DISPATCH_FINISHED, method, args, null, null, null, listeners));
  }

  public static interface Debugger {
    void debug(EventDebugEvent event);
  }


  public static class DispatchEvent extends EventDebugEvent {
    private static int ourDispatchCounter = 0;
    private final int myDispatchID;
    private final Method myListenerMethod;
    private final Object[] myListenerArguments;
    private final Object myListener;
    private final ThreadGate myGate;
    private final Throwable myException;
    private final Pair[] myListenerGatePairs;

    public DispatchEvent(int dispatchID, EventType type, Method listenerMethod, Object[] listenerArguments, Object listener,
      ThreadGate gate, Throwable exception, Pair[] listenerGatePairs) {

      super(type);
      if (dispatchID < 0) {
        synchronized (DispatchEvent.class) {
          dispatchID = ++ourDispatchCounter;
        }
      }
      myDispatchID = dispatchID;
      myListenerMethod = listenerMethod;
      myListenerArguments = listenerArguments;
      myListener = listener;
      myGate = gate;
      myException = exception;
      myListenerGatePairs = listenerGatePairs;
    }

    public Method getListenerMethod() {
      return myListenerMethod;
    }

    public Object[] getListenerArguments() {
      return myListenerArguments;
    }

    public Object getListener() {
      return myListener;
    }

    public ThreadGate getGate() {
      return myGate;
    }

    public Throwable getException() {
      return myException;
    }

    public Pair[] getListenerGatePairs() {
      return myListenerGatePairs;
    }

    public int getDispatchID() {
      return myDispatchID;
    }

    public String toString() {
      StringBuffer sb = new StringBuffer(super.toString());
      sb.append(" d").append(myDispatchID);
      sb.append(" ").append(getMethodAndArguments());
      sb.append(" ").append(getListenerInfo());
      return sb.toString();
    }

    public String getMethodAndArguments() {
      StringBuffer buffer = new StringBuffer();
      if (myListenerMethod != null) {
        String className = myListenerMethod.getDeclaringClass().getName();
        className = className.substring(className.lastIndexOf('.') + 1);
        buffer.append("m[").append(className).append('.');
        buffer.append(myListenerMethod.getName()).append('(');
        if (myListenerArguments != null) {
          for (int i = 0; i < myListenerArguments.length; i++) {
            if (i > 0)
              buffer.append(", ");
            buffer.append(myListenerArguments[i]);
          }
        }
        buffer.append(')');
      }
      return buffer.toString();
    }

    public String getListenerInfo() {
      StringBuffer buffer = new StringBuffer();
      if (myListener != null)
        buffer.append("l[").append(myListener).append("]");
      if (myListenerGatePairs != null)
        buffer.append("lc").append(myListenerGatePairs.length);
      return buffer.toString();
    }
  }

  public static class EventDebugEvent {
    private static int ourEventCounter = 0;
    private static long ourFirstEventTime = 0;

    private final int myEventID;
    private final long myTimeMillis;
    private final Throwable myStackTrace;
    private final EventType myEventType;
    private final Thread myThread;

    public EventDebugEvent(EventType eventType) {
      myTimeMillis = System.currentTimeMillis();
      synchronized (EventDebugEvent.class) {
        if (ourEventCounter == 0)
          ourFirstEventTime = myTimeMillis;
        myEventID = ++ourEventCounter;
      }
      myStackTrace = new Throwable();
      myEventType = eventType;
      myThread = Thread.currentThread();
    }

    public int getEventID() {
      return myEventID;
    }

    public long getTimeMillis() {
      return myTimeMillis;
    }

    public Throwable getStackTrace() {
      return myStackTrace;
    }

    public EventType getEventType() {
      return myEventType;
    }

    public Thread getThread() {
      return myThread;
    }

    public String toString() {
      return "e" + myEventID + " t" + (myTimeMillis - ourFirstEventTime) + " " + myEventType + " t[" +
        myThread.getName() +
        "]";
    }

    public long getMillisSinceFirstEvent() {
      return myTimeMillis - ourFirstEventTime;
    }
  }

  public static final class EventType {
    private final String myName;

    private EventType(String name) {
      myName = name;
    }

    public String getName() {
      return myName;
    }

    public String toString() {
      return myName;
    }
  }
}
