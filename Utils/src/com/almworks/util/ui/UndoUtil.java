package com.almworks.util.ui;

import com.almworks.util.collections.ElementVisitor;
import com.almworks.util.ui.swing.Shortcuts;
import org.almworks.util.Util;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEdit;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * @author dyoma
 */
public class UndoUtil {
  public static final ComponentProperty<Editable> CUSTOM_EDITABLE = ComponentProperty.createProperty("customEditable");
  private static final ComponentProperty<UndoManager> UNDO_MANAGER = ComponentProperty.createProperty("undoManager");
  private static final ComponentProperty<UndoableEditListener> UNDO_LISTENER =
    ComponentProperty.createProperty("undoManager");
  private static final String UNDO_ACTION_KEY = "Undo";
  private static final String REDO_ACTION_KEY = "Redo";
  private static final AbstractAction UNDO_ACTION = new AbstractAction("Undo") {
    public void actionPerformed(ActionEvent evt) {
      UndoManager manager = UNDO_MANAGER.getClientValue((JComponent) evt.getSource());
      try {
        if (manager.canUndo()) {
          manager.undo();
        }
      } catch (CannotUndoException e) {
      }
    }
  };
  private static final AbstractAction REDO_ACTION = new AbstractAction("Redo") {
    public void actionPerformed(ActionEvent evt) {
      UndoManager manager = UNDO_MANAGER.getClientValue((JComponent) evt.getSource());
      try {
        if (manager.canRedo()) {
          manager.redo();
        }
      } catch (CannotRedoException e) {
      }
    }
  };

  @Nullable
  private static Editable getEditable(JComponent component) {
    Editable editable = CUSTOM_EDITABLE.getClientValue(component);
    if (editable == null && (component instanceof JTextComponent)) {
      final JTextComponent textComponent = (JTextComponent) component;
      return new EditableTextComponent(textComponent);
    }
    return editable;
  }

  public static boolean isUndoInstalled(JComponent component) {
    ActionMap actionMap = component.getActionMap();
    if (UNDO_MANAGER.getClientValue(component) != null) {
      assert actionMap.get(UNDO_ACTION_KEY) != null;
      assert actionMap.get(REDO_ACTION_KEY) != null;
      assert UNDO_LISTENER.getClientValue(component) != null;
      return true;
    }
    assert actionMap.get(UNDO_ACTION_KEY) == null;
    assert actionMap.get(REDO_ACTION_KEY) == null;
    assert UNDO_LISTENER.getClientValue(component) == null;
    return false;
  }

  public static void addUndoSupport(JComponent component) {
    Editable editable = getEditable(component);
    if (editable == null)
      return;
    if (isUndoInstalled(component))
      return;
    final UndoManager undoManager = new UndoManager();
    UNDO_MANAGER.putClientValue(component, undoManager);
    UndoableEditListener listener = new UndoableEditListener() {
      public void undoableEditHappened(UndoableEditEvent evt) {
        final UndoableEdit edit = evt.getEdit();
        undoManager.addEdit(edit);
      }
    };

    UndoableEditListener textPaneListener = new UndoableEditListener() {
      public void undoableEditHappened(UndoableEditEvent evt) {
        final UndoableEdit edit = evt.getEdit();
        if (!edit.getPresentationName().contains("style")) {
          undoManager.addEdit(edit);
        }

      }
    };
    if (component instanceof JTextPane) {
      editable.addUndoableEditListener(textPaneListener);
    } else {
      editable.addUndoableEditListener(listener);
    }
    ActionMap actionMap = component.getActionMap();
    UNDO_LISTENER.putClientValue(component, listener);
    actionMap.put(UNDO_ACTION_KEY, UNDO_ACTION);
    component.getInputMap().put(Shortcuts.UNDO, UNDO_ACTION_KEY);
    actionMap.put(REDO_ACTION_KEY, REDO_ACTION);
    component.getInputMap().put(Shortcuts.REDO, REDO_ACTION_KEY);
  }

  public static void removeUndoSupport(JComponent component) {
    Editable editable = getEditable(component);
    if (editable == null)
      return;
    UndoManager manager = UNDO_MANAGER.getClientValue(component);
    ActionMap actionMap = component.getActionMap();
    if (manager == null) {
      assert actionMap.get(UNDO_ACTION_KEY) == null;
      assert actionMap.get(REDO_ACTION_KEY) == null;
      assert UNDO_LISTENER.getClientValue(component) == null;
      return;
    }
    editable.removeUndoableEditListener(UNDO_LISTENER.getClientValue(component));
    actionMap.remove(UNDO_ACTION_KEY);
    actionMap.remove(REDO_ACTION_KEY);
    UNDO_MANAGER.putClientValue(component, null);
    UNDO_LISTENER.putClientValue(component, null);
  }

  public static void addUndoToAllDescendants(JComponent ancestor) {
    UIUtil.visitComponents(ancestor, JComponent.class, new ElementVisitor<JComponent>() {
      public boolean visit(JComponent component) {
        addUndoSupport(component);
        return true;
      }
    });
  }

  public static void removeUndoFromAllDescendants(JComponent ancestor) {
    UIUtil.visitComponents(ancestor, JComponent.class, new ElementVisitor<JComponent>() {
      public boolean visit(JComponent component) {
        removeUndoSupport(component);
        return true;
      }
    });
  }

  public static void discardUndo(JComponent component) {
    assert getEditable(component) != null;
    UndoManager manager = UNDO_MANAGER.getClientValue(component);
    if (manager != null)
      manager.discardAllEdits();
  }

  public interface Editable {
    public void addUndoableEditListener(UndoableEditListener listener);

    public void removeUndoableEditListener(UndoableEditListener listener);
  }


  private static class EditableTextComponent implements Editable {
    private static final ComponentProperty<PropertyChangeListener> DOCUMENT_LISTENER =
      ComponentProperty.createProperty("documentListener");
    private final JTextComponent myTextComponent;

    public EditableTextComponent(JTextComponent textComponent) {
      myTextComponent = textComponent;
    }

    public void addUndoableEditListener(final UndoableEditListener listener) {
      PropertyChangeListener documentListener = new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent evt) {
          assert "document".equals(evt.getPropertyName());
          assert evt.getSource().equals(myTextComponent);
          assert Util.equals(evt.getNewValue(), myTextComponent.getDocument());
          Document prevDocument = (Document) evt.getOldValue();
          if (prevDocument != null)
            prevDocument.removeUndoableEditListener(listener);
          myTextComponent.getDocument().addUndoableEditListener(listener);
          UNDO_MANAGER.getClientValue(myTextComponent).discardAllEdits();
        }
      };
      myTextComponent.addPropertyChangeListener("document", documentListener);
      DOCUMENT_LISTENER.putClientValue(myTextComponent, documentListener);
      myTextComponent.getDocument().addUndoableEditListener(listener);
    }

    public void removeUndoableEditListener(UndoableEditListener listener) {
      myTextComponent.getDocument().removeUndoableEditListener(listener);
      PropertyChangeListener documentListener = DOCUMENT_LISTENER.getClientValue(myTextComponent);
      if (documentListener != null) {
        myTextComponent.removePropertyChangeListener("document", documentListener);
        DOCUMENT_LISTENER.putClientValue(myTextComponent, null);
      }
    }
  }
}
