package com.almworks.util.components;

import com.almworks.util.advmodel.AListModel;
import com.almworks.util.advmodel.ConvertingListDecorator;
import com.almworks.util.collections.Convertor;
import com.almworks.util.models.BaseTableColumnAccessor;
import com.almworks.util.models.TableColumnAccessor;
import com.almworks.util.ui.UIUtil;

import javax.swing.*;
import java.awt.*;

/**
 * @author : Dyoma
 */
public class ASortedTableSample {
  private JComponent myWholePanel;
  private JPanel myDataPlace;
  private JPanel myColumnPlace;
  private ASortedTable<String> myTable;
  private JPanel mySubsetEditorPlace;

  public ASortedTableSample() {
    AListSample dataEditor = new AListSample();
    AListSample columnEditor = new AListSample();
    myTable.setDataModel(dataEditor.getList().getCollectionModel());
    myTable.setColumnModel((AListModel) new ConvertingListDecorator<String, TableColumnAccessor<String, String>>(
      columnEditor.getList().getCollectionModel(), new Convertor<String, TableColumnAccessor<String, String>>() {
      public TableColumnAccessor<String, String> convert(final String columnName) {
        return new MyTableColumnAccessor(columnName);
      }
    }));
    setComponent(myDataPlace, dataEditor.getWholePanel());
    setComponent(myColumnPlace, columnEditor.getWholePanel());
    mySubsetEditorPlace.setLayout(UIUtil.createBorderLayout());
    mySubsetEditorPlace.add(myTable.createColumnsEditor().getComponent());
  }

  private void setComponent(JPanel place, JComponent component) {
    place.setLayout(UIUtil.createBorderLayout());
    place.removeAll();
    place.add(component, BorderLayout.CENTER);
  }

  public static void main(String[] args) {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        LinkSample.showFrame(new ASortedTableSample().myWholePanel, "ATable Sample");
      }
    });
  }

  private class MyTableColumnAccessor extends BaseTableColumnAccessor<String, String> {
    private final String myColumnName;

    public MyTableColumnAccessor(String columnName) {
      super(columnName, (CollectionRenderer<String>) null, String.CASE_INSENSITIVE_ORDER);
      myColumnName = columnName;
    }

    public String getValue(String dataElement) {
      return dataElement + ":" + myColumnName;
    }

    public boolean equals(Object obj) {
      if (!(obj instanceof MyTableColumnAccessor))
        return false;
      return myColumnName.equals(((MyTableColumnAccessor) obj).myColumnName);
    }

    public int hashCode() {
      return myColumnName.hashCode();
    }
  }
}
