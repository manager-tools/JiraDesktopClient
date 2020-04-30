package com.almworks.util.collections;

import com.almworks.util.Pair;
import com.almworks.util.commons.Condition;
import com.almworks.util.commons.Factory;
import com.almworks.util.commons.Function;
import com.almworks.util.commons.Procedure;
import org.almworks.util.ArrayUtil;
import org.almworks.util.Collections15;
import org.almworks.util.Util;
import org.almworks.util.detach.Detach;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static java.lang.Math.min;
import static org.almworks.util.Collections15.arrayList;

/**
 * @author : Dyoma
 */
public class Containers {
  @SuppressWarnings({"RawUseOfParameterizedType"})
  private static final Comparator NO_ORDER = new Comparator() {
    public int compare(Object o1, Object o2) {
      return 0;
    }
  };

  @SuppressWarnings({"RawUseOfParameterizedType"})
  private static final Comparator COMPARABLES_COMPARATOR = new Comparator() {
    public int compare(Object o1, Object o2) {
      if (o1 == null) {
        return o2 == null ? 0 : -1;
      } else if (o2 == null) {
        return 1;
      }
      if (!(o1 instanceof Comparable)) {
        assert false : o1.getClass() + " " + o1;
        if (!(o2 instanceof Comparable)) {
          assert false : o2.getClass() + " " + o2;
          return 0;
        }
        return 1;
      } else if (!(o2 instanceof Comparable)) {
        assert false : o2.getClass() + " " + o2;
        return -1;
      }
      //noinspection RawUseOfParameterizedType
      return ((Comparable) o1).compareTo(o2);
    }
  };
  private static final Comparator<Collection> SIZE_COMPARATOR = new Comparator<Collection>() {
    public int compare(Collection o1, Collection o2) {
      return compareInts(o1.size(), o2.size());
    }
  };
  private static final Comparator<Object> HASH_CODE_COMPARATOR = new Comparator<Object>() {
    public int compare(Object o1, Object o2) {
      long h1 = o1 == null ? 0 : o1.hashCode();
      long h2 = o2 == null ? 0 : o2.hashCode();
      long res = h1 - h2;
      if (res == 0)
        return 0;
      return res < 0 ? -1 : 1;
    }
  };
  private static final Comparator<Object> IDENTITY_HASH_CODE_COMPARATOR = new Comparator<Object>() {
    @Override
    public int compare(Object o1, Object o2) {
      int h1 = o1 == null ? 0 : System.identityHashCode(o1);
      int h2 = o2 == null ? 0 : System.identityHashCode(o2);
      return Util.compareInts(h1, h2);
    }
  };

  private static final Comparator<Object> STRING_VALUEOF_COMPARATOR = new Comparator<Object>() {
    public int compare(Object o1, Object o2) {
      return String.valueOf(o1).compareTo(String.valueOf(o2));
    }
  };
  private static final Comparator<Object> TO_STRING_COMPARATOR = new Comparator<Object>() {
    public int compare(Object o1, Object o2) {
      String v1 = o1 == null ? "" : String.valueOf(o1);
      String v2 = o2 == null ? "" : String.valueOf(o2);
      return String.CASE_INSENSITIVE_ORDER.compare(v1, v2);
    }
  };
  public static final Comparator<Object> DEFAULT_COMPARATOR = new Comparator<Object>() {
    @Override
    public int compare(Object o1, Object o2) {
      if (o1 == o2) return 0;
      Comparable comp1 = Util.castNullable(Comparable.class, o1);
      Comparable comp2 = Util.castNullable(Comparable.class, o2);
      if (comp1 == null && comp2 == null) return 0;
      if (comp1 != null && comp2 != null) return comp1.compareTo(comp2);
      return comp1 == null ? -1 : 1;
    }
  };

  public static <T> Comparator<T> stringComparator() {
    return (Comparator<T>) STRING_VALUEOF_COMPARATOR;
  }

  private static final Factory SYNCHRONIZED_MAP_FACTORY = new Factory() {
    public Object create() {
      return Collections.synchronizedMap(Collections15.hashMap());
    }
  };

