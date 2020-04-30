package com.almworks.jira.provider3.gui.edit.workflow.duplicate;

import com.almworks.api.actions.ItemActionUtils;
import com.almworks.api.application.ItemWrapper;
import com.almworks.api.application.UiItem;
import com.almworks.api.syncreg.ItemHypercubeImpl;
import com.almworks.api.syncreg.ItemHypercubeUtils;
import com.almworks.integers.LongArray;
import com.almworks.integers.LongList;
import com.almworks.integers.WritableLongList;
import com.almworks.items.gui.edit.*;
import com.almworks.items.gui.edit.editors.composition.DelegatingFieldEditor;
import com.almworks.items.gui.edit.editors.composition.MandatoryEditor;
import com.almworks.items.gui.edit.engineactions.EngineConsts;
import com.almworks.items.gui.edit.helper.EditDescriptor;
import com.almworks.items.gui.edit.helper.EditFeature;
import com.almworks.items.gui.edit.util.TopEditor;
import com.almworks.items.gui.edit.util.VerticalLinePlacement;
import com.almworks.items.gui.meta.EnumTypesCollector;
import com.almworks.items.gui.meta.GuiFeaturesManager;
import com.almworks.items.gui.meta.LoadedItemKey;
import com.almworks.items.sync.EditPrepare;
import com.almworks.items.sync.SyncManager;
import com.almworks.items.sync.VersionSource;
import com.almworks.jira.provider3.app.connection.JiraConnection3;
import com.almworks.jira.provider3.gui.MetaSchema;
import com.almworks.jira.provider3.gui.edit.ResolvedField;
import com.almworks.jira.provider3.gui.edit.editors.JiraEditUtils;
import com.almworks.jira.provider3.gui.edit.editors.ResolutionEditor;
import com.almworks.jira.provider3.gui.edit.workflow.ActionApplication;
import com.almworks.jira.provider3.gui.edit.workflow.WFActionEditor2;
import com.almworks.jira.provider3.gui.edit.workflow.WFActionsLoader2;
import com.almworks.jira.provider3.links.actions.AddLinksEditor;
import com.almworks.jira.provider3.links.actions.DirectionalLinkType;
import com.almworks.jira.provider3.schema.Resolution;
import com.almworks.jira.provider3.sync.ServerFields;
import com.almworks.util.LogHelper;
import com.almworks.util.Pair;
import com.almworks.util.config.Configuration;
import com.almworks.util.text.NameMnemonic;
import com.almworks.util.ui.actions.ActionContext;
import com.almworks.util.ui.actions.CantPerformException;
import com.almworks.util.ui.actions.UpdateRequest;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.almworks.util.detach.DetachComposite;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

class ResolveAsDuplicate extends TopEditor {
  private static final Pattern HARDCODED_RESOLVE_ACTION_NAME_PATTERN = Pattern.compile("^RESOLVE(\\s+.*)?$", Pattern.CASE_INSENSITIVE);
  private static final String HARDCODED_DUPLICATES_LINK = "duplicates";
  private static final String HARDCODED_DUPLICATE_RESOLUTION = "duplicate";

  public static final EditFeature INSTANCE = new ResolveAsDuplicate();

  private final TypedKey<DirectionalLinkType> DUPLICATE_KEY = TypedKey.create("resolveAsDuplicate/link");
  private final TypedKey<LoadedItemKey> RESOLUTION_KEY = TypedKey.create("resolveAsDuplicate/resolution");

//  private final AddLinksEditor myAddLinkEditor = AddLinksEditor.inlineAddLinks("Add Link", false);
  private final WFActionEditor2 myActionEditor;
  private final MandatoryEditor myResolutionEditor;

  private ResolveAsDuplicate() {
    super(NameMnemonic.rawText("Resolve as Duplicate"));
    myResolutionEditor = new MandatoryEditor(ResolutionEditor.BASIC_EDITOR, "Resolution is not set");
    myActionEditor = new WFActionEditor2.Builder()
      .addBottom(ServerFields.LINKS)
      .replace(ServerFields.RESOLUTION, myResolutionEditor)
      .create();
  }

