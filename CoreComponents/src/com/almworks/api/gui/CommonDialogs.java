package com.almworks.api.gui;

import com.almworks.util.components.EditableText;
import com.almworks.util.ui.TextEditor;
import com.almworks.util.ui.UIUtil;
import com.almworks.util.ui.actions.ActionContext;
import com.almworks.util.ui.actions.CantPerformException;

import javax.swing.*;
import javax.swing.text.JTextComponent;

/**
 * @author dyoma
 */
public class CommonDialogs {
  /**
   * Subclasses TextEditor for a different presentation:
   * JTextField wrapped in a JPanel. Used to prevent
   * the field from growing in height.
   */
  private static class MyTextEditor extends TextEditor {
    private final JComponent panel = UIUtil.placeOnTop(super.getComponent());

    @Override
    public JComponent getComponent() {
      return panel;
    }

    public JTextComponent getTextComponent() {
      return (JTextComponent)super.getComponent();
    }
  }

  public static DialogEditorBuilder createRenameDialog(ActionContext context, EditableText editableText, String title)
    throws CantPerformException {
    final MyTextEditor editor = new MyTextEditor();
    editor.setTarget(editableText);

    final DialogManager dialogManager = context.getSourceObject(DialogManager.ROLE);
    final DialogEditorBuilder builder = dialogManager.createEditor("renameDialog");
    builder.hideApplyButton();
    builder.setContent(editor);
    builder.setTitle(title);

    final JTextComponent textComponent = editor.getTextComponent();
    textComponent.selectAll();
    builder.setInitialFocusOwner(textComponent);

    return builder;
  }
}