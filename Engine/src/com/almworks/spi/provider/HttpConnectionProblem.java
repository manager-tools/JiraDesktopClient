package com.almworks.spi.provider;

import com.almworks.api.connector.ConnectorException;
import com.almworks.api.engine.ConnectionSynchronizer;
import com.almworks.api.engine.SyncProblem;
import com.almworks.util.ui.actions.ActionContext;
import com.almworks.util.ui.actions.CantPerformException;

import java.util.Date;

public class HttpConnectionProblem implements SyncProblem {
  private final ConnectorException myException;
  private final Date myCreationTime;
  private final ConnectionSynchronizer myConnectionSynchronizer; 

  public HttpConnectionProblem(ConnectionSynchronizer connectionSynchronizer, ConnectorException exception) {
    myConnectionSynchronizer = connectionSynchronizer;
    myException = exception;
    myCreationTime = new Date();
  }

  public String getLongDescription() {
    return myException.getLongDescription();
  }

  public String getMediumDescription() {
    return myException.getMediumDescription();
  }

  public String getShortDescription() {
    return myException.getShortDescription();
  }

  public ConnectionSynchronizer getConnectionSynchronizer() {
    return myConnectionSynchronizer;
  }

  public Date getTimeHappened() {
    return myCreationTime;
  }

  public boolean isResolvable() {
    return false;
  }

  public void resolve(ActionContext context) throws CantPerformException {
  }

  public boolean isSerious() {
    return false;
  }
}
