package com.almworks.api.engine;

import com.almworks.items.api.DP;
import com.almworks.util.bool.BoolExpr;
import com.almworks.util.commons.Procedure;
import com.almworks.util.exec.ThreadGate;
import org.almworks.util.detach.Detach;

public interface RemoteQuery {
  String getQueryName();

  SyncTask reload(Procedure<SyncTask> runFinally);

  BoolExpr<DP> getFilter();

  int getCount();

  /**
   * Assynchronously gets query url and passes it to specified acceptor via specified {@link com.almworks.util.exec.ThreadGate}
   * @param queryAcceptor
   * @param gate
   * @return returns {@link Detach} to cancel url obtaining. Detaching it before acceptor gets url doesn't garantee that
   * it doesn't get it ever later. Detaching isn't nessary when url is obtained all requiered detach should be done by
   * implementor.
   */
  Detach getQueryUrl(Procedure<String> queryAcceptor, ThreadGate gate);
}
