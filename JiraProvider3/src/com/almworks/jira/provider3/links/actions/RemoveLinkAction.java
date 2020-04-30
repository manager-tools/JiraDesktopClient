package com.almworks.jira.provider3.links.actions;

import com.almworks.api.actions.ItemActionUtils;
import com.almworks.api.application.ItemUiModel;
import com.almworks.api.application.ItemWrapper;
import com.almworks.api.engine.Engine;
import com.almworks.integers.LongArray;
import com.almworks.integers.LongList;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBOperationCancelledException;
import com.almworks.items.gui.meta.GuiFeaturesManager;
import com.almworks.items.sync.EditDrain;
import com.almworks.items.sync.ItemVersion;
import com.almworks.items.sync.SyncManager;
import com.almworks.jira.provider3.gui.MetaSchema;
import com.almworks.jira.provider3.gui.actions.BaseDiscardSlavesAction;
import com.almworks.jira.provider3.gui.actions.DeleteSlavesCommit;
import com.almworks.jira.provider3.links.JiraLinks;
import com.almworks.jira.provider3.links.LinksTree;
import com.almworks.jira.provider3.links.LoadedLink2;
import com.almworks.jira.provider3.schema.Link;
import com.almworks.util.components.ATable;
import com.almworks.util.images.Icons;
import com.almworks.util.ui.actions.ActionContext;
import com.almworks.util.ui.actions.AnAction;
import com.almworks.util.ui.actions.CantPerformException;
import com.almworks.util.ui.actions.UpdateContext;

import javax.swing.*;
import java.awt.*;
import java.util.List;

class RemoveLinkAction extends BaseDiscardSlavesAction<LoadedLink2> {
  public static final AnAction INSTANCE = new RemoveLinkAction();

  private RemoveLinkAction() {
    super("Remove Selected Links", Icons.ACTION_REMOVE_LINK, LoadedLink2.DB_LINK);
  }

  @Override
  protected void updatePresentation(UpdateContext context, List<LoadedLink2> slaves) {
  }

  @Override
  protected void confirmAndDelete(ActionContext context, List<LoadedLink2> links) throws CantPerformException {
    ItemWrapper model = context.getSourceObject(ItemUiModel.ITEM_WRAPPER);
    GuiFeaturesManager features = context.getSourceObject(GuiFeaturesManager.ROLE);
    String ownKey = model.getModelKeyValue(MetaSchema.issueKey(features));
    if (ownKey == null) ownKey = "New Issue";
    LinksTree linksTree = LinksTree.create(features, ownKey);
    linksTree.update(links);
    ATable<?> table = linksTree.showAsTree();
    table.setEnabled(false);
    table.resizeColumns();
    Boolean upload = ItemActionUtils.askConfirmation(context, new JScrollPane(table), "Remove Issue Links", "JIRA.removeLinks", true);
    if (upload == null) return;
    deleteLinks(context, links, upload);
  }

  private void deleteLinks(ActionContext context, List<LoadedLink2> links, Boolean upload) throws CantPerformException {
    LongArray slaves = ItemActionUtils.collectItems(links);
    Engine engine = context.getSourceObject(Engine.ROLE);
    context.getSourceObject(SyncManager.ROLE).commitEdit(slaves, new MyCommit(engine, slaves, upload, context.getComponent()));
  }

  @Override
  protected boolean canDelete(LoadedLink2 slave, ActionContext context) {
    return true;
  }

  private static class MyCommit extends DeleteSlavesCommit {
    public MyCommit(Engine engine, LongArray slaves, boolean upload, Component component) {
      super(engine, slaves, component, upload, new DBAttribute[] {Link.SOURCE.getAttribute(), Link.TARGET.getAttribute()});
    }

    @Override
    public void performCommit(EditDrain drain) throws DBOperationCancelledException {
      LongList links = getSlaves();
      JiraLinks.deleteLinks(drain, links);
      for (ItemVersion link : drain.readItems(links)) addSlaveToUpload(link);
    }
  }
}
