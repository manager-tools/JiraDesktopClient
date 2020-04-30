package com.almworks.jira.provider3.gui.edit.editors.move;

import com.almworks.explorer.PrimaryItemKeyTransferHandler;
import com.almworks.integers.LongArray;
import com.almworks.items.api.DBOperationCancelledException;
import com.almworks.items.api.DBReader;
import com.almworks.items.api.DP;
import com.almworks.items.dp.DPEquals;
import com.almworks.items.gui.edit.*;
import com.almworks.items.gui.edit.editors.text.ScalarValueKey;
import com.almworks.items.gui.edit.util.BaseFieldEditor;
import com.almworks.items.gui.edit.util.FieldEditorUtil;
import com.almworks.items.gui.edit.util.SimpleComponentControl;
import com.almworks.items.sync.EditPrepare;
import com.almworks.items.sync.ItemVersion;
import com.almworks.items.sync.VersionSource;
import com.almworks.items.sync.util.ItemValues;
import com.almworks.items.sync.util.TransactionCacheKey;
import com.almworks.items.util.SyncAttributes;
import com.almworks.jira.provider3.schema.Issue;
import com.almworks.jira.provider3.schema.IssueKeyComparator;
import com.almworks.jira.provider3.schema.Project;
import com.almworks.jira.provider3.services.JiraPatterns;
import com.almworks.util.LogHelper;
import com.almworks.util.Pair;
import com.almworks.util.bool.BoolExpr;
import com.almworks.util.text.NameMnemonic;
import com.almworks.util.text.TextUtil;
import gnu.trove.TLongObjectHashMap;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class MoveParentEditor extends BaseFieldEditor implements ParentEditor {
  private final TypedKey<ComponentControl.Enabled> EDITABLE = TypedKey.create("parent/editable");
  private final ScalarValueKey.Text KEYS = new ScalarValueKey.Text("parent/keys", true);
  private final TypedKey<Pair<Long, String>> COMMON_PRJ = TypedKey.create("parent/commonPrj");
  private final TransactionCacheKey<Long> PARENT = TransactionCacheKey.create("parent/resolved");

  public MoveParentEditor(NameMnemonic labelText) {
    super(labelText);
  }

  @Override
  public void prepareModel(VersionSource source, EditItemModel model, EditPrepare editPrepare) {
    ParentSupport parents = ParentSupport.ensureLoaded(source, model);
    boolean hasNew = false;
    List<String> keys = Collections15.arrayList();
    List<ItemVersion> allParents = source.readItems(parents.getAllParents());
    for (ItemVersion issue : allParents) {
      if (issue.getItem() == 0) continue;
      String key = issue.getValue(Issue.KEY);
      if (key == null) hasNew = true;
      else keys.add(key);
    }
    Collections.sort(keys, IssueKeyComparator.INSTANCE);
    KEYS.setText(model, collectText(keys, hasNew));
    ArrayList<ItemVersion> allIssues = Collections15.arrayList();
    allIssues.addAll(allParents);
    allIssues.addAll(source.readItems(model.getEditingItems()));
    Pair<Long, String> commonProject = chooseCommonProject(allIssues);
    ComponentControl.Enabled enabled;
    if (commonProject == null || commonProject.getSecond() == null) enabled = ComponentControl.Enabled.DISABLED;
    else if (hasNew || keys.size() > 1) enabled = ComponentControl.Enabled.DISABLED;
    else if (model.getEditingItems().size() > 1) enabled = ComponentControl.Enabled.ENABLED;
    else enabled = ComponentControl.Enabled.NOT_APPLICABLE;
    model.putHint(EDITABLE, enabled);
    model.putHint(COMMON_PRJ, commonProject);
    model.registerEditor(this);
  }

  private Pair<Long, String> chooseCommonProject(List<ItemVersion> issues) {
    ItemVersion common = null;
    for (ItemVersion issue : issues) {
      if (issue.getItem() <= 0) continue;
      Long project = issue.getValue(Issue.PROJECT);
      if (project == null || project <= 0) continue;
      if (common == null) common = issue.forItem(project);
      else if (common.getItem() != project) return null;
    }
    if (common == null) return null;
    String key = common.getValue(Project.KEY);
    return Pair.create(common.getItem(), key);
  }

  @NotNull
  @Override
  public List<? extends ComponentControl> createComponents(Lifespan life, EditItemModel model) {
    ComponentControl component = createComponent(life, model);
    if (component == null) return Collections.emptyList();
    return Collections.singletonList(component);
  }

  @Nullable
  public ComponentControl createComponent(Lifespan life, EditItemModel model) {
    JTextField field = new JTextField(15);
    return attachComponent(life, model, field);
  }

  public ComponentControl attachComponent(Lifespan life, EditItemModel model, JTextField field) {
    ComponentControl.Enabled enabled = model.getValue(EDITABLE);
    if (enabled == null) return null;
    field.setText(KEYS.getText(model));
    field.setTransferHandler(PrimaryItemKeyTransferHandler.getInstance(true));
    KEYS.listenTextComponent(life, model, field);
    FieldEditorUtil.registerComponent(model, this, field);
    return SimpleComponentControl.singleLine(field, this, model, enabled);
  }

  private String collectText(List<String> keys, boolean hasNew) {
    List<String> allKeys;
    if (hasNew) {
      ArrayList<String> copy = Collections15.arrayList(keys);
      copy.add(0, "<new>");
      allKeys = copy;
    } else allKeys = keys;
    return TextUtil.separateToString(allKeys, ", ");
  }

  @Override
  public boolean isChanged(EditItemModel model) {
    if (!KEYS.isChanged(model)) return false;
    Long connection = model.getSingleEnumValue(SyncAttributes.CONNECTION);
    if (connection == null || connection <= 0) {
      LogHelper.warning("Model missing connection");
      return false;
    }
    List<String> keys = Issue.extractIssueKeys(KEYS.getValue(model));
    return keys.size() <= 1;
  }

  @Override
  public boolean hasValue(EditModelState model) {
    return true;
  }

  @Override
  public void verifyData(DataVerification verifyContext) {
    EditItemModel model = verifyContext.getModel();
    boolean changed = KEYS.isChanged(model);
    if (!changed) return;
    String text = KEYS.getText(model);
    List<String> keys = Issue.extractIssueKeys(text);
    if (keys.isEmpty()) {
      text = text.trim();
      if (!text.isEmpty()) verifyContext.addError(this, "'" + text + "' is not a valid parent key");
      return;
    }
    if (keys.size() != 1) {
      verifyContext.addError(this, "Expected single parent key");
      return;
    }
    String prj = JiraPatterns.extractProjectKeyNoLog(keys.get(0));
    Pair<Long, String> project = model.getValue(COMMON_PRJ);
    if (project == null || project.getSecond() == null) {
      LogHelper.error("No common project", project, text);
      return;
    }
    if (prj == null) {
      LogHelper.error("Cannot extract project key", keys, text);
      return;
    }
    if (!prj.equalsIgnoreCase(project.getSecond())) verifyContext.addError(this, "Cannot move subtask to another project");
  }

  @Override
  public void onItemsChanged(EditItemModel model, TLongObjectHashMap<ItemValues> newValues) {
    FieldEditorUtil.assertNotChanged(model, newValues, Issue.PARENT, this);
  }

  @Override
  public void commit(CommitContext context) throws CancelCommitException {
    MoveController.performCommit(context);
  }

  @Override
  public long getSingleParent(@NotNull EditItemModel model, @NotNull DBReader reader, ItemVersion issue) throws CancelCommitException {
    if (!model.getAllEditors().contains(this)) return issue != null ? Issue.getParent(issue) : 0;
    Long parent = PARENT.get(reader);
    if (parent == null) {
      parent = resolveParent(reader, model);
      PARENT.put(reader, parent);
    }
    return parent > 0 ? parent : 0;
  }

  private long resolveParent(DBReader reader, EditItemModel model) {
    String keys = KEYS.getValue(model);
    if (keys == null || !KEYS.isChanged(model)) return 0;
    List<String> keysList = Issue.extractIssueKeys(keys);
    if (keysList.size() != 1) {
      LogHelper.error("Cannot change parent to several values", keys);
      throw new DBOperationCancelledException();
    }
    Long thisConnection = model.getSingleEnumValue(SyncAttributes.CONNECTION);
    if (thisConnection == null) {
      LogHelper.error("Missing this connection");
      throw new DBOperationCancelledException();
    }
    BoolExpr<DP> query =
      DPEquals.create(Issue.KEY, keysList.get(0)).and(DPEquals.create(SyncAttributes.CONNECTION, thisConnection));
    LongArray issues = reader.query(query).copyItemsSorted();
    if (issues.size() != 1) {
      LogHelper.warning("Parent not found", issues); // todo JC-121
      return 0;
    }
    return issues.get(0);
  }
}
