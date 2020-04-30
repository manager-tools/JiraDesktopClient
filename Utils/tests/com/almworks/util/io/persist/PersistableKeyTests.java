package com.almworks.util.io.persist;

import org.almworks.util.TypedKey;
import org.almworks.util.TypedKeyRegistry;
import util.external.CompactChar;

import java.io.IOException;

/**
 * :todoc:
 *
 * @author sereda
 */
public class PersistableKeyTests extends PersistableFixture {

  public void testBasicContract() throws IOException {
    doTestWriteRead(Key.BBB);
    doTestWriteRead(Key.CCC);
    doTestWriteRead(Key.AAA);
  }

  private void doTestWriteRead(Key value) throws IOException {
    PersistableKey p = PersistableKey.create(Key.REGISTRY, Key.class);
    p.set(value);
    PersistableUtil.storePersistable(p, out());

    PersistableKey p2 = PersistableKey.create(Key.REGISTRY, Key.class);
    PersistableUtil.restorePersistable(p2, in());

    assertTrue(p.access() == p2.access());
    assertTrue(value == p2.access());
  }

  public void testBadName() throws IOException {
    CompactChar.writeString(out(), "habahaba");
    PersistableKey p = PersistableKey.create(Key.REGISTRY, Key.class);
    try {
      PersistableUtil.restorePersistable(p, in());
      fail("successfully restored bad key: " + p.access());
    } catch (FormatException e) {
      // ok!
    }
  }


  private static final class Key extends TypedKey<String> {
    private static final TypedKeyRegistry<Key> REGISTRY = TypedKeyRegistry.create();

    public static final Key AAA = new Key("AAA");
    public static final Key BBB = new Key("BBB");
    public static final Key CCC = new Key("CCC");

    public Key(String name) {
      super(name, null, REGISTRY);
    }
  }
}
