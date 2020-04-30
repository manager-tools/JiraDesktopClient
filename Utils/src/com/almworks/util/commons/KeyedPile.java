package com.almworks.util.commons;


import com.almworks.util.collections.Convertor;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;

import java.util.*;

public class KeyedPile implements Cloneable {
  private final Class<? extends TypedKey> myKeyClass;
  private Map<? extends TypedKey, ?> myValues = Collections15.hashMap();
  private boolean myReadOnly = false;

  public KeyedPile() {
    this(TypedKey.class);
  }

  public KeyedPile(Class<? extends TypedKey> keyClass) {
    myKeyClass = keyClass;
  }

  public KeyedPile(KeyedPile copyFrom) {
    this();
    copyFrom(copyFrom);
  }

  public void copyFrom(KeyedPile pile) {
    assert !myReadOnly;
    if (myReadOnly)
      return;
    myValues.clear();
    if (pile != null)
      myValues.putAll((Map)pile.myValues);
  }

  public <T> T get(TypedKey<T> key) {
    if (!check(key, true, false))
      return null;
    return key.getFrom(myValues);
  }

  public <T> T getValue(TypedKey<T> key) {
    if (!check(key, true, false))
      return null;
    T value = key.getFrom(myValues);
    Class<T> clazz = key.getValueClass();
    if (clazz == null) {
      assert false : key;
      return null;
    }
    if (Collection.class.isAssignableFrom(clazz)) {
      if (List.class.isAssignableFrom(clazz)) {
        if (value == null) {
          return (T) Collections15.emptyList();
        } else {
          if (myReadOnly) {
            // already unmodifiable
            return value;
          } else {
            return (T) Collections.unmodifiableList((List) value);
          }
        }
      } else {
        assert false : "unexpected collection " + clazz;
      }
    }
    return value;
  }

  public void set(TypedKey key, Object value) {
    if (!check(key, true, true))
      return;
    key.putTo(myValues, value);
  }

  public <T> void setValue(TypedKey<T> key, T value) {
    if (!check(key, false, true))
      return;
    key.putTo(myValues, value);
  }

  public <T> void addValue(TypedKey<List<T>> key, T value) {
    if (!check(key, true, true))
      return;
    List<T> list = getOrCreateList(key);
    list.add(value);
  }

  public <T> void addUniqueValue(TypedKey<List<T>> key, T value) {
    if(check(key, true, true)) {
      final List<T> list = getOrCreateList(key);
      if(!list.contains(value)) {
        list.add(value);
      }
    }
  }

  public <T> void addValues(TypedKey<List<T>> key, Collection<? extends T> values) {
    if (!check(key, true, true))
      return;
    List<T> list = getOrCreateList(key);
    list.addAll(values);
  }

  public <T> void addUniqueValues(TypedKey<List<T>> key, Collection<? extends T> values) {
    if (!check(key, true, true))
      return;
    List<T> list = getOrCreateList(key);
    for (T value : values) {
      if (!list.contains(value))
        list.add(value);
    }
  }

  public <T> T unsetValue(TypedKey<T> key) {
    if (!check(key, true, true))
      return null;
    return key.removeFrom(myValues);
  }

  public void setReadOnly() {
    if (myReadOnly)
      return;
    myReadOnly = true;
    for (Map.Entry<? extends TypedKey, ?> e : myValues.entrySet()) {
      Object value = e.getValue();
      if (Collections.class.isInstance(value)) {
        if (List.class.isInstance(value)) {
          e.getKey().putTo(myValues, Collections.unmodifiableList((List)value));
        } else {
          assert false : "unexpected " + value.getClass();
        }
      }
    }
  }

  public Set<TypedKey> getKeys() {
    return Collections.unmodifiableSet(myValues.keySet());
  }

  public boolean containsKey(TypedKey<?> key) {
    return myValues.containsKey(key);
  }

  protected <T> List<T> getOrCreateList(TypedKey<List<T>> key) {
    List<T> list = key.getFrom(myValues);
    if (list == null) {
      list = Collections15.arrayList();
      key.putTo(myValues, list);
    }
    return list;
  }

  private boolean check(TypedKey key, boolean allowCollection, boolean writeOperation) {
    boolean check;
    check = myKeyClass.isInstance(key);
    assert check : key.getClass().getName() + " " + myKeyClass.getName();
    if (!check)
      return false;
    Class valueClass = key.getValueClass();
    check = valueClass != null;
    assert check : key + " " + this;
    if (!check)
      return false;
    if (!allowCollection) {
      check = !Collection.class.isAssignableFrom(valueClass);
      assert check : valueClass;
      if (!check)
        return false;
    }
    if (writeOperation) {
      assert !myReadOnly;
      if (myReadOnly)
        return false;
    }
    return true;
  }

  public String toString() {
    return myValues.toString();
  }

  public KeyedPile clone() {
    try {
      KeyedPile instance = (KeyedPile) super.clone();
      instance.myValues = Collections15.hashMap();
      for (Map.Entry<? extends TypedKey, ?> entry : myValues.entrySet()) {
        Object value = entry.getValue();
        if (value instanceof List) {
          value = new ArrayList((List)value);
        }
        entry.getKey().putTo(instance.myValues, value);
      }
      instance.myReadOnly = false;
      return instance;
    } catch (CloneNotSupportedException e) {
      throw new Error(String.valueOf(this));
    }
  }

  public static <T> Convertor<KeyedPile, T> keyConvertor(final TypedKey<T> key) {
    return new Convertor<KeyedPile, T>() {
      @Override
      public T convert(KeyedPile value) {
        return value.getValue(key);
      }
    };
  }

  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;

    KeyedPile keyedPile = (KeyedPile) o;

    if (myKeyClass != null ? !myKeyClass.equals(keyedPile.myKeyClass) : keyedPile.myKeyClass != null)
      return false;
    if (myValues != null ? !myValues.equals(keyedPile.myValues) : keyedPile.myValues != null)
      return false;

    return true;
  }

  public int hashCode() {
    int result;
    result = (myKeyClass != null ? myKeyClass.hashCode() : 0);
    result = 31 * result + (myValues != null ? myValues.hashCode() : 0);
    return result;
  }
}
