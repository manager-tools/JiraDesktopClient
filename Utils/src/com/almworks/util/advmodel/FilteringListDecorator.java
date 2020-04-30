package com.almworks.util.advmodel;

import com.almworks.util.collections.Convertor;
import com.almworks.util.commons.Condition;
import org.almworks.util.detach.Lifespan;

import java.util.Arrays;
import java.util.Collection;

/**
 * @author : Dyoma
 */
public class FilteringListDecorator <T> extends FilteringConvertingListDecorator<T, T> {
  private FilteringListDecorator(AListModel<T> source) {
    super(source, Convertor.<T>identity());
  }

  public static <T> FilteringListDecorator<T> create(AListModel<T> source) {
    FilteringListDecorator<T> decorator = new FilteringListDecorator<T>(source);
    decorator.resynch();
    return decorator;
  }

  public static <T> FilteringListDecorator<T> create(Lifespan lifespan, AListModel<T> source) {
    FilteringListDecorator<T> decorator = new FilteringListDecorator<T>(source);
    lifespan.add(decorator.getDetach());
    return decorator;
  }

  public static <T> FilteringListDecorator<T> create(Lifespan lifespan, AListModel<T> source, Condition<? super T> condition) {
    FilteringListDecorator<T> decorator = new FilteringListDecorator<T>(source);
    decorator.setFilter(condition);
    lifespan.add(decorator.getDetach());
    return decorator;
  }

  public static <T> FilteringListDecorator<T> createNotStarted(AListModel<T> source) {
    return new FilteringListDecorator<T>(source);
  }

  public static <T> AListModel<T> exclude(Lifespan lifespan, AListModel<T> source, Collection<? extends T> excluded) {
    FilteringListDecorator<T> result = create(lifespan, source);
    result.setFilter(Condition.not(Condition.inCollection(excluded)));
    return result;
  }

  public static <T> AListModel<T> include(Lifespan lifespan, AListModel<T> source, Collection<? extends T> included) {
    FilteringListDecorator<T> result = create(lifespan, source);
    result.setFilter(Condition.inCollection(included));
    return result;
  }

  public static <T> AListModel<T> exclude(Lifespan life, AListModel<T> source, T ... items) {
    return exclude(life, source, Arrays.asList(items));
  }  
}

