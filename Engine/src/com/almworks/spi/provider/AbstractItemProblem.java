package com.almworks.spi.provider;

import com.almworks.api.engine.ConnectionSynchronizer;
import com.almworks.api.engine.ItemSyncProblem;
import com.almworks.api.engine.SyncProblem;
import com.almworks.util.Pair;
import com.almworks.util.model.SetHolderModel;
import com.almworks.util.ui.actions.ActionContext;
import com.almworks.util.ui.actions.CantPerformException;
import org.almworks.util.Util;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.Lifecycle;
import org.jetbrains.annotations.NotNull;

import java.util.Date;

public abstract class AbstractItemProblem implements ItemSyncProblem {
  protected final long myItem;
  protected final String myDisplayableId;
  protected final ConnectionContext myContext;
  private final Date myTimeCreated;
  protected final Pair<String, Boolean> myCredentialState;
  // guarded by itself
  private final Lifecycle myRemoveFromLists = new Lifecycle();

  /**
   * @param displayableId user-readable item ID (JIRA issue key, Bugzilla bug ID)
   */
  public AbstractItemProblem(long item, String displayableId, long timeCreated, ConnectionContext context, Pair<String, Boolean> credentialState) {
    assert context != null;
    assert item > 0L;
    myItem = item;
    myDisplayableId = Util.NN(displayableId, "DB:" + item);
    myTimeCreated = new Date(timeCreated);
    myCredentialState = credentialState;
    myContext = context;
  }

  public ConnectionSynchronizer getConnectionSynchronizer() {
    return myContext.getContainer().getActor(ConnectionSynchronizer.ROLE);
  }

  public Date getTimeHappened() {
    return myTimeCreated;
  }

  public Pair<String, Boolean> getCredentialState() {
    return myCredentialState;
  }

  public boolean isCauseForRemoval() {
    return false;
  }

  public boolean isResolvable() {
    return false;
  }

  public void resolve(ActionContext context) throws CantPerformException {
  }

  public boolean isSerious() {
    return true;
  }

  public long getItem() {
    return myItem;
  }

  @Override
  public Detach addToCollection(final SetHolderModel<SyncProblem> problems) {
    Detach removeFromProblems = new Detach() { @Override protected void doDetach() throws Exception {
        problems.remove(AbstractItemProblem.this);
    }};
    synchronized (myRemoveFromLists) {
      myRemoveFromLists.lifespan().add(removeFromProblems);
    }
    problems.add(this);
    return removeFromProblems;
  }

  public void disappear() {
    ConnectionSynchronizer connSync = getConnectionSynchronizer();
    if (connSync != null) {
      // just in case...
      connSync.removeProblem(this);
    }
    synchronized (myRemoveFromLists) {
      myRemoveFromLists.cycle();
    }
  }

  @NotNull
  public String getDisplayableId() {
    return myDisplayableId;
  }

  public String getMediumDescription() {
    return getShortDescription();
  }
}
