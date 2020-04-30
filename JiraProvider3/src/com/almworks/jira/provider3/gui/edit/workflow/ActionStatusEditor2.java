package com.almworks.jira.provider3.gui.edit.workflow;

import com.almworks.items.gui.edit.ComponentControl;
import com.almworks.items.gui.edit.EditItemModel;
import com.almworks.items.gui.edit.editors.MockEditor;
import com.almworks.items.gui.edit.util.FieldEditorUtil;
import com.almworks.items.gui.edit.util.SimpleComponentControl;
import com.almworks.items.sync.EditPrepare;
import com.almworks.items.sync.VersionSource;
import com.almworks.jira.provider3.schema.Status;
import com.almworks.util.text.NameMnemonic;
import org.almworks.util.TypedKey;
import org.almworks.util.Util;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.Collections;
import java.util.List;

class ActionStatusEditor2 extends MockEditor {
  private final TypedKey<String> STATUS_NAME = TypedKey.create("statusName");
  private final WFActionEditor2 myWorkflowEditor;

  ActionStatusEditor2(WFActionEditor2 workflowEditor) {
    super(NameMnemonic.rawText("Status"));
    myWorkflowEditor = workflowEditor;
  }

  @Override
  public void prepareModel(VersionSource source, EditItemModel model, EditPrepare editPrepare) {
    ActionApplication application = myWorkflowEditor.getActionApplication(model);
    if (application == null) return;
    long statusItem = application.getTargetStatus();
    String statusName = statusItem > 0 ? source.forItem(statusItem).getValue(Status.NAME) : null;
    model.putHint(STATUS_NAME, statusName);
    model.registerEditor(this);
  }

  @NotNull
  @Override
  public List<? extends ComponentControl> createComponents(Lifespan life, EditItemModel model) {
    String statusName = model.getValue(STATUS_NAME);
    statusName = Util.NN(statusName).trim();
    if (statusName.length() == 0) statusName = "<Unknown>";
    JTextField field = new JTextField();
    field.setText(statusName);
    field.setEditable(false);
    FieldEditorUtil.registerComponent(model, this, field);
    return Collections.singletonList(
      new SimpleComponentControl(field, ComponentControl.Dimensions.SINGLE_LINE, this, model,
        ComponentControl.Enabled.NOT_APPLICABLE, NameMnemonic.rawText("Status")));
  }
}