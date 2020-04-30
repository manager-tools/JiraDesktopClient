package com.almworks.api.gui;

import com.almworks.util.commons.Procedure;
import com.almworks.util.properties.Role;
import com.almworks.util.ui.DialogEditor;

import javax.swing.*;

/**
 * @author : Dyoma
 */
public interface DialogEditorBuilder extends BasicWindowBuilder<DialogEditor> {
  Role<DialogEditor> CONTENT = Role.role("content");
  EditingEvent APPLY_EVENT = new EditingEvent("APPLY_EVENT", true);
  EditingEvent CANCEL_EVENT = new EditingEvent("CANCEL_EVENT", false);
  EditingEvent OK_EVENT = new EditingEvent("OK_EVENT", true);

  boolean isChangesApplied();

  public void hideApplyButton();

  public void setBottomLineComponent(JComponent component);

  void addStateListener(Procedure<EditingEvent> closeProcedure);

  void setModal(boolean modal);

  EditingEvent getLastEvent();

  void pressOk();


  public final class EditingEvent {
    private final String myDebugName;
    private final boolean myAppling;

    public EditingEvent(String debugName, boolean appling) {
      myDebugName = debugName;
      myAppling = appling;
    }

    public boolean isApplying() {
      return myAppling;
    }

    public String toString() {
      return myDebugName;
    }
  }
}
