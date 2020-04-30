package com.almworks.jira.provider3.sync.download2.details.fields;

import com.almworks.items.entities.api.Entity;
import com.almworks.items.entities.api.EntityKey;
import com.almworks.jira.provider3.sync.download2.details.JsonIssueField;
import com.almworks.restconnector.json.JSONKey;
import com.almworks.util.LogHelper;
import com.almworks.util.collections.Convertor;
import org.almworks.util.Collections15;
import org.almworks.util.Util;
import org.jetbrains.annotations.Nullable;
import org.json.simple.JSONArray;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * @see com.almworks.jira.provider3.sync.download2.details.JsonIssueField.FilteredValue
 */
public class ScalarField<T> implements JsonIssueField {
  private final Convertor<Object, T> myConvertor;
  private final EntityKey<T> myKey;

  private ScalarField(Convertor<Object, T> convertor, EntityKey<T> key) {
    myConvertor = convertor;
    myKey = key;
  }

  public static <T> ScalarField<T> independent(EntityKey<T> key, Convertor<Object, T> convertor) {
    return new ScalarField<T>(convertor, key);
  }

  @Override
  public Collection<? extends ParsedValue> loadValue(Object jsonValue) {
    T value = myConvertor.convert(jsonValue);
    return SimpleKeyValue.single(myKey, value);
  }

  @Override
  public Collection<? extends ParsedValue> loadNull() {
    return SimpleKeyValue.single(myKey, null);
  }

  public static JsonIssueField text(EntityKey<String> key) {
    // http://snow:10500/browse/JC-123
    return independent(key, JSONKey.emptyTextToNull(JSONKey.TEXT_TRIM));
  }

  public static ScalarField<Integer> integer(EntityKey<Integer> key) {
    return independent(key, JSONKey.INTEGER);
  }

  public static JsonIssueField bool(EntityKey<Boolean> key) {
    return independent(key, JSONKey.BOOLEAN);
  }

  public static JsonIssueField boolNullFalse(EntityKey<Boolean> key) {
    return independent(key, JSONKey.FALSE_TO_NULL);
  }

  public static ScalarField<Entity> entity(EntityKey<Entity> key, Convertor<Object, Entity> convertor) {
    return independent(key, convertor);
  }

  public static <T> JsonIssueField collection(EntityKey<Collection<T>> key, Convertor<Object, T> elementConvertor) {
    return independent(key, new Collect<T>(elementConvertor));
  }

  public JsonIssueField nullValue(@Nullable T nullValue) {
    return new NullDecorator(new SimpleKeyValue<T>(myKey, nullValue), this);
  }

  public static JsonIssueField date(EntityKey<Date> key) {
    return independent(key, JSONKey.DATE);
  }

  private static class Collect<T> extends Convertor<Object, Collection<T>> {
    private final Convertor<Object, T> myElementConvertor;

    public Collect(Convertor<Object, T> elementConvertor) {
      assert elementConvertor != null;
      myElementConvertor = elementConvertor;
    }

    @Override
    public Collection<T> convert(Object jsonValue) {
      List<Object> array;
      if (jsonValue == null) array = Collections15.emptyList();
      else {
        //noinspection unchecked
        array = Util.castNullable(JSONArray.class, jsonValue);
        if (array == null) {
          LogHelper.error("Expected array", jsonValue);
          return null;
        }
      }
      ArrayList<T> valueList = Collections15.arrayList();
      for (Object element : array) {
        T converted = myElementConvertor.convert(element);
        if (converted == null) continue;
        valueList.add(converted);
      }
      return valueList;
    }
  }
}
