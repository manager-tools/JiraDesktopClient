package com.almworks.util.components;

import com.almworks.util.advmodel.AListModel;
import com.almworks.util.advmodel.FixedListModel;
import com.almworks.util.components.plaf.LAFUtil;
import com.almworks.util.debug.DebugFrame;
import com.almworks.util.models.SimpleColumnAccessor;
import com.almworks.util.models.TableColumnAccessor;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.Comparator;

public class MacTableDemo implements Runnable {
  public static void main(String[] args) {
    LAFUtil.initializeLookAndFeel();
    SwingUtilities.invokeLater(new MacTableDemo());
  }

  public void run() {
    DefaultTableModel dm = new DefaultTableModel(
      new Object[][] {
        {"-", "JRA-1010", "Whatever should be whatevered", "Today"},
        {"-", "JRA-1011", "Whatever should be whatevered-2", "Today"},
      },
      new Object[] {"Flags", "Key", "Summary", "Updated"}
    );
    JTable table = new JTable(dm);

    JTableHeader tableHeader = table.getTableHeader();
    final TableCellRenderer cellRenderer = tableHeader.getDefaultRenderer();

    tableHeader.setDefaultRenderer(new TableCellRenderer() {
      public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
        int row, int column)
      {
        return cellRenderer.getTableCellRendererComponent(table, value, true, true, row, column);
      }
    });

    AListModel<String> data = FixedListModel.create("one crocodile", "zwei krokodielen", "tri krokodila");
    AListModel<TableColumnAccessor<String,?>> columns = FixedListModel.<TableColumnAccessor<String,?>>create(
      new SimpleColumnAccessor<String>("How many crocos?") {
        public Comparator<String> getComparator() {
          return String.CASE_INSENSITIVE_ORDER;
        }
      }
    );

    JScrollPane atableScrollpane = new JScrollPane();
    ATable<String> aTable = ATable.createInscrollPane(atableScrollpane);
    aTable.setDataModel(data);

    aTable.setColumnModel(columns);


    ASortedTable<String> sortedTable = new ASortedTable<String>();
    sortedTable.setCollectionModel(data);
    sortedTable.setColumnModel(columns);
//    DebugFrame.show(new JScrollPane(table));

//    DebugFrame.show(atableScrollpane);

    JSplitPane pane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true, sortedTable, atableScrollpane);
    pane.setDividerLocation(250);
    DebugFrame.show(pane, 500, 500);
  }
}
