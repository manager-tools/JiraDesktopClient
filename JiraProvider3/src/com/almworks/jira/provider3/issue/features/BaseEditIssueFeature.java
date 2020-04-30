package com.almworks.jira.provider3.issue.features;

import com.almworks.api.application.ItemWrapper;
import com.almworks.gui.InitialWindowFocusFinder;
import com.almworks.integers.LongArray;
import com.almworks.integers.LongList;
import com.almworks.integers.WritableLongList;
import com.almworks.items.gui.edit.*;
import com.almworks.items.gui.edit.engineactions.EngineConsts;
import com.almworks.items.gui.edit.helper.EditDescriptor;
import com.almworks.items.gui.edit.util.DefaultValues;
import com.almworks.items.gui.edit.util.TopEditor;
import com.almworks.items.sync.EditPrepare;
import com.almworks.items.sync.SyncManager;
import com.almworks.items.sync.VersionSource;
import com.almworks.jira.provider3.app.connection.JiraConnection3;
import com.almworks.jira.provider3.attachments.AttachmentsEditor;
import com.almworks.jira.provider3.gui.actions.JiraActions;
import com.almworks.jira.provider3.gui.edit.EditMetaSchema;
import com.almworks.jira.provider3.gui.edit.EditorsScheme;
import com.almworks.jira.provider3.gui.edit.editors.ResolutionEditor;
import com.almworks.jira.provider3.gui.edit.editors.move.ParentSupport;
import com.almworks.jira.provider3.issue.editor.ScreenIssueEditor;
import com.almworks.jira.provider3.links.actions.LinksEditor;
import com.almworks.jira.provider3.permissions.IssuePermissions;
import com.almworks.jira.provider3.sync.ServerFields;
import com.almworks.util.Pair;
import com.almworks.util.config.Configuration;
import com.almworks.util.text.NameMnemonic;
import com.almworks.util.ui.actions.ActionContext;
import com.almworks.util.ui.actions.CantPerformException;
import com.almworks.util.ui.actions.PresentationMapping;
import com.almworks.util.ui.actions.UpdateRequest;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public abstract class BaseEditIssueFeature extends TopEditor {
  public static final EditorsScheme SCHEME = new EditorsScheme(EditMetaSchema.DEFAULT)
    .addEditor(ServerFields.RESOLUTION, ResolutionEditor.NO_CHECK_EDITOR)
    .addEditor(ServerFields.ATTACHMENT, AttachmentsEditor.INSTANCE)
    .addEditor(ServerFields.LINKS, LinksEditor.INSTANCE)
    .fix();
  private static final TypedKey<DefaultValues> DEFAULTS = TypedKey.create("defaults");
  private static final List<String> ADDITIONAL_ACTIONS = Collections15.unmodifiableListCopy(
    JiraActions.EDITOR_ATTACH_FILES,
    JiraActions.EDITOR_ATTACH_SCREENSHOT,
    JiraActions.EDITOR_ATTACH_TEXT,
    "",
    JiraActions.EDITOR_ADD_LINKS);
  private static final Map<String, PresentationMapping<?>> ADDITIONAL_PRESENTATION = PresentationMapping.ENABLED_NONAME_PLUS_DESCRIPTION;

  private final ScreenIssueEditor myEditor;

  public BaseEditIssueFeature(ScreenIssueEditor editor) {
    super(NameMnemonic.EMPTY);
    myEditor = editor;
  }

  public static void commonDescriptorSetup(EditDescriptor.Impl descriptor) {
    descriptor.addCommonActions(EditDescriptor.COMMON_ACTIONS);
    descriptor.addCommonActions("");
    descriptor.addCommonActions(ADDITIONAL_ACTIONS, ADDITIONAL_PRESENTATION);
    descriptor.addRightActions(JiraActions.RIGHT_ACTION_IDS);
  }

  public static DefaultEditModel.Root setupEditIssues(WritableLongList itemsToLock, JiraConnection3 connection, Collection<ItemWrapper> issues) throws CantPerformException {
    return setupEditIssues(itemsToLock, connection, LongArray.create(ItemWrapper.GET_ITEM.collectList(issues)));
  }

  public static DefaultEditModel.Root setupEditIssues(WritableLongList itemsToLock, JiraConnection3 connection, LongArray items) {
    DefaultEditModel.Root model = DefaultEditModel.Root.editItems(items);
    itemsToLock.addAll(items);
    EngineConsts.setupConnection(model, connection);
    return model;
  }

  /**
   * Collect context issues and checks that all of them:<br>
   * 1. Belongs to the same connection<br>
   * 2. Connection allows upload<br>
   * 3. None of issue is locked for edit<br>
   * 4. Use has permission to edit each issue
   */
  public static Pair<JiraConnection3, List<ItemWrapper>> getContextIssues(ActionContext context) throws CantPerformException {
    try {
      List<ItemWrapper> issues = context.getSourceCollection(ItemWrapper.ITEM_WRAPPER);
      SyncManager manager = context.getSourceObject(SyncManager.ROLE);
      JiraConnection3 connection = null;
      for (ItemWrapper issue : issues) {
        CantPerformException.ensure(!issue.services().isRemoteDeleted());
        JiraConnection3 c = CantPerformException.cast(JiraConnection3.class, issue.getConnection());
        if (connection == null) {
          connection = c;
          CantPerformException.ensure(connection.isUploadAllowed());
        }
        else CantPerformException.ensure(c == connection);
        long issueItem = issue.getItem();
        if (manager.findLock(issueItem) != null) throw new CantPerformException();
        CantPerformException.ensure(IssuePermissions.hasPermission(issue, IssuePermissions.EDIT_ISSUE));
      }
      connection = CantPerformException.ensureNotNull(connection);
      return Pair.create(connection, issues);
    } catch (CantPerformException e) {
//      if (LogHelper.isLoggable(Level.INFO)) LogHelper.debug("[BaseEditIssueFeature#getContextIssues]", e);
      throw e;
    }
  }

  @Override
  public final EditDescriptor checkContext(ActionContext context, UpdateRequest updateRequest) throws CantPerformException {
    return ScreenIssueEditor.descriptor(doCheckContext(context, updateRequest));
  }

  @NotNull
  protected abstract EditDescriptor.Impl doCheckContext(ActionContext context, UpdateRequest updateRequest) throws CantPerformException;

  @Nullable
  @Override
  protected Pair<DefaultEditModel.Child, ? extends List<? extends FieldEditor>> createNestedModel(VersionSource source, EditItemModel parent, EditPrepare editPrepare) {
    LongList items = parent.getEditingItems();
    DefaultEditModel.Child child = DefaultEditModel.Child.editItems(parent, items, false);
    EngineConsts.setupNestedModel(parent, child);
    ParentSupport.copyParent(parent, child);
    DefaultValues defaultValues = parent.getValue(DEFAULTS);
    if (defaultValues != null) {
      child.putHint(DEFAULTS, defaultValues);
      defaultValues.setDefaults(source.getReader(), child);
    }
    return Pair.create(child, Collections.singletonList(myEditor));
  }

  @Nullable
  @Override
  protected JComponent doEditModel(Lifespan life, final EditItemModel nested, Configuration config) {
    JComponent component = myEditor.createComponent(life, nested, config);
    JComponent focusOwner = myEditor.getDefaultFocusOwner(nested);
    if (focusOwner != null) InitialWindowFocusFinder.setInitialWindowComponent(focusOwner);
    return component;
  }

  @Nullable
  protected EditItemModel getIssueModelFromNested(EditItemModel nested) {
    return myEditor.getNestedModel(nested);
  }

  @Override
  protected void doCommit(CommitContext childContext) throws CancelCommitException {
    childContext.commitEditors(null);
    DefaultValues defaultValues = childContext.getModel().getValue(DEFAULTS);
    if (defaultValues != null) defaultValues.commitDefaults(childContext);
  }

  protected static void setDefaults(DefaultEditModel.Root model, DefaultValues defaults) {
    model.putHint(DEFAULTS, defaults);
  }
}
