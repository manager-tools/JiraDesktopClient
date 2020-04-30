package com.almworks.util.components;

import com.almworks.util.advmodel.AListModel;
import com.almworks.util.advmodel.ListModelHolder;
import com.almworks.util.models.TableColumnAccessor;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.Lifespan;

import javax.swing.event.TableColumnModelListener;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.util.Enumeration;

/**
 * @author : Dyoma
 */
public class ATableModel <T> {
  private final ListModelHolder<T> myData = ListModelHolder.create();
  private final ListModelHolder<TableColumnAccessor<? super T, ?>> myColumns = ListModelHolder.create();
  private final TableColumnModel mySwingColumnModel;
  private SelectionAccessor<T> mySelection;

  ATableModel(TableColumnModel columnModel) {
    mySwingColumnModel = columnModel;
  }

  public AListModel<T> getDataModel() {
    return myData;
  }

  protected AListModel<? extends T> getCurrentDataModel() {
    return myData.getModel();
  }

  public ListModelHolder<TableColumnAccessor<? super T, ?>> getColumnModel() {
    return myColumns;
  }

  public void setColumnModel(AListModel<? extends TableColumnAccessor<? super T, ?>> columns) {
    myColumns.setModel(columns);
  }

  public Detach setDataModel(AListModel<? extends T> dataModel) {
    return myData.setModel(dataModel);
  }

  public int getVisualOrderOf(TableColumnAccessor column) {
    Enumeration<TableColumn> tableColumns = mySwingColumnModel.getColumns();
    int order = 0;
    while (tableColumns.hasMoreElements()) {
      TableColumn tableColumn = tableColumns.nextElement();
      if (column == tableColumn.getIdentifier())
        break;
      order++;
    }
    return order;
  }

  public TableColumn getColumnAt(int visualIndex) {
    if (visualIndex >= mySwingColumnModel.getColumnCount())
      return null;
    return mySwingColumnModel.getColumn(visualIndex);
  }

  public void addTableColumnListener(Lifespan life, final TableColumnModelListener listener) {
    life.add(addTableColumnListener(listener, mySwingColumnModel));
  }

  public static Detach addTableColumnListener(final TableColumnModelListener listener, final TableColumnModel model) {
    model.addColumnModelListener(listener);
    return new Detach() {
      protected void doDetach() {
        model.removeColumnModelListener(listener);
      }
    };
  }

  protected TableColumnModel getSwingColumnModel() {
    return mySwingColumnModel;
  }

  public SelectionAccessor<T> getSelectionAccessor() {
    return mySelection;
  }

  void setSelection(SelectionAccessor<T> selection) {
    assert mySelection == null;
    assert selection != null;
    mySelection = selection;
  }
}
