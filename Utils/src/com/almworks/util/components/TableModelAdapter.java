package com.almworks.util.components;

import com.almworks.util.advmodel.AListModel;
import com.almworks.util.models.TableColumnAccessor;
import org.almworks.util.detach.Detach;

import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

/**
 * @author : Dyoma
 */
class TableModelAdapter<T> extends AbstractTableModel {
  private ATableModel<T> myTableModel;

  public TableModelAdapter() {
  }

  protected final TableColumnAccessor<? super T, ?> getColumn(int index) {
    return myTableModel.getColumnModel().getAt(index);
  }

  public String getColumnName(int column) {
    return getColumn(column).getColumnHeaderText();
  }

  private void fireHeaderChanged() {
    fireHeaderChanged(TableModelEvent.ALL_COLUMNS, TableModelEvent.UPDATE);
  }

  private void fireHeaderChanged(int column, int type) {
    fireTableChanged(new TableModelEvent(this, TableModelEvent.HEADER_ROW, TableModelEvent.HEADER_ROW, column, type));
  }

  public int getColumnCount() {
    return myTableModel != null ? myTableModel.getColumnModel().getSize() : 0;
  }

  public int getRowCount() {
    return myTableModel != null ? myTableModel.getDataModel().getSize() : 0;
  }

  public Object getValueAt(int rowIndex, int columnIndex) {
    return getColumn(columnIndex).getValue(myTableModel.getDataModel().getAt(rowIndex));
  }

  public boolean isCellEditable(int rowIndex, int columnIndex) {
    TableColumnAccessor<?, ?> column = getColumn(columnIndex);
    CollectionEditor<?> dataEditor = column.getDataEditor();
    return dataEditor != null;
  }

  public ATableModel<T> getTableModel() {
    return myTableModel;
  }

  public void setColumns(AListModel<? extends TableColumnAccessor<? super T, ?>> columnsModel) {
    myTableModel.setColumnModel(columnsModel);
  }

  public AListModel<TableColumnAccessor<? super T,?>> getColumns() {
    return myTableModel.getColumnModel();
  }

  public Detach setData(AListModel<? extends T> dataModel) {
    return myTableModel.setDataModel(dataModel);
  }

  public AListModel<? extends T> getData() {
    return myTableModel.getDataModel();
  }

  void init(TableColumnModel columnModel) {
    assert myTableModel == null;
    myTableModel = createTableModel(columnModel);
    myTableModel.getDataModel().addListener(new AListModel.Listener() {
      public void onInsert(int index, int length) {
        fireTableRowsInserted(index, index + length - 1);
      }

      public void onRemove(int index, int length, AListModel.RemovedEvent event) {
        fireTableRowsDeleted(event.getFirstIndex(), event.getLastIndex());
      }

      public void onListRearranged(AListModel.AListEvent event) {
        fireTableRowsUpdated(event.getLowAffectedIndex(), event.getHighAffectedIndex());
      }

      public void onItemsUpdated(AListModel.UpdateEvent event) {
        fireTableRowsUpdated(event.getLowAffectedIndex(), event.getHighAffectedIndex());
      }
    });
    myTableModel.getColumnModel().addListener(new AListModel.Adapter() {
      public void onInsert(int index, int length) {
        fireTableChanged(new HeaderTableModelEvent(TableModelAdapter.this, TableModelEvent.INSERT, index, length));
      }

      public void onRemove(int index, int length, AListModel.RemovedEvent event) {
        HeaderTableModelEvent e = new HeaderTableModelEvent(TableModelAdapter.this, TableModelEvent.DELETE,
          event.getFirstIndex(), event.getLength());
        fireTableChanged(e);
      }

      @Override
      public void onItemsUpdated(AListModel.UpdateEvent event) {
        for (int i = event.getLowAffectedIndex(); i <= event.getHighAffectedIndex(); ++i) {
          if (event.isUpdated(i)) {
            fireTableChanged(new HeaderTableModelEvent(TableModelAdapter.this, TableModelEvent.UPDATE, i, 1));
          }
        }
      }

      public void onChange() {
        fireHeaderChanged();
      }
    });
  }

  protected ATableModel<T> createTableModel(TableColumnModel columnModel) {
    return new ATableModel<T>(columnModel);
  }

  boolean isInitialized() {
    return myTableModel != null;
  }

  static class HeaderTableModelEvent extends TableModelEvent {
    private final int myIndex;
    private final int myLength;

    public HeaderTableModelEvent(TableModel source, int type, int index, int length) {
      super(source, HEADER_ROW, HEADER_ROW, ALL_COLUMNS, type);
      myIndex = index;
      myLength = length;
    }

    public int getIndex() {
      return myIndex;
    }

    public int getLength() {
      return myLength;
    }

    public int getLastIndex() {
      return myIndex + myLength - 1;
    }
  }
}
