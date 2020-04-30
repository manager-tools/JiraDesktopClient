package com.almworks.util.collections;

import com.almworks.util.Pair;
import org.almworks.util.Collections15;
import org.almworks.util.detach.Detach;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * @author dyoma
 */
public class MultiMap<K, V> implements Iterable<Pair<K, V>> {
  public static MultiMap EMPTY = new MultiMap(Collections.emptyMap());

  private final Map<K, List<V>> myValues;

  public MultiMap() {
    this(new HashMap<K, List<V>>());
  }

  public MultiMap(int initialCapacity) {
    this(new HashMap<K, List<V>>(initialCapacity));
  }

  private MultiMap(Map<K, List<V>> storage) {
    myValues = storage;
  }

  public void add(K key, V value) {
    List<V> list = myValues.get(key);
    if (list == null) {
      list = new ArrayList<V>(1);
      myValues.put(key, list);
    }
    list.add(value);
  }

  public void addAll(K key, Collection<? extends V> values) {
    for (V value : values) {
      add(key, value);
    }
  }

  public void addAll(MultiMap<K, V> map) {
    for (Pair<K, V> pair : map) {
      add(pair.getFirst(), pair.getSecond());
    }
  }

  public void addAll(Map<K, V> map) {
    for (Map.Entry<K,V> entry : map.entrySet()) {
      add(entry.getKey(), entry.getValue());
    }
  }

  public Detach addReturningDetach(final K key, final V value) {
    add(key, value);
    return new Detach() {
      protected void doDetach() {
        remove(key, value);
      }
    };
  }

  public void clear() {
    myValues.clear();
  }

  /**
   * @return <code>true</code> if atleast one row contains atleast one occurence equal to <code>value</code>. <br>
   * <code>false</code> means noone row contains the specified <code>value</code>
   */
  public boolean containsValue(V value) {
    for (List<V> list : myValues.values()) {
      if (list.contains(value))
        return true;
    }
    return false;
  }

  /**
   * @return true iff <code>value</code> was in specified row, true means the collection was changed
   */
  public boolean remove(K key, V value) {
    List<V> list = myValues.get(key);
    if (list == null)
      return false;
    boolean removed = list.remove(value);
    if (!removed)
      return false;
    if (list.isEmpty())
      myValues.remove(key);
    return removed;
  }

  /**
   * @return <code>null</code> iff no values were stored for specified key, <b>not empty</b> list otherwise.
   */
  @Nullable
  public List<V> removeAll(K key) {
    return myValues.remove(key);
  }

  /**
   * @return unmodifiable list of all values or null if there is no values
   */
  @Nullable
  public List<V> getAll(K key) {
    List<V> values = myValues.get(key);
    return values != null ? Collections.unmodifiableList(values) : null;
  }

  @Nullable
  public List<V> getWritableCopyOrNull(K key) {
    List<V> values = myValues.get(key);
    return (values == null || values.size() == 0) ? null : Collections15.arrayList(values);
  }

  @Nullable
  public List<V> getAllEditable(K key) {
    List<V> values = myValues.get(key);
    return values;
  }

  public boolean containsKey(K key) {
    return myValues.containsKey(key);
  }

  @NotNull
  public Set<K> keySet() {
    return myValues.keySet();
  }

  public static <K, V> MultiMap<K, V> create() {
    return new MultiMap<K, V>();
  }

  public static MultiMap<String, String> create(int initialCapacity) {
    return new MultiMap<String, String>(initialCapacity);
  }

  public static <K, V> MultiMap<K, V> empty() {
    return EMPTY;
  }

  public static <K, V> MultiMap<K, V> create(MultiMap<K, V> map) {
    return create(map.toPairList());
  }

  public static <K, V> MultiMap<K, V> createCopyOrNull(MultiMap<K, V> map) {
    return map == null ? null : create(map.toPairList());
  }

  public static <K, V> MultiMap<K, V> create(Collection<Pair<K, V>> collection) {
    MultiMap<K, V> map = create();
    for (Pair<K, V> pair : collection) {
      map.add(pair.getFirst(), pair.getSecond());
    }
    return map;
  }

  public List<V> replaceAll(K key, V value) {
    List<V> removed = removeAll(key);
    add(key, value);
    return removed;
  }

  public List<V> replaceAll(K key, Collection<? extends V> values) {
    List<V> removed = removeAll(key);
    addAll(key, values);
    return removed;
  }

  public List<Pair<K, V>> toPairList() {
    List<Pair<K, V>> pairs = Collections15.arrayList();
    for (Map.Entry<K, List<V>> entry : myValues.entrySet()) {
      K key = entry.getKey();
      for (V value : entry.getValue()) {
        pairs.add(Pair.create(key, value));
      }
    }
    return pairs;
  }

  public Map<K, List<V>> toListMap() {
    return Collections15.hashMap(myValues);
  }

  public boolean hasValue(K key, V value) {
    List<V> allValues = myValues.get(key);
    return allValues != null && allValues.contains(value);
  }

  public Collection<V> values() {
    if (myValues.size() == 0)
      return Collections15.emptyCollection();
    List<V> result = Collections15.arrayList();
    for (List<V> valueList : myValues.values()) {
      if (valueList != null) {
        result.addAll(valueList);
      }
    }
    return result;
  }

  public Iterator<Pair<K, V>> iterator() {
    return Collections.unmodifiableList(toPairList()).iterator();
  }

  public V getSingle(K key) {
    List<V> values = myValues.get(key);
    if (values == null) {
      return null;
    } else {
      int count = values.size();
      if (count == 0)
        return null;
      if (count == 1)
        return values.get(0);
      assert false : this + " has " + count + " values for " + key + ": " + values;
      return null;
    }
  }

  public int size() {
    int size = 0;
    for (List<V> list : myValues.values()) {
      size += list.size();
    }
    return size;
  }

  @Nullable
  public V removeLast(K key) {
    List<V> list = myValues.get(key);
    if (list == null)
      return null;
    int size = list.size();
    if (size == 0)
      return null;
    V value = list.remove(size - 1);
    if (list.isEmpty())
      myValues.remove(key);
    return value;
  }

  @Nullable
  public V getLast(K key) {
    List<V> list = myValues.get(key);
    if (list == null)
      return null;
    int size = list.size();
    if (size == 0)
      return null;
    return list.get(size - 1);
  }

  public boolean isEmpty() {
    return myValues.isEmpty();
  }

  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;

    MultiMap multiMap = (MultiMap) o;

    if (!myValues.equals(multiMap.myValues))
      return false;

    return true;
  }

  public int hashCode() {
    return myValues.hashCode();
  }

  public String toString() {
    return myValues.toString();
  }
}
