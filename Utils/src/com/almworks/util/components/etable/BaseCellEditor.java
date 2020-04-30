package com.almworks.util.components.etable;

import com.almworks.util.components.CollectionEditor;
import com.almworks.util.components.plaf.macosx.Aqua;
import com.almworks.util.components.renderer.CellState;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.*;
import java.util.EventObject;

/**
 * Base class for JTextComponent-based TaskWork cell editors.
 * Includes the boilerplate
 * @param <C> Type of JTextComponent.
 */
public abstract class BaseCellEditor<C extends JTextComponent, V> implements
  ColumnEditor, CollectionEditor<V>, KeyListener, DocumentListener, FocusListener
{
  protected static enum Direction {
    RIGHT(0, 1), LEFT(0, -1), DOWN(1, 0), UP(-1, 0), NONE(0, 0);

    private final int myRowDelta;
    private final int myColDelta;

    Direction(int rowDelta, int colDelta) {
      myRowDelta = rowDelta;
      myColDelta = colDelta;
    }

    Direction opposite() {
      switch(this) {
        case RIGHT: return LEFT;
        case LEFT: return RIGHT;
        case DOWN: return UP;
        case UP: return DOWN;
        case NONE: return NONE;
      }
      assert false : this;
      return this;
    }
  }

  protected EdiTableManager myManager;
  protected final C myField;
  protected final JPanel myPanel;

  protected V myEdited;
  protected boolean myDirty;

  public BaseCellEditor(C field) {
    myField = field;
    myField.setBorder(new LineBorder(Aqua.MAC_LIGHT_BORDER_COLOR, 1));
    myField.addFocusListener(this);
    myField.addKeyListener(this);
    myField.getDocument().addDocumentListener(this);


    myPanel = new JPanel(new BorderLayout());
    myPanel.setOpaque(false);
    myPanel.add(myField, BorderLayout.CENTER);
  }

  /* * * ColumnEditor methods * * */

  @Override
  public void setManager(EdiTableManager manager) {
    myManager = manager;
  }

  @Override
  public void editCell(JTable table, int row, int col) {
    table.getSelectionModel().setSelectionInterval(row, row);
    table.editCellAt(row, col, SHOULD_EDIT_MARKER);
    myField.requestFocus();
  }

  /* * * CollectionEditor methods * * */

  @Override
  public JComponent getEditorComponent(CellState state, V item) {
    myField.setFont(state.getFont());
    doSetValue(myField, item);
    return myPanel;
  }

  protected abstract void doSetValue(C field, V item);

  @Override
  public boolean shouldEdit(EventObject event) {
    if(event == SHOULD_EDIT_MARKER) {
      return true;
    }

    if(event instanceof MouseEvent) {
      final MouseEvent me = (MouseEvent) event;
      if(me.getClickCount() == 1 && me.getButton() == MouseEvent.BUTTON1) {
        return true;
      }
    }

    return false;
  }

  @Override
  public boolean shouldSelect(JTable table, int row, int column, V item) {
    return true;
  }

  @Override
  public boolean startEditing(V item) {
    myEdited = item;
    myDirty = false;
    return true;
  }

  @Override
  public boolean stopEditing(V item, boolean commitEditedData) {
    if(commitEditedData) {
      assert item == myEdited;
    }

    if(commitEditedData && myDirty) {
      doSaveEdit(myField, item);
    }

    myEdited = null;
    myDirty = false;
    return true;
  }

  protected abstract void doSaveEdit(C field, V item);

  /* * * KeyListener methods * * */

  public void keyPressed(KeyEvent e) {
    final int mods = e.getModifiers();
    final int code = e.getKeyCode();

    // Tab/Shift-Tab move editing to the next/previous column.
    if(code == KeyEvent.VK_TAB) {
      if(mods == 0) {
        moveEditing(e, getTabDirection());
      } else if(mods == KeyEvent.SHIFT_MASK) {
        moveEditing(e, getTabDirection().opposite());
      }
      return;
    }

    // Enter/Shift-Enter move editing to the next/previous row.
    if(code == KeyEvent.VK_ENTER) {
      if(mods == 0) {
        moveEditing(e, getEnterDirection());
      } else if(mods == KeyEvent.SHIFT_MASK) {
        moveEditing(e, getEnterDirection().opposite());
      }
      return;
    }

    if(mods != 0) {
      return;
    }

    // Arrow keys move editing as appropriate.
    if(code == KeyEvent.VK_DOWN) {
      moveEditing(e, Direction.DOWN);
    } else if(code == KeyEvent.VK_UP) {
      moveEditing(e, Direction.UP);
    } else if(code == KeyEvent.VK_RIGHT) {
      if(myField.getCaretPosition() == myField.getText().length()) {
        moveEditing(e, Direction.RIGHT);
      }
    } else if(code == KeyEvent.VK_LEFT) {
      if(myField.getCaretPosition() == 0) {
        moveEditing(e, Direction.LEFT);
      }
    }
  }

  protected Direction getTabDirection() {
    return Direction.RIGHT;
  }

  protected Direction getEnterDirection() {
    return Direction.DOWN;
  }

  private void moveEditing(KeyEvent e, Direction d) {
    if(d != Direction.NONE) {
      e.consume();
      myManager.moveEditing(this, d.myRowDelta, d.myColDelta);
    }
  }

  public void keyReleased(KeyEvent e) {}

  public void keyTyped(KeyEvent e) {}

  /* * * DocumentListener methods * * */

  public void changedUpdate(DocumentEvent e) {
    myDirty = true;
  }

  public void insertUpdate(DocumentEvent e) {
    myDirty = true;
  }

  public void removeUpdate(DocumentEvent e) {
    myDirty = true;
  }

  /* * * FocusListener methods * * */

  public void focusGained(FocusEvent e) {
    myField.selectAll();
  }

  public void focusLost(FocusEvent e) {}
}
