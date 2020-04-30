package com.almworks.api.application;

import com.almworks.api.engine.Connection;
import com.almworks.util.collections.Convertor;
import com.almworks.util.components.SelectionRole;
import com.almworks.util.properties.PropertyMap;
import com.almworks.util.ui.actions.DataRole;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Loaded version of a primary item.
 * @author dyoma
 */
public interface ItemWrapper extends UiItem {
  Convertor<ItemWrapper, Long> GET_CONNECTION_ITEM = new Convertor<ItemWrapper, Long>() {
    public Long convert(ItemWrapper wrapper) {
      Connection connection = wrapper.getConnection();
      return connection == null ? null : connection.getConnectionItem();
    }
  };
  Convertor<ItemWrapper, PropertyMap> GET_LAST_DB_VALUES = new Convertor<ItemWrapper, PropertyMap>() {
    @Override
    public PropertyMap convert(ItemWrapper value) {
      return value != null ? value.getLastDBValues() : null;
    }
  };

  DataRole<ItemWrapper> ITEM_WRAPPER = DataRole.createRole(ItemWrapper.class);
  SelectionRole<ItemWrapper> FIRST_ITEM_WRAPPER = SelectionRole.first(ITEM_WRAPPER);
  SelectionRole<ItemWrapper> LAST_ITEM_WRAPPER = SelectionRole.last(ITEM_WRAPPER);

  @NotNull
  DBStatus getDBStatus();

  LoadedItemServices services();

  @Nullable
  Connection getConnection();

  MetaInfo getMetaInfo();

  @Nullable
  String getItemUrl();

  boolean isEditable();

  <T> T getModelKeyValue(ModelKey<? extends T> key);

  PropertyMap getLastDBValues();

  enum DBStatus {
    DB_NOT_CHANGED,
    DB_MODIFIED,
    DB_CONFLICT,
    DB_NEW,
    DB_CONNECTION_NOT_READY;

    public boolean isUploadable() {
      return this == DB_MODIFIED || this == DB_NEW;
    }

    public boolean isDiscardable() {
      return this == DB_MODIFIED || this == DB_CONFLICT || this == DB_NEW;
    }
  }
}
