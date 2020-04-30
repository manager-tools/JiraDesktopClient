package com.almworks.items.api;

import com.almworks.util.commons.Procedure;
import com.almworks.util.exec.ThreadGate;
import util.concurrent.Synchronized;

public class DBResultTests extends MemoryDatabaseFixture {
  public static final int TIMEOUT = 200;

  public void testOnSuccess() throws InterruptedException {
    final Synchronized<Long> v = new Synchronized<Long>(null);
    Long r = successfulWrite().onSuccess(ThreadGate.STRAIGHT, new Procedure<Long>() {
      @Override
      public void invoke(Long arg) {
        v.set(arg);
      }
    }).waitForCompletion();
    assertNotNull(r);
    assertTrue(v.waitForValue(r, TIMEOUT));

    v.set(null);
    r = successfulRead().onSuccess(ThreadGate.STRAIGHT, new Procedure<Long>() {
      @Override
      public void invoke(Long arg) {
        v.set(arg);
      }
    }).waitForCompletion();
    assertNotNull(r);
    assertTrue(v.waitForValue(r, TIMEOUT));

    v.set(null);
    r = failedRead().onSuccess(ThreadGate.STRAIGHT, new Procedure<Long>() {
      @Override
      public void invoke(Long arg) {
        v.set(1L);
      }
    }).waitForCompletion();
    assertNull(r);
    assertFalse(v.waitForValue(1L, TIMEOUT));

    v.set(null);
    r = failedWrite().onSuccess(ThreadGate.STRAIGHT, new Procedure<Long>() {
      @Override
      public void invoke(Long arg) {
        v.set(1L);
      }
    }).waitForCompletion();
    assertNull(r);
    assertFalse(v.waitForValue(1L, TIMEOUT));
  }

  public void testOnFailure() throws InterruptedException {
    final Synchronized<Long> v = new Synchronized<Long>(null);
    Long r = successfulWrite().onFailure(ThreadGate.STRAIGHT, new Procedure<DBResult<Long>>() {
      @Override
      public void invoke(DBResult<Long> arg) {
        v.set(1L);
      }
    }).waitForCompletion();
    assertNotNull(r);
    assertFalse(v.waitForValue(1L, TIMEOUT));

    v.set(null);
    r = successfulRead().onFailure(ThreadGate.STRAIGHT, new Procedure<DBResult<Long>>() {
      @Override
      public void invoke(DBResult<Long> arg) {
        v.set(1L);
      }
    }).waitForCompletion();
    assertNotNull(r);
    assertFalse(v.waitForValue(1L, TIMEOUT));

    v.set(null);
    r = failedRead().onFailure(ThreadGate.STRAIGHT, new Procedure<DBResult<Long>>() {
      @Override
      public void invoke(DBResult<Long> arg) {
        assertFalse(arg.isSuccessful());
        assertTrue(arg.isDone());
        assertTrue(arg.getError() instanceof DBOperationCancelledException);
        v.set(1L);
      }
    }).waitForCompletion();
    assertNull(r);
    assertTrue(v.waitForValue(1L, TIMEOUT));

    v.set(null);
    r = failedWrite().onFailure(ThreadGate.STRAIGHT, new Procedure<DBResult<Long>>() {
      @Override
      public void invoke(DBResult<Long> arg) {
        assertFalse(arg.isSuccessful());
        assertTrue(arg.isDone());
        assertTrue(arg.getError() instanceof DBOperationCancelledException);
        v.set(1L);
      }
    }).waitForCompletion();
    assertNull(r);
    assertTrue(v.waitForValue(1L, TIMEOUT));
  }

