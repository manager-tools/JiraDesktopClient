package com.almworks.items.gui.edit.visibility;

import com.almworks.items.gui.edit.EditItemModel;
import com.almworks.items.gui.edit.FieldEditor;
import com.almworks.util.LogHelper;
import com.almworks.util.collections.ChangeListener;
import com.almworks.util.collections.SimpleModifiable;
import com.almworks.util.commons.Procedure;
import com.almworks.util.config.Configuration;
import com.almworks.util.ui.actions.ActionRegistry;
import com.almworks.util.ui.actions.DataRole;
import org.almworks.util.Collections15;
import org.almworks.util.detach.Lifespan;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class FieldVisibilityController extends SimpleModifiable implements ChangeListener {
  public static final DataRole<FieldVisibilityController> ROLE = DataRole.createRole(FieldVisibilityController.class);

  private static final String LEVEL_SETTING = "showLevel";
  public static final String FIELD_VISIBILITY_ACTION = "JIRA.edit.fieldVisibility";

  private final Map<FieldEditor, ? extends EditorVisibility> myFields;
  private final EditItemModel myModel;
  private final Set<FieldEditor> myVisible = Collections15.hashSet();
  private final Procedure<? super Collection<FieldEditor>> myLayoutProcedure;
  private final Configuration myConfig;
  private final VisibilityDescription myDescription;
  private int myVisibilityLevel;

  public FieldVisibilityController(EditItemModel model, Map<FieldEditor, ? extends EditorVisibility> fields,
    Procedure<? super Collection<FieldEditor>> layoutProcedure, Configuration config, VisibilityDescription description) {
    myModel = model;
    myFields = fields;
    myLayoutProcedure = layoutProcedure;
    myConfig = config;
    myDescription = description;
    myVisibilityLevel = myDescription.getDefault();
  }

  public static void registerActions(ActionRegistry actions) {
    actions.registerAction(FIELD_VISIBILITY_ACTION, VisibilityModeAction.INSTANCE);
  }

  public void start(Lifespan life) {
    myModel.addAWTChangeListener(life, this);
    myVisibilityLevel = myConfig.getIntegerSetting(LEVEL_SETTING, myDescription.getDefault());
    updateVisible();
  }

  public void setVisibilityLevel(int level) {
    if (myVisibilityLevel == level) return;
    myVisibilityLevel = level;
    myConfig.setSetting(LEVEL_SETTING, myVisibilityLevel);
    updateVisible();
    fireChanged();
  }

  @Override
  public void onChange() {
    updateVisible();
  }

  private void updateVisible() {
    boolean changed = false;
    LogHelper.debug("Update field visibility started");
    for (FieldEditor field : myFields.keySet()) {
      boolean isVisible = isVisible(field);
      if (isVisible != myVisible.contains(field)) {
        changed = true;
        if (isVisible) myVisible.add(field);
        else myVisible.remove(field);
      }
    }
    LogHelper.debug("Update field visibility done", changed);
    if (changed) myLayoutProcedure.invoke(myVisible);
  }

  private boolean isVisible(FieldEditor field) {
    EditorVisibility visibility = myFields.get(field);
    return visibility == null || visibility.isVisible(myModel, myVisibilityLevel);
  }

  public boolean hasAnyField() {
    return !myFields.isEmpty();
  }

  public int getVisibilityLevel() {
    return myVisibilityLevel;
  }

  public VisibilityDescription getDescription() {
    return myDescription;
  }
}
