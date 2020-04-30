package com.almworks.util.io.persist;

import com.almworks.util.collections.Factories;
import com.almworks.util.commons.Factory;
import com.almworks.util.tests.BaseTestCase;

import java.io.*;
import java.util.Arrays;

/**
 * @author Vasya
 */
public class PersistableUtilTests extends BaseTestCase {
  public void testStore() throws IOException {
    doSimpleStoreTest(new Integer(1), Factories.<Persistable<Integer>>newInstance(PersistableInteger.class));
    doSimpleStoreTest(new Long(43), Factories.<Persistable<Long>>newInstance(PersistableLong.class));
    doSimpleStoreTest(new Boolean(true), Factories.<Persistable<Boolean>>newInstance(PersistableBoolean.class));
    doSimpleStoreTest(new Boolean(false), Factories.<Persistable<Boolean>>newInstance(PersistableBoolean.class));
    MyPersistableContainer c = new MyPersistableContainer();
    c.persist(new PersistableBoolean(true));
    c.persist(new PersistableBoolean(true));
    PersistableUtil.storePersistable(c);
    doIt(c, null);
  }

  private <T> void doSimpleStoreTest(T sampleValue, Factory<Persistable<T>> factory) throws IOException {
    Persistable<T> p = factory.create();
    try {
      PersistableUtil.storePersistable(p, new DataOutputStream(new ByteArrayOutputStream(1)));
      fail("No IlegalStateException");
    } catch (IllegalStateException e) {
      //normal
    }
    try {
      PersistableUtil.storePersistable(p);
      fail("No IOException");
    } catch (IllegalStateException e) {
      //normal
    }
    PersistableUtil.restorePersistable(p, new DataInputStream(new ByteArrayInputStream(createBytes())));
    doIt(p, sampleValue);
  }

  private <T> void doIt(Persistable<T> p, T sampleValue) throws IOException {
    p.set(sampleValue);
    PersistableUtil.storePersistable(p, new DataOutputStream(new ByteArrayOutputStream(1)));
    PersistableUtil.storePersistable(p);
    PersistableUtil.restorePersistable(p, new DataInputStream(new ByteArrayInputStream(createBytes())));
    try {
      PersistableUtil.restorePersistable(p, (byte[]) null);
      fail("No FormatException");
    } catch (Exception e) {
      //normal
    }
    PersistableUtil.restorePersistable(p, createBytes());
    p.access();
    p.clear();
  }

  public static class MyPersistableContainer extends PersistableContainer {
    public MyPersistableContainer() {
      super();
    }
  }

  private byte[] createBytes() {
    final byte[] bytes = new byte[2];
    Arrays.fill(bytes, Byte.MAX_VALUE);
    return bytes;
  }
}
