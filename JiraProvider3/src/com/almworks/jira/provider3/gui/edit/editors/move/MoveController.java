package com.almworks.jira.provider3.gui.edit.editors.move;

import com.almworks.api.application.ItemKey;
import com.almworks.integers.LongList;
import com.almworks.items.gui.edit.*;
import com.almworks.items.gui.edit.editors.enums.single.SingleEnumFieldEditor;
import com.almworks.items.gui.meta.LoadedItemKey;
import com.almworks.items.sync.ItemVersion;
import com.almworks.items.sync.VersionSource;
import com.almworks.jira.provider3.remotedata.issue.MoveIssueStep;
import com.almworks.jira.provider3.schema.Issue;
import com.almworks.jira.provider3.schema.IssueType;
import com.almworks.util.LogHelper;
import com.almworks.util.collections.LongSet;
import com.almworks.util.commons.Condition;
import com.almworks.util.commons.Procedure;
import org.almworks.util.TypedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class MoveController {
  public static final Condition<LoadedItemKey> IS_SUBTASK = new IsSubtask(true);
  public static final Condition<LoadedItemKey> IS_GENERIC = new IsSubtask(false);

  public static final FieldEditor PROJECT = ProjectEditor.INSTANCE;
  public static final FieldEditor COMMON_ISSUE_TYPE = new IssueTypeEditor(false);
  public static final FieldEditor MOVE_ISSUE_TYPE = new IssueTypeEditor(true);
  public static final FieldEditor PARENT = new ReadonlyParentEditor();

  public static final int MODE_ALL = 0;
  public static final int MODE_SUBTASK = 1;
  public static final int MODE_GENERIC = 2;
  public static final int MODE_DISABLED = 3;

  private static final TypedKey<MoveController> KEY = TypedKey.create("moveController");
  private static final TypedKey<Integer> CURRENT_MODE = TypedKey.create("issueType/controller/mode");

  private final ParentSupport myParentSupport;
  private ProjectEditor myProjectEditor;
  private IssueTypeEditor myTypeEditor;
  private ParentEditor myParentEditor;
  private int myDefaultMode;

  private MoveController(ParentSupport parentSupport, int defaultMode) {
    myParentSupport = parentSupport;
    myDefaultMode = defaultMode;
  }

  @NotNull
  public static MoveController ensureLoaded(VersionSource source, EditModelState model) {
    MoveController controller = model.getValue(KEY);
    if (controller == null) {
      ParentSupport parentSupport = ParentSupport.ensureLoaded(source, model);
      int defaultMode;
      if (!model.isNewItem()) {
        if (parentSupport.isGenericOnly()) defaultMode = MODE_GENERIC;
        else if (parentSupport.isSubtaskOnly()) defaultMode = MODE_SUBTASK;
        else defaultMode = MODE_DISABLED;
      } else {
        defaultMode = parentSupport.isGenericOnly() ? MODE_GENERIC : MODE_SUBTASK;
      }
      controller = new MoveController(parentSupport, defaultMode);
      model.putHint(KEY, controller);
    }
    return controller;
  }

  @Nullable("If not installed")
  public static MoveController getInstance(EditModelState model) {
    return model.getValue(KEY);
  }

  public static void setNewSubtaskParent(EditModelState model, long parent) {
    if (!model.isNewItem()) {
      LogHelper.error("Not a new issue model", parent);
      return;
    }
    ParentSupport.prepareSubtask(model, parent);
  }

  public static Boolean changeFlag(Boolean current, boolean set, boolean clear) {
    if (current == null) {
      if (set) return true;
      if (clear) return false;
      return null;
    }
    return current || set;
  }

  public LongList getAllParents() {
    return myParentSupport.getAllParents();
  }

  public boolean isGenericOnly() {
    return myParentSupport.isGenericOnly();
  }

  void setProjectEditor(ProjectEditor projectEditor) {
    myProjectEditor = projectEditor;
  }

  void setTypeEditor(IssueTypeEditor typeEditor) {
    myTypeEditor = typeEditor;
  }

  void setParentEditor(ParentEditor parentEditor) {
    myParentEditor = parentEditor;
  }

  int getCurrentMode(EditModelState model) {
    Integer mode = model.getValue(CURRENT_MODE);
    if (mode != null && checkMode(mode)) return mode;
    return myDefaultMode;
  }

  public void setCurrentMode(EditModelState model, int mode) {
    if (checkMode(mode)) model.putHint(CURRENT_MODE, mode);
  }

  private boolean checkMode(int mode) {
    return mode >= MODE_ALL && mode <= MODE_DISABLED;
  }

  public static void performCommit(CommitContext context) throws CancelCommitException {
    MoveController controller = MoveController.getInstance(context.getModel());
    if (controller != null) controller.commit(context);
  }

  private static final TypedKey<LongSet> COMMITTED = TypedKey.create("committedIssues");
  private void commit(CommitContext context) throws CancelCommitException {
    EditItemModel model = context.getModel();
    LongSet committed = model.getValue(COMMITTED);
    if (committed == null) {
      committed = new LongSet();
      model.putHint(COMMITTED, committed);
    }
    if (!committed.addValue(context.getItem())) return; // already committed
    if (model.isNewItem()) commitNewIssue(context);
    else commitEdit(context);
    if (myProjectEditor != null) myProjectEditor.updateDefaults(context);
    if (myTypeEditor != null) myTypeEditor.updateDefaults(context);
  }

  private void commitEdit(CommitContext context) throws CancelCommitException {
    EditItemModel model = context.getModel();
    ItemVersion project = getChangedValue(context, myProjectEditor);
    ItemVersion type = getChangedValue(context, myTypeEditor);
    List<FieldEditor> commitEditors = model.getCommitEditors();
    ItemVersion issue = context.readTrunk();
    if (type == null) type = issue.readValue(Issue.ISSUE_TYPE);
    if (type == null) throw new CancelCommitException();
    if (commitEditors.contains(myParentEditor)) { // Parent has been changed
      long parent = myParentEditor.getSingleParent(model, context.getReader(), issue);
      if (!IssueType.isSubtask(type, parent > 0)) throw new CancelCommitException();
      if (parent > 0) context.getCreator().setValue(Issue.PARENT, parent);
      else context.getCreator().setValue(Issue.PARENT, (Long) null);
      context.getCreator().setValue(Issue.ISSUE_TYPE, type.getItem());
    } else { // Parent has not been changed
      Long parent = issue.getValue(Issue.PARENT);
      Boolean subtask = type.getValue(IssueType.SUBTASK);
      if (subtask == null) {
        LogHelper.warning("Missing subtask flag", type.getValue(IssueType.NAME), type);
        subtask = false;
      }
      if (!subtask) parent = null;
      else if (parent == null) {
        LogHelper.warning("Parent not specified");
        throw new CancelCommitException();
      }
      context.getCreator().setValue(Issue.PARENT, parent);
      context.getCreator().setValue(Issue.ISSUE_TYPE, type.getItem());
    }
    if (project != null) context.getCreator().setValue(Issue.PROJECT, project.getItem());
    if (commitEditors.contains(myProjectEditor) || commitEditors.contains(myTypeEditor) || commitEditors.contains(myParentEditor))
      commitMove(context);
  }

  private static long commitMove(CommitContext context) throws CancelCommitException {
    final long parent = Issue.getParent(context.readTrunk());
    ParentSupport parents = ParentSupport.getInstance(context.getModel());
    if (parents == null) {
      LogHelper.error("Should not happen", context);
      throw new CancelCommitException();
    }
    final long oldParent = parents.getInitialParent(context.getItem());
    context.afterCommit(new Procedure<CommitContext>() {
      @Override
      public void invoke(CommitContext context) {
        MoveIssueStep.addHistory(context.getCreator(), oldParent, parent);
      }
    });
    return parent;
  }

  private void commitNewIssue(CommitContext context) throws CancelCommitException {
    EditItemModel model = context.getModel();
    ItemVersion project = getChangedValue(context, myProjectEditor);
    ItemVersion type = getChangedValue(context, myTypeEditor);
    if (type == null) throw new CancelCommitException();
    long parent = myParentEditor.getSingleParent(model, context.getReader(), null);
    if (parent > 0) {
      project = context.readTrunk(parent).readValue(Issue.PROJECT);
      if (project == null || !IssueType.isSubtask(type, true)) throw new CancelCommitException();
      context.getCreator().setValue(Issue.PARENT, parent);
    } else {
      if (project == null) throw new CancelCommitException();
      if (!IssueType.isSubtask(type, false)) throw new CancelCommitException();
    }
    context.getCreator().setValue(Issue.ISSUE_TYPE, type.getItem());
    context.getCreator().setValue(Issue.PROJECT, project.getItem());
  }

  @Nullable
  private ItemVersion getChangedValue(CommitContext context, @Nullable SingleEnumFieldEditor editor) {
    if (editor == null) return null;
    EditItemModel model = context.getModel();
    if (!model.getCommitEditors().contains(editor)) return null;
    ItemKey value = editor.getValue(model);
    if (value == null || value.getItem() <= 0) return null;
    return context.readTrunk(value.getItem());
  }

  private static class IsSubtask extends Condition<LoadedItemKey> {
    private final boolean mySubtask;

    public IsSubtask(boolean subtask) {
      mySubtask = subtask;
    }

    @Override
    public boolean isAccepted(LoadedItemKey value) {
      return IssueType.isSubtask(value, mySubtask);
    }
  }}
