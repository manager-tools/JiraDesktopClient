package com.almworks.util.collections;

import com.almworks.integers.LongList;
import com.almworks.util.commons.Condition;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public class PrimitiveUtils {
  public static <T> LongList collect(Convertor<? super T, Long> convertor, Collection<T> objects) {
    return selectThenCollect(null, convertor, objects);
  }

  public static <T> LongList selectThenCollect(
    @Nullable Condition<? super T> condition, Convertor<? super T, Long> convertor, Collection<T> objects)
  {
    if (objects == null || objects.isEmpty()) {
      return LongList.EMPTY;
    }

    final com.almworks.integers.LongArray result = new com.almworks.integers.LongArray(objects.size());
    for(final T t : objects) {
      if(condition == null || condition.isAccepted(t)) {
        final Long v = convertor.convert(t);
        if(v != null) {
          result.add(v);
        }
      }
    }

    result.sortUnique();
    return result;
  }
}
