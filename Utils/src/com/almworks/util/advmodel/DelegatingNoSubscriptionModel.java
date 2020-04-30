package com.almworks.util.advmodel;

import com.almworks.util.collections.ChangeListener;
import com.almworks.util.collections.Equality;
import com.almworks.util.commons.Condition;
import com.almworks.util.exec.ThreadGate;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.DetachComposite;
import org.almworks.util.detach.Lifespan;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * @author dyoma
 */
public abstract class DelegatingNoSubscriptionModel<T> implements AListModel<T> {
  protected abstract AListModel<T> getDelegate();

  public Iterator<T> iterator() {
    return getDelegate().iterator();
  }

  public int getSize() {
    return getDelegate().getSize();
  }

  public T getAt(int index) {
    return getDelegate().getAt(index);
  }

  public List<T> getAt(int[] indices) {
    return getDelegate().getAt(indices);
  }

  public List<T> subList(int fromIndex, int toIndex) {
    return getDelegate().subList(fromIndex, toIndex);
  }

  public int detectIndex(Condition<? super T> condition) {
    return getDelegate().detectIndex(condition);
  }

  public T detect(Condition<? super T> condition) {
    return getDelegate().detect(condition);
  }

  public List<T> toList() {
    return getDelegate().toList();
  }

  public int indexOf(Object element) {
    return getDelegate().indexOf(element);
  }

  public int indexOf(T element, Equality<? super T> equality) {
    return getDelegate().indexOf(element, equality);
  }

  public int indexOf(Object element, int from, int to) {
    return getDelegate().indexOf(element, from, to);
  }

  public int indexOf(T element, int from, int to, Equality<? super T> equality) {
    return getDelegate().indexOf(element, from, to, equality);
  }

  public boolean containsAny(Collection<? extends T> elements) {
    return getDelegate().containsAny(elements);
  }

  public boolean contains(T element) {
    return getDelegate().contains(element);
  }

  public int[] indeciesOf(Collection<?> elements) {
    return getDelegate().indeciesOf(elements);
  }

  public void forceUpdateAt(int index) {
    getDelegate().forceUpdateAt(index);
  }

  public final void addChangeListener(Lifespan life, ChangeListener listener) {
    addChangeListener(life, ThreadGate.STRAIGHT, listener);
  }

  public void addChangeListener(Lifespan life, ThreadGate gate, ChangeListener listener) {
    AROList.doAddChangeListener(life, listener, this, gate);
  }

  @Deprecated
  public final Detach addAWTChangeListener(ChangeListener listener) {
    DetachComposite life = new DetachComposite();
    addChangeListener(life, ThreadGate.AWT, listener);
    return life;
  }

  public final void addAWTChangeListener(Lifespan life, ChangeListener listener) {
    addChangeListener(life, ThreadGate.AWT, listener);
  }

  public Detach addListStructureListener(ChangeListener listener) {
    return AROList.addListStructureListener(listener, this);
  }
}
