package com.almworks.explorer.qbuilder.filter;

import com.almworks.util.advmodel.AComboboxModel;
import com.almworks.util.advmodel.AListModel;
import com.almworks.util.advmodel.SelectionInListModel;
import com.almworks.util.advmodel.SelectionListener;
import com.almworks.util.properties.ChangeSupport;
import com.almworks.util.properties.PropertyKey;
import com.almworks.util.properties.PropertyMap;
import com.almworks.util.properties.PropertyModelMap;
import org.almworks.util.Util;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author dyoma
 */
abstract class ComboBoxConstraintKey<T> extends PropertyKey<AComboboxModel<T>, T> {
  public ComboBoxConstraintKey(String prefix) {
    super(prefix + ".unit");
  }

  public ChangeState isChanged(PropertyModelMap models, PropertyMap originalValues) {
    AComboboxModel<T> model = models.get(this);
    assert model != null : this;
    return ChangeState.choose(this, originalValues, model.getSelectedItem());
  }

  public void setInitialValue(PropertyMap values, T value) {
    assert !(values instanceof PropertyModelMap) : values.toString();
    values.put(getValueKey(), value);
  }

  @Nullable
  public T getModelValue(PropertyModelMap properties) {
    AComboboxModel<T> model = properties.get(this);
    assert model != null : this;
    return model.getSelectedItem();
  }

  public void setModelValue(PropertyModelMap properties, T value) {
    if (value == null || Util.equals(getModelValue(properties), value))
      return;
    AComboboxModel<T> model = properties.get(this);
    assert model != null : this;
    model.setSelectedItem(value);
  }

  public void installModel(final ChangeSupport changeSupport, PropertyModelMap propertyMap) {
    final SelectionInListModel<T> model =
      SelectionInListModel.createForever((AListModel<T>) getVariantsModel(), getDefaultSelection());
    model.addSelectionListener(Lifespan.FOREVER, new SelectionListener.SelectionOnlyAdapter() {
      public void onSelectionChanged() {
        changeSupport.fireChanged(getValueKey(), null, model.getSelectedItem());
      }
    });
    propertyMap.put(this, model);
  }

  @NotNull
  protected abstract AListModel<? extends T> getVariantsModel();

  @Nullable
  protected abstract T getDefaultSelection();
}
