package com.almworks.api.application;

import com.almworks.util.collections.ChangeListener1;
import com.almworks.util.collections.Convertor;
import com.almworks.util.properties.PropertyMap;
import com.almworks.util.properties.PropertyMapValueDecorator;
import com.almworks.util.ui.actions.AnAction;
import com.almworks.util.ui.actions.DataRole;
import org.almworks.util.TypedKey;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * @author : Dyoma
 */
public interface LoadedItem extends ItemWrapper {
  DataRole<LoadedItem> LOADED_ITEM = DataRole.createRole(LoadedItem.class);
  Convertor<LoadedItem, PropertyMap> GET_VALUES = new Convertor<LoadedItem, PropertyMap>() {
    @Override
    public PropertyMap convert(LoadedItem value) {
      return value != null ? value.getValues() : null;
    }
  };

  PropertyMap getValues();

  List<? extends AnAction> getWorkflowActions();

  List<? extends AnAction> getActions();

  boolean hasProblems();

  boolean isOutOfDate();

  void addAWTListener(ChangeListener1<? extends LoadedItem> listener);

  void removeAWTListener(ChangeListener1<? extends LoadedItem> listener);

  <T> boolean setValue(TypedKey<T> key, T value);

  void setValueDecorator(@Nullable PropertyMapValueDecorator decorator);
}
