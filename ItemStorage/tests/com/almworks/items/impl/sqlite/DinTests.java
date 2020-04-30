package com.almworks.items.impl.sqlite;

import com.almworks.util.tests.BaseTestCase;

import java.util.HashSet;
import java.util.Set;

public class DinTests extends BaseTestCase {
  public void testDins() throws InterruptedException {
    Set<Integer> dins = new HashSet<Integer>();
    for (int i = 0; i < 20; i++) {
      int din = Schema.generateDin();
      System.out.println(Schema.formatDin(din));
      assertTrue(dins.add(din));
      Thread.sleep(100);
    }
  }
}
