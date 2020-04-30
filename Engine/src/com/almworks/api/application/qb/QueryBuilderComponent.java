package com.almworks.api.application.qb;

import com.almworks.util.properties.Role;
import com.almworks.util.ui.DialogEditor;
import com.almworks.util.ui.TextEditor;

import javax.swing.*;

/**
 * @author : Dyoma
 */
public interface QueryBuilderComponent {
  Role<QueryBuilderComponent> ROLE = Role.role("queryBuilder", QueryBuilderComponent.class);

  JCheckBox createRunImmediatlyCheckbox();

  DialogEditor createQueryEditor(TextEditor nameEditor, FilterEditor filterEditor);
}
