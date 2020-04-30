package com.almworks.jira.provider3.app.connection;

import com.almworks.api.application.ItemWrapper;
import com.almworks.api.application.LoadedItem;
import com.almworks.api.application.WorkflowComponent2;
import com.almworks.api.application.util.ExportDescription;
import com.almworks.api.application.util.ItemExport;
import com.almworks.api.application.util.WorkflowToolbarEntry;
import com.almworks.api.application.viewer.Comment;
import com.almworks.api.application.viewer.textdecorator.TextDecoratorRegistry;
import com.almworks.api.config.MiscConfig;
import com.almworks.api.gui.MainMenu;
import com.almworks.engine.gui.attachments.Attachment;
import com.almworks.items.gui.edit.engineactions.EditItemAction;
import com.almworks.items.gui.edit.engineactions.NewItemAction;
import com.almworks.items.gui.edit.helper.EditDescriptor;
import com.almworks.items.gui.edit.helper.EditFeature;
import com.almworks.items.gui.meta.GuiFeaturesManager;
import com.almworks.items.gui.meta.LoadedModelKey;
import com.almworks.items.gui.meta.util.AbstractMetaInfo;
import com.almworks.items.sync.util.identity.DBStaticObject;
import com.almworks.jira.provider3.attachments.JiraAttachments;
import com.almworks.jira.provider3.gui.MetaSchema;
import com.almworks.jira.provider3.gui.actions.JiraActions;
import com.almworks.jira.provider3.gui.edit.workflow.duplicate.ResolveAsDuplicateSupport;
import com.almworks.jira.provider3.gui.viewer.IssueViewer3;
import com.almworks.jira.provider3.links.actions.AddLinksFeature;
import com.almworks.jira.provider3.remotedata.issue.edit.PrepareIssueUpload;
import com.almworks.timetrack.gui.TimeTrackingActions;
import com.almworks.util.components.ToolbarBuilder;
import com.almworks.util.config.Configuration;
import com.almworks.util.exec.Context;
import com.almworks.util.i18n.text.LocalizedAccessor;
import com.almworks.util.ui.UIComponentWrapper2;
import com.almworks.util.ui.actions.ActionContext;
import com.almworks.util.ui.actions.AnAction;
import com.almworks.util.ui.actions.CantPerformException;
import com.almworks.util.ui.actions.IdActionProxy;
import org.almworks.util.Collections15;
import org.almworks.util.Util;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class JiraMetaInfo3 extends AbstractMetaInfo {
  private static final LocalizedAccessor.Value M_NO_DETAILS_SHORT = PrepareIssueUpload.I18N.getFactory("message.issue.noDetails.short");
  private static final LocalizedAccessor.Value M_NO_DETAILS_HTML = PrepareIssueUpload.I18N.getFactory("message.issue.noDetails.fullHtml");

  private static final List<AnAction> ACTIONS = Collections.unmodifiableList(createActions());
  private static final List<DBStaticObject> SLAVE_KEYS =
    Collections15.unmodifiableListCopy(MetaSchema.KEY_COMMENTS_LIST, MetaSchema.KEY_ATTACHMENTS_LIST,
      MetaSchema.KEY_LINKS_LIST, MetaSchema.KEY_WORKLOG_LIST);
  private final Configuration myConfig;
  private final TextDecoratorRegistry myDecorators;
  private final JiraProvider3 myProvider;

  private JiraMetaInfo3(Configuration config, TextDecoratorRegistry decorators, JiraProvider3 provider) {
    super(provider.getFeaturesManager());
    myConfig = config;
    myDecorators = decorators;
    myProvider = provider;
  }

  public static JiraMetaInfo3 create(Configuration config, TextDecoratorRegistry decorators, JiraProvider3 provider) {
    return new JiraMetaInfo3(config, decorators, provider);
  }

  @Override
  protected UIComponentWrapper2 createViewerComponent(Configuration config) {
    return new IssueViewer3(config, myDecorators, getGuiFeatures(), myProvider.getActor(MiscConfig.ROLE));
  }

  @Override
  public List<? extends AnAction> getWorkflowActions() {
    List<AnAction> actions = Collections15.arrayList();
    actions.addAll(myProvider.getWorkflowActions().getModel().toList());
    return Collections.unmodifiableList(actions);
  }

  @Override
  public List<? extends AnAction> getActions() {
    return ACTIONS;
  }

  @Override
  protected List<DBStaticObject> getSlavesToDiscardKeys() {
    return SLAVE_KEYS;
  }

  @Override
  public boolean canImport(ActionContext context, ItemWrapper target, List<ItemWrapper> items)
    throws CantPerformException
  {
    String targetKey = MetaSchema.issueKey(target);
    if (targetKey == null)
      return false;
    JiraConnection3 connection = Util.castNullable(JiraConnection3.class, target.getConnection());
    return toLoadedList(connection, Collections.singletonList(targetKey), items) != null;
  }

  @Nullable
  private List<LoadedItem> toLoadedList(JiraConnection3 connection, List<String> issueKeys, List<? extends ItemWrapper> wrappers) {
    if (connection == null || issueKeys.isEmpty()) return null;
    List<LoadedItem> loadedItems = Collections15.arrayList();
    for (ItemWrapper item : wrappers) {
      LoadedItem loaded = Util.castNullable(LoadedItem.class, item);
      if (loaded == null) return null;
      if (loaded.getConnection() != connection) return null;
      String artifactKey = MetaSchema.issueKey(item);
      if (artifactKey != null)
        for (String s : issueKeys)
          if (artifactKey.equals(s))
            return null;
      loadedItems.add(loaded);
    }
    return loadedItems;
  }

  @Override
  public void importItems(List<ItemWrapper> targets, List<? extends ItemWrapper> items, ActionContext context)
    throws CantPerformException
  {
    List<String> keys = new ArrayList<String>();
    for (ItemWrapper iw : targets)
      keys.add(MetaSchema.issueKey(iw));
    JiraConnection3 connection = Util.castNullable(JiraConnection3.class, targets.get(0).getConnection());
    List<LoadedItem> loaded = CantPerformException.ensureNotEmpty(toLoadedList(connection, keys, items));
    EditFeature editor = AddLinksFeature.linkIssues(connection, loaded, keys);
    EditDescriptor descriptor = EditItemAction.preparePerform(context, editor);
    NewItemAction.peform(context, editor, descriptor);
  }

  @Override
  public void acceptFiles(ActionContext context, ItemWrapper item, List<File> fileList) throws CantPerformException {
    JiraAttachments.attachFiles(context, item, fileList);
  }

  @Override
  public void acceptImage(ActionContext context, ItemWrapper issue, Image image) throws CantPerformException {
    JiraAttachments.attachImage(context, issue, image);
  }

  @Override
  public String getPartialDownloadHtml() {
    return M_NO_DETAILS_HTML.create();
  }

  @Override
  public String getPartialDownloadShort() {
    return M_NO_DETAILS_SHORT.create();
  }

  private static List<AnAction> createActions() {
    List<AnAction> actions = Collections15.arrayList();
    actions.add(new IdActionProxy(JiraActions.ADD_COMMENT));
    actions.add(new IdActionProxy(JiraActions.ATTACH_FILE));
    actions.add(new IdActionProxy(JiraActions.ATTACH_SCREEN_SHOT));
    actions.add(new IdActionProxy(JiraActions.ATTACH_TEXT));
    actions.add(new IdActionProxy(JiraActions.CREATE_SUBTASK));
    actions.add(new IdActionProxy(JiraActions.ASSIGN_TO));
    actions.add(new IdActionProxy(JiraActions.ADD_LINKS));
    actions.add(new IdActionProxy(JiraActions.LINK_TWO_ISSUES));
    actions.add(new IdActionProxy(ResolveAsDuplicateSupport.RESOLVE_AS_DUPLICATE));
    actions.add(new IdActionProxy(JiraActions.MOVE_ISSUE));
    actions.add(new IdActionProxy(JiraActions.VOTE_FOR_ISSUE));
    actions.add(new IdActionProxy(JiraActions.WATCH_ISSUE));
    actions.add(new IdActionProxy(JiraActions.LOG_WORK));
    actions.add(TimeTrackingActions.START_WORK);
    actions.add(TimeTrackingActions.STOP_WORK);
    actions.add(new IdActionProxy(JiraActions.VIEW_TIME_REPORT));
    actions.add(new IdActionProxy(JiraActions.EDIT_WATCHERS));
    return actions;
  }

  @Override
  protected ToolbarBuilder createToolbar(boolean singleArtifact) {
    Configuration toolbarConfig = myConfig.getOrCreateSubset("toolbar");
    ToolbarBuilder builder = ToolbarBuilder.smallEnabledButtons();
    builder.addAction(MainMenu.Edit.EDIT_ITEM);
    builder.addAction(JiraActions.ADD_COMMENT);
    builder.addAction(JiraActions.EDIT_COMMENT);
    builder.addAction(JiraActions.REPLY_TO_COMMENT);
    builder.addAction(JiraActions.ATTACH_FILE);
    builder.addAction(JiraActions.ATTACH_SCREEN_SHOT);
    builder.addAction(JiraActions.ATTACH_TEXT);
    builder.addAction(JiraActions.CREATE_SUBTASK);

//      builder.addSeparator();
    builder.addAction(JiraActions.ASSIGN_TO);
    builder.addAction(JiraActions.ADD_LINKS);
    WorkflowComponent2 workflowComponent = Context.get(WorkflowComponent2.class);
    if (workflowComponent != null)
      builder.add(new WorkflowToolbarEntry(toolbarConfig.getOrCreateSubset("workflow"),
        workflowComponent.getWorkflowActions()));
    builder.add(new WorkflowToolbarEntry(toolbarConfig.getOrCreateSubset("workflow"), myProvider.getWorkflowActions().getModel()));
    builder.addAction(ResolveAsDuplicateSupport.RESOLVE_AS_DUPLICATE);
    builder.addAction(JiraActions.MOVE_ISSUE);

//      builder.addSeparator();
    builder.addAction(MainMenu.Edit.DOWNLOAD);
    if (!singleArtifact) {
      builder.addAction(MainMenu.Edit.UPLOAD);
      builder.addAction(MainMenu.Edit.DISCARD);
    }

//      builder.addSeparator();
    builder.addAction(MainMenu.Edit.TAG);

    builder.addAction(JiraActions.VOTE_FOR_ISSUE);
    builder.addAction(JiraActions.WATCH_ISSUE);

//      builder.addSeparator();
    builder.addAction(JiraActions.LOG_WORK);
    builder.addAction(TimeTrackingActions.START_WORK);
    builder.addAction(TimeTrackingActions.STOP_WORK);

    return builder;
  }

  @Override
  public ExportDescription getExportDescription() {
    GuiFeaturesManager features = getGuiFeatures();
    ItemExport key = features.findExport(MetaSchema.EXPORT_KEY);
    ItemExport summary = features.findExport(MetaSchema.EXPORT_SUMMARY);
    LoadedModelKey<List<Comment>> comments = features.findListModelKey(MetaSchema.KEY_COMMENTS_LIST, Comment.class);
    LoadedModelKey<List<Attachment>> attachments = features.findListModelKey(MetaSchema.KEY_ATTACHMENTS_LIST, Attachment.class);
    return new ExportDescription("issue", features.getExports(), key, summary, comments, attachments);
  }
}
