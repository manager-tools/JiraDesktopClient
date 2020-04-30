package com.almworks.jira.provider3.issue.features.edit;

import com.almworks.api.application.ItemWrapper;
import com.almworks.api.application.ModelKey;
import com.almworks.api.application.UiItem;
import com.almworks.api.gui.MainMenu;
import com.almworks.integers.WritableLongList;
import com.almworks.items.api.DBReader;
import com.almworks.items.gui.edit.*;
import com.almworks.items.gui.edit.editors.composition.DelegatingFieldEditor;
import com.almworks.items.gui.edit.editors.enums.multi.BaseMultiEnumEditor;
import com.almworks.items.gui.edit.editors.enums.single.SingleEnumFieldEditor;
import com.almworks.items.gui.edit.engineactions.EngineConsts;
import com.almworks.items.gui.edit.helper.EditDescriptor;
import com.almworks.items.gui.edit.merge.*;
import com.almworks.items.gui.edit.util.BaseScalarFieldEditor;
import com.almworks.items.gui.meta.GuiFeaturesManager;
import com.almworks.items.sync.EditPrepare;
import com.almworks.items.sync.SyncManager;
import com.almworks.items.sync.VersionSource;
import com.almworks.items.sync.util.ShadowVersionSource;
import com.almworks.jira.provider3.comments.gui.MergeCommentVersion;
import com.almworks.jira.provider3.gui.edit.EditorsScheme;
import com.almworks.jira.provider3.gui.edit.ResolvedField;
import com.almworks.jira.provider3.gui.edit.editors.JiraEditUtils;
import com.almworks.jira.provider3.gui.edit.editors.ResolutionEditor;
import com.almworks.jira.provider3.gui.edit.editors.move.MoveController;
import com.almworks.jira.provider3.gui.timetrack.LoadedWorklog;
import com.almworks.jira.provider3.gui.timetrack.edit.MergeWorklogVersion;
import com.almworks.jira.provider3.gui.viewer.CommentImpl;
import com.almworks.jira.provider3.issue.editor.ScreenIssueEditor;
import com.almworks.jira.provider3.issue.features.BaseEditIssueFeature;
import com.almworks.jira.provider3.issue.features.edit.screens.ScreenChooser;
import com.almworks.jira.provider3.sync.ServerFields;
import com.almworks.util.LogHelper;
import com.almworks.util.components.plaf.macosx.Aqua;
import com.almworks.util.components.plaf.patches.Aero;
import com.almworks.util.config.Configuration;
import com.almworks.util.ui.AdjustedSplitPane;
import com.almworks.util.ui.DocumentFormAugmentor;
import com.almworks.util.ui.UIUtil;
import com.almworks.util.ui.actions.ActionContext;
import com.almworks.util.ui.actions.CantPerformException;
import com.almworks.util.ui.actions.UpdateRequest;
import gnu.trove.TLongObjectHashMap;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.almworks.util.Util;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.*;

class MergeFeature extends BaseEditIssueFeature {
  private static final EditorsScheme SCHEME = new EditorsScheme(BaseEditIssueFeature.SCHEME).addEditor(ServerFields.RESOLUTION, ResolutionEditor.NO_CHECK_EDITOR).fix();
  private static final ScreenIssueEditor EDITOR = new ScreenIssueEditor(true, new ScreenChooser(SCHEME, false));
  private static final TypedKey<MergeTableEditor> DIFF_TABLE_KEY = TypedKey.create("diffTable");

  public MergeFeature() {
    super(EDITOR);
  }

  @NotNull
  @Override
  public EditDescriptor.Impl doCheckContext(ActionContext context, UpdateRequest updateRequest) throws CantPerformException {
    updateRequest.watchRole(ItemWrapper.ITEM_WRAPPER);
    ItemWrapper issue = context.getSourceObject(ItemWrapper.ITEM_WRAPPER);
    CantPerformException.ensure(!issue.services().isRemoteDeleted());
    CantPerformException.ensure(issue.getDBStatus() == ItemWrapper.DBStatus.DB_CONFLICT);
    CantPerformException.ensure(context.getSourceObject(SyncManager.ROLE).findLock(issue.getItem()) == null);
    CantPerformException.ensure(JiraEditUtils.getIssueConnection(issue).isUploadAllowed());
    SyncManager syncManager = context.getSourceObject(SyncManager.ROLE);
    checkNotLocked(syncManager, getComments(context, issue));
    checkNotLocked(syncManager, LoadedWorklog.getWorklogs(issue));
    EditDescriptor.Impl descriptor = EditDescriptor.Impl.frame("merge.", EditIssueFeature.I18N.getString("edit.screens.window.merge.title"), EditIssueFeature.getWindowPrefSize());
    descriptor.addCommonActions(MainMenu.Merge.COMMIT_UPLOAD, MainMenu.NewItem.SAVE_DRAFT, MainMenu.NewItem.DISCARD,
      MainMenu.Merge.HIDE_NOT_CHANGED, MainMenu.Merge.RESOLVE_LOCAL, MainMenu.Merge.RESOLVE_REMOTE/*, JiraActions.MERGE_RESOLVE_COMMENT*/);
    descriptor.setDescriptionStrings(
      EditIssueFeature.I18N.getString("edit.screens.window.merge.notUploaded.title"),
      EditIssueFeature.I18N.getString("edit.screens.window.merge.notUploaded.message"),
      EditIssueFeature.I18N.getString("edit.screens.window.merge.saveDescription"),
      EditIssueFeature.I18N.getString("edit.screens.window.merge.uploadDescription"));
    return descriptor;
  }