  public static int compareInts(int a, int b) {
    return (a < b ? -1 : (a == b ? 0 : 1));
  }

  public static int compareLongs(long a, long b) {
    return (a < b ? -1 : (a == b ? 0 : 1));
  }

  /**
   * Checks if two comparators represents same order.
   *
   * @return -1 comparators are different<p>
   *         0 comparators are equal<p>
   *         1 one of comparators is reversed version of another
   */
  public static int checkSameComparators(Comparator<?> comparator1, Comparator<?> comparator2) {
    if (comparator1 == null || comparator2 == null)
      return -1;
    if (Util.equals(comparator1, comparator2))
      return 0;
    Comparator<?> original1 = ReverseComparator.extractOriginal(comparator1);
    Comparator<?> original2 = ReverseComparator.extractOriginal(comparator2);
    if (!Util.equals(original1, original2))
      return -1;
    return 1;
  }

  public static boolean isReversedOrder(Comparator<?> comparator) {
    return comparator instanceof ReverseComparator<?>;
  }

  @NotNull
  public static <T> List<T> collectList(Iterator<? extends T> iterator) {
    ArrayList<T> result = new ArrayList<T>();
    while (iterator.hasNext()) {
      T t = iterator.next();
      result.add(t);
    }
    return result;
  }

  public static <T> List<T> collectList(Enumeration<? extends T> enumeration) {
    ArrayList<T> result = new ArrayList<T>();
    while (enumeration.hasMoreElements()) {
      T t = enumeration.nextElement();
      result.add(t);
    }
    return result;
  }

  public static <C, T> List<T> collectList(Collection<C> containerCollection, Convertor<C, Collection<T>> extractor) {
    List<T> result = Collections15.arrayList();
    for (C aContainerCollection : containerCollection)
      result.addAll(extractor.convert(aContainerCollection));
    return result;
  }

  public static <T> Set<T> collectSet(Iterator<T> iterator) {
    Set<T> result = Collections15.hashSet();
    while (iterator.hasNext())
      result.add(iterator.next());
    return result;
  }


  public static <T extends Comparable> Comparator<T> comparablesComparator() {
    return COMPARABLES_COMPARATOR;
  }

  public static <T extends Comparable> Comparator<T> comparablesComparator(final boolean nullsFirst) {
    final Comparator<T> comparator = COMPARABLES_COMPARATOR;
    return nullableComparator(comparator, nullsFirst);
  }

  public static <T> Comparator<T> nullableComparator(final Comparator<T> comparator, final boolean nullsFirst) {
    return new Comparator<T>() {
      public int compare(T o1, T o2) {
        return o1 != null && o2 != null ? comparator.compare(o1, o2) : compareNull(o1, o2, nullsFirst);
      }
    };
  }

  public static int compareNull(Object o1, Object o2, boolean nullsFirst) {
    if (o1 == o2)
      return 0;
    if (o1 == null)
      return nullsFirst ? -1 : 1;
    else
      return nullsFirst ? 1 : -1;
  }

  public static <E, V> Comparator<E> convertingComparator(final ReadAccessor<? super E, ? extends V> accessor,
    final Comparator<? super V> comparator)
  {
    if (accessor == null || comparator == null) {
      assert false;
      return null;
    }
    return new ConvertingComparator<E, V>(comparator, accessor);
  }

  public static <E, V> Comparator<E> convertingComparator(
    final Convertor<? super E, V> convertor, Comparator<? super V> comparator)
  {
    return new ConvertingComparator<E, V>(comparator, convertor.toReadAccessor());
  }

  public static <E, V extends Comparable<V>> Comparator<E> convertingComparator(final Convertor<? super E, V> convertor) {
    return convertingComparator(convertor, comparablesComparator());
  }

  public static <T> boolean isOrderValid(List<T> elements, Comparator<? super T> comparator) {
    Iterator<T> iterator = elements.iterator();
    if (!iterator.hasNext())
      return true;
    T prev = iterator.next();
    while (iterator.hasNext()) {
      T t = iterator.next();
      if (comparator.compare(prev, t) > 0)
        return false;
      prev = t;
    }
    return true;
  }