  @Override
  public EditDescriptor checkContext(ActionContext context, UpdateRequest updateRequest) throws CantPerformException {
    updateRequest.watchRole(ItemWrapper.ITEM_WRAPPER);
    updateRequest.watchModifiableRole(SyncManager.MODIFIABLE);
    Pair<JiraConnection3,List<ItemWrapper>> pair = JiraEditUtils.selectIssuesWrappers(context);
    List<ItemWrapper> issues = pair.getSecond();
    JiraConnection3 connection = pair.getFirst();
    CantPerformException.ensureNotEmpty(issues);
    CantPerformException.ensure(connection.isUploadAllowed());
    updateRequest.watchModifiableRole(SyncManager.MODIFIABLE);
    ItemActionUtils.checkNotLocked(context, issues);
    CantPerformException.ensure(connection.isUploadAllowed());
    ActionApplication action = chooseAction(context, issues, connection);
    EditDescriptor.Impl descriptor = EditDescriptor.Impl.notModal(action.getWindowId(), getWindowTitle(issues), null);
    chooseLinkType(connection);
    chooseResolution(connection);
    descriptor.addCommonActions(EditDescriptor.COMMON_ACTIONS);
    descriptor.setDescriptionStrings(
      "Resolve as Duplicate",
      "Updated issue was saved in the local database.",
      "Save updated issues in the local database without uploading to server",
      "Save updated issues and upload to server");
    return descriptor;
  }

  private String getWindowTitle(List<ItemWrapper> issues) {
    String issueTitle;
    if (issues.size() == 1) {
      String key = MetaSchema.issueKey(issues.get(0));
      issueTitle = key != null ? key : "Issue";
    } else issueTitle = issues.size() + " Issues";
    return "Resolve " + issueTitle + " as Duplicate";
  }

  @NotNull
  private DirectionalLinkType chooseLinkType(JiraConnection3 connection) throws CantPerformException {
    List<DirectionalLinkType> links = getLinks(connection);
    DirectionalLinkType candidate = null;
    for (DirectionalLinkType link : links) {
      if (!HARDCODED_DUPLICATES_LINK.equalsIgnoreCase(link.getDescription())) continue;
      if (candidate != null) throw new CantPerformException();
      candidate = link;
    }
    return CantPerformException.ensureNotNull(candidate);
  }

  private List<DirectionalLinkType> getLinks(JiraConnection3 connection) {
    GuiFeaturesManager manager = connection.getActor(GuiFeaturesManager.ROLE);
    DetachComposite life = new DetachComposite();
    try {
      return Collections15.arrayList(DirectionalLinkType.createModel(life, manager, connection.getConnectionItem()));
    } finally {
      life.detach();
    }
  }

  private LoadedItemKey chooseResolution(JiraConnection3 connection) throws CantPerformException {
    EnumTypesCollector.Loaded resolutions = CantPerformException.ensureNotNull(connection.getGuiFeatures().getEnumTypes().getType(Resolution.ENUM_TYPE));
    DetachComposite life = new DetachComposite();
    ArrayList<LoadedItemKey> keys;
    try {
      ItemHypercubeImpl cube = new ItemHypercubeImpl();
      ItemHypercubeUtils.adjustForConnection(cube, connection);
      keys = Collections15.arrayList(CantPerformException.ensureNotNull(resolutions.getValueModel(life, cube)).toList());
    } finally {
      life.detach();
    }
    LoadedItemKey duplicate = null;
    for (LoadedItemKey key : keys) {
      if (!HARDCODED_DUPLICATE_RESOLUTION.equalsIgnoreCase(key.getDisplayName())) continue;
      CantPerformException.ensure(duplicate == null);
      duplicate = key;
    }
    return CantPerformException.ensureNotNull(duplicate);
  }

