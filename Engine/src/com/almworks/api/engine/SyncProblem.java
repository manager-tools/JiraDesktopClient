package com.almworks.api.engine;

import com.almworks.util.ui.actions.ActionContext;
import com.almworks.util.ui.actions.CantPerformException;
import com.almworks.util.ui.actions.DataRole;
import org.jetbrains.annotations.Nullable;

import java.util.Date;

/**
 * :todoc:
 *
 * @author sereda
 */
public interface SyncProblem {
  DataRole<SyncProblem> DATA = DataRole.createRole(SyncProblem.class);

  ConnectionSynchronizer getConnectionSynchronizer();

  Date getTimeHappened();

  String getShortDescription();

  String getMediumDescription();

  @Nullable
  String getLongDescription();

  boolean isResolvable();

  void resolve(ActionContext context) throws CantPerformException;

  boolean isSerious();
}
