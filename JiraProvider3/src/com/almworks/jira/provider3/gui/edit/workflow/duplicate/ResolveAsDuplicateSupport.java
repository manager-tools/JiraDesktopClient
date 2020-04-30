package com.almworks.jira.provider3.gui.edit.workflow.duplicate;

import com.almworks.items.api.DBOperationCancelledException;
import com.almworks.items.api.DBResult;
import com.almworks.items.gui.edit.engineactions.EditItemAction;
import com.almworks.items.sync.DBDrain;
import com.almworks.items.sync.DownloadProcedure;
import com.almworks.items.sync.SyncManager;
import com.almworks.jira.provider3.sync.ServerFields;
import com.almworks.util.images.Icons;
import com.almworks.util.properties.Role;
import com.almworks.util.ui.actions.ActionRegistry;
import com.almworks.util.ui.actions.AnAction;
import org.picocontainer.Startable;

public class ResolveAsDuplicateSupport implements Startable {
  public static final Role<ResolveAsDuplicateSupport> ROLE = Role.role(ResolveAsDuplicateSupport.class);
  public static final String RESOLVE_AS_DUPLICATE = "Jira.Edit.ResolveAsDuplicate";
  public static final AnAction ACTION = new EditItemAction("Resolve as Duplicate\u2026", Icons.RESOLVE_DUPLICATE, null, ResolveAsDuplicate.INSTANCE);

  private final ActionRegistry myActions;
  private final SyncManager myManager;
  private volatile long myResolutionField;

  public ResolveAsDuplicateSupport(ActionRegistry actions, SyncManager manager) {
    myActions = actions;
    myManager = manager;
  }

  @Override
  public void start() {
    myActions.registerAction(ResolveAsDuplicateSupport.RESOLVE_AS_DUPLICATE, ACTION);
    myManager.writeDownloaded(new DownloadProcedure<DBDrain>() {
      private long myField;

      @Override
      public void write(DBDrain drain) throws DBOperationCancelledException {
        myField = drain.materialize(ServerFields.RESOLUTION.getDBField());
      }

      @Override
      public void onFinished(DBResult<?> result) {
        if (result.isSuccessful()) myResolutionField = myField;
      }
    });
  }

  public long getResolutionField() {
    return myResolutionField;
  }

  @Override
  public void stop() {
  }
}
