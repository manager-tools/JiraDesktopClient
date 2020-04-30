package com.almworks.util.components;

import com.almworks.util.advmodel.AListModel;
import com.almworks.util.advmodel.OrderListModel;
import com.almworks.util.advmodel.SubsetModel;
import com.almworks.util.models.BaseTableColumnAccessor;
import com.almworks.util.models.TableColumnAccessor;
import com.almworks.util.tests.GUITestCase;
import org.almworks.util.detach.Lifespan;

import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

/**
 * @author dyoma
 */
public class JTableAdapterTests extends GUITestCase {
  private TableColumnModel myColumnModel;
  private SubsetModel<TableColumnAccessor> mySubset;

  protected void setUp() throws Exception {
    super.setUp();
    TableModelAdapter tableModel = new TableModelAdapter();
    JTableAdapter table = new JTableAdapter(tableModel);
    myColumnModel = table.getColumnModel();
    OrderListModel<TableColumnAccessor> columns = OrderListModel.create();
    tableModel.init(myColumnModel);
    mySubset = SubsetModel.create(Lifespan.FOREVER, columns, false);
    tableModel.setColumns((AListModel) mySubset);

    tableModel.setData(AListModel.EMPTY);
    columns.addAll(new BaseTableColumnAccessor[]{
          new MockTableColumnAccessor("1"),
          new MockTableColumnAccessor("2"),
          new MockTableColumnAccessor("3"),
          new MockTableColumnAccessor("4")});
  }

  protected void tearDown() throws Exception {
    mySubset = null;
    myColumnModel = null;
    super.tearDown();
  }

  public void testAddingColumns() {
    mySubset.addFromFullSet(new int[]{0});
    assertEquals(1, myColumnModel.getColumnCount());
    TableColumn column1 = myColumnModel.getColumn(0);
    assertEquals("1", column1.getHeaderValue());

    mySubset.addFromFullSet(new int[]{1});
    assertEquals(2, myColumnModel.getColumnCount());
    assertSame(column1, myColumnModel.getColumn(0));
    TableColumn column2 = myColumnModel.getColumn(1);
    assertEquals("2", column2.getHeaderValue());

    mySubset.addFromFullSet(new int[]{2, 3});
    assertEquals(4, myColumnModel.getColumnCount());
    assertSame(column1, myColumnModel.getColumn(0));
    assertSame(column2, myColumnModel.getColumn(1));
    assertEquals("3", myColumnModel.getColumn(2).getHeaderValue());
    TableColumn column3 = myColumnModel.getColumn(3);
    assertEquals("4", column3.getHeaderValue());

    assertEquals(0, column1.getModelIndex());
    assertEquals(3, column3.getModelIndex());
    mySubset.removeAllAt(new int[]{1, 2});
    assertEquals(2, myColumnModel.getColumnCount());
    assertSame(column1, myColumnModel.getColumn(0));
    assertSame(column3, myColumnModel.getColumn(1));
    assertEquals(0, column1.getModelIndex());
    assertEquals(1, column3.getModelIndex());
  }

  public void testReordering() {
    mySubset.addFromFullSet(new int[]{0, 1});
    myColumnModel.moveColumn(0, 1);
    TableColumn column2 = myColumnModel.getColumn(0);
    assertEquals("2", column2.getHeaderValue());
    assertEquals("1", myColumnModel.getColumn(1).getHeaderValue());

    assertEquals("1", mySubset.getAt(0).getName());
    mySubset.removeAllAt(new int[]{0});
    assertEquals(1, myColumnModel.getColumnCount());
    assertEquals("2", column2.getHeaderValue());
    assertSame(column2, myColumnModel.getColumn(0));
  }

  public void testAddingToTheBeginning() {
    TableColumnAccessor first = mySubset.getComplementSet().getAt(0);
    TableColumnAccessor second = mySubset.getComplementSet().getAt(1);
    mySubset.insertFromComplementSet(0, first);
    TableColumn column1 = myColumnModel.getColumn(0);
    assertEquals("1", column1.getHeaderValue());
    assertEquals(0, column1.getModelIndex());

    mySubset.insertFromComplementSet(0, second);
    TableColumn column2 = myColumnModel.getColumn(0);
    assertEquals("2", column2.getHeaderValue());
    assertSame(column1, myColumnModel.getColumn(1));
    assertEquals(1, column1.getModelIndex());
    assertEquals(0, column2.getModelIndex());
  }

  private static class MockTableColumnAccessor extends BaseTableColumnAccessor {
    protected MockTableColumnAccessor(String name) {
      super(name);
    }

    public Object getValue(Object object) {
      return null;
    }
  }
}
