package com.almworks.util.tests;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;

/**
 * :todoc:
 *
 * @author sereda
 */
public class ManyExceptions extends RuntimeException {
  private final Throwable myMainThreadCause;
  private final List<Throwable> myOtherCauses;
  private static final String LINE = "-------------------------------------------------------------------------------------------------";

  public ManyExceptions(Throwable throwable, List<Throwable> caughtExceptions) {
    super(throwable);
    myMainThreadCause = throwable;
    myOtherCauses = caughtExceptions;
  }

  public void printStackTrace(PrintStream s) {
    synchronized (s) {
      s.println(LINE);
      boolean needLine = false;
      if (myMainThreadCause != null) {
        s.print("M: ");
        myMainThreadCause.printStackTrace(s);
        needLine = true;
      }
      if (myOtherCauses != null) {
        if (needLine)
          s.println("|||||| OTHER THREADS ||||||");
        int i = 0;
        needLine = false;
        for (Iterator<Throwable> iterator = myOtherCauses.iterator(); iterator.hasNext();) {
          if (needLine)
            s.println("|||||||||||||||||||||||||||");
          s.print(++i + ": ");
          Throwable throwable = iterator.next();
          throwable.printStackTrace(s);
          needLine = true;
        }
      }
      if (!needLine)
        s.println("EMPTY");
      s.println(LINE);
    }
  }

  public void printStackTrace(PrintWriter s) {
    synchronized (s) {
      s.println(LINE);
      boolean needLine = false;
      if (myMainThreadCause != null) {
        s.print("M: ");
        myMainThreadCause.printStackTrace(s);
        needLine = true;
      }
      if (myOtherCauses != null) {
        if (needLine)
          s.println("|||||| OTHER THREADS ||||||");
        int i = 0;
        needLine = false;
        for (Iterator<Throwable> iterator = myOtherCauses.iterator(); iterator.hasNext();) {
          if (needLine)
            s.println("|||||||||||||||||||||||||||");
          s.print(++i + ": ");
          Throwable throwable = iterator.next();
          throwable.printStackTrace(s);
          needLine = true;
        }
      }
      if (!needLine)
        s.println("EMPTY");
      s.println(LINE);
    }
  }
}
