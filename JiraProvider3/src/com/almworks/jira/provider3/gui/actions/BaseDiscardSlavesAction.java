package com.almworks.jira.provider3.gui.actions;

import com.almworks.api.application.ItemWrapper;
import com.almworks.api.application.UiItem;
import com.almworks.items.sync.SyncManager;
import com.almworks.jira.provider3.app.connection.JiraConnection3;
import com.almworks.util.ui.actions.ActionContext;
import com.almworks.util.ui.actions.CantPerformException;
import com.almworks.util.ui.actions.SimpleAction;
import com.almworks.util.ui.actions.UpdateContext;
import org.almworks.util.TypedKey;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.List;

public abstract class BaseDiscardSlavesAction<S extends UiItem> extends SimpleAction {
  private final TypedKey<S> myDataRole;

  public BaseDiscardSlavesAction(@Nullable String name, @Nullable Icon icon, TypedKey<S> dataRole) {
    super(name, icon);
    myDataRole = dataRole;
    watchRole(ItemWrapper.ITEM_WRAPPER);
    watchRole(dataRole);
  }

  @Override
  protected void customUpdate(UpdateContext context) throws CantPerformException {
    List<ItemWrapper> issues = context.getSourceCollection(ItemWrapper.ITEM_WRAPPER);
    for (ItemWrapper issue : issues) {
      JiraConnection3 connection = CantPerformException.ensureNotNull(issue.services().getConnection(JiraConnection3.class));
      CantPerformException.ensure(connection.isUploadAllowed());
    }
    List<S> slaves = getSlaves(context);
    updatePresentation(context, slaves);
  }

  protected abstract void updatePresentation(UpdateContext context, List<S> slaves);

  @Override
  protected void doPerform(ActionContext context) throws CantPerformException {
    confirmAndDelete(context, getSlaves(context));
  }

  protected abstract void confirmAndDelete(ActionContext context, List<S> slaves) throws CantPerformException;

  protected abstract boolean canDelete(S slave, ActionContext context);

  private List<S> getSlaves(ActionContext context) throws CantPerformException {
    List<S> slaves = CantPerformException.ensureNotEmpty(context.getSourceCollection(myDataRole));
    SyncManager manager = context.getSourceObject(SyncManager.ROLE);
    for (S slave : slaves) {
      CantPerformException.ensure(canDelete(slave, context));
      CantPerformException.ensure(manager.findLock(slave.getItem()) == null);
    }
    return slaves;
  }
}
