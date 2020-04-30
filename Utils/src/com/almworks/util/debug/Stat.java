package com.almworks.util.debug;

import com.almworks.util.Env;
import org.almworks.util.Collections15;

import java.util.Iterator;
import java.util.Map;

/**
 * :todoc:
 *
 * @author sereda
 */
public class Stat {
  static final Stat INSTANCE = createInstance();
  private final boolean myWorks;
  private final Map<String, CodeTraceStat> myStats = Collections15.hashMap();
  private final Thread myOutputThread;

  private static Stat createInstance() {
    return new Stat(Env.isRunFromIDE());
  }

  Stat(boolean works) {
    myWorks = works;
    myOutputThread = new Thread(new Runnable() {
      public void run() {
        runOutput();
      }
    }, "Stat.output");
    myOutputThread.setDaemon(true);
  }

  Trace createTrace(String codeName) {
    if (!myWorks)
      return null;
    startThread();
    Trace trace = new Trace();
    trace.setCodeName(codeName);
 //   trace.setStartTime(System.nanoTime());
    trace.setStartTime(System.currentTimeMillis() * 1000000);
    return trace;
  }

  private Stat startThread() {
    if (!myOutputThread.isAlive())
      try {
        myOutputThread.start();
      } catch (Exception e) {
        // no problem
      }
    return this;
  }

  void countTrace(Trace trace) {
    if (!myWorks)
      return;
    if (trace == null)
      return;
//    trace.setFinishTime(System.nanoTime());
    trace.setFinishTime(System.currentTimeMillis() * 1000000);
    CodeTraceStat collection = getStat(trace.getCodeName());
    collection.update(trace);
  }

  private CodeTraceStat getStat(String codeName) {
    synchronized (myStats) {
      CodeTraceStat stat = myStats.get(codeName);
      if (stat == null) {
        stat = new CodeTraceStat(codeName);
        myStats.put(codeName, stat);
      }
      return stat;
    }
  }

  private void runOutput() {
    if (!myWorks)
      return;
    synchronized (myStats) {
      while (!myOutputThread.isInterrupted()) {
        for (Iterator<CodeTraceStat> iterator = myStats.values().iterator(); iterator.hasNext();) {
          CodeTraceStat stat = iterator.next();
          System.out.println("stat: " + stat.getCodeName() + "; exec: " + stat.getRunCount() +
            "; total: " + stat.getTotalTime() + "ms; meantime: " + stat.getMeanTime() + "ms");
        }
        try {
          myStats.wait(30000);
        } catch (InterruptedException e) {
          break;
        }
      }
    }
  }
}



