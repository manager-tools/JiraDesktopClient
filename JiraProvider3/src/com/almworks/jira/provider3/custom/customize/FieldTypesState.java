package com.almworks.jira.provider3.custom.customize;

import com.almworks.jira.provider3.custom.LoadAllFields;
import com.almworks.jira.provider3.custom.impl.CustomFieldsComponent;
import com.almworks.jira.provider3.custom.loadxml.ConfigKeys;
import com.almworks.util.LogHelper;
import com.almworks.util.advmodel.AListModel;
import com.almworks.util.advmodel.OrderListModel;
import com.almworks.util.collections.Containers;
import com.almworks.util.collections.Convertor;
import com.almworks.util.collections.Modifiable;
import com.almworks.util.components.Canvas;
import com.almworks.util.components.CanvasRenderer;
import com.almworks.util.components.SizeCalculator1D;
import com.almworks.util.components.renderer.CellState;
import com.almworks.util.components.renderer.Renderers;
import com.almworks.util.models.ColumnSizePolicy;
import com.almworks.util.models.TableColumnAccessor;
import com.almworks.util.models.TableColumnBuilder;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.almworks.util.Util;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class FieldTypesState {
  private final List<Map<TypedKey<?>, ?>> myInitial;
  private final Map<String, Map<TypedKey<?>, ?>> myTypesByKey = Collections15.hashMap();
  private final OrderListModel<Map<TypedKey<?>, ?>> myCurrentTypes = OrderListModel.create();

  public FieldTypesState(List<Map<TypedKey<?>, ?>> initial) {
    myInitial = initial;
  }

  public Modifiable getModifiable() {
    return myCurrentTypes;
  }

  public boolean isModified() {
    if (myInitial.size() != myCurrentTypes.getSize()) return true;
    for (int i = 0; i < myInitial.size(); i++) {
      Map<TypedKey<?>, ?> initial = myInitial.get(i);
      Map<TypedKey<?>, ?> current = myCurrentTypes.getAt(i);
      if (!Util.equals(initial, current)) return true;
    }
    return false;
  }

  public List<Map<TypedKey<?>, ?>> getCurrent() {
    return Collections15.arrayList(myCurrentTypes.toList());
  }

  public void reset() {
    myCurrentTypes.setElements(myInitial);
    updateTypeByKey();
  }

  private void updateTypeByKey() {
    myTypesByKey.clear();
    for (int i = 0; i < myCurrentTypes.getSize(); i++) {
      Map<TypedKey<?>, ?> map = myCurrentTypes.getAt(i);
      String key = ConfigKeys.KEY.getFrom(map);
      if (key != null) myTypesByKey.put(key, map);
      else LogHelper.error("Missing key", map);
    }
  }

  public AListModel<? extends Map<TypedKey<?>, ?>> getModel() {
    return myCurrentTypes;
  }

  public TableColumnAccessor<Map<TypedKey<?>,?>,String> getFieldKeyColumn() {
    return CustomFieldTypesEditor.createStringColumn(LoadAllFields.KEY, "Key", CustomFieldTypesEditor.SAMPLE_KEY).setValueCanvasRenderer(new CanvasRenderer<String>() {
      @Override
      public void renderStateOn(CellState state, Canvas canvas, String key) {
        Map<TypedKey<?>, ?> type = myTypesByKey.get(key);
        if (type == null) canvas.setFontStyle(Font.ITALIC);
        CustomFieldTypesEditor.KEY_RENDERER.renderStateOn(state, canvas, key);
      }
    })
      .setValueComparator(CustomFieldTypesEditor.KEY_COMPARATOR)
      .createColumn();
  }

  public TableColumnAccessor<Map<TypedKey<?>, ?>, Integer> getFieldCountColumn(final LoadAllFields allFields) {
    return new TableColumnBuilder<Map<TypedKey<?>, ?>, Integer>().setConvertor(new Convertor<Map<TypedKey<?>, ?>, Integer>() {
      @Override
      public Integer convert(Map<TypedKey<?>, ?> value) {
        String key = ConfigKeys.KEY.getFrom(value);
        if (key == null)
          return 0;
        return allFields.countFields(key);
      }
    }).setValueComparator(Containers.comparablesComparator())
      .setSizePolicy(new ColumnSizePolicy.Calculated(SizeCalculator1D.text("9999"), ColumnSizePolicy.FREE))
      .setValueCanvasRenderer(Renderers.rightAlignRenderer())
      .setName("Fields #")
      .setId("FieldsNumber")
      .createColumn();
  }

  public void update(List<Map<TypedKey<?>,?>> loaded) {
    ArrayList<Map<TypedKey<?>, ?>> current = Collections15.arrayList(myCurrentTypes.toList());
    ArrayList<Map<TypedKey<?>, ?>> loadedCopy = Collections15.arrayList(loaded);
    CustomFieldsComponent.mergeTypes(loadedCopy, current);
    for (int i = 0; i < myCurrentTypes.getSize(); i++) {
      Map<TypedKey<?>, ?> updated = current.get(i);
      if (myCurrentTypes.getAt(i) != updated) myCurrentTypes.replaceAt(i, updated);
    }
    myCurrentTypes.addAll(loadedCopy);
    updateTypeByKey();
  }
}
