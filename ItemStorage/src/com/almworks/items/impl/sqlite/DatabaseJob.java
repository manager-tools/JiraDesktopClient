package com.almworks.items.impl.sqlite;

import com.almworks.items.impl.dbadapter.DBCallback;
import com.almworks.sqlite4java.SQLiteException;
import com.almworks.util.collections.ChangeListener;
import com.almworks.util.threads.ThreadSafe;
import org.almworks.util.Collections15;
import org.almworks.util.Log;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public abstract class DatabaseJob {
  private ChangeListener myListener;

  @NotNull
  private State myState = State.PENDING;

  private TransactionContext myCurrentContext;
  private int myPriority;
  private boolean myHurried;
  private Boolean mySuccess;

  private DBCallback myCallback;
  private List<DBCallback> myCallbacks;
  private Throwable myError;
  private SQLiteException mySqliteError;

  public final synchronized void addCallback(DBCallback callback) {
    if (callback == null)
      return;
    if (myCallback == null) {
      myCallback = callback;
    } else {
      if (myCallbacks == null)
        myCallbacks = Collections15.arrayList(1);
      myCallbacks.add(callback);
    }
  }

  public final void execute(TransactionContext context) throws Throwable {
    synchronized (this) {
      if (myState != State.PENDING) {
        // todo any reporting here?
//        assert false : this;
        return;
      }
      myState = State.RUNNING;
      myCurrentContext = context;
    }
    try {
      dbrun(context);
    } catch (Throwable e) {
      storeError(e);
      throw e;
    } finally {
      synchronized (this) {
        myCurrentContext = null;
        if (myState == State.RUNNING) {
          myState = State.FINISHED;
        } else if (myState != State.CANCELLED) {
          Log.warn(this + ": unexpected state " + myState);
        }
      }
    }
  }

  private void storeError(Throwable e) throws SQLiteException {
    myError = e;
    if (e instanceof SQLiteException) {
      mySqliteError = (SQLiteException) e;
    } else {
      Throwable cause = e.getCause();
      if (cause instanceof SQLiteException) {
        mySqliteError = (SQLiteException) cause;
      }
    }
  }

  public Throwable getError() {
    return myError;
  }

  public SQLiteException getSqliteError() {
    return mySqliteError;
  }

  protected abstract void dbrun(TransactionContext context) throws Throwable;

  /**
   * Used as a key in a map of jobs - can be used to make jobs expunge each other.
   */
  public Object getIdentity() {
    return this;
  }

  public final synchronized void setListener(ChangeListener listener) {
    assert listener == null ^ myListener == null : myListener + " " + listener;
    myListener = listener;
  }

  /**
   * The priority may be used to reorder pending jobs or select the most important job.
   * As a convention, if priority is >0, the result is critical to the UI (the user needs
   * it asap), if priority <=0, it is a background job.
   *
   * @return job priority
   */
  public final synchronized int getPriority() {
    return myPriority;
  }

  public final void setPriority(int priority) {
    ChangeListener notify = null;
    synchronized (this) {
      if (priority != myPriority)
        notify = myListener;
      myPriority = priority;
    }
    if (notify != null) {
      notify.onChange();
    }
  }


  @NotNull
  public final synchronized State getState() {
    return myState;
  }


  public abstract TransactionType getTransactionType();


  /**
   * By calling this method, database queue tells the job to finish as soon as possible (as there are more critical
   * jobs waiting). It is up to the job how to react to this request.
   * <p/>
   * NB: this method is called NOT from a database thread (the database thread is busy running dbrun)
   */
  @ThreadSafe
  public final void hurry() {
    boolean wasHurried;
    synchronized (this) {
      wasHurried = myHurried;
      myHurried = true;
    }
    if (!wasHurried) {
      handleHurry();
    }
  }

  public boolean isHurried() {
    return myHurried;
  }

  public final void cancel() {
    TransactionContext context;
    synchronized (this) {
      if (myState != State.RUNNING && myState != State.PENDING)
        return;
      myState = State.CANCELLED;
      context = myCurrentContext;
    }
    if (context != null) {
      context.cancel();
    }
    handleCancel();
  }

  public synchronized boolean isCancelled() {
    return myState == State.CANCELLED;
  }

  final void setFinished(boolean success) {
    synchronized (this) {
      if (mySuccess != null) {
        return;
      }
      mySuccess = success;
    }
    handleFinished(success);
  }

  protected void handleCancel() {
  }

  /**
   * to be overridden if job can hurry up
   */
  protected void handleHurry() {
  }

  protected void handleFinished(boolean success) {
    DBCallback callback;
    DBCallback[] callbacks = null;
    synchronized (this) {
      callback = myCallback;
      if (myCallbacks != null) {
        callbacks = myCallbacks.toArray(new DBCallback[myCallbacks.size()]);
      }
    }
    if (success) {
      if (callback != null) {
        notifySuccess(callback);
      }
      if (callbacks != null) {
        for (DBCallback c : callbacks) {
          notifySuccess(c);
        }
      }
    } else {
      if (callback != null) {
        notifyFailure(callback);
      }
      if (callbacks != null) {
        for (DBCallback c : callbacks) {
          notifyFailure(c);
        }
      }
    }
  }

  private void notifySuccess(DBCallback callback) {
    if (callback != null) {
      try {
        callback.dbSuccess();
      } catch (Throwable e) {
        Log.error(e);
        if (e instanceof ThreadDeath)
          throw (ThreadDeath) e;
      }
    }
  }

  private void notifyFailure(DBCallback callback) {
    if (callback != null) {
      try {
        callback.dbFailure(myError);
      } catch (Throwable e) {
        Log.error(e);
        if (e instanceof ThreadDeath)
          throw (ThreadDeath) e;
      }
    }
  }

  public boolean isSuccessful() {
    Boolean success = mySuccess;
    return success != null && success;
  }


  public enum TransactionType {
    /**
     * Transaction promises to only read data from the database. At the end of
     * transaction, rollback may be performed, so even if the transaction writes
     * anything, it will be lost.
     * <p/>
     * Jobs with READ_ROLLBACK may be grouped under a single transaction.
     */
    READ_ROLLBACK,

    /**
     * Transaction promises not to write anything to the MAIN database, but it may
     * write to TEMP database. At the end of transaction, commit is performed.
     */
    READ_COMMIT,

    /**
     * Transaction will write to the MAIN database.
     */
    WRITE
  }


  public enum State {
    PENDING,
    RUNNING,
    FINISHED,
    CANCELLED;
  }
}
