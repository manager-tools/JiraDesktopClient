package com.almworks.util.collections;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Iterator;

/**
 * @author : Dyoma
 */
public class ConvertingIterator <D, R> implements Iterator<R> {
  private final Iterator<? extends D> mySource;
  private final Convertor<D, R> myConvertor;

  public ConvertingIterator(Iterator<? extends D> source, Convertor<D, R> convertor) {
    mySource = source;
    myConvertor = convertor;
  }

  public static <D, R> Iterator<R> create(Iterator<? extends D> source, Convertor<D, R> convertor) {
    return new ConvertingIterator<D, R>(source, convertor);
  }

  public static <D, R> Iterator<R> create(Collection<? extends D> source, Convertor<D, R> convertor) {
    return create(source.iterator(), convertor);
  }

  @Nullable
  public static <D, R> Iterable<R> iterable(
    @Nullable final Iterable<? extends D> source,
    @NotNull final Convertor<D, R> convertor)
  {
    if(source == null) {
      return null;
    }
    
    return new Iterable<R>() {
      public Iterator<R> iterator() {
        assert source != null;
        return create(source.iterator(), convertor);
      }
    };
  }

  public boolean hasNext() {
    return mySource.hasNext();
  }

  public R next() {
    return myConvertor.convert(mySource.next());
  }

  public void remove() {
    mySource.remove();
  }
}
