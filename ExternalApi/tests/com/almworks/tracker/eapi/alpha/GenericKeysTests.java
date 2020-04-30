package com.almworks.tracker.eapi.alpha;

import junit.framework.TestCase;

public class GenericKeysTests extends TestCase {

  public void testComparator() {
    GenericKeys.IdComparator c = new GenericKeys.IdComparator();
    assertTrue(c.compare("MS-14", "MS-19") < 0);
    assertTrue(c.compare("MSA-14", "AMS-19") > 0);
    assertTrue(c.compare("MSA-14", "") < 0);
    assertTrue(c.compare("MSA-14-AA", "MSA-14") > 0);
    assertTrue(c.compare("MSA-14-AA", "MSA-14-BB") < 0);
  }

}
