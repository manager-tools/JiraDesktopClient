package com.almworks.util.advmodel;

import com.almworks.util.commons.Condition;
import com.almworks.util.commons.Function;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;

public class ListModelUtils {
  private static final Function<AListModel, Object> GET_FIRST = new Function<AListModel, Object>() {
    public Object invoke(AListModel model) {
      return model.getSize() > 0 ? model.getAt(0) : null;
    }
  };

  public static <T> AListModel<T> addFilterAndSorting(Lifespan life, AListModel<T> model,
    @Nullable Condition<? super T> filter, @Nullable Comparator<? super T> order)
  {
    if (life.isEnded())
      return model;
    if (filter != null) {
      model = FilteringListDecorator.create(life, model, filter);
    }
    if (order != null) {
      model = SortedListDecorator.create(life, model, order);
    }
    return model;
  }

  public static <T> Function<AListModel<? extends T>, T> getFirstOrNull() {
    return (Function<AListModel<? extends T>, T>) (Function)GET_FIRST;
  }
}
