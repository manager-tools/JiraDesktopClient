package org.almworks.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Array;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings({"StaticMethodOnlyUsedInOneClass"})
public class Collections15 {
  private static final SortedSet<?> EMPTY_SORTED_SET = new TreeSet();
  private static final SortedMap<?, ?> EMPTY_SORTED_MAP = new TreeMap();
  private static final Enumeration EMPTY_ENUMERATION = new EmptyEnumeration();
  private static final EmptyIterator EMPTY_ITERATOR = new EmptyIterator();
  private static final EmptyIterable EMPTY_ITERABLE = new EmptyIterable();

  public static <T> List<T> emptyList() {
    return (List<T>) Collections.EMPTY_LIST;
  }

  public static <T> List<T> NNList(List<T> c) {
    return c == null ? Collections.<T>emptyList() : c;
  }

  @NotNull
  public static <K, V> HashMap<K, V> hashMap() {
    return new HashMap<K, V>();
  }

  public static <V> Map<Integer, V> intMap() {
    return hashMap();
  }

  public static <K, V> HashMap<K, V> hashMap(int initialCapacity) {
    return new HashMap<K, V>(initialCapacity);
  }

  public static <K, V> Map<K, V> hashMap(@Nullable Map<? extends K, ? extends V> map) {
    return map == null ? new HashMap<K, V>() : new HashMap<K, V>(map);
  }

  public static <K, V> Map<K, V> synchronizedHashMap() {
    return Collections.synchronizedMap(new HashMap<K, V>());
  }

  public static <K, V> WeakHashMap<K, V> weakMap() {
    return new WeakHashMap<K, V>();
  }

  public static <K, V> LinkedHashMap<K, V> linkedHashMap() {
    return new LinkedHashMap<K, V>();
  }

  public static <K, V> LinkedHashMap<K, V> linkedHashMap(Map<K, V> source) {
    LinkedHashMap<K, V> result = new LinkedHashMap<K, V>();
    if (source != null)
      result.putAll(source);
    return result;
  }

