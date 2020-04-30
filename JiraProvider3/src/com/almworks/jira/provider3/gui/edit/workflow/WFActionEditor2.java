package com.almworks.jira.provider3.gui.edit.workflow;

import com.almworks.integers.LongList;
import com.almworks.items.gui.edit.*;
import com.almworks.items.gui.edit.editors.composition.NestedModelEditor;
import com.almworks.items.gui.edit.util.PerItemModelValue;
import com.almworks.items.sync.EditPrepare;
import com.almworks.items.sync.ItemVersionCreator;
import com.almworks.items.sync.VersionSource;
import com.almworks.jira.provider3.comments.gui.BaseEditComment;
import com.almworks.jira.provider3.gui.edit.EditMetaSchema;
import com.almworks.jira.provider3.gui.edit.EditorsScheme;
import com.almworks.jira.provider3.gui.edit.FieldSet;
import com.almworks.jira.provider3.gui.edit.ResolvedField;
import com.almworks.jira.provider3.gui.edit.editors.ResolutionEditor;
import com.almworks.jira.provider3.schema.Issue;
import com.almworks.jira.provider3.sync.ServerFields;
import com.almworks.util.Pair;
import com.almworks.util.text.NameMnemonic;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.almworks.util.Util;

import java.util.ArrayList;
import java.util.List;

public class WFActionEditor2 extends NestedModelEditor {
  private static final EditorsScheme WORKFLOW = new EditorsScheme(EditMetaSchema.DEFAULT).addEditor(ServerFields.RESOLUTION, ResolutionEditor.MANDATORY_EDITOR).fix();

  private final TypedKey<ActionApplication> myApplicationKey = TypedKey.create("actionApplication");
  private final PerItemModelValue<Long> myInitialStatuses = PerItemModelValue.hint("initialStatuses");

  private final ActionStatusEditor2 myStatusEditor;

  private final EditorsScheme myScheme;
  private final List<ServerFields.Field> myBottomFields;

  private WFActionEditor2(EditorsScheme scheme, ServerFields.Field[] bottomFields) {
    super(NameMnemonic.rawText("Workflow Action"));
    myScheme = scheme;
    myBottomFields = Collections15.arrayList(bottomFields);
    myBottomFields.add(ServerFields.COMMENTS);
    myStatusEditor = new ActionStatusEditor2(this);
  }

  @Override
  protected Pair<DefaultEditModel.Child, ? extends List<? extends FieldEditor>> createNestedModel(VersionSource source, EditItemModel parent, EditPrepare editPrepare) {
    ActionApplication application = getActionApplication(parent);
    if (application == null) return null;
    if (parent.isNewItem()) return null;
    LongList items = parent.getEditingItems();
    DefaultEditModel.Child nested = DefaultEditModel.Child.editItems(parent, items, true);
    nested.putHint(myApplicationKey, application);
    for (int i = 0; i < items.size(); i++) {
      long item = items.get(i);
      Long status = source.forItem(item).getValue(Issue.STATUS);
      myInitialStatuses.put(parent, item, status);
    }
    ArrayList<FieldEditor> editors = collectEditors(source, nested, application);
    return Pair.create(nested, editors);
  }

  private ArrayList<FieldEditor> collectEditors(VersionSource source, DefaultEditModel.Child model, ActionApplication application) {
    FieldSet fieldSet = application.collectFieldSet(source);
    fieldSet.addFields(source, myBottomFields);
    fieldSet.createEditors(source, myScheme);
    ArrayList<FieldEditor> editors = Collections15.arrayList();
    editors.add(myStatusEditor);
    fieldSet.extractEditors(model, editors, ResolvedField.BY_DISPLAY_NAME, ResolvedField.filterFields(myBottomFields).not());
    for (ServerFields.Field field : myBottomFields) fieldSet.extractEditor(model, editors, field.getJiraId());
    fieldSet.addAttributeProviders(editors);
    return editors;
  }

  @Override
  public boolean hasDataToCommit(EditItemModel model) {
    EditModelState nested = getNestedModel(model);
    return nested != null && getActionApplication(nested) != null;
  }

  @Override
  public boolean hasValue(EditModelState model) {
    return true;
  }

  @Override
  public void commit(CommitContext parentContext) throws CancelCommitException {
    CommitContext context = commitNested(parentContext);
    if (context == null) return;
    ActionApplication application = getActionApplication(context.getModel());
    if (application == null) return;
    setStatus(context.getCreator(), application.getTargetStatus());
    long comment = BaseEditComment.COMMENT_SLAVE.getCreatedItem(context.readTrunk());
    long initialStatus = Util.NN(myInitialStatuses.get(parentContext.getModel(), context.getItem()), 0l);
    long action = application.getAction(context.getItem());
    if (action > 0) WorkflowStep.addHistory(context.getCreator(), action, comment, initialStatus);
  }

  private void setStatus(ItemVersionCreator issue, long status) {
    if (status <= 0) status = 0;
    else if (issue.forItem(status).isInvisible()) status = 0;
    issue.setValue(Issue.STATUS, status > 0 ? status : null);
  }

  public void setActionApplication(EditItemModel model, ActionApplication application) {
    model.putHint(myApplicationKey, application);
  }

  public ActionApplication getActionApplication(EditModelState model) {
    return model.getValue(myApplicationKey);
  }

  public static class Builder {
    private final List<ServerFields.Field> myBottomEditors = Collections15.arrayList();
    private final EditorsScheme myScheme;

    public Builder() {
      myScheme = new EditorsScheme(WORKFLOW);
    }

    public Builder replace(ServerFields.Field field, FieldEditor editor) {
      myScheme.addEditor(field, editor);
      return this;
    }

    public Builder addBottom(ServerFields.Field editor) {
      myBottomEditors.add(editor);
      return this;
    }

    public WFActionEditor2 create() {
      return new WFActionEditor2(myScheme, myBottomEditors.toArray(new ServerFields.Field[myBottomEditors.size()]));
    }
  }
}
