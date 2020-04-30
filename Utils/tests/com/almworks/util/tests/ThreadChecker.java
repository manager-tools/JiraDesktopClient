package com.almworks.util.tests;

import org.almworks.util.Collections15;
import org.almworks.util.Failure;

import java.awt.*;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * @author : Dyoma
 */
public class ThreadChecker {
  private static final Map<Method, Constraint> ourConstraints = Collections15.hashMap();

  public static void checkThread(Method method) {
    Constraint constraint = ourConstraints.get(method);
    assert constraint == null || constraint.isProperThread() : failureDetails(method);
  }

  public static void register0(Class clazz, String methodName, Constraint constraint) {
    register(clazz, methodName, new Class[0], constraint);
  }

  public static void register1(Class clazz, String methodName, Class argument, Constraint constraint) {
    register(clazz, methodName, new Class[]{argument}, constraint);
  }

  public static void register2(Class clazz, String methodName, Class argument1, Class argument2,
                                          Constraint constraint) {
    register(clazz, methodName, new Class[]{argument1, argument2}, constraint);
  }

  public static void register(Class clazz, String methodName, Class[] arguments, Constraint constraint) {
    try {
      Method method = clazz.getMethod(methodName, arguments);
      register(method, constraint);
    } catch (NoSuchMethodException e) {
      throw new Failure(e);
    }
  }

  private static void register(Method method, Constraint constraint) {
    if (ourConstraints.containsKey(method))
      return;
    ourConstraints.put(method, constraint);
  }

  private static String failureDetails(Method method) {
    String threadName = Thread.currentThread().getName();
    String methodSignature = method.toString();
    return "Thread: " + threadName + " method: " + methodSignature;
  }

  public static class Constraint {
    public static final Constraint ANY = new Constraint(true, true, "ANY");
    public static final Constraint AWT = new Constraint(true, false, "AWT");
    public static final Constraint NOT_AWT = new Constraint(false, true, "Not AWT");

    private final boolean myAWTAllowed;
    private final boolean myNotAWTAllowed;
    private final String myDebugName;

    public Constraint(boolean AWTAllowed, boolean notAWTAllowed, String debugName) {
      myAWTAllowed = AWTAllowed;
      myNotAWTAllowed = notAWTAllowed;
      myDebugName = debugName;
    }

    public boolean isProperThread() {
      return EventQueue.isDispatchThread() ? myAWTAllowed : myNotAWTAllowed;
    }

    public String toString() {
      return "Constraint [" + myDebugName + "]";
    }
  }
}
