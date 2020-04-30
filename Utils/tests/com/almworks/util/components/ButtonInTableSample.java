package com.almworks.util.components;

import com.almworks.util.debug.DebugFrame;
import com.almworks.util.images.Icons;
import com.almworks.util.ui.ColorUtil;
import com.almworks.util.ui.SingleChildLayout;

import javax.swing.*;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;

public class ButtonInTableSample {
  private final JPanel myWholePanel = new JPanel(new BorderLayout());
  private final JTable myTable = new MyJTable();
  private final DefaultTableModel myModel = new DefaultTableModel();


  public ButtonInTableSample() {
    myTable.setRowHeight(28);

    myModel.addColumn("Column");
    myTable.setModel(myModel);

    TableColumn column = new TableColumn();
    column.setCellRenderer(new MyRenderer());
    column.setHeaderValue("Column");

    DefaultTableColumnModel columnModel = new DefaultTableColumnModel();
    columnModel.addColumn(column);
    myTable.setColumnModel(columnModel);

    myWholePanel.add(new JScrollPane(myTable));

    //myModel.addColumn("Column");
    for (int i = 1000000; i < 2000000; i += 20001)
      myModel.addRow(new Object[]{Integer.toString(i)});

    setupButtonWork();
  }

  private void setupButtonWork() {
    myTable.addMouseMotionListener(new MouseMotionAdapter() {
      public void mouseDragged(MouseEvent e) {
      }

      public void mouseMoved(MouseEvent e) {
        Point point = e.getPoint();
        int row = myTable.rowAtPoint(point);
        int col = myTable.columnAtPoint(point);
        if (row < 0 || col < 0)
          return;
        if (!isActiveCell(row, col))
          return;
        TableColumn column = myTable.getColumnModel().getColumn(col);
        TableCellRenderer renderer = column.getCellRenderer();
        Component component = renderer.getTableCellRendererComponent(myTable, null, false, false, row, col);
        //myTable.
      }
    });

  }

  private boolean isActiveCell(int row, int col) {
    return true;
  }

  public static void main(String[] args) {
    new ButtonInTableSample().run();
  }

  private void run() {
    DebugFrame.show(myWholePanel, 600, 450);
  }


  private String delegateTooltip(JComponent component, MouseEvent event) {
    Point point = event.getPoint();
    Component c = component.getComponentAt(point);
    if (c == null)
      return component.getToolTipText();
    point.translate(-c.getX(), -c.getY());
    MouseEvent newEvent = new MouseEvent(c, event.getID(),
      event.getWhen(), event.getModifiers(),
      point.x, point.y, event.getClickCount(),
      event.isPopupTrigger());
    return ((JComponent) component).getToolTipText(newEvent);
  }


  private class MyRenderer implements TableCellRenderer {
    private final JPanel myPanel = new JPanel(new BorderLayout()) {
      public String getToolTipText(MouseEvent event) {
        return delegateTooltip(this, event);
      }
    };

    private final JButton myButton = new JButton("Do!");
    private final JLabel myLabel = new JLabel();

    public MyRenderer() {
      myButton.setRolloverEnabled(true);
      myButton.setIcon(Icons.ACTION_GENERIC_ADD);
      myButton.setRolloverIcon(Icons.ACTION_GENERIC_REMOVE);
      myButton.setToolTipText("press me!");

      JPanel panel = new JPanel(new SingleChildLayout()) {
        public String getToolTipText(MouseEvent event) {
          return delegateTooltip(this, event);
        }
      };
      panel.add(myButton);
      myPanel.add(panel, BorderLayout.WEST);
      myPanel.add(myLabel, BorderLayout.CENTER);
      myPanel.setOpaque(true);
    }

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
      int row, int column) {
      if (row >= 0) {
        Object obj = null;
        try {
          obj = myModel.getValueAt(row, column);
        } catch (Exception e) {
          return myPanel;
        }
        String s = String.valueOf(obj);
        myLabel.setText(s);
      } else {
        myLabel.setText(Integer.toString(row));
      }
      myPanel.setBackground(getColor(isSelected, hasFocus));
      return myPanel;
    }

    private Color getColor(boolean selected, boolean hasFocus) {
      Color color = Color.WHITE;
      if (selected)
        color = ColorUtil.between(color, Color.BLUE, 0.03F);
      if (hasFocus)
        color = ColorUtil.between(color, Color.RED, 0.03F);
      return color;
    }
  }

  private static class MyJTable extends JTable {
    private TableCellRenderer overer = null;
    private int overRow;
    private int overColumn;


/*
    public boolean mouseOver(int row, int column, EventObject e){
        if (overer != null ) {
            return false;
        }

	if (row < 0 || row >= getRowCount() ||
	    column < 0 || column >= getColumnCount()) {
	    return false;
	}

        if (!isCellEditable(row, column))
            return false;

        if (editorRemover == null) {
            KeyboardFocusManager fm =
                KeyboardFocusManager.getCurrentKeyboardFocusManager();
            editorRemover = new CellEditorRemover(fm);
            fm.addPropertyChangeListener("permanentFocusOwner", editorRemover);
        }

        TableCellEditor editor = getCellEditor(row, column);
        if (editor != null && editor.isCellEditable(e)) {
	    editorComp = prepareEditor(editor, row, column);
	    if (editorComp == null) {
		removeEditor();
		return false;
	    }
	    editorComp.setBounds(getCellRect(row, column, false));
	    add(editorComp);
	    editorComp.validate();

	    setCellEditor(editor);
	    setEditingRow(row);
	    setEditingColumn(column);
	    editor.addCellEditorListener(this);

	    return true;
        }
        return false;
    }
*/
  }
}