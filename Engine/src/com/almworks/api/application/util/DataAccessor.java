package com.almworks.api.application.util;

import com.almworks.api.application.ModelKey;
import com.almworks.api.application.ModelMap;
import com.almworks.util.properties.PropertyMap;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.almworks.util.Util;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

public interface DataAccessor<T> {
  T getValue(ModelMap model);

  boolean hasValue(ModelMap model);

  void setValue(PropertyMap values, T value);

  T getValue(PropertyMap values);

  void copyValue(ModelMap to, PropertyMap from, ModelKey<T> key);

  boolean hasValue(PropertyMap values);

  void takeSnapshot(PropertyMap to, ModelMap from);

  boolean isEqualValue(ModelMap models, PropertyMap values);

  boolean isEqualValue(PropertyMap values1, PropertyMap values2);

  /**
   * @return string representation of value for text match purpose.<br>
   * null return value means that default algorithm should be applied.<br>
   * If there is no value that can match a pattern the empty string should be returned
   */
  String matchesPatternString(PropertyMap map);

  class SimpleDataAccessor<T> implements DataAccessor<T> {
    private final TypedKey<T> myKey;

    public SimpleDataAccessor(String name) {
      myKey = TypedKey.create(name);
    }

    public SimpleDataAccessor(TypedKey<T> key) {
      myKey = key;
    }

    public T getValue(ModelMap model) {
      return model.get(myKey);
    }

    public void setValue(PropertyMap values, T value) {
      values.put(myKey, value);
    }

    public T getValue(PropertyMap values) {
      return values.get(myKey);
    }

    public void copyValue(ModelMap to, PropertyMap from, ModelKey<T> key) {
      if (isEqualValue(to, from))
        return;
      setValue(to, getValue(from));
      to.valueChanged(key);
    }

    protected final void setValue(ModelMap to, T value) {
      to.put(myKey, value);
    }

    public boolean hasValue(ModelMap model) {
      return isExistingValue(getValue(model));
    }

    public boolean hasValue(PropertyMap values) {
      return isExistingValue(values.get(myKey));
    }

    protected boolean isExistingValue(T value) {
      return getCanonicalValueForComparison(value) != null;
    }

    public void takeSnapshot(PropertyMap to, ModelMap from) {
      setValue(to, getValue(from));
    }

    public boolean isEqualValue(ModelMap models, PropertyMap values) {
      T v1 = getValue(values);
      T v2 = getValue(models);
      return isEqualValue(v1, v2);
    }

    public boolean isEqualValue(PropertyMap values1, PropertyMap values2) {
      T v1 = getValue(values1);
      T v2 = getValue(values2);
      return isEqualValue(v1, v2);
    }

    @Override
    public String matchesPatternString(PropertyMap map) {
      return null;
    }

    protected boolean isEqualValue(@Nullable T v1, @Nullable T v2) {
      return Util.equals(getCanonicalValueForComparison(v1), getCanonicalValueForComparison(v2));
    }

    protected Object getCanonicalValueForComparison(@Nullable T value) {
      if (value == null)
        return value;
      if (value instanceof String && ((String) value).length() == 0)
        return null;
      if (value instanceof Object[] && ((Object[])value).length == 0)
        return null;
      if (value instanceof Collection && ((Collection)value).isEmpty())
        return null;
      return value;
    }

    public static <T> DataAccessor<List<T>> createCopyList(String name) {
      return new SimpleDataAccessor<List<T>>(name) {
        @Override
        public List<T> getValue(ModelMap model) {
          return createCopy(super.getValue(model));
        }

        @Override
        public List<T> getValue(PropertyMap values) {
          return createCopy(super.getValue(values));
        }

        private List<T> createCopy(List<T> value) {
          return value != null ? Collections15.arrayList(value) : null;
        }
      };
    }
  }
}
