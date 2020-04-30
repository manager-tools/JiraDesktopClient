package com.almworks.util.io.persist;

import com.almworks.util.tests.BaseTestCase;

import java.io.*;

/**
 * :todoc:
 *
 * @author sereda
 */
public abstract class PersistableFixture extends BaseTestCase {
  protected ByteArrayOutputStream myLastOutputStream;
  protected ByteArrayInputStream myLastInputStream;

  protected DataOutput out() {
    myLastInputStream = null;
    if (myLastOutputStream == null)
      myLastOutputStream = new ByteArrayOutputStream();
    return new DataOutputStream(myLastOutputStream);
  }

  protected DataInput in() {
    if (myLastInputStream == null) {
      if (myLastOutputStream == null)
        throw new IllegalStateException("no preceding out()");
      myLastInputStream = new ByteArrayInputStream(myLastOutputStream.toByteArray());
      myLastOutputStream = null;
    }
    return new DataInputStream(myLastInputStream);
  }
}