  public static <T> boolean isOrderValidAt(T[] array, int index, Comparator<T> comparator) {
    return isOrderValidAt(Arrays.asList(array), index, comparator);
  }

  /**
   * Checks that element in list at indices (index-1), index, (index+1) are ordered by comparator
   */
  public static <T> boolean isOrderValidAt(List<T> list, int index, Comparator<T> comparator) {
    int minIndex = index > 0 ? index - 1 : index;
    int maxIndex = index < list.size() - 1 ? index + 1 : index;
    for (int i = minIndex; i < maxIndex; i++)
      if (comparator.compare(list.get(i), list.get(i + 1)) > 0)
        return false;
    return true;
  }

  public static <T> Comparator<T> noOrder() {
    return NO_ORDER;
  }

  public static <T> Iterator<T> notNullIterator(final Iterator<T> iterator) {
    return Condition.<T>notNull().filterIterator(iterator);
  }

  public static <T> Comparator<T> reverse(Comparator<T> comparator) {
    if (comparator instanceof ReverseComparator) {
      return ((ReverseComparator<T>) comparator).getOriginal();
    } else
      return new ReverseComparator<T>(comparator);
  }

  public static <T> Comparator<T> twoLevelComparator(final Comparator<T> comparator1, final Comparator<T> comparator2) {
    if (comparator1 == null)
      return comparator2;
    else if (comparator2 == null)
      return comparator1;
    return new TwoLevelComparator<T>(comparator1, comparator2);
  }

  public static <T> Set<T> intersect(Collection<? extends T> set1, Collection<? extends T> set2) {
    if (set1.isEmpty() || set2.isEmpty())
      return Collections15.emptySet();
    Set<T> result = Collections15.hashSet();
    for (T item : set1)
      if (set2.contains(item))
        result.add(item);
    return result;
  }

  public static boolean checkNotNulls(Object[] objects) {
    //noinspection ForLoopReplaceableByForEach
    for (int i = 0; i < objects.length; i++) {
      Object object = objects[i];
      if (object == null)
        return false;
    }
    return true;
  }

  /**
   * Returns set theoretic difference (members in a and not in b).
   */
  @NotNull
  public static <T> Set<T> complement(Collection<? extends T> a, Collection<? extends T> b) {
    Set<T> result = Collections15.hashSet(a);
    if (b != null)
      result.removeAll(b);
    return result;
  }

  public static <T> void apply(Procedure<? super T> procedure, Iterable<? extends T> collection) {
    for (T t : collection)
      procedure.invoke(t);
  }

  public static <T> void apply(Procedure<? super T> procedure, T... elements) {
    apply(procedure, arrayList(elements));
  }

  public static <T> Comparator<List<T>> lexicographicComparator(final boolean nullsFirst, final boolean largerListsFirst, final Comparator<? super T> comparator) {
    return new LexicographicalListsComparator<T>(nullsFirst, largerListsFirst, comparator);
  }

  @Nullable
  public static <T> T getAtOrNull(Enumeration<T> enumeration, int index) {
    while (enumeration.hasMoreElements()) {
      T item = enumeration.nextElement();
      if (index == 0)
        return item;
      index--;
    }
    return null;
  }

  public static int countElements(Iterator<?> iterator) {
    int count = 0;
    while (iterator.hasNext()) {
      iterator.next();
      count++;
    }
    return count;
  }

  public static int calcSum(int[] array) {
    return calcSum(array, 0, array.length);
  }

  public static int calcSum(int[] array, int offset, int length) {
    int total = 0;
    for (int i = 0; i < length; i++)
      total += array[i + offset];
    return total;
  }

  public static <T extends Collection> Comparator<T> countComparator() {
    return (Comparator) SIZE_COMPARATOR;
  }

  public static <R> Function.Const<R> constF(final R value) {
    return Function.Const.create(value);
  }

  public static <T> Detach synchonizedRemove(final Collection<T> collection, final T item) {
    return new Detach() {
      protected void doDetach() throws Exception {
        synchronized (collection) {
          collection.remove(item);
        }
      }
    };
  }