  private void checkNotLocked(SyncManager syncManager, List<? extends UiItem> slaves) throws CantPerformException {
    for (UiItem slave : slaves) CantPerformException.ensure(syncManager.findLock(slave.getItem()) == null);
  }

  private List<? extends UiItem> getComments(ActionContext context, ItemWrapper issue) throws CantPerformException {
    ModelKey<List<CommentImpl>> commentsKey = CommentImpl.getModelKey(context.getSourceObject(GuiFeaturesManager.ROLE));
    List<CommentImpl> comments = issue.getModelKeyValue(commentsKey);
    return comments != null ? comments : Collections15.<UiItem>emptyList();
  }

  @Override
  public DefaultEditModel.Root setupModel(ActionContext context, WritableLongList itemsToLock) throws CantPerformException {
    ItemWrapper issue = context.getSourceObject(ItemWrapper.ITEM_WRAPPER);
    DefaultEditModel.Root model = setupEditIssues(itemsToLock, JiraEditUtils.getIssueConnection(issue), Collections.singleton(issue));
    addToLock(itemsToLock, getComments(context, issue));
    addToLock(itemsToLock, LoadedWorklog.getWorklogs(issue));
    return model;
  }

  private void addToLock(WritableLongList itemsToLock, List<? extends UiItem> items) {
    for (UiItem item : items) itemsToLock.add(item.getItem());
  }
  @Override
  protected VersionSource createSource(DBReader reader) {
    return ShadowVersionSource.base(reader);
  }

  @Override
  protected void afterChildPrepared(VersionSource source, EditItemModel child, EditPrepare editPrepare) {
    EngineConsts.ensureGuiFeatureManager(source, child);
    EditItemModel issueModel = getIssueModelFromNested(child);
    if (issueModel == null) return;
    MoveController move = MoveController.getInstance(issueModel);
    if (move != null) move.setCurrentMode(issueModel, MoveController.MODE_DISABLED);
    MergeTableEditor diffTable = MergeTableEditor.prepare(collectMerger(source.getReader(), issueModel));
    issueModel.putValue(DIFF_TABLE_KEY, diffTable);
  }


  private static final Set<ServerFields.Field> NOT_STANDARD_MERGE = Collections15.hashSet(ServerFields.COMMENTS, ServerFields.WORK_LOG,
    ServerFields.PARENT, ServerFields.ATTACHMENT, ServerFields.LINKS);
  private Collection<MergeValue> collectMerger(DBReader reader, EditItemModel issueModel) {
    ArrayList<MergeValue> mergers = Collections15.arrayList();
    for (ServerFields.Field field : ServerFields.EDITABLE_FIELDS) {
      if (NOT_STANDARD_MERGE.contains(field)) continue;
      FieldEditor editor = ResolvedField.findEditor(issueModel, FieldEditor.class, field.getJiraId());
      if (editor == null) {
        LogHelper.error("No editor found for field", field);
        continue;
      }
      MergeValue mergeValue = createMergeValue(reader, issueModel, editor);
      if (mergeValue != null) mergers.add(mergeValue);
    }
    for (Map.Entry<String, FieldEditor> entry : ResolvedField.getEditorsMap(issueModel).entrySet()) {
      if (ServerFields.isStatic(entry.getKey())) continue;
      MergeValue mergeValue = createMergeValue(reader, issueModel, entry.getValue());
      if (mergeValue != null) mergers.add(mergeValue);
    }
    MergeCommentVersion.collectMergers(reader, issueModel, mergers);
    MergeWorklogVersion.collectMergers(reader, issueModel, mergers);
    return mergers;
  }

