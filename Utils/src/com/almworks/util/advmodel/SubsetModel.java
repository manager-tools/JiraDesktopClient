package com.almworks.util.advmodel;

import com.almworks.util.commons.Condition;
import org.almworks.util.Collections15;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Terms:<br/>
 * Full Set - that was passed in constructor.<br/>
 * Sub Set - a selection from Full Set.<br/>
 * Complement = Full Set - Sub Set
 *
 * @author : Dyoma
 */
public abstract class SubsetModel <T> extends AROList<T> {
  private static final SubsetModel<?> EMPTY_SUBSET_MODEL = new Empty<Object>();

  public static <T> SubsetModel<T> create(Lifespan lifespan, AListModel<? extends T> fullSet, final boolean byDefaultInSubset) {
    return new DefaultSubsetModel<T>(lifespan, fullSet, byDefaultInSubset);
  }

  public static <T> SubsetModel<T> empty() {
    return (SubsetModel<T>) EMPTY_SUBSET_MODEL;
  }

  public abstract void setFull();

  public abstract AListModel<T> getComplementSet();

  public abstract AListModel<T> getFullSet();

  public abstract void addFromComplementSet(List<T> items);

  public abstract void insertFromComplementSet(int index, T item);

  public abstract void addFromFullSet(int... indices);

  public abstract void removeAllAt(int... indices);

  public abstract void removeAll(@NotNull List<T> items);

  public abstract void removeAll(@NotNull Condition<? super T> which);

  public abstract void setSubsetIndices(int... originalIndices);

  public abstract void setSubset(Collection<T> items);

  public abstract void add(T item);

  public abstract void swap(int index1, int index2);  

  public Detach addListener(Listener<? super T> listener) {
    return getImageModel().addListener(listener);
  }

  protected abstract AListModel<T> getImageModel();

  public void removeFirstListener(Condition<Listener> condition) {
    getImageModel().removeFirstListener(condition);
  }

  public Detach addRemovedElementListener(RemovedElementsListener<T> listener) {
    return getImageModel().addRemovedElementListener(listener);
  }

  public int getSize() {
    return getImageModel().getSize();
  }

  public T getAt(int index) {
    return getImageModel().getAt(index);
  }

  public List<T> getUnmodifiableImage() {
    return Collections15.unmodifiableListCopy(getImageModel().toList());
  }

  public void forceUpdateAt(int index) {
    getImageModel().forceUpdateAt(index);
  }

  private static class Empty<T> extends SubsetModel<T> {
    protected AListModel<T> getImageModel() {
      return AListModel.EMPTY;
    }

    public void setFull() {
    }

    public AListModel<T> getComplementSet() {
      return AListModel.EMPTY;
    }

    public AListModel<T> getFullSet() {
      return AListModel.EMPTY;
    }

    public void addFromComplementSet(List<T> items) {
      assert items.isEmpty() : items;
    }

    public void insertFromComplementSet(int index, T item) {
      assert false : index + " " + item;
    }

    public void addFromFullSet(int... indices) {
      //noinspection PrimitiveArrayArgumentToVariableArgMethod
      assert indices.length == 0 : Arrays.asList(indices);
    }

    public void removeAllAt(int... indices) {
      //noinspection PrimitiveArrayArgumentToVariableArgMethod
      assert indices.length == 0 : Arrays.asList(indices);
    }

    @Override
    public void removeAll(List<T> items) {
      //noinspection PrimitiveArrayArgumentToVariableArgMethod
      assert items.isEmpty() : items;
    }

    @Override
    public void removeAll(@NotNull Condition<? super T> which) {
      assert false : which;
    }

    public void setSubsetIndices(int... originalIndices) {
      //noinspection PrimitiveArrayArgumentToVariableArgMethod
      assert originalIndices.length == 0 : Arrays.asList(originalIndices);
    }

    public void setSubset(Collection<T> items) {
      assert items.isEmpty() : items;
    }

    public void add(T item) {
      assert false : item;
    }

    public void swap(int index1, int index2) {
      assert false : index1 + " " + index2;
    }
  }
}
