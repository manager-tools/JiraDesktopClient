package com.almworks.api.exec;

import com.almworks.util.tests.BaseTestCase;

public class ExceptionHashTests extends BaseTestCase {
  public void testSame() {
    final RuntimeException r1 = new RuntimeException(), r2 = new RuntimeException();
    check(r1, r2, true);

    final RuntimeException r3 = new RuntimeException("5Runtime8"), r4 = new RuntimeException("000Runtime11");
    check(r3, r4, true);

    final RuntimeException r5 = new RuntimeException(), r6 = new RuntimeException("foo");
    check(r5, r6, true);

    final Throwable t1 = new Throwable("throwable 333 is here", r1), t2 = new Throwable("throwable 5 is here", r2);
    check(t1, t1, true);
    check(t1, t2, true);

    final Throwable t3 = new Throwable(r3), t4 = new Throwable(r4);
    check(t3, t4, true);
  }

  public void testNotSame() {
    final RuntimeException r1 = new RuntimeException();
    final RuntimeException r2 = new RuntimeException();
    check(r1, r2, false);

    final Throwable t1 = new Throwable("foo", r1); final Exception e1 = new Exception("foo", r1);
    final Exception e2 = new Exception("foo", r1), e3 = new Exception("foo", r2);
    check(t1, e1, false);
    check(e1, e2, false);
    check(e2, e3, false);

    final Exception e4 = new Exception(new Exception(new Exception())), e5 = new Exception(new Exception());
    check(e4, e5, false);
  }

  private void check(Throwable t1, Throwable t2, boolean same) {
    final ExceptionHash h1 = ExceptionHash.createHash(t1);
    final ExceptionHash h2 = ExceptionHash.createHash(t2);
    assertEquals(same, ExceptionHash.sameException(h1, h2));
  }
}
