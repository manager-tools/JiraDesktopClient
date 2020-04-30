package com.almworks.items.impl.sqlite;

import com.almworks.sqlite4java.SQLiteException;


interface DatabaseRunnable {
  void dbrun(TransactionContext context) throws SQLiteException, InterruptedException;
}
