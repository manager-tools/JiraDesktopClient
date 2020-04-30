package com.almworks.util.components;

import com.almworks.util.advmodel.AListModel;
import com.almworks.util.components.plaf.macosx.Aqua;
import com.almworks.util.components.renderer.CellState;
import com.almworks.util.ui.SingleChildLayout;
import com.almworks.util.ui.swing.SwingTreeUtil;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.awt.event.*;
import java.util.EventObject;

/**
 * A generic {@link AbstractButton}-based renderer and editor
 * used to create button-based editable table columns.
 * @param <T> The type of the edited thing.
 * @param <B> The type of the button.
 */
public abstract class ButtonActor<T, B extends AbstractButton>
  implements CollectionRenderer<T>, CollectionEditor<T>, ActionListener, MouseListener
{
  /**
   * A {@link JRadioButton}-based {@code ButtonActor}.
   * @param <T> The type of the edited thing.
   */
  public static abstract class Radio<T> extends ButtonActor<T, JRadioButton> {
    public Radio() {
      super(new JRadioButton());
    }
  }

  /**
   * A {@link JCheckBox}-based {@code ButtonActor}.
   * @param <T> The type of the edited thing.
   */
  public static abstract class Checkbox<T> extends ButtonActor<T, JCheckBox> {
    public Checkbox() {
      super(new JCheckBox());
    }
  }

  protected final RenderingPanel myPanel = new RenderingPanel();
  protected final B myButton;

  public ButtonActor(B button) {
    myButton = button;
    myPanel.setLayout(new SingleChildLayout(SingleChildLayout.PREFERRED));
    myButton.setAlignmentX(0.5F);
    myButton.setAlignmentY(0.5F);
    myPanel.add(myButton);
    myPanel.setOpaque(true);
    myButton.setOpaque(false);
    myButton.addActionListener(this);
    myPanel.addMouseListener(this);
    Aqua.makeSmallComponent(myButton);
  }

  protected T myEdited;

  public JComponent getRendererComponent(CellState state, T item) {
    setup(state, item);
    return myPanel;
  }

  protected void setup(CellState state, T item) {
    myButton.setSelected(isSelected(item));
    myPanel.setBorder(state.getBorder());
    myPanel.setBackground(state.getBackground());
  }

  /**
   * @param item The edited item.
   * @return Whether the given item is selected or not.
   */
  protected abstract boolean isSelected(T item);

  public JComponent getEditorComponent(CellState state, T item) {
    setup(state, item);
    return myPanel;
  }

  public boolean startEditing(T item) {
    assert (myEdited == null || myEdited == item) : myEdited + " " + item;
    myEdited = item;
    return true;
  }

  public boolean stopEditing(T item, boolean commitEditedData) {
    if (commitEditedData) {
      assert myEdited == item : myEdited + " " + item;
    }
    myEdited = null;
    return true;
  }

  public boolean shouldEdit(EventObject event) {
    if (event instanceof MouseEvent) return true;
    if (event instanceof KeyEvent) {
      KeyStroke stroke = KeyStroke.getKeyStrokeForEvent((KeyEvent) event);
      return myButton.getInputMap().get(stroke) != null;
    }
    return false;
  }

  public boolean shouldSelect(JTable table, int row, int column, T item) {
    return false;
  }

  protected JTable stopEdit() {
    final JTable table = SwingTreeUtil.findAncestorOfType(myPanel, JTable.class);
    if (table != null) {
      final TableCellEditor editor = table.getCellEditor();
      if (editor != null) {
        editor.stopCellEditing();
      }
      table.requestFocus();
    }
    return table;
  }

  public void mouseClicked(MouseEvent e) {}

  public void mousePressed(MouseEvent e) {
    stopEdit();
  }

  public void mouseReleased(MouseEvent e) {}

  public void mouseEntered(MouseEvent e) {}

  public void mouseExited(MouseEvent e) {}

  protected void stopEditAndUpdate(T item) {
    final JTable table = stopEdit();
    if (table != null) {
      final Container p = table.getParent();
      if (p instanceof ATable) {
        final AListModel model = ((ATable) p).getDataModel();
        final int i = model.indexOf(item);
        if (i >= 0) {
          model.forceUpdateAt(i);
        }
      }
    }
  }

  public void actionPerformed(ActionEvent e) {
    final T edited = myEdited;
    if (edited == null) {
      assert false : this;
      return;
    }
    act(edited);
    stopEditAndUpdate(edited);
  }

  /**
   * Perform an appropriate action on the given item.
   * @param item The edited item.
   */
  protected abstract void act(T item);
}
