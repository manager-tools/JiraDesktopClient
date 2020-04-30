package com.almworks.util.collections;

import java.util.AbstractCollection;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

/**
 * @author : Dyoma
 */
public class FlattenCollection <T> extends AbstractCollection<T> {
  private final Collection<Collection<T>> mySources;

  public FlattenCollection(Collection<Collection<T>> sources) {
    mySources = sources;
  }

  public Iterator<T> iterator() {
    return FlattenIterator.create(ConvertingIterator.create(mySources.iterator(), new Convertor<Collection<T>, Iterator<T>>() {
      public Iterator<T> convert(Collection<T> e) {
        return e != null ? e.iterator() : null;
      }
    }));
  }

  public int size() {
    Iterator<Collection<T>> iterator = notNulls();
    int sum = 0;
    while (iterator.hasNext()) {
      Collection<T> set = iterator.next();
      sum += set.size();
    }
    return sum;
  }

  public boolean isEmpty() {
    Iterator<Collection<T>> iterator = notNulls();
    while (iterator.hasNext()) {
      Collection<T> set = iterator.next();
      if (!set.isEmpty())
        return false;
    }
    return true;
  }

  private Iterator<Collection<T>> notNulls() {
    Iterator<Collection<T>> iterator = Containers.notNullIterator(mySources.iterator());
    return iterator;
  }

  public static <E> Collection<E> create(Collection<E> collection1, Collection<E> collection2) {
    Collection[] collections = new Collection[]{collection1, collection2};
    return create(collections);
  }

  public static <E> Collection<E> create(Collection[] collections) {
    return create((Collection) Arrays.asList(collections));
  }

  public static <E> Collection<E> create(Collection<Collection<E>> collections) {
    return new FlattenCollection<E>(collections);
  }

  public static <E> Collection<E> create(Collection<E> col1, Collection<E> col2, Collection<E> col3) {
    return create(new Collection[]{col1, col2, col3});
  }
}
