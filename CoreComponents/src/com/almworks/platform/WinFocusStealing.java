package com.almworks.platform;

import com.almworks.util.Env;
import com.sun.jna.Library;
import com.sun.jna.Native;

class WinFocusStealing {
  public static void allowFocusStealing(int pid) {
    if(Env.isWindows() && pid > 0) {
      User32.INSTANCE.AllowSetForegroundWindow(pid);
    }
  }

  private static interface User32 extends Library {
    User32 INSTANCE = Env.isWindows() ? (User32) Native.loadLibrary("User32", User32.class) : null;
    boolean AllowSetForegroundWindow(int processId);
  }
}