  public void testFinallyDoWithResult() throws InterruptedException {
    final Synchronized<Long> v = new Synchronized<Long>(null);
    Long r = successfulWrite().finallyDoWithResult(ThreadGate.STRAIGHT, new Procedure<DBResult<Long>>() {
      @Override
      public void invoke(DBResult<Long> arg) {
        assertTrue(arg.isSuccessful());
        assertTrue(arg.isDone());
        v.set(1L);
      }
    }).waitForCompletion();
    assertNotNull(r);
    assertTrue(v.waitForValue(1L, TIMEOUT));

    v.set(null);
    r = successfulRead().finallyDoWithResult(ThreadGate.STRAIGHT, new Procedure<DBResult<Long>>() {
      @Override
      public void invoke(DBResult<Long> arg) {
        assertTrue(arg.isSuccessful());
        assertTrue(arg.isDone());
        v.set(1L);
      }
    }).waitForCompletion();
    assertNotNull(r);
    assertTrue(v.waitForValue(1L, TIMEOUT));

    v.set(null);
    r = failedRead().finallyDoWithResult(ThreadGate.STRAIGHT, new Procedure<DBResult<Long>>() {
      @Override
      public void invoke(DBResult<Long> arg) {
        assertFalse(arg.isSuccessful());
        assertTrue(arg.isDone());
        assertTrue(arg.getError() instanceof DBOperationCancelledException);
        v.set(1L);
      }
    }).waitForCompletion();
    assertNull(r);
    assertTrue(v.waitForValue(1L, TIMEOUT));

    v.set(null);
    r = failedWrite().finallyDoWithResult(ThreadGate.STRAIGHT, new Procedure<DBResult<Long>>() {
      @Override
      public void invoke(DBResult<Long> arg) {
        assertFalse(arg.isSuccessful());
        assertTrue(arg.isDone());
        assertTrue(arg.getError() instanceof DBOperationCancelledException);
        v.set(1L);
      }
    }).waitForCompletion();
    assertNull(r);
    assertTrue(v.waitForValue(1L, TIMEOUT));
  }
  
  public void testFinallyDo() throws InterruptedException {
    final Synchronized<Long> v = new Synchronized<Long>(null);
    Long r = successfulWrite().finallyDo(ThreadGate.STRAIGHT, new Procedure<Long>() {
      @Override
      public void invoke(Long arg) {
        v.set(arg);
      }
    }).waitForCompletion();
    assertNotNull(r);
    assertTrue(v.waitForValue(r, TIMEOUT));

    v.set(null);
    r = successfulRead().finallyDo(ThreadGate.STRAIGHT, new Procedure<Long>() {
      @Override
      public void invoke(Long arg) {
        v.set(arg);
      }
    }).waitForCompletion();
    assertNotNull(r);
    assertTrue(v.waitForValue(r, TIMEOUT));

    v.set(1L);
    r = failedRead().finallyDo(ThreadGate.STRAIGHT, new Procedure<Long>() {
      @Override
      public void invoke(Long arg) {
        v.set(arg);
      }
    }).waitForCompletion();
    assertNull(r);
    assertTrue(v.waitForValue(null, TIMEOUT));

    v.set(1L);
    r = failedWrite().finallyDo(ThreadGate.STRAIGHT, new Procedure<Long>() {
      @Override
      public void invoke(Long arg) {
        v.set(arg);
      }
    }).waitForCompletion();
    assertNull(r);
    assertTrue(v.waitForValue(null, TIMEOUT));
  }
  
  

  private DBResult<Long> failedWrite() {
    return db.writeForeground(new WriteTransaction<Long>() {
      @Override
      public Long transaction(DBWriter writer) throws DBOperationCancelledException {
        throw new DBOperationCancelledException();
      }
    });
  }

  private DBResult<Long> failedRead() {
    return db.readForeground(new ReadTransaction<Long>() {
      @Override
      public Long transaction(DBReader reader) throws DBOperationCancelledException {
        throw new DBOperationCancelledException();
      }
    });
  }

  private DBResult<Long> successfulRead() {
    return db.readForeground(new ReadTransaction<Long>() {
      @Override
      public Long transaction(DBReader reader) throws DBOperationCancelledException {
        return reader.findMaterialized(TestData.ARRAY);
      }
    });
  }

  private DBResult<Long> successfulWrite() {
    return db.writeForeground(new WriteTransaction<Long>() {
      @Override
      public Long transaction(DBWriter writer) throws DBOperationCancelledException {
        return writer.materialize(TestData.ARRAY);
      }
    });
  }
}
