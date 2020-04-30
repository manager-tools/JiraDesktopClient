package com.almworks.items.impl.sqlite;

import com.almworks.items.impl.dbadapter.DBRead;

public class DBReadJob extends DatabaseJob {
  private final DBRead myRead;

  public DBReadJob(DBRead read) {
    addCallback(read);
    myRead = read;
  }

  protected void dbrun(TransactionContext context) throws Throwable {
    myRead.read(context);
  }

  public TransactionType getTransactionType() {
    return TransactionType.READ_ROLLBACK;
  }
}