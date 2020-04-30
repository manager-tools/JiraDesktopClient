package com.almworks.util.collections;

import org.almworks.util.Collections15;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public class StackIterator<T> implements Iterator<T> {
  private final List<Iterator<? extends T>> myStack = Collections15.arrayList();

  public static <T> StackIterator<T> create(Iterator<? extends T> iterator) {
    StackIterator<T> it = new StackIterator<T>();
    it.push(iterator);
    return it;
  }

  public void push(Iterator<? extends T> iterator) {
    myStack.add(iterator);
  }

  @Override
  public boolean hasNext() {
    for (int i = myStack.size() - 1; i >= 0; i--) {
      Iterator<? extends T> it = myStack.get(i);
      if (it.hasNext()) return true;
    }
    return false;
  }

  @Override
  public T next() {
    while (!myStack.isEmpty()) {
      int last = myStack.size() - 1;
      Iterator<? extends T> it = myStack.get(last);
      if (it.hasNext()) return it.next();
      myStack.remove(last);
    }
    throw new NoSuchElementException();
  }

  @Override
  public void remove() {
    if (myStack.isEmpty()) throw new NoSuchElementException();
    myStack.get(myStack.size() - 1).remove();
  }
}
