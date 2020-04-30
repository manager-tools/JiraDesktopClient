package com.almworks.util.advmodel;

import com.almworks.util.collections.Convertor;
import com.almworks.util.commons.Condition;
import com.almworks.util.threads.ThreadAWT;
import com.almworks.util.threads.Threads;
import org.almworks.util.Util;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.Nullable;

public class FilteringConvertingListDecorator<T, D> extends ImageBasedDecorator<T, D> {
  private final Convertor<? super T, ? extends D> myConvertor;

  @Nullable
  private Condition<? super T> myFilter;

  public FilteringConvertingListDecorator(AListModel<? extends T> source, Convertor<? super T, ? extends D> convertor) {
    super(source);
    myConvertor = convertor;
  }

  public boolean isAccepted(int sourceIndex) {
    Condition<? super T> filter = myFilter;
    return filter == null || filter.isAccepted(getSource().getAt(sourceIndex));
  }

  protected D createImage(T sourceItem) {
    return myConvertor.convert(sourceItem);
  }

  @ThreadAWT
  public void setFilter(@Nullable Condition<? super T> filter) {
    AListModel<? extends T> source = getSource();
    if (source != null && source.getSize() > 0) {
      // if source is zero-sized, then this setFilter is probably called in initialization code and we don't care
      // about threads
      Threads.assertAWTThread();
    }
//    if (filter == null)
//      filter = Condition.always();
    if (!Util.equals(filter, myFilter)) {
      myFilter = filter;
    }
    resynch();
  }

  @Nullable
  public Condition<? super T> getFilter() {
    return myFilter;
  }

  public static <T, D> FilteringConvertingListDecorator<T, D> create(Lifespan lifespan, AListModel<? extends T> source,
    @Nullable Condition<? super T> filter, Convertor<? super T, ? extends D> convertor)
  {
    FilteringConvertingListDecorator<T, D> result = new FilteringConvertingListDecorator<T, D>(source, convertor);
    result.setFilter(filter);
    lifespan.add(result.getDetach());
    return result;
  }
}
