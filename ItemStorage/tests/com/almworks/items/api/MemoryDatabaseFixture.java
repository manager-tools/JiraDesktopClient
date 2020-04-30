package com.almworks.items.api;

import com.almworks.util.collections.Functional;
import com.almworks.util.commons.Procedure;
import com.almworks.util.exec.Context;
import com.almworks.util.exec.InstanceProvider;
import com.almworks.util.exec.ThreadGate;

public abstract class MemoryDatabaseFixture extends DatabaseFixture {
  protected Database db;
  protected final Runnable failRunnable = new Runnable() { public void run() { fail(); } };

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    this.db = createMemoryDatabase();
    // Make Database.require() return db
    provideDatabaseToContext();    
  }

  protected Database createMemoryDatabase() {
    return createSQLiteMemoryDatabase();
  }

  protected void flushWriteQueue() {
    flushWriteQueue(db);
  }

  protected void flushReadQueue(DBPriority queue) {
    db.read(queue, new ReadTransaction<Object>() {
      @Override
      public Object transaction(DBReader reader) throws DBOperationCancelledException {
        return null;
      }
    }).waitForCompletion();
  }

  protected void provideDatabaseToContext() {
    Context.add(InstanceProvider.instance(db, Database.ROLE), "db");
    Context.globalize();
  }

  public final void writeNoFail(final Procedure<DBWriter> write) {
    db.writeForeground(new WriteTransaction<Object>() {
      @Override
      public Object transaction(DBWriter writer) throws DBOperationCancelledException {
        write.invoke(writer);
        return null;
      }
    }).onFailure(ThreadGate.STRAIGHT, Functional.<DBResult<Object>>ignore(failRunnable))
      .waitForCompletion();
  }
}
