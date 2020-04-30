package com.almworks.api.application.util;

import com.almworks.api.application.ExportValueType;
import com.almworks.api.application.ModelKey;
import com.almworks.api.application.ModelMap;
import com.almworks.util.Pair;
import com.almworks.util.collections.Convertor;
import com.almworks.util.commons.Function;
import com.almworks.util.commons.Procedure2;
import com.almworks.util.properties.PropertyMap;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.Date;
import java.util.Map;

public class ModelKeyUtils {

  public static final GenericToStringConvertor ANY_TO_STRING = new GenericToStringConvertor();

  @Nullable
  public static Pair<String,ExportValueType> getFormattedString(ModelKey<?> key, PropertyMap values, NumberFormat numberFormat,
    DateFormat dateFormat, boolean isMultipleString)
  {
    Object value = key.getValue(values);
    if (value == null)
      return null;
    if (value instanceof Number) {
      String formatted;
      if (value instanceof BigDecimal) {
        BigDecimal decimal = ((BigDecimal) value);
        formatted = numberFormat.format(decimal.doubleValue());
      } else if (value instanceof BigInteger) {
        BigInteger integer = ((BigInteger) value);
        if (integer.bitLength() < 64)
          formatted = numberFormat.format(integer.longValue());
        else
          formatted = numberFormat.format(integer.doubleValue());
      } else if (value instanceof Float || value instanceof Double) {
        formatted = numberFormat.format(((Number) value).doubleValue());
      } else {
        formatted = numberFormat.format(((Number) value).longValue());
      }
      return Pair.create(formatted, ExportValueType.NUMBER);
    } else if (value instanceof Date) {
      String formatted = dateFormat.format(new Date(((Date) value).getTime()));
      return Pair.create(formatted, ExportValueType.DATE);
    } else {
      return Pair.create(value.toString(), isMultipleString ? ExportValueType.LARGE_STRING : ExportValueType.STRING );
    }
  }

  public static <Z> Z replaceFastCached(Map<Z, Z> cache, Z value, Function<Z, Z> convertor) {
    Z result = cache.get(value);
    if (result == null) {
      synchronized (cache) {
        result = cache.get(value);
        if (result == null) {
          result = convertor.invoke(value);
          if (result != null) {
            cache.put(result, result);
          }
        }
      }
    }
    return result;
  }

  public static <T> void setModelValue(ModelKey<T> key, T value, ModelMap model) {
    PropertyMap map = new PropertyMap();
    key.setValue(map, value);
    if (key.isEqualValue(model, map))
      return;
    key.copyValue(model, map);
  }

  public static <T> Function<PropertyMap, T> getModelKeyValue(final ModelKey<T> key) {
    return new Function<PropertyMap, T>() {
      @Override
      public T invoke(PropertyMap argument) {
        return key.getValue(argument);
      }
    };
  }

  public static <T> Procedure2<T, ModelMap> update(final ModelKey<T> key) {
    return new Procedure2<T, ModelMap>() {
      @Override
      public void invoke(T t, ModelMap model) {
        setModelValue(key, t, model);
      }
    };
  }

  public static class GenericToStringConvertor extends Convertor<Object, String> {
    @Override
      public String convert(Object value) {
      if (value == null)
        return "";
      if (value instanceof String)
        return (String) value;
      return String.valueOf(value);
    }
  }
}