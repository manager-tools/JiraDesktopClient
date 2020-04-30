package com.almworks.util.http;

import com.almworks.util.Env;
import com.almworks.util.RunnableRE;
import org.almworks.util.Failure;
import org.almworks.util.RuntimeInterruptedException;
import org.apache.commons.httpclient.*;
import util.concurrent.SynchronizedBoolean;

import java.io.IOException;

public class ExtendedHttpConnection extends HttpConnection {
  private static boolean NO_GUARDING = Env.getBoolean("no.connection.guard", false);

  public ExtendedHttpConnection(HostConfiguration hostConfiguration) {
    super(hostConfiguration);
  }

  public synchronized void open() throws IOException {
    int timeout = getParams().getConnectionTimeout();
    if (NO_GUARDING || timeout <= 0) {
      // no timeout
      performOpen();
      return;
    }
    // enforce timeout due to JDK bug http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6223635
    HttpConnectionManager manager = getHttpConnectionManager();
    assert manager instanceof ExtendedHttpConnectionManager;
    InfiniteOperationGuard guard = ((ExtendedHttpConnectionManager) manager).getConnector();
    InfiniteOperationGuard.Job job = guard.execute(new RunnableRE<Void, IOException>() {
      public Void run() throws IOException {
        performOpen();
        return null;
      }
    });
    SynchronizedBoolean finished = job.getFinished();
    try {
      finished.waitForValue(true, timeout);
    } catch (InterruptedException e) {
      throw new RuntimeInterruptedException(e);
    }
    if (!finished.get()) {
      job.abort();
      throw new ConnectTimeoutException("Forced connection timeout " + timeout + " ms");
    }
    Exception e = job.getException();
    if (e != null) {
      if (e instanceof IOException)
        throw new HttpException(e.getMessage(), e);
      else
        throw new Failure(e);
    }
  }

  private void performOpen() throws IOException {
    super.open();
  }
}
