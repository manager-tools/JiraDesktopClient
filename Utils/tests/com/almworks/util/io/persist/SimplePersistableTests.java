package com.almworks.util.io.persist;

import com.almworks.util.collections.Factories;
import com.almworks.util.commons.Factory;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * :todoc:
 *
 * @author sereda
 */
public class SimplePersistableTests extends PersistableFixture {
  public void testPersistableInteger() throws IOException {
    doSimpleTest(new Integer(3), Factories.<Persistable<Integer>>newInstance(PersistableInteger.class));
    doStore(new PersistableInteger(1));
    doStore(new PersistableInteger(-1));
  }

  public void testPersistableLong() throws IOException {
    doSimpleTest(new Long(43), Factories.<Persistable<Long>>newInstance(PersistableLong.class));
    doStore(new PersistableLong(1));
    doStore(new PersistableLong(-1));
  }

  public void testPersistableBoolean() throws IOException {
    doSimpleTest(new Boolean(true), Factories.<Persistable<Boolean>>newInstance(PersistableBoolean.class));
    doSimpleTest(new Boolean(false), Factories.<Persistable<Boolean>>newInstance(PersistableBoolean.class));
    doStore(new PersistableBoolean(true));
    doStore(new PersistableBoolean(false));
  }

  public void testPersistableString() throws IOException {
    doSimpleTest("Abc", Factories.<Persistable<String>>newInstance(PersistableString.class));
    doStore(new PersistableString(""));
  }

  private <T> void doSimpleTest(T sampleValue, Factory<Persistable<T>> factory) throws IOException {
    Persistable<T> p = factory.create();
    try {
      p.access();
      fail("Assert IllegalStateException");
    } catch (IllegalStateException e) {
      //normal
    }
    try {
      doStore(p);
      fail("Assert IllegalStateException");
    } catch (IllegalStateException e) {
      //normal
    }
    assertFalse(p.isInitialized());
    p.set(sampleValue);
    assertEquals(sampleValue, p.access());
    assertTrue(p.isInitialized());
    doStore(p);
    PersistableUtil.storePersistable(p, out());
    Persistable<T> p2 = factory.create();
    p2.restore(in());
    assertEquals(p.access(), p2.access());
    doStore(p2);
    p.clear();
  }

  private void doStore(Persistable p) throws IOException {
    p.store(new DataOutputStream(new ByteArrayOutputStream(1)));
  }
}
