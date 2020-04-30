package com.almworks.items.impl;

import com.almworks.items.api.DBOperationCancelledException;
import com.almworks.items.api.DBPriority;
import com.almworks.items.api.DBWriter;
import com.almworks.items.api.WriteTransaction;
import com.almworks.items.impl.dbadapter.DBCallback;
import com.almworks.items.impl.migrations.DBMigrationProcedure;
import com.almworks.items.impl.sqlite.DatabaseContext;
import com.almworks.items.impl.sqlite.DatabaseManager;
import com.almworks.util.concurrent.DetachLatch;
import org.almworks.util.Log;

import java.util.Collection;

class DBMigrator implements WriteTransaction<Void>, DBCallback {
  public static void runMigrations(DatabaseManager manager, DatabaseContext context) {
    new DBMigrator(manager, context).runMigrations();
  }

  private final DatabaseManager myManager;
  private final Collection<DBMigrationProcedure> myMigrations;
  private final DetachLatch myLatch = new DetachLatch();

  private DBMigrator(DatabaseManager manager, DatabaseContext context) {
    myManager = manager;
    myMigrations = context.getConfiguration().getMigrations();
  }

  private void runMigrations() {
    if(myMigrations != null && !myMigrations.isEmpty()) {
      myManager.write(DBPriority.FOREGROUND, new WriteHandle<Void>(this)).addCallback(this);
      myLatch.awaitOrThrowRuntime();
    }
  }

  @Override
  public Void transaction(DBWriter writer) throws DBOperationCancelledException {
    for(final DBMigrationProcedure migration : myMigrations) {
      migration.migrateIfNeeded(writer);
    }
    return null;
  }

  @Override
  public void dbSuccess() {
    myLatch.detach();
  }

  @Override
  public void dbFailure(Throwable throwable) {
    Log.warn(throwable);
    myLatch.detach();
  }
}
