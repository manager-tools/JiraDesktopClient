package com.almworks.jira.provider3.links.actions;

import com.almworks.api.application.ItemWrapper;
import com.almworks.api.application.LoadedItem;
import com.almworks.integers.LongArray;
import com.almworks.integers.WritableLongList;
import com.almworks.items.api.DBReader;
import com.almworks.items.gui.edit.DefaultEditModel;
import com.almworks.items.gui.edit.EditItemModel;
import com.almworks.items.gui.edit.engineactions.EngineConsts;
import com.almworks.items.gui.edit.helper.EditDescriptor;
import com.almworks.items.gui.edit.helper.EditFeature;
import com.almworks.items.gui.edit.util.FieldEditorUtil;
import com.almworks.items.gui.meta.GuiFeaturesManager;
import com.almworks.items.sync.EditPrepare;
import com.almworks.items.sync.util.BranchSource;
import com.almworks.jira.provider3.app.connection.JiraConnection3;
import com.almworks.jira.provider3.gui.actions.JiraActions;
import com.almworks.jira.provider3.gui.edit.editors.JiraEditUtils;
import com.almworks.util.LogHelper;
import com.almworks.util.Pair;
import com.almworks.util.config.Configuration;
import com.almworks.util.text.NameMnemonic;
import com.almworks.util.ui.actions.ActionContext;
import com.almworks.util.ui.actions.AnAction;
import com.almworks.util.ui.actions.CantPerformException;
import com.almworks.util.ui.actions.UpdateRequest;
import org.almworks.util.TypedKey;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.List;

public abstract class AddLinksFeature implements EditFeature {
  public static final EditFeature INSTANCE = new Feature();
  public static final AnAction ACTION_REMOVE_LINKS = RemoveLinkAction.INSTANCE;
  public static final AnAction ACTION_LINK_TWO_ISSUES = LinkTwoIssues.INSTANCE;
  public static final AnAction ACTION_VIEW_ALL_LINKED = ViewAllLinkedAction.ALL_LINKS;
  public static final AnAction ACTION_VIEW_LINKED = ViewLinkedIssuesAction.INSTANCE;

  private static final AddLinksEditor EDITOR = AddLinksEditor.linkIssues(NameMnemonic.rawText("T&argets"), NameMnemonic.parseString("Link &Type"));
  private final TypedKey<List<LoadedItem>> myIssuesKey = TypedKey.create("linkIssues");

  protected DefaultEditModel.Root setupModel(JiraConnection3 connection, List<LoadedItem> issues) {
    DefaultEditModel.Root model = DefaultEditModel.Root.editItems(LongArray.create(ItemWrapper.GET_ITEM.collectList(issues)));
    EngineConsts.setupConnection(model, connection);
    model.putHint(myIssuesKey, issues);
    return model;
  }

  public void prepareEdit(DBReader reader, DefaultEditModel.Root model, @Nullable EditPrepare editPrepare) {
    BranchSource source = BranchSource.trunk(reader);
    EngineConsts.ensureGuiFeatureManager(source, model);
    EDITOR.prepareModel(source, model, editPrepare);
  }

  private CreateLinksOutboundForm createForm(Lifespan life, EditItemModel model) {
    GuiFeaturesManager features = EngineConsts.getGuiFeaturesManager(model);
    if (features == null) return null;
    final CreateLinksOutboundForm form = new CreateLinksOutboundForm(features);
    List<LoadedItem> issues = model.getValue(myIssuesKey);
    if (issues != null) form.setSourceIssues(issues);
    EDITOR.attach(life, model, form.getLinkType(), form.getOppositeIssues());
    return form;
  }

  public static EditFeature linkIssues(JiraConnection3 connection, List<LoadedItem> loaded, List<String> keys) {
    return new LinkIssues(connection, loaded, keys);
  }

  @Override
  public JComponent editModel(Lifespan life, EditItemModel model, Configuration editorConfig) {
    CreateLinksOutboundForm form = createForm(life, model);
    if (form == null) return null;
    JPanel panel = form.getWholePanel();
    FieldEditorUtil.setupTopWhitePanel(life, panel);
    return panel;
  }

  private static class Feature extends AddLinksFeature {
    @Override
    public EditDescriptor checkContext(ActionContext context, UpdateRequest updateRequest) throws CantPerformException {
      updateRequest.watchRole(ItemWrapper.ITEM_WRAPPER);
      if (!JiraEditUtils.selectLoadedIssues(context).getFirst().isUploadAllowed()) throw new CantPerformException();
      EditDescriptor.Impl descriptor = JiraActions.addLinksDescriptor("Create Outbound Links");
      descriptor.setContextKey(JiraEditUtils.getContextKey(this, context));
      return descriptor;
    }

    @Override
    public DefaultEditModel.Root setupModel(ActionContext context, WritableLongList itemsToLock)
      throws CantPerformException
    {
      Pair<JiraConnection3, List<LoadedItem>> issues = JiraEditUtils.selectLoadedIssues(context);
      return setupModel(issues.getFirst(), issues.getSecond());
    }
  }

  static class LinkIssues extends AddLinksFeature {
    private final JiraConnection3 myConnection;
    private final List<LoadedItem> myOutbound;
    private final List<String> myInitialKeys;

    public LinkIssues(JiraConnection3 connection, List<LoadedItem> outbound, List<String> initialKeys) {
      LogHelper.assertError(!outbound.isEmpty(), "Missing issues");
      myConnection = connection;
      myOutbound = outbound;
      myInitialKeys = initialKeys;
    }

    @Override
    public EditDescriptor checkContext(ActionContext context, UpdateRequest updateRequest) throws CantPerformException {
      EditDescriptor.Impl descriptor = JiraActions.addLinksDescriptor("Link Issues");
      descriptor.setContextKey(JiraEditUtils.getContextKey(this, myOutbound.get(0)));
      return descriptor;
    }

    @Override
    public DefaultEditModel.Root setupModel(ActionContext context, WritableLongList itemsToLock) throws CantPerformException {
      return setupModel(myConnection, myOutbound);
    }

    @Override
    public void prepareEdit(DBReader reader, DefaultEditModel.Root model, @Nullable EditPrepare editPrepare) {
      super.prepareEdit(reader, model, editPrepare);
      EDITOR.setOppositeKeys(model, myInitialKeys);
    }
  }
}
