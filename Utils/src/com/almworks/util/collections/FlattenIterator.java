package com.almworks.util.collections;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * @author : Dyoma
 */
public class FlattenIterator<T> implements Iterator<T> {
  private final Iterator<Iterator<T>> mySource;
  private Iterator<T> myCurrent;
  private T myNext = null;
  private boolean myNextValid = false;

  public FlattenIterator(Iterator<Iterator<T>> source) {
    mySource = Containers.notNullIterator(source);
    myCurrent = mySource.hasNext() ? mySource.next() : null;
  }

  public static <T> Iterator<T> create(Iterator<Iterator<T>> iterator) {
    return new FlattenIterator<T>(iterator);
  }

  public boolean hasNext() {
    if (myNextValid)
      return true;
    if (myCurrent == null)
      return false;
    if (myCurrent.hasNext()) {
      myNext = myCurrent.next();
      myNextValid = true;
      return true;
    }
    myCurrent = null;
    while (mySource.hasNext()) {
      myCurrent = mySource.next();
      if (myCurrent.hasNext())
        break;
      myCurrent = null;
    }
    if (myCurrent == null)
      return false;
    myNext = myCurrent.next();
    myNextValid = true;
    return true;
  }

  public T next() {
    if (!hasNext())
      throw new NoSuchElementException();
    assert myNextValid;
    myNextValid = false;
    return myNext;
  }

  public void remove() {
    if (myNextValid)
      throw new UnsupportedOperationException("Not implemented yet");
    myCurrent.remove();
  }

  public static <T> Iterator<T> create(Collection<T>... collections) {
    return create(Arrays.asList(collections));
  }

  public static <T> Iterator<T> create(Collection<? extends Collection<T>> collections) {
    Convertor<Collection<T>, Iterator<T>> convertor = new Convertor<Collection<T>, Iterator<T>>() {
      public Iterator<T> convert(Collection<T> t) {
        return t.iterator();
      }
    };
    return create(ConvertingIterator.create(collections, convertor));
  }
}
