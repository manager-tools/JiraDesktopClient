package com.almworks.util.collections;

import org.almworks.util.Util;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * @author Dyoma
 */
public class Comparing {

  /**
   * Compares elements using {@link org.almworks.util.Util#equals(Object,Object)}
   *
   * @param objects array to compare
   * @param list    {@link List} to compare
   * @return true iff both elements have same size and all {@link # equals (Object, Object)} returns true for each pair.
   */
  public static boolean areOrdersEqual(Object[] objects, List<?> list) {
    if (objects.length != list.size())
      return false;
    for (int i = 0; i < objects.length; i++) {
      Object object = objects[i];
      if (!Util.equals(object, list.get(i)))
        return false;
    }
    return true;
  }

  public static boolean areOrdersEqual(@Nullable List<?> list1, @Nullable List<?> list2) {
    if (list1 == null || list2 == null)
      return list1 == list2;
    int size = list1.size();
    if (size != list2.size())
      return false;
    for (int i = 0; i < size; i++) {
      Object obj = list1.get(i);
      if (!Util.equals(list1.get(i), list2.get(i)))
        return false;
    }
    return true;
  }

  public static boolean areCollectionsOrderEqual(Collection<?> col1, Collection<?> col2) {
    if ((col1 instanceof List) && (col2 instanceof List))
      return areOrdersEqual((List<?>) col1, (List<?>) col2);
    if (col1.size() != col2.size())
      return false;
    Iterator<?> iterator1 = col1.iterator();
    for (Iterator<?> iterator = col2.iterator(); iterator.hasNext();) {
      Object obj = iterator.next();
      if (!Util.equals(obj, iterator1.next()))
        return false;
    }
    return true;
  }

  public static boolean areSetsEqual(Collection<?> collection1, Collection<?> collection2) {
    if (collection1 == collection2)
      return true;
    if (collection1 == null || collection2 == null)
      return false;
    if (collection1.size() == 0 && collection2.size() == 0)
      return true;
    return new HashSet(collection1).equals(new HashSet(collection2));
  }

  public static int compare(Comparable obj1, Comparable obj2) {
    if (obj1 == null)
      return obj2 != null ? -1 : 0;
    if (obj2 == null)
      return 1;
    return obj1.compareTo(obj2);
  }

  public static <T> int compare(Comparator<T> comparator, T o1, T o2) {
    if (comparator == null)
      return 0;
    if (o1 == null)
      return o2 != null ? -1 : 0;
    if (o2 == null)
      return 1;
    return comparator.compare(o1, o2);
  }

  public static int hashCode(@Nullable Object object) {
    return object != null ? object.hashCode() : 0;
  }
}
