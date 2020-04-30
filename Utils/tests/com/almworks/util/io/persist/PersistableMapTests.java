package com.almworks.util.io.persist;

import java.io.IOException;

/**
 * :todoc:
 *
 * @author sereda
 */
public class PersistableMapTests extends PersistableFixture {
  public void testBasicContract() throws IOException {
    PersistableHashMap<Integer, Long> map = PersistableHashMap.create(new PersistableInteger(), new PersistableLong());
    map.access().put(new Integer(Integer.MAX_VALUE), new Long(Long.MAX_VALUE));
    map.access().put(new Integer(Integer.MIN_VALUE), new Long(Long.MIN_VALUE));
    map.access().put(new Integer(1), new Long(-1));
    map.access().put(new Integer(-1), new Long(1));
    PersistableUtil.storePersistable(map, out());

    PersistableHashMap<Integer, Long> map2 = PersistableHashMap.create(new PersistableInteger(), new PersistableLong());
    PersistableUtil.restorePersistable(map2, in());

    assertEquals(map.access(), map2.access());
  }
}
