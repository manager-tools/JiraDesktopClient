package com.almworks.jira.provider3.sync;

import com.almworks.api.connector.CancelledException;
import com.almworks.api.connector.ConnectorException;
import com.almworks.api.engine.SyncTask;
import com.almworks.api.engine.util.SyncNotAllowedException;
import com.almworks.jira.provider3.app.connection.JiraConnection3;
import com.almworks.jira.provider3.sync.jql.impl.JqlQueryBuilder;
import com.almworks.restconnector.RestSession;
import com.almworks.spi.provider.ConnectionNotConfiguredException;
import com.almworks.util.LogHelper;
import com.almworks.util.i18n.text.CurrentLocale;
import com.almworks.util.i18n.text.LocalizedAccessor;
import com.almworks.util.threads.CanBlock;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ConnectorManager {
  public static final LocalizedAccessor LOCAL = CurrentLocale.createAccessor(JqlQueryBuilder.class.getClassLoader(), "com/almworks/jira/provider3/sync/message");
  private final Object myReadWriteLock = new Object();
  private final List<CheckServer> myCheckers = new CopyOnWriteArrayList<>();
  private int myReadCount;
  private boolean myWritePending = false;
  private final JiraConnection3 myConnection;

  public ConnectorManager(JiraConnection3 connection) {
    myConnection = connection;
  }

  public void addServerCheck(CheckServer checker) {
    myCheckers.add(checker);
  }

  @CanBlock
  public void synchronousDownload(ConnectorOperation operation) throws CancelledException, ConnectionNotConfiguredException, SyncNotAllowedException {
    lockRead();
    try {
      syncNoLock(operation);
    } finally {
      unlockRead();
    }
  }

  @CanBlock
  public void synchronousUpload(ConnectorOperation operation) throws ConnectionNotConfiguredException {
    if (operation == null) return;
    boolean success = false;
    try {
      lockWrite();
      success = true;
    } catch (CancelledException e) {
      operation.onCancelled();
      return;
    } finally {
      if (!success) operation.onCompleted(SyncTask.State.CANCELLED);
    }
    try {
      syncNoLock(operation);
    } catch (SyncNotAllowedException e) {
      // ignoring
    } finally {
      unlockWrite();
    }
  }

  private void syncNoLock(ConnectorOperation operation) throws ConnectionNotConfiguredException, SyncNotAllowedException {
    SyncTask.State finalResult = SyncTask.State.CANCELLED;
    try {
      RestSession session = myConnection.getConfigHolder().createSession();
      if (session == null) {
        LogHelper.error("Failed to create connector");
        throw new ConnectionNotConfiguredException();
      }
      finalResult = SyncTask.State.FAILED;
      try {
        for (CheckServer checker : myCheckers) checker.check(session);
        operation.perform(session);
      } finally {
        session.dispose();
      }
      finalResult = SyncTask.State.DONE;
    } catch (SyncNotAllowedException e) {
      operation.onError(e);
      throw e;
    } catch (ConnectorException e) {
      operation.onError(e);
    } finally {
      operation.onCompleted(finalResult);
    }
  }

  private void lockRead() throws CancelledException {
    synchronized (myReadWriteLock) {
      while (true) {
        if (myReadCount >= 0 && !myWritePending) {
          myReadCount++;
          return;
        }
        try {
          myReadWriteLock.wait(100);
        } catch (InterruptedException e) {
          throw new CancelledException(e);
        }
      }
    }
  }

  private void lockWrite() throws CancelledException {
    synchronized (myReadWriteLock) {
      try {
        while (true) {
          if (myReadCount == 0) {
            myReadCount = -1;
            return;
          } else if (myReadCount > 0) myWritePending = true;
          try {
            myReadWriteLock.wait(100);
          } catch (InterruptedException e) {
            throw new CancelledException(e);
          }
        }
      } finally {
        myWritePending = false;
      }
    }
  }

  private void unlockRead() {
    synchronized (myReadWriteLock) {
      if (myReadCount <=0) LogHelper.error("Negative lock count");
      else {
        myReadCount--;
        if (myReadCount == 0) myReadWriteLock.notifyAll();
      }
    }
  }

  private void unlockWrite() {
    synchronized (myReadWriteLock) {
      if (myReadCount != -1) LogHelper.error("Expected write in progress", myReadCount);
      else if (myReadCount < 0) myReadCount = 0;
      myWritePending = false;
      myReadWriteLock.notifyAll();
    }
  }

  public interface CheckServer {
    void check(RestSession session) throws ConnectorException;
  }
}
