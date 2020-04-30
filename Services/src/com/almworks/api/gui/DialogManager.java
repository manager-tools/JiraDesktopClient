package com.almworks.api.gui;

import com.almworks.util.properties.Role;

/**
 * @author : Dyoma
 */
public interface DialogManager {
  Role<DialogManager> ROLE = Role.role("dialogManager");

  DialogBuilder createBuilder(String windowId);

  DialogEditorBuilder createEditor(String windowId);

  void showErrorMessage(String title, String message);

  void showMessage(String title, Object message, int messageType);

  DialogBuilder createMainBuilder(String windowId);
}
