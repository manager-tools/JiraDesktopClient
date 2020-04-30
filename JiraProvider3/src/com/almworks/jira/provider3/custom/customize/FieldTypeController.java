package com.almworks.jira.provider3.custom.customize;

import com.almworks.jira.provider3.custom.LoadAllFields;
import com.almworks.jira.provider3.custom.loadxml.ConfigKeys;
import com.almworks.util.advmodel.AListModel;
import com.almworks.util.advmodel.FixedListModel;
import com.almworks.util.advmodel.SelectionListener;
import com.almworks.util.collections.ChangeListener;
import com.almworks.util.components.ASortedTable;
import org.almworks.util.TypedKey;
import org.almworks.util.Util;

import javax.swing.*;
import java.util.List;
import java.util.Map;

class FieldTypeController {
  private boolean myDuringUpdate = false;
  private final ASortedTable<Map<TypedKey<?>, ?>> myTypeTable;
  private final ASortedTable<Map<TypedKey<?>, ?>> myFieldTable;
  private final LoadAllFields myFields;

  public FieldTypeController(ASortedTable<Map<TypedKey<?>, ?>> typeTable, ASortedTable<Map<TypedKey<?>, ?>> fieldTable, LoadAllFields fields) {
    myTypeTable = typeTable;
    myFieldTable = fieldTable;
    myFields = fields;
  }

  public static void install(ASortedTable<Map<TypedKey<?>, ?>> typeTable, ASortedTable<Map<TypedKey<?>, ?>> fieldTable, FieldTypesState types, LoadAllFields fields) {
    typeTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    fieldTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    final FieldTypeController controller = new FieldTypeController(typeTable, fieldTable, fields);
    AListModel<? extends Map<TypedKey<?>, ?>> typesModel = types.getModel();
    typesModel.addListener(new SelectionListener.Adapter() {
      @Override
      public void onInsert(int index, int length) {
        controller.myFieldTable.repaint(); // Added type may make field known
      }
    });
    typeTable.setDataModel(typesModel);
    typeTable.getSelectionAccessor().addAWTChangeListener(new ChangeListener() {
      @Override
      public void onChange() {
        controller.onTypeSelected();
      }
    });
    fieldTable.getSelectionAccessor().addAWTChangeListener(new ChangeListener() {
      @Override
      public void onChange() {
        controller.onFieldSelected();
      }
    });
    controller.onTypeSelected();
  }

  private void onFieldSelected() {
    if (myDuringUpdate) return;
    myDuringUpdate = true;
    try {
      Map<TypedKey<?>, ?> field = myFieldTable.getSelectionAccessor().getSelection();
      onFieldSelected(field);
    } finally {
      myDuringUpdate = false;
    }
  }

  private void onFieldSelected(Map<TypedKey<?>, ?> field) {
    if (field != null) {
      String key = LoadAllFields.KEY.getFrom(field);
      List<? extends Map<TypedKey<?>, ?>> typesList = myTypeTable.getCollectionModel().toList();
      for (int i = 0; i < typesList.size(); i++) {
        Map<TypedKey<?>, ?> type =  typesList.get(i);
        String typeKey = ConfigKeys.KEY.getFrom(type);
        if (Util.equals(typeKey, key)) {
          myTypeTable.getSelectionAccessor().setSelectedIndex(i);
          myTypeTable.scrollSelectionToView();
          return;
        }
      }
    }
    myTypeTable.getSelectionAccessor().clearSelection();
  }

  private void onTypeSelected() {
    if (myDuringUpdate) return;
    myDuringUpdate = true;
    try {
      Map<TypedKey<?>, ?> type = myTypeTable.getSelectionAccessor().getSelection();
      onTypeSelected(type);
    } finally {
      myDuringUpdate = false;
    }
  }

  private void onTypeSelected(Map<TypedKey<?>, ?> type) {
    if (type == null) myFieldTable.setDataModel(FixedListModel.create(myFields.getAllFields()));
    else {
      String key = ConfigKeys.KEY.getFrom(type);
      myFieldTable.setDataModel(FixedListModel.create(myFields.getFieldsByKey(key)));
    }
  }
}
