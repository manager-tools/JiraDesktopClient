package com.almworks.util.advmodel;

import com.almworks.util.TODO;
import com.almworks.util.collections.Convertor;
import com.almworks.util.commons.Condition;
import org.almworks.util.detach.Detach;

import java.util.Collection;
import java.util.List;

/**
 * @author dyoma
 */
public class FixedListModel<T> extends AROList<T> {
  private final T[] myElements;
  @SuppressWarnings({"RawUseOfParameterizedType"})
  private static final Convertor CONVERT_LIST = new Convertor<List, AListModel>() {
    @SuppressWarnings({"RawUseOfParameterizedType"})
    public AListModel convert(List list) {
      return create(list);
    }
  };

  private FixedListModel(T[] elements) {
    myElements = elements;
  }

  public Detach addListener(Listener<? super T> listener) {
    return Detach.NOTHING;
  }

  public void removeFirstListener(Condition<Listener> condition) {
  }

  public Detach addRemovedElementListener(RemovedElementsListener<T> listener) {
    return Detach.NOTHING;
  }

  public int getSize() {
    return myElements.length;
  }

  public T getAt(int index) {
    return myElements[index];
  }

  public void forceUpdateAt(int index) {
    throw TODO.shouldNotHappen(String.valueOf(index));
  }

  public static <T> AListModel<T> create(T ... elements) {
    Object[] copy = new Object[elements.length];
    System.arraycopy(elements, 0, copy, 0, elements.length);
    return new FixedListModel(copy);
  }

  @SuppressWarnings("unchecked")
  public static <T> AListModel<T> create(Collection<? extends T> elements) {
    if (elements == null) return EMPTY;
    return new FixedListModel(elements.toArray(new Object[elements.size()]));
  }

  public static <T> Convertor<List<T>, AListModel<T>> convertList() {
    return CONVERT_LIST;
  }
}
