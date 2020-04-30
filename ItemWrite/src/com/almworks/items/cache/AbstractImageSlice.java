package com.almworks.items.cache;

import com.almworks.integers.IntArray;
import com.almworks.integers.IntList;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.cache.util.AttributeLoader;
import org.almworks.util.Collections15;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public abstract class AbstractImageSlice implements ImageSlice {
  @Override
  public void addData(DataLoader<?>... loaders) {
    addData(Arrays.asList(loaders));
  }

  @Override
  public void addAttributes(DBAttribute<?>... attributes) {
    addAttributes(Arrays.asList(attributes));
  }

  @Override
  public void addAttributes(Collection<? extends DBAttribute<?>> attributes) {
    List<DataLoader<?>> loaders = Collections15.arrayList();
    for (DBAttribute<?> attribute : attributes) loaders.add(AttributeLoader.create(attribute));
    addData(loaders);
  }

  @Override
  public void removeData(DataLoader<?>... loaders) {
    removeData(Arrays.asList(loaders));
  }

  @Override
  public <T> int findIndexByValue(int fromIndex, DataLoader<? extends T> loader, T value) {
    int count = getActualCount();
    for (int i = fromIndex; i < count; i++) {
      long item = getItem(i);
      T itemValue = getValue(item, loader);
      if (Util.equals(itemValue, value)) return i;
    }
    return -1;
  }

  @Override
  public <S, T> int findIndexByValue(int fromIndex, DataLoader<S> loader1, S value1, DataLoader<T> loader2, T value2) {
    int index = fromIndex;
    while (index >= 0) {
      int index1 = findIndexByValue(index, loader1, value1);
      if (index1 < 0) return -1;
      index = findIndexByValue(index1, loader2, value2);
      if (index == index1) return index;
    }
    return -1;
  }

  @Override
  public <T> long findItemByValue(DataLoader<T> loader, T value) {
    int index = findIndexByValue(0, loader, value);
    return index >= 0 ? getItem(index) : 0;
  }

  @NotNull
  @Override
  public <T> IntList selectIndexesByValue(DataLoader<T> loader, T value) {
    int index = 0;
    IntArray result = new IntArray();
    while ((index = findIndexByValue(index, loader, value)) >= 0) {
      result.add(index);
      index++;
    }
    return result.isEmpty() ? IntList.EMPTY : result;
  }
}
