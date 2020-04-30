package com.almworks.items.impl.sqlite;

import com.almworks.items.impl.DBTriggerCounterpart;
import com.almworks.items.impl.dbadapter.DBTransaction;
import org.almworks.util.Log;

import java.util.List;

final class DBTransactionJob extends DatabaseJob {
  private final DBTransaction myTransaction;
  private final TransactionObserver myTransactionObserver;

  private long myIcn;

  public DBTransactionJob(DBTransaction transaction, TransactionObserver transactionObserver) {
    myTransactionObserver = transactionObserver;
    addCallback(transaction);
    myTransaction = transaction;
  }

  public TransactionType getTransactionType() {
    return TransactionType.WRITE;
  }

  protected void handleFinished(boolean success) {
    super.handleFinished(success);
    if (success && myIcn > 0) {
      // todo pass myChange as a hint for the last icn
      myTransactionObserver.notifyTransaction(myIcn);
    }
    myIcn = 0;
  }

  protected void dbrun(TransactionContext context) throws Throwable {
    myTransaction.transaction(context);
    if (context.hasItemChanges()) {
      context.flushChangedItemsICN();
      applyTriggers(context);
      myIcn = context.getIcn();
      context.setProperty(Schema.NEXT_ICN, myIcn + 1);
    }
  }

  private void applyTriggers(TransactionContext context) {
    List<DBTriggerCounterpart> triggers = context.getDatabaseContext().getConfiguration().getTriggers();
    if (triggers.isEmpty())
      return;
    boolean triggerCalled = false;
    for (DBTriggerCounterpart trigger : triggers) {
      try {
        trigger.apply(context);
        triggerCalled = true;
      } catch (Throwable e) {
        Log.error("error while applying trigger " + trigger, e);
        if (e instanceof ThreadDeath)
          throw (ThreadDeath) e;
      }
    }
    if (triggerCalled) {
      try {
        context.flushChangedItemsICN();
      } catch (Throwable e) {
        Log.error("error while flushing icn after triggers", e);
        if (e instanceof ThreadDeath)
          throw (ThreadDeath) e;
      }
    }
  }
}
