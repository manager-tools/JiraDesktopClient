package org.almworks.util.detach;

import junit.framework.TestCase;

public class DetachCompositeTests extends TestCase {
  public void testPurge() {
    runPurgeTest(0, 1, 2, 5, 18);
    runPurgeTest(0, 1, 2, 5, 19);
    runPurgeTest(0, 1, 2, 5, 18, 19);
    runPurgeTest(1, 2, 5, 18, 19);
    runPurgeTest(2, 5, 18, 19);
    runPurgeTest(3, 5, 18, 19);
    runPurgeTest(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19);
    runPurgeTest(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19);
    runPurgeTest(0, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19);
    runPurgeTest(0, 1, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19);
    runPurgeTest(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18);
    runPurgeTest(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 19);
  }

  public void runPurgeTest(int... indexes) {
    DetachComposite composite = new DetachComposite();
    Detach[] detaches = new Detach[20];
    for (int i = 0; i < detaches.length; i++) {
      detaches[i] = new DetachComposite();
      composite.add(detaches[i]);
    }
    for (int index : indexes) detaches[index].detach();
    assertEquals(detaches.length, composite.count());
    composite.maybePurge(true);
    assertEquals(detaches.length - indexes.length, composite.count());
    composite.detach();
    for (Detach detach : detaches) assertTrue(detach.isDetached());
  }
}