  public static <K, V> LinkedHashMap<K, V> linkedHashMap(int initialCapacity, float loadFactor, boolean accessOrder,
    final int maximumSize)
  {
    if (maximumSize > 0) {
      //noinspection CloneableClassWithoutClone
      return new LinkedHashMap<K, V>(initialCapacity, loadFactor, accessOrder) {
        @SuppressWarnings({"RefusedBequest"})
        protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
          return size() > maximumSize;
        }
      };
    } else {
      return new LinkedHashMap<K, V>(initialCapacity, loadFactor, accessOrder);
    }
  }

  @NotNull
  public static <T> Set<T> emptySet() {
    return (Set<T>) Collections.EMPTY_SET;
  }

  public static <T> HashSet<T> hashSet() {
    return new HashSet<T>();
  }

  public static <T> Collection<T> emptyCollection() {
    return emptyList();
  }

  public static <K, V> Map<K, V> emptyMap() {
    return Collections.EMPTY_MAP;
  }

  @NotNull
  public static <T> ArrayList<T> arrayList() {
    return new ArrayList<T>();
  }

  public static <T> ArrayList<T> arrayList(T... array) {
    return array == null ? Collections15.<T>arrayList() : arrayList(Arrays.asList(array));
  }

  /**
   * @deprecated {@link #unmodifiableListCopy(Object[])}
   */
  @Deprecated
  public static <T> List<T> unmodifiableArrayList(T... array) {
    return Collections.unmodifiableList(Arrays.asList(array));
  }

  public static <T> List<T> list(T... array) {
    return arrayList(Arrays.asList(array));
  }

  public static <T extends Comparable<T>> ArrayList<T> sortedArrayList(T... array) {
    ArrayList<T> list = arrayList(array);
    Collections.sort(list);
    return list;
  }

  public static <T> Set<T> set(T... array) {
    return hashSet(Arrays.asList(array));
  }

  public static <T> List<T> arrayList(int capacity) {
    return new ArrayList<T>(capacity);
  }

  public static <T> ArrayList<T> arrayList(@Nullable Collection<? extends T> list) {
    return list == null ? new ArrayList<T>() : new ArrayList<T>(list);
  }

  public static <T> List<T> arrayList(@Nullable Iterable<? extends T> iterable) {
    return arrayList(iterable, -1);
  }
  
  /** @param size size estimate; if < 0, unknown */
  public static <T> List<T> arrayList(@Nullable Iterable<? extends T> iterable, int size) {
    if (iterable == null) return new ArrayList<T>();
    List<T> list;
    if (size < 0) list = arrayList();
    else list = arrayList(size);
    for (Iterator<? extends T> i = iterable.iterator(); i.hasNext();) {
      list.add(i.next());
    }
    return list;
  }

  public static <T> List<T> unmodifiableListCopy(@Nullable Collection<? extends T> list) {
    return list == null || list.isEmpty() ? Collections15.<T>emptyList() :
      Collections.unmodifiableList(arrayList(list));
  }

  public static <T> Set<T> unmodifiableSetCopy(@Nullable Collection<? extends T> collection) {
    return collection == null || collection.isEmpty() ? Collections15.<T>emptySet() :
      Collections.unmodifiableSet(hashSet(collection));
  }

  public static <T> HashSet<T> setUnion(@Nullable Collection<? extends T>... collections) {
    HashSet<T> union = hashSet();
    if (collections != null)
      for (Collection<? extends T> collection : collections)
        union.addAll(collection);
    return union;
  }

  public static <T> Set<T> unmodifiableSetUnion(@Nullable Collection<? extends T>... collections) {
    Set<T> union = null;
    if (collections != null)
      for (Collection<? extends T> collection : collections)
        (union = (union == null ? Collections15.<T>hashSet() : union)).addAll(collection);
    return union == null ? Collections15.<T>emptySet() : Collections.unmodifiableSet(union);
  }

  public static <T> List<T> synchronizedList() {
    return Collections.synchronizedList(Collections15.<T>arrayList());
  }

  public static <T> Set<T> unmodifiableSetCopy(@Nullable T... items) {
    if (items == null || items.length == 0)
      return emptySet();
    else
      return unmodifiableSetCopy(Arrays.asList(items));
  }

  public static <T, V> Map<T, V> unmodifiableMapCopy(@Nullable Map<T, V> map) {
    return map == null || map.isEmpty() ? Collections15.<T, V>emptyMap() :
      Collections.unmodifiableMap(linkedHashMap(map));
  }

  /**
   * Works as {@link Collections#unmodifiableMap}, but instead of throwing NPE on null returns empty map.
   * */
  public static <K, V> Map<K, V> unmodifiableMap(@Nullable Map<K, V> map) {
    return map == null ? Collections15.<K, V>emptyMap() : Collections.unmodifiableMap(map);
  }

  public static <K, V> SortedMap<K, V> treeMap() {
    return new TreeMap<K, V>();
  }

  public static <K, V> SortedMap<K, V> treeMap(Comparator<? super K> comparator) {
    return new TreeMap<K, V>(comparator);
  }

  public static <K, V> SortedMap<K, V> treeMap(@Nullable SortedMap<K, V> map) {
    return map == null ? new TreeMap<K, V>() : new TreeMap<K, V>(map);
  }

  public static <T> SortedSet<T> treeSet() {
    return new TreeSet<T>();
  }

  public static <T> SortedSet<T> treeSet(Comparator<? super T> comparator) {
    return new TreeSet<T>(comparator);
  }

  public static <T> SortedSet<T> treeSet(Comparator<? super T> comparator, T... elements) {
    final SortedSet<T> set = treeSet(comparator);
    for(final T e : elements) {
      set.add(e);
    }
    return set;
  }

  public static <T> LinkedList<T> linkedList() {
    return new LinkedList<T>();
  }

  public static <T> LinkedList<T> linkedList(@Nullable Collection<? extends T> collection) {
    return collection == null ? new LinkedList<T>() : new LinkedList<T>(collection);
  }

  public static <T> Set<T> hashSet(@Nullable Collection<? extends T> collection) {
    return collection == null ? new HashSet<T>() : new HashSet<T>(collection);
  }

  public static <T> LinkedHashSet<T> linkedHashSet() {
    return new LinkedHashSet<T>();
  }

  public static SortedSet<String> emptySortedSet() {
    return (SortedSet<String>) EMPTY_SORTED_SET;
  }

  public static <T> SortedSet<T> treeSet(@Nullable Collection<? extends T> collection) {
    return collection == null ? new TreeSet<T>() : new TreeSet<T>(collection);
  }

  public static <T> SortedSet<T> treeSet(@Nullable Collection<? extends T> collection,
    Comparator<? super T> comparator)
  {
    TreeSet<T> result = new TreeSet<T>(comparator);
    if (collection != null)
      result.addAll(collection);
    return result;
  }

  public static <T> Set<T> hashSet(@Nullable T... array) {
    if (array == null)
      return hashSet();
    Set<T> set = hashSet(array.length);
    for (T t : array)
      set.add(t);
    return set;
  }

  public static <T> Set<T> hashSet(int size) {
    return new HashSet<T>(size);
  }

  public static SortedMap<String, String> emptySortedMap() {
    return (SortedMap<String, String>) EMPTY_SORTED_MAP;
  }

  public static <T> Set<T> linkedHashSet(@Nullable Collection<? extends T> data) {
    LinkedHashSet<T> set = linkedHashSet();
    if (data != null)
      set.addAll(data);
    return set;
  }

  public static <T> Enumeration<T> emptyEnumeration() {
    return EMPTY_ENUMERATION;
  }

  public static <K, V> Map<K, V> hashMap(int capacity, float loadFactor) {
    return new HashMap<K, V>(capacity, loadFactor);
  }

  public static <T> Enumeration<T> enumerate(@Nullable Collection<T> collection) {
    if (collection == null || collection.isEmpty())
      return emptyEnumeration();
    return new ArrayEnumeration<T>(collection.toArray());
  }

  public static <T> T[] arrayCopy(T[] array) {
    T[] copy = (T[]) Array.newInstance(array.getClass().getComponentType(), array.length);
    System.arraycopy(array, 0, copy, 0, array.length);
    return copy;
  }

  public static <T> List<T> listCopyWith(Collection<T> collection, T... items) {
    List<T> result = arrayList(collection.size() + items.length);
    result.addAll(collection);
    for (T item : items)
      result.add(item);
    return result;
  }

  public static <T> Set<T> setCopyWith(Collection<T> collection, T... items) {
    final Set<T> result = hashSet(collection);
    for(final T item : items) {
      result.add(item);
    }
    return result;
  }

  public static <T> Set<? extends T> asSet(Collection<? extends T> collection) {
    if (collection instanceof Set<?>)
      //noinspection CastConflictsWithInstanceof
      return (Set<? extends T>) collection;
    return hashSet(collection);
  }

  public static int[] copyIntCollection(Collection<Integer> collection) {
    int[] artifacts = new int[collection.size()];
    int i = 0;
    for (Integer id : collection) {
      artifacts[i] = id;
      i++;
    }
    return artifacts;
  }

  public static <T> ListIterator<T> emptyIterator() {
    return EMPTY_ITERATOR;
  }

  public static <T> Iterable<T> emptyIterable() {
    return EMPTY_ITERABLE;
  }

  @Nullable
  public static <K, V> Map<K, V> linkedHashMapCopyOrNull(@Nullable Map<K, V> map) {
    if (map == null || map.size() == 0)
      return null;
    LinkedHashMap<K, V> result = linkedHashMap();
    result.putAll(map);
    return result;
  }

  public static <T> Set<T> linkedHashSet(@Nullable T... array) {
    LinkedHashSet<T> result = linkedHashSet();
    if (array != null)
      result.addAll(Arrays.asList(array));
    return result;
  }

  public static <T> List<T> listWith(List<? extends T> head, T... tail) {
    List<T> result = arrayList(head.size() + tail.length);
    result.addAll(head);
    for (T t : tail)
      result.add(t);
    return result;
  }

  public static <T> List<T> unmodifiableListCopy(T... objects) {
    if (objects == null || objects.length == 0)
      return emptyList();
    if (objects.length == 1)
      return Collections.singletonList(objects[0]);
    List<T> list = Arrays.asList(objects);
    return unmodifiableListCopy(list);
  }

  public static <T> List<T> addMaybeCreate(List<T> list, T element) {
    if (list == null)
      list = arrayList();
    list.add(element);
    return list;
  }

  public static <T, C extends Collection<? super T>> C add(@NotNull C coll, T element) {
    coll.add(element);
    return coll;
  }

  public static <T, C extends Collection<? super T>> C remove(@NotNull C coll, T element) {
    coll.remove(element);
    return coll;
  }

  public static <T> Iterator<T> singletonIterator(final T value) {
    return new Iterator<T>() {
      private boolean myAtEnd = false;

      public boolean hasNext() {
        return !myAtEnd;
      }

      public T next() {
        if (myAtEnd)
          throw new NoSuchElementException();
        myAtEnd = true;
        return value;
      }

      public void remove() {
        throw new UnsupportedOperationException();
      }
    };
  }

  public static <A, B, C> Map<A, C> mergeMaps(Map<? extends A, ? extends B> first,
    Map<? extends B, ? extends C> second)
  {
    if (first == null || first.isEmpty() || second == null || second.isEmpty())
      return Collections.emptyMap();
    LinkedHashMap<A, C> r = linkedHashMap();
    for (Map.Entry<? extends A, ? extends B> e : first.entrySet()) {
      C c = second.get(e.getValue());
      if (c != null) {
        r.put(e.getKey(), c);
      }
    }
    return r;
  }

  public static <A, B, C> Map<B, C> mergeMaps2(Map<? extends A, ? extends B> first,
    Map<? extends A, ? extends C> second)
  {
    if (first == null || first.isEmpty() || second == null || second.isEmpty())
      return Collections.emptyMap();
    LinkedHashMap<B, C> r = linkedHashMap();
    for (Map.Entry<? extends A, ? extends B> e : first.entrySet()) {
      C c = second.get(e.getKey());
      if (c != null) {
        r.put(e.getValue(), c);
      }
    }
    return r;
  }

  public static <T> Iterable<T> iterableOnce(final Iterator<T> iterator) {
    return new Iterable<T>() {
      @Override
      public Iterator<T> iterator() {
        return iterator;
      }
    };
  }

  /**
   * Copies the agrument to allow remove modifications without modifing original list.
   * @return not null empty list if argument is empty or null or new copy if argument is not empty
   */
  @NotNull
  public static <T> List<T> copyForRemove(List<? extends T> list) {
    if (list != null && !list.isEmpty()) return arrayList(list);
    return Collections15.<T>emptyList();
  }

  public static <T> Deque<T> arrayDeque() {
    return new ArrayDeque<T>();
  }
  
  public static <K, V> ConcurrentHashMap<K, V> concurrentHashMap() {
    return new ConcurrentHashMap<K, V>();
  }

  private static class ArrayEnumeration<T> implements Enumeration<T> {
    private int myIndex;
    private final Object[] myElements;

    public ArrayEnumeration(Object[] elements) {
      myElements = elements == null ? Const.EMPTY_OBJECTS : elements;
      myIndex = 0;
    }

    public boolean hasMoreElements() {
      return myIndex < myElements.length;
    }

    public T nextElement() {
      if (myIndex >= myElements.length)
        throw new NoSuchElementException();
      return (T) myElements[myIndex++];
    }
  }


  private static class EmptyIterator<T> implements ListIterator<T> {
    public boolean hasNext() {
      return false;
    }

    public T next() {
      throw new NoSuchElementException();
    }

    public void remove() {
      throw new UnsupportedOperationException();
    }

    public boolean hasPrevious() {
      return false;
    }

    public T previous() {
      throw new NoSuchElementException();
    }

    public int nextIndex() {
      return 0;
    }

    public int previousIndex() {
      return -1;
    }

    public void set(T o) {
      throw new UnsupportedOperationException();
    }

    public void add(T o) {
      throw new UnsupportedOperationException();
    }
  }


  private static class EmptyEnumeration implements Enumeration {
    public boolean hasMoreElements() {
      return false;
    }

    public Object nextElement() {
      throw new NoSuchElementException();
    }
  }

  private static class EmptyIterable<T> implements Iterable<T> {
    @Override
    public Iterator<T> iterator() {
      return emptyIterator();
    }
  }
}
