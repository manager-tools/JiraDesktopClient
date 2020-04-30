package com.almworks.util.ui;

import com.almworks.util.collections.ChangeListener;
import com.almworks.util.collections.Modifiable;
import com.almworks.util.collections.SimpleModifiable;
import com.almworks.util.components.EditableText;
import com.almworks.util.ui.actions.CantPerformExceptionExplained;
import com.almworks.util.ui.swing.DocumentUtil;
import org.almworks.util.detach.Lifespan;

import javax.swing.*;
import java.awt.*;

/**
 * @author : Dyoma
 */
public class TextEditor implements DialogEditor {
  private final JTextField myEditor;
  private final TextWatcher myWatcher = new TextWatcher();
  private final SimpleModifiable myModifiable = new SimpleModifiable();
  private EditableText myTarget;

  public TextEditor() {    
    this(new JTextField());
    DocumentUtil.addChangeListener(Lifespan.FOREVER, myEditor.getDocument(), myModifiable);
  }

  public TextEditor(JTextField editor) {
    myEditor = editor;
    myEditor.setDragEnabled(true);
    myModifiable.addChangeListener(Lifespan.FOREVER, myWatcher);
  }


  public void setTarget(EditableText editableText) {
    myTarget = editableText;
    reset();
  }

  public void apply() throws CantPerformExceptionExplained {
    assert myTarget != null;
    myTarget.setText(getCurrentText());
    myTarget.setUserTyped(myWatcher.isUserTyped());
  }

  public void reset() {
    assert myTarget != null;
    myWatcher.resetText();
  }

  @Override
  public Modifiable getModifiable() {
    return myModifiable;
  }

  public boolean isModified() {
    return myTarget != null && !getTargetText().equals(getCurrentText());
  }

  private String getTargetText() {
    return myTarget.getText();
  }

  private String getCurrentText() {
    return DocumentUtil.getDocumentText(myEditor.getDocument());
  }

  public JComponent getComponent() {
    return myEditor;
  }

  public void dispose() {

  }

  public boolean isDefaultContent() {
    return myWatcher.isDefaultContent();
  }

  public void setDefaultContent(String defaultText) {
    myWatcher.setDefaultContent(defaultText);
  }

  private class TextWatcher implements ChangeListener {
    private boolean myUserTyped = false;

    public void onChange() {
      myUserTyped = true;
      updateBackground();
    }

    public void resetText() {
      setExternalText(getTargetText());
    }

    public boolean isUserTyped() {
      return myUserTyped;
    }

    public void setDefaultContent(String defaultText) {
      if (myUserTyped)
        return;
      setExternalText(defaultText);
    }

    private void setExternalText(String defaultText) {
      UIUtil.setFieldText(myEditor, defaultText);
      myUserTyped = false;
      updateBackground();
      if (isDefaultContent())
        myEditor.selectAll();
    }

    private void updateBackground() {
      Color bg;
      if (isDefaultContent())
        bg = UIManager.getColor("ToolTip.background");
      else
        bg = UIManager.getColor("TextField.background");
      myEditor.setBackground(bg);
    }

    public boolean isDefaultContent() {
      return !myUserTyped && (myTarget == null || myTarget.isDefaultContent());
    }
  }
}
