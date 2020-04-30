package com.almworks.util.ui;

import com.almworks.util.advmodel.AComboboxModel;
import com.almworks.util.advmodel.AListModel;
import com.almworks.util.advmodel.OrderListModel;
import com.almworks.util.collections.ChangeListener;
import com.almworks.util.collections.Modifiable;
import com.almworks.util.components.AComboBox;
import com.almworks.util.components.ADateField;
import com.almworks.util.components.SelectionAccessor;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.model.ValueModel;
import com.almworks.util.properties.BooleanPropertyKey;
import com.almworks.util.properties.PropertyKey;
import com.almworks.util.properties.PropertyMap;
import com.almworks.util.properties.PropertyModelMap;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.DetachComposite;
import org.almworks.util.detach.Lifespan;

import javax.swing.*;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author dyoma
 */
public class ModelMapBinding implements Modifiable {
  private final PropertyModelMap myModels = new PropertyModelMap(null);
  private final PropertyMap myOriginalValues;

  public ModelMapBinding(PropertyMap values) {
    myOriginalValues = new PropertyMap(null, values);
  }

  public Detach addAWTChangeListener(ChangeListener listener) {
    return myModels.addAWTChangeListener(listener);
  }

  public void addAWTChangeListener(Lifespan life, ChangeListener listener) {
    myModels.addAWTChangeListener(life, listener);
  }

  public void addChangeListener(Lifespan life, ChangeListener listener) {
    myModels.addChangeListener(life, listener);
  }

  public void addChangeListener(Lifespan life, ThreadGate gate, ChangeListener listener) {
    myModels.addChangeListener(life, gate, listener);
  }

  private <M> M getModelForRole(PropertyKey<M, ?> roleKey) {
    ensureInstalled(roleKey);
    M model = myModels.get(roleKey.getModelKey());
    assert model != null : roleKey;
    return model;
  }

  private void ensureInstalled(PropertyKey<?, ?> roleKey) {
    if (!myModels.containsKey(roleKey)) {
      myModels.installProperty(roleKey);
      roleKey.tryCopyValue(myModels, myOriginalValues);
      assert myModels.containsKey(roleKey);
    }
  }

  public void setDocument(Lifespan life, PropertyKey<Document, ?> key, JTextComponent field) {
    UIUtil.setDocument(life, field, getModelForRole(key));
  }

  public void setDate(Lifespan life, PropertyKey<ValueModel<Date>, ?> key, final ADateField field) {
    ValueModel<Date> valueModel = getModelForRole(key);
    field.setDateModel(valueModel);
    life.add(new Detach() {
      protected void doDetach() throws Exception {
        field.setDateModel(ValueModel.<Date>create());
      }
    });
  }

  public <T> Detach setCombobox(PropertyKey<AComboboxModel<T>, ?> key, final AComboBox<T> comboBox) {
    comboBox.setModel(getModelForRole(key));
    return new Detach() {
      protected void doDetach() {
        comboBox.setModel(AComboboxModel.EMPTY_COMBOBOX);
      }
    };
  }

  public <T> Detach setMultipleSelection(final PropertyKey<OrderListModel<T>, List<T>> key,
    final SelectionAccessor<T> selection)
  {
    final OrderListModel<T> model = getModelForRole(key);
    final DetachComposite detach = new DetachComposite();
    final ListenerGuard guard = new ListenerGuard();
    selection.setSelected(model.toList());
    detach.add(model.addListener(new AListModel.Adapter() {
      public void onInsert(int index, int length) {
        if (!guard.acquire())
          return;
        if (detach.isEnded())
          return;
        try {
          for (int i = 0; i < length; i++)
            selection.addSelection(model.getAt(index + i));
        } finally {
          guard.release();
        }
      }
    }));
    detach.add(model.addRemovedElementListener(new AListModel.RemovedElementsListener<T>() {
      public void onBeforeElementsRemoved(AListModel.RemoveNotice<T> elements) {
        if (!guard.acquire())
          return;
        if (detach.isEnded())
          return;
        try {
          selection.removeSelection(elements.getList());
        } finally {
          guard.release();
        }
      }
    }));
    detach.add(selection.addListener(new SelectionAccessor.Listener<T>() {
      public void onSelectionChanged(T newSelection) {
        if (!guard.acquire())
          return;
        if (detach.isEnded())
          return;
        try {
          key.setModelValue(myModels, selection.getSelectedItems());
        } finally {
          guard.release();
        }
      }
    }));
    return detach;
  }

  public Detach setBoolean(BooleanPropertyKey key, JToggleButton button) {
    return getModelForRole(key).attachWidget(button, false);
  }

  public Detach setInvertedBoolean(BooleanPropertyKey key, JToggleButton button) {
    return getModelForRole(key).attachWidget(button, true);
  }

  public Detach setConditionalText(PropertyKey.EnablingKey<Document, ?> key, JToggleButton button,
    JTextComponent field)
  {
    DetachComposite detach = new DetachComposite();
    setDocument(detach, key, field);
    detach.add(setBoolean(key.getEnableKey(), button));
    detach.add(ComponentEnabler.create(button, field).createDetach());
    return detach;
  }

  public boolean wasChanged(PropertyKey<?, ?> key) {
    assert myModels.containsKey(key) : key.getName();
    return key.isChanged(myModels, myOriginalValues).isChanged();
  }

  public <T> T getValue(PropertyKey<?, T> key) {
    ensureInstalled(key);
    return key.getModelValue(myModels);
  }

  public void setValues(PropertyMap values) {
    Map<TypedKey, PropertyKey> map = Collections15.hashMap();
    for (PropertyKey<?, ?> key : myModels.keySet()) {
      map.put(key.getValueKey(), (PropertyKey<?, ?>) key);
    }
    for (TypedKey<?> key : values.keySet()) {
      PropertyKey<?, ?> propertyKey = map.get(key);
      if (propertyKey == null)
        continue;
      propertyKey.copyValue(myModels, values);
    }
  }

  public void getCurrentValues(PropertyMap values) {
    for (PropertyKey<?, ?> key : myModels.keySet())
      getCurrentValue(key, values);
  }

  public <V> void getCurrentValue(PropertyKey<?, V> key, PropertyMap values) {
    values.put(key.getValueKey(), key.getModelValue(myModels));
  }

  public Set<? extends PropertyKey<?, ?>> keySet() {
    return myModels.keySet();
  }

  public boolean getBooleanValue(BooleanPropertyKey key) {
    return getValue(key);
  }

  public <V> void setModelValue(PropertyKey<?, V> key, V value) {
    key.setModelValue(myModels, value);
  }
}
