package com.almworks.restconnector.json;

import com.almworks.util.collections.ConvertingList;
import com.almworks.util.collections.Convertor;
import org.jetbrains.annotations.NotNull;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.Collections;
import java.util.List;

public class ArrayKey<T> extends JSONKey<JSONArray> {
  private static final CastConvertor<JSONArray> CONVERTOR = CastConvertor.create(JSONArray.class);

  /**
   * Access root object as array
   */
  public static final ArrayKey<JSONObject> ROOT_ARRAY = objectArray("");
  public static final ArrayKey<String> ROOT_STRINGS = textArray("");

  private final Convertor<Object, T> myElementConvertor;

  public ArrayKey(String name, Convertor<Object, T> elementConvertor) {
    super(name, CONVERTOR);
    myElementConvertor = elementConvertor;
  }

  public static ArrayKey<String> textArray(String name) {
    return new ArrayKey<String>(name, TEXT);
  }

  public static ArrayKey<JSONObject> objectArray(String name) {
    return new ArrayKey<JSONObject>(name, OBJECT);
  }

  public static ArrayKey<Integer> numberArray(String name) {
    return new ArrayKey<Integer>(name, INTEGER);
  }

  public T element(Object jsonObject, int index) {
    JSONArray array = getValue(jsonObject);
    Object element = array == null ? null : array.get(index);
    return element == null ? null : myElementConvertor.convert(element);
  }

  public int size(Object jsonObject) {
    JSONArray array = getValue(jsonObject);
    return array == null ? 0 : array.size();
  }

  @NotNull
  public List<T> list(Object jsonObject) {
    @SuppressWarnings("unchecked")
    List<Object> array = getValue(jsonObject);
    if (array == null || array.isEmpty()) return Collections.emptyList();
    return ConvertingList.create(array, new Convertor<Object, T>() {
      @Override
      public T convert(Object value) {
        return value == null ? null : myElementConvertor.convert(value);
      }
    });
  }

}
