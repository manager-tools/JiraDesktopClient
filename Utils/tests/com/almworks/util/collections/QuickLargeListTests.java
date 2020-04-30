package com.almworks.util.collections;

import com.almworks.util.tests.BaseTestCase;
import org.almworks.util.RuntimeInterruptedException;
import util.concurrent.CountDown;

public class QuickLargeListTests extends BaseTestCase {
  public void test() throws InterruptedException {
    final int THREADS = 10;
    final int COUNT = 1000000;
    assert true;
    final int P = COUNT / THREADS;
    final QuickLargeList<String> table = new QuickLargeList<String>(10);
    final CountDown start = new CountDown(THREADS + 1);
    final CountDown stop = new CountDown(THREADS + 1);
    for (int i = 0; i < THREADS; i++) {
      new Thread() {
        public void run() {
          try {
            start.release();
            start.acquire();
            for (int j = 0; j < P; j++) {
              int x;
              synchronized(QuickLargeListTests.class) {
                table.add("a" + j);
                x = table.size() - 1;
              }
              synchronized(QuickLargeListTests.class) {
                table.set(x, "s" + j);
              }
            }
          } catch (InterruptedException e) {
            throw new RuntimeInterruptedException(e);
          } finally {
            stop.release();
          }
        }
      }.start();
    }
    start.release();
    stop.release();
    while (!stop.attempt(1000)) {
      synchronized(QuickLargeListTests.class) {
        System.out.println("table.size() = " + table.size());
      }
    }
    System.out.println("table.size() = " + table.size());
    assertEquals(COUNT, table.size());
  }
}