  public static <T> Comparator<T> hashCodeComparator() {
    //noinspection unchecked
    return (Comparator<T>) HASH_CODE_COMPARATOR;
  }

  public static <T> Comparator<T> identityHashCodeComparator() {
    return (Comparator<T>) IDENTITY_HASH_CODE_COMPARATOR;
  }

  public static <K, V> Factory<Map<K, V>> synchronizedMapFactory() {
    return SYNCHRONIZED_MAP_FACTORY;
  }

  /**
   * @return a pair of added items and removed items (not nulls)
   */
  @NotNull
  public static <T> Pair<Collection<T>, Collection<T>> diffSet(@NotNull Collection<T> from, @NotNull Collection<T> to) {
    Collection<T> intersection = intersect(from, to);
    if (intersection.isEmpty())
      return Pair.create(to, from);
    Collection<T> added = Collections15.linkedHashSet(to);
    Collection<T> removed = Collections15.linkedHashSet(from);
    added.removeAll(intersection);
    removed.removeAll(intersection);
    return Pair.create(added, removed);
  }

  public static <T> boolean intersects(@Nullable Collection<? extends T> a, @Nullable Collection<? extends T> b) {
    if (a == null || b == null || a.isEmpty() || b.isEmpty())
      return false;
    for (T elem : a) {
      if (b.contains(elem))
        return true;
    }
    return false;
  }

  public static <T> boolean intersects(@Nullable Collection<? extends T> a, @Nullable T ... b) {
    if (b == null || a == null || b.length == 0 || a.isEmpty())
      return false;
    for (T elem : b) {
      if (a.contains(elem))
        return true;
    }
    return false;
  }

  public static boolean containsAll(Collection<?> collection, Iterator<?> iterator) {
    while (iterator.hasNext()) {
      if (!collection.contains(iterator.next())) return false;
    }
    return true;
  }

  public static boolean containsAny(Collection<?> superSet, Collection<?> subset) {
    for (Object o : subset) {
      if (superSet.contains(o)) return true;
    }
    return false;
  }

  /**
   * Converts list to a sorted list containing only unique elements. Note that a different object is always returned even if the list was sorted and unique.
   * @param list not modified in any case
   * @return writable, read-only or probably argument. The result doesnt contain equal elements
   */
  public static <T extends Comparable<? super T>> List<T> toUniqueSortedList(List<T> list) {
    if (list == null || list.isEmpty()) return Collections15.emptyList();
    if (list.size() == 1) return list;
    Comparable[] array = list.toArray(new Comparable[list.size()]);
    Arrays.sort(array);
    int size = ArrayUtil.removeSubsequentDuplicates(array, 0, array.length);
    List<T> result = Collections15.<T>arrayList(size);
    T[] arrayT = (T[])array;
    for (int i = 0; i < size; i++) result.add(arrayT[i]);
    return result;
  }

  public static <T extends Comparable<? super T>> Comparator<Collection<? extends T>> collectionComparator(final boolean nullsFirst) {
    Comparator<T> elementComparator = comparablesComparator();
    return Containers.<T>collectionComparator(nullsFirst, elementComparator);
  }

  public static <T> Comparator<Collection<? extends T>> collectionComparator(final boolean nullsFirst, final Comparator<? super T> elementComparator) {
    return new Comparator<Collection<? extends T>>() {
      @Override
      public int compare(Collection<? extends T> o1, Collection<? extends T> o2) {
        List<T> sorted1 = arrayList(o1);
        List<T> sorted2 = arrayList(o2);
        Collections.sort(sorted1, elementComparator);
        Collections.sort(sorted2, elementComparator);
        Iterator<? extends T> i1 = sorted1.iterator();
        Iterator<? extends T> i2 = sorted2.iterator();
        for (int k = 0, sz1 = o1.size(), sz2 = o2.size(), sz = min(sz1, sz2); k < sz; ++k) {
          if (k >= sz1) return -1;
          if (k >= sz2) return 1;
          assert i1.hasNext() && i2.hasNext();
          T t1 = i1.next();
          T t2 = i2.next();
          if (t1 != null || t2 != null) {
            if (t1 == null) return nullsFirst ? -1 : 1;
            if (t2 == null) return nullsFirst ? 1 : -1;
            int cmp = elementComparator.compare(t1, t2);
            if (cmp != 0) return cmp;
          }
       }
       return 0;
      }
    };
  }