  @NotNull
  private ActionApplication chooseAction(ActionContext context, List<ItemWrapper> issues, JiraConnection3 connection) throws CantPerformException {
    ResolveAsDuplicateSupport support = context.getSourceObject(ResolveAsDuplicateSupport.ROLE);
    long resolutionField = support.getResolutionField();
    CantPerformException.ensure(resolutionField > 0);
    WFActionsLoader2 loader =  connection.getProvider().getWorkflowActions();
    Collection<String> actionNames = loader.getActioNames();
    ActionApplication application = null;
    for (String name : actionNames) {
      if (!HARDCODED_RESOLVE_ACTION_NAME_PATTERN.matcher(name).matches()) continue;
      ActionApplication app = ActionApplication.create(issues, connection, name);
      if (app == null) continue;
      if (!app.getFields().contains(resolutionField)) continue;
      if (application != null) throw new CantPerformException();
      application = app;
    }
    return CantPerformException.ensureNotNull(application);
  }

  @Override
  public DefaultEditModel.Root setupModel(ActionContext context, WritableLongList itemsToLock) throws CantPerformException {
    Pair<JiraConnection3,List<ItemWrapper>> pair = JiraEditUtils.selectIssuesWrappers(context);
    List<ItemWrapper> issues = pair.getSecond();
    JiraConnection3 connection = pair.getFirst();
    LongArray issueItems = LongArray.create(UiItem.GET_ITEM.collectList(issues));
    CantPerformException.ensure(!issueItems.isEmpty());
    DirectionalLinkType linkType = chooseLinkType(connection);
    LoadedItemKey resolution = chooseResolution(connection);
    ActionApplication action = chooseAction(context, issues, connection);
    itemsToLock.addAll(issueItems);
    DefaultEditModel.Root model = DefaultEditModel.Root.editItems(issueItems);
    model.putHint(DUPLICATE_KEY, linkType);
    model.putHint(RESOLUTION_KEY, resolution);
    EngineConsts.setupConnection(model, connection);
    myActionEditor.setActionApplication(model, action);
    return model;
  }

  @Override
  protected Pair<DefaultEditModel.Child, ? extends List<? extends FieldEditor>> createNestedModel(VersionSource source, EditItemModel parent, EditPrepare editPrepare) {
    LongList issues = parent.getEditingItems();
    ActionApplication action = myActionEditor.getActionApplication(parent);
    if (action == null) return null;
    DefaultEditModel.Child child = DefaultEditModel.Child.editItems(parent, issues, false);
    child.copyHints(parent, DUPLICATE_KEY, RESOLUTION_KEY);
    myActionEditor.setActionApplication(child, action);
    return Pair.create(child, Arrays.asList(myActionEditor));
  }

  @Override
  protected JComponent doEditModel(Lifespan life, EditItemModel model, Configuration config) {
    DirectionalLinkType link = model.getValue(DUPLICATE_KEY);
    EditItemModel editorsModel = myActionEditor.getNestedModel(model);
    if (link == null || editorsModel == null) {
      LogHelper.error("Missing data", link, editorsModel);
      return null;
    }
    AddLinksEditor editor = ResolvedField.findEditor(editorsModel, AddLinksEditor.class, ServerFields.LINKS.getJiraId());
    if (editor == null) LogHelper.error("Missing link editor", ResolvedField.findEditor(editorsModel, FieldEditor.class, ServerFields.LINKS.getJiraId()));
    else editor.setLinkType(editorsModel, link);
    return VerticalLinePlacement.buildTopComponent(life, model, myActionEditor);
  }

  @Override
  public void afterModelFixed(EditItemModel model) {
    super.afterModelFixed(model);
    model = getNestedModel(model);
    if (model == null) {
      LogHelper.error("Missing model");
      return;
    }
    LoadedItemKey resolution = model.getValue(RESOLUTION_KEY);
    EditItemModel editorsModel = myActionEditor.getNestedModel(model);
    if (resolution == null || editorsModel == null) {
      LogHelper.error("Missing resolution data", resolution, editorsModel);
      return;
    }
    DelegatingFieldEditor.ModelWrapper resolutionModel = myResolutionEditor.getWrapperModel(editorsModel);
    ResolutionEditor.BASIC_EDITOR.setExternalValue(resolutionModel, resolution.getItem(), resolution);
  }

  @Override
  protected void doCommit(CommitContext childContext) throws CancelCommitException {
    childContext.commitEditors(null);
  }
}
