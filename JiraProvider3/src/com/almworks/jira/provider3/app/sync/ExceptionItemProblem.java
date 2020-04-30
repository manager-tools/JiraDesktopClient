package com.almworks.jira.provider3.app.sync;

import com.almworks.api.connector.ConnectorException;
import com.almworks.jira.provider3.app.connection.JiraConnection3;
import com.almworks.spi.provider.AbstractItemProblem;

/**
 * @author dyoma
*/
public class ExceptionItemProblem extends AbstractItemProblem {
  private final ConnectorException myException;
  private final Cause myCause;

  public ExceptionItemProblem(long item, String key, ConnectorException e, JiraConnection3 connection, Cause cause) {
    super(item, key, System.currentTimeMillis(), connection.getContext(), connection.getCredentialState());
    myException = e;
    myCause = cause;
  }

  public String getShortDescription() {
    return myException.getShortDescription();
  }

  public String getLongDescription() {
    return myException.getLongDescription();
  }

  public Cause getCause() {
    return myCause;
  }
}