  public static <T> Comparator<T> toStringComparator() {
    return (Comparator<T>) TO_STRING_COMPARATOR;
  }

  public static <T> boolean equal(List<? extends T> l1, List<? extends T> l2, Equality<? super T> eq) {
    if (l1 == l2) return true;
    if (l1 == null || l2 == null) return false;
    int sz = l1.size();
    if (sz != l2.size()) return false;
    for (int i = 0; i < sz; ++ i) {
      if (!eq.areEqual(l1.get(i), l2.get(i))) return false;
    }
    return true;
  }

  public static <T> Equality<T> fromComparator(final Comparator<T> comparator) {
    return new Equality<T>() {
      @Override
      public boolean areEqual(T o1, T o2) {
        return comparator.compare(o1, o2) == 0;
      }
    };
  }

  public static <K,V> Convertor<Map.Entry<K,V>, V> getValue() {
    return new Convertor<Map.Entry<K, V>, V>() {
      @Override
      public V convert(Map.Entry<K, V> entry) {
        return entry.getValue();
      }
    };
  }

  private static class ReverseComparator<T> implements Comparator<T> {
    private final Comparator<T> myComparator;

    public ReverseComparator(Comparator<T> comparator) {
      assert comparator != null;
      assert !(comparator instanceof ReverseComparator);
      myComparator = comparator;
    }

    public static <T> Comparator<T> extractOriginal(Comparator<T> comparator) {
      if (comparator instanceof ReverseComparator)
        return ((ReverseComparator<T>) comparator).getOriginal();
      return comparator;
    }

    public int compare(T t, T t1) {
      return myComparator.compare(t1, t);
    }

    public boolean equals(Object o) {
      return o instanceof ReverseComparator && Util.equals(myComparator, ((ReverseComparator<?>) o).myComparator);
    }

    public Comparator<T> getOriginal() {
      return myComparator;
    }

    public int hashCode() {
      return -myComparator.hashCode();
    }
  }


  private static class ConvertingComparator<E, V> implements Comparator<E> {
    private final Comparator<? super V> myComparator;
    private final ReadAccessor<? super E, ? extends V> myAccessor;

    public ConvertingComparator(Comparator<? super V> comparator, ReadAccessor<? super E, ? extends V> accessor) {
      assert comparator != null;
      assert accessor != null;
      myComparator = comparator;
      myAccessor = accessor;
    }

    public int compare(E e, E e1) {
      return myComparator.compare(myAccessor.getValue(e), myAccessor.getValue(e1));
    }

    public boolean equals(Object o) {
      if (!(o instanceof ConvertingComparator))
        return false;
      ConvertingComparator<?, ?> other = (ConvertingComparator<?, ?>) o;
      return Util.equals(myComparator, other.myComparator) && Util.equals(myAccessor, other.myAccessor);
    }

    public int hashCode() {
      return myAccessor.hashCode() ^ myComparator.hashCode();
    }
  }


  private static class TwoLevelComparator<T> implements Comparator<T> {
    private final Comparator<T> myComparator1;
    private final Comparator<T> myComparator2;

    public TwoLevelComparator(Comparator<T> comparator1, Comparator<T> comparator2) {
      assert comparator1 != null;
      assert comparator2 != null;
      myComparator1 = comparator1;
      myComparator2 = comparator2;
    }

    public int compare(T o1, T o2) {
      int result = myComparator1.compare(o1, o2);
      return result != 0 ? result : myComparator2.compare(o1, o2);
    }

    public boolean equals(Object obj) {
      if (!(obj instanceof TwoLevelComparator))
        return false;
      TwoLevelComparator<?> other = (TwoLevelComparator<?>) obj;
      return myComparator1.equals(other.myComparator1) && myComparator2.equals(other.myComparator2);
    }

    public int hashCode() {
      return myComparator1.hashCode() - myComparator2.hashCode();
    }
  }
}
