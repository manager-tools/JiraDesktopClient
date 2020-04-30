package com.almworks.jira.provider3.sync.download2.meta.core;

import com.almworks.api.connector.CancelledException;
import com.almworks.items.entities.api.collector.transaction.EntityTransaction;
import com.almworks.jira.connector2.JiraInternalException;
import com.almworks.jira.provider3.sync.download2.process.util.ProgressInfo;
import com.almworks.restconnector.RestSession;

public abstract class MetaOperation {
  private final double myEstimate;

  protected MetaOperation(double estimate) {
    myEstimate = estimate;
  }

  public double getEstimate() {
    return myEstimate;
  }

  public abstract void perform(RestSession session, EntityTransaction transaction, ProgressInfo progress, LoadMetaContext context) throws CancelledException, JiraInternalException;
}