  private static MergeValue createMergeValue(DBReader reader, EditItemModel issueModel, FieldEditor editor) {
    BaseScalarFieldEditor<?> scalarEditor = Util.castNullable(BaseScalarFieldEditor.class, editor);
    if (scalarEditor != null) return ScalarMergeValue.load(reader, issueModel, scalarEditor);
    SingleEnumFieldEditor singleEnum = Util.castNullable(SingleEnumFieldEditor.class, editor);
    if (singleEnum != null) return SingleEnumMergeValue.load(reader, issueModel, singleEnum);
    BaseMultiEnumEditor multiEnum = Util.castNullable(BaseMultiEnumEditor.class, editor);
    if (multiEnum != null) return MultiEnumMergeValue.load(reader, issueModel, multiEnum);
    DelegatingFieldEditor delegating = Util.castNullable(DelegatingFieldEditor.class, editor);
    if (delegating != null) {
      DelegatingFieldEditor.ModelWrapper wrapper = delegating.getWrapperModel(issueModel);
      return createMergeValue(reader, wrapper, wrapper.getEditor());
    }
    LogHelper.error("Unknown field editor", editor);
    return null;
  }

  @Nullable
  @Override
  protected JComponent doEditModel(Lifespan life, EditItemModel nested, Configuration config) {
    EditItemModel issueModel = getIssueModelFromNested(nested);
    if (issueModel == null) return null;
    JComponent issueEditor = super.doEditModel(life, nested, config);
    if (issueEditor == null) return null;
    MergeTableEditor diffTable = issueModel.getValue(DIFF_TABLE_KEY);
    if (diffTable == null) return issueEditor;
    diffTable.notifyModel(life, issueModel);
    JScrollPane table = diffTable.createTable(life, config.getOrCreateSubset("mergeTable"));
    AdjustedSplitPane splitPane = UIUtil.createSplitPane(table, issueEditor, false, config, "splitter", 0.3d, 0);
    createSplitPane(issueEditor, table, splitPane);
    splitPane.setOpaque(false);
    UIUtil.addOuterBorder(splitPane, UIUtil.EDITOR_PANEL_BORDER);
    new DocumentFormAugmentor().augmentForm(life, splitPane, false);
    return splitPane;
  }

  private void createSplitPane(JComponent issueEditor, JScrollPane table, AdjustedSplitPane splitPane) {
    if (Aero.isAero()) {
      Aero.cleanScrollPaneBorder(issueEditor);
      Aero.cleanScrollPaneBorder(table);
      Aero.makeBorderedDividerSplitPane(splitPane);
      splitPane.setBorder(Aero.getAeroBoxBorder());
    } else if (Aqua.isAqua()) {
      Aqua.cleanScrollPaneBorder(issueEditor);
      Aqua.cleanScrollPaneBorder(table);
      Aqua.makeLeopardStyleSplitPane(splitPane);
      splitPane.setBorder(Aqua.MAC_BORDER_BOX);
    }
  }

  @Override
  public boolean hasDataToCommit(EditItemModel model) {
    return super.hasDataToCommit(model) || !collectByItem(getNestedModel(model)).isEmpty();
  }

  @NotNull
  private TLongObjectHashMap<List<MergeValue>> collectByItem(@Nullable EditItemModel nested) {
    EditItemModel issueModel = getIssueModelFromNested(nested);
    TLongObjectHashMap<List<MergeValue>> result = new TLongObjectHashMap<>();
    if (issueModel == null) return result;
    MergeTableEditor diffTable = issueModel.getValue(DIFF_TABLE_KEY);
    if (diffTable == null) return result;
    Collection<MergeValue> values = diffTable.getMergeValues();
    while (!values.isEmpty()) {
      Iterator<MergeValue> it = values.iterator();
      MergeValue aValue = it.next();
      it.remove();
      long item = aValue.getItem();
      ArrayList<MergeValue> forItem = Collections15.arrayList();
      forItem.add(aValue);
      boolean hasResolved = aValue.isResolved();
      boolean resolved = !isUnresolved(aValue);
      while (it.hasNext()) {
        MergeValue next = it.next();
        if (next.getItem() != item) continue;
        it.remove();
        forItem.add(next);
        if (isUnresolved(next)) resolved = false;
        if (next.isResolved()) hasResolved = true;
      }
      if (!resolved || !hasResolved) continue;
      result.put(item, forItem);
    }
    return result;
  }

  private boolean isUnresolved(MergeValue aValue) {
    return aValue.isConflict() && !aValue.isResolved();
  }

  @Override
  protected void doCommit(CommitContext childContext) throws CancelCommitException {
    super.doCommit(childContext);
    TLongObjectHashMap<List<MergeValue>> byItem = collectByItem(childContext.getModel());
    for (long item : byItem.keys()) {
      List<MergeValue> values = byItem.get(item);
      if (item == childContext.getItem()) childContext.getDrain().markMerged(item);
      else for (MergeValue value : values) value.commit(childContext);
    }
  }
}
