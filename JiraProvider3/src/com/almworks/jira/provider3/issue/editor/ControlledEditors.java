package com.almworks.jira.provider3.issue.editor;

import com.almworks.items.gui.edit.ComponentControl;
import com.almworks.items.gui.edit.EditItemModel;
import com.almworks.items.gui.edit.FieldEditor;
import com.almworks.jira.provider3.gui.edit.ResolvedField;
import com.almworks.util.ui.swing.SwingTreeUtil;
import org.almworks.util.Collections15;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.*;

class ControlledEditors {
  private final Lifespan myLife;
  private final EditItemModel myModel;
  private final Set<FieldEditor> myControlledEditors;
  private final Map<FieldEditor, List<? extends ComponentControl>> myComponents = Collections15.hashMap();

  private ControlledEditors(Lifespan life, EditItemModel model, Collection<FieldEditor> controlledEditors) {
    myLife = life;
    myModel = model;
    myControlledEditors = Collections15.hashSet(controlledEditors);
  }

  public static ControlledEditors create(Lifespan life, EditItemModel model, List<FieldEditor> controlledEditors) {
    ControlledEditors result = new ControlledEditors(life, model, controlledEditors);
    for (FieldEditor editor : controlledEditors) {
      List<? extends ComponentControl> components = editor.createComponents(life, model);
      result.myComponents.put(editor, components);
    }
    return result;
  }

  @Nullable
  public List<? extends ComponentControl> getComponents(String fieldId) {
    FieldEditor editor = getEditor(fieldId);
    return getComponents(editor);
  }

  @Nullable
  public List<? extends ComponentControl> getComponents(FieldEditor editor) {
    if (editor == null) return null;
    return myComponents.get(editor);
  }

  @Nullable
  public FieldEditor getEditor(String fieldId) {
    FieldEditor editor = ResolvedField.findEditor(myModel, FieldEditor.class, fieldId);
    if (!myControlledEditors.contains(editor)) return null;
    return editor;
  }

  public EditItemModel getModel() {
    return myModel;
  }

  public Lifespan lifespan() {
    return myLife;
  }

  public List<FieldEditor> selectEditors(Set<String> fieldIds) {
    Map<String, FieldEditor> map = ResolvedField.getEditorsMap(myModel);
    ArrayList<FieldEditor> result = Collections15.arrayList();
    for (Map.Entry<String, FieldEditor> entry : map.entrySet()) if (fieldIds.contains(entry.getKey())) result.add(entry.getValue());
    return result;
  }

  @Nullable
  public ComponentControl getEditorComponent(Component component) {
    for (List<? extends ComponentControl> list : myComponents.values()) {
      for (ComponentControl control : list) {
        JComponent ancestor = control.getComponent();
        if (SwingTreeUtil.isAncestor(ancestor, component)) return control;
      }
    }
    return null;
  }
}
