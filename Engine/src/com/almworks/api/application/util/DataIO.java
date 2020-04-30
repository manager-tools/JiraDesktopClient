package com.almworks.api.application.util;

import com.almworks.api.application.LoadedItemServices;
import com.almworks.api.application.ModelKey;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.sync.ItemVersion;
import com.almworks.util.properties.PropertyMap;

public interface DataIO<V> {
  DataIO<Object> EMPTY = new DataIO<Object>() {
    public void extractValue(ItemVersion itemVersion, LoadedItemServices itemServices, PropertyMap values, ModelKey<Object> modelKey) {
    }

    @Override
    public String toString() {
      return "EmptyIO";
    }
  };

  void extractValue(ItemVersion itemVersion, LoadedItemServices itemServices, PropertyMap values, ModelKey<V> key);

  /** Implementors should implement toString for debug purposes */
  String toString();

  class SimpleIO<V> implements DataIO<V> {
    private final DBAttribute<V> myAttribute;

    public SimpleIO(DBAttribute<V> attribute) {
      myAttribute = attribute;

    }

    public DBAttribute<V> getAttribute() {
      return myAttribute;
    }

    protected void setValue(V domValue, PropertyMap values, ModelKey<V> key) {
      key.setValue(values, domValue);
    }

    @Override
    public void extractValue(ItemVersion itemVersion, LoadedItemServices itemServices, PropertyMap values, ModelKey<V> key) {
      setValue(extractValue(itemVersion.getValue(myAttribute), itemVersion, itemServices), values, key);
    }

    @Override
    public String toString() {
      return toString("SimpleIO");
    }

    protected String toString(String type) {
      return String.format(type + "[%s]", myAttribute);
    }

    protected V extractValue(V dbValue, ItemVersion version, LoadedItemServices itemServices) {
      return dbValue;
    }
  }
}
