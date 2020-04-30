package com.almworks.util.components;

import org.almworks.util.Collections15;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

public abstract class AbstractATreeWritableNode<T> implements ATreeNode<T> {
  @Nullable
  public ATreeNode<T> getChildOrNull(int childIndex) {
    if (childIndex < 0)
      return null;
    if (childIndex >= getChildCount())
      return null;
    return getChildAt(childIndex);
  }

  @Nullable
  public ATreeNode<T> getNextSibling() {
    ATreeNode<T> parent = getParent();
    if (parent == null)
      return null;
    return parent.getChildOrNull(parent.getIndex(this) + 1);
  }

  @Nullable
  public ATreeNode<T> getPrevSibling() {
    ATreeNode<T> parent = getParent();
    if (parent == null)
      return null;
    return parent.getChildOrNull(parent.getIndex(this) - 1);
  }

  public void add(ATreeNode<T> child) {
    insert(child, getChildCount());
  }

  public int removeFromParent() {
    ATreeNode<T> parent = getParent();
    if (parent == null) {
      assert false;
      return -1;
    }
    int index = parent.getIndex(this);
    parent.remove(index);
    return index;
  }

  public List<T> childObjectsToList() {
    List<T> result = Collections15.arrayList(getChildCount());
    for (int i = 0; i < getChildCount(); i++)
      result.add(getChildAt(i).getUserObject());
    return result;
  }

  @Override
  public Iterator<T> childObjectsIterator() {
    return new Iterator<T>() {
      private final int mySize = getChildCount();
      private int myNextPos = 0;

      @Override
      public boolean hasNext() {
        return myNextPos < mySize;
      }

      @Override
      public T next() {
        T result = getChildAt(myNextPos).getUserObject();
        myNextPos++;
        return result;
      }

      @Override
      public void remove() {
        throw new UnsupportedOperationException();
      }
    };
  }

  public void addAllOrdered(Collection<ATreeNode<T>> children, Comparator<? super T> comparator) {
    for (ATreeNode<T> child : children) {
      addOrdered(child, comparator);
    }
  }

  public void addOrdered(ATreeNode<T> child, @Nullable Comparator<? super T> comparator) {
    if (comparator == null) {
      add(child);
      return;
    }
    T userObject = child.getUserObject();
    for (int i = 0; i < getChildCount(); i++) {
      if (comparator.compare(userObject, getChildAt(i).getUserObject()) <= 0) {
        insert(child, i);
        return;
      }
    }
    insert(child, getChildCount());
  }

  public List<ATreeNode<T>> removeAll() {
    List<ATreeNode<T>> result = Collections15.arrayList();
    while (getChildCount() > 0)
      result.add(remove(0));
    return result;
  }

  @NotNull
  public ATreeNode<T> getRoot() {
    ATreeNode<T> node = this;
    while (true) {
      ATreeNode<T> parent = node.getParent();
      if (parent == null)
        return node;
      node = parent;
    }
  }
}
