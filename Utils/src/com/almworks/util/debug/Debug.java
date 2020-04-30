package com.almworks.util.debug;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Debug {
//  public static Map<Object, Throwable> __trace = new Hashtable();
//  public static Object __debug;
//  public static long __tracked_atom = 28671;
//  public static boolean __debugProgress = true;
//  public static int _a = 0;
//  public static int _b = 0;

//  public static void __out(String s) {
//    System.out.println(s);
//  }

  public static String trace() {
    StringWriter out = new StringWriter();
    new Exception().printStackTrace(new PrintWriter(out));
    return out.toString();
  }

  private static void enableFocusLogging() {
    enableLogging("java.awt.focus.Component");
  }

  private static void enableEventLogging() {
    enableLogging("java.awt.event.Component");
  }

  private static void enableLogging(String name) {
    Logger focusLog = Logger.getLogger(name);

    // The logger should log all messages
    focusLog.setLevel(Level.ALL);

    // Create a new handler
    ConsoleHandler handler = new ConsoleHandler();

    // The handler must handle all messages
    handler.setLevel(Level.ALL);

    // Add the handler to the logger
    focusLog.addHandler(handler);
  }
}
