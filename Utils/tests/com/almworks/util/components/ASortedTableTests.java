package com.almworks.util.components;

import com.almworks.util.advmodel.OrderListModel;
import com.almworks.util.components.renderer.Renderers;
import com.almworks.util.models.BaseTableColumnAccessor;
import com.almworks.util.models.ColumnAccessor;
import com.almworks.util.models.ColumnSizePolicy;
import com.almworks.util.models.TableColumnAccessor;
import com.almworks.util.tests.GUITestCase;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

/**
 * @author : Dyoma
 */
public class ASortedTableTests extends GUITestCase {
  private static final int TABLE_COLUMN_DEFAULT_WIDTH = 75;
  private static final int POSITIVE_VALUE = 100;

  private CollectionRenderer myCellRenderer;
  private ASortedTable<Object> myTableView;
  private OrderListModel<Object> myData;
  private OrderListModel<TableColumnAccessor<Object, ?>> myColumns;

  protected void setUp() throws Exception {
    super.setUp();
    myTableView = new ASortedTable<Object>();
    myCellRenderer = Renderers.createDefaultRenderer();
    myData = new OrderListModel<Object>();
    myColumns = OrderListModel.create();
    myTableView.setDataModel(myData);
    myTableView.setColumnModel(myColumns);
  }

  public void testAddingColumn() {
    myColumns.addElement(new MyTableColumn("1", myCellRenderer));
    TableColumnModel columnModel = myTableView.getTableColumnModel();
    TableColumn column = columnModel.getColumn(0);
    assertSame(myCellRenderer, ((JTableAdapter.TableCellRendererAdapter) column.getCellRenderer()).getRenderer());
    assertNull(column.getHeaderRenderer());
  }

  public void testHeaderUpdatedOnAddingColumn() {
    final TableColumnModelEvent[] event = new TableColumnModelEvent[1];
    myTableView.getTableColumnModel().addColumnModelListener(new TableColumnModelListener() {
      public void columnMarginChanged(ChangeEvent e) {

      }

      public void columnSelectionChanged(ListSelectionEvent e) {

      }

      public void columnAdded(TableColumnModelEvent e) {
        event[0] = e;
      }

      public void columnMoved(TableColumnModelEvent e) {

      }

      public void columnRemoved(TableColumnModelEvent e) {

      }
    });
    assertNotNull(event);
  }

  public void testPreferedWidth() {
    myColumns.addElement(new MyTableColumn("a", myCellRenderer, -1));
    final TableColumnModel columnModel = myTableView.getTableColumnModel();
    assertEquals(TABLE_COLUMN_DEFAULT_WIDTH, columnModel.getColumn(0).getPreferredWidth());
    myColumns.addElement(new MyTableColumn("b", myCellRenderer, POSITIVE_VALUE));
    final TableColumn column = columnModel.getColumn(1);
    assertEquals(POSITIVE_VALUE, column.getPreferredWidth());
    assertEquals(POSITIVE_VALUE, column.getMinWidth());
    assertEquals(POSITIVE_VALUE, column.getMaxWidth());
  }

  private static class MyTableColumn extends BaseTableColumnAccessor<Object, String> {
    private final int myPreferedWigth;

    public MyTableColumn(String name, CollectionRenderer dataRenderer) {
      this(name, dataRenderer, -1);
    }

    public MyTableColumn(String name, CollectionRenderer cellRenderer, int preferedWidth) {
      super(name, cellRenderer);
      myPreferedWigth = preferedWidth;
    }

    public String getValue(Object o) {
      return getName();
    }

    public int getPreferredWidth(JTable table, ATableModel<Object> tableModel, ColumnAccessor<Object> renderingAccessor,
      int columnIndex) {
      return myPreferedWigth;
    }

    public ColumnSizePolicy getSizePolicy() {
      return ColumnSizePolicy.FIXED;
    }
  }
}
