package com.almworks.tracker.alpha;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

class AlphaImplUtils {
  static void assertAWT() {
    assert SwingUtilities.isEventDispatchThread();
  }

  static boolean equals(Object o1, Object o2) {
    if (o1 == null)
      return o2 == null;
    else
      return o1.equals(o2);
  }

  static void inAWT(Runnable runnable) {
    if (EventQueue.isDispatchThread())
      runnable.run();
    else
      SwingUtilities.invokeLater(runnable);
  }

  static java.util.List<String> tokenizeExec(String execString) {
    java.util.List<String> result = new ArrayList<String>();
    if (execString == null)
      return result;
    int len = execString.length();
    int i = 0;
    boolean word = false;
    boolean quoted = false;
    boolean escaped = false;
    StringBuffer current = new StringBuffer();
    for (; i < len; i++) {
      char c = execString.charAt(i);
      boolean consumed = false;
      if (c == '"') {
        if (!word) {
          quoted = true;
          word = true;
          consumed = true;
        } else {
          if (quoted && !escaped) {
            result.add(current.toString());
            current.setLength(0);
            word = false;
            quoted = false;
            consumed = true;
          }
        }
      } else if (c == '%') {
        if (!escaped && i < len - 2 && execString.charAt(i + 1) == '2' && execString.charAt(i + 2) == '0') {
          c = ' ';
          i += 2;
        }
      } else if (c == ' ') {
        if (!word) {
          consumed = true;
        } else {
          if (!escaped && !quoted) {
            result.add(current.toString());
            current.setLength(0);
            word = false;
            consumed = true;
          }
        }
      } else if (c == '\\') {
        if (quoted && !escaped) {
          escaped = true;
          consumed = true;
        }
      }
      if (!consumed) {
        current.append(c);
        escaped = false;
        word = true;
      }
    }
    if (current.length() > 0)
      result.add(current.toString());
    return result;
  }
}
