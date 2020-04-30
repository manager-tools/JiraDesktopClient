package com.almworks.util.collections;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * A {@code FilteringIterator} is an {@code Iterator} based on
 * some source {@code Iterator} and a filter. It represents a
 * sequence of those elements from the base sequence that
 * satisfy the filter. A filter is a {@link Convertor} to
 * {@link Boolean}.
 * @param <R> Element type.
 * @author Pavel Zvyagin
 */
public class FilteringIterator<R> implements Iterator<R> {
  /**
   * Factory method taking a source iterator and a filter.
   * @param source The source {@link Iterator}.
   * @param filter The {@link Convertor} instance used as a filter.
   * @param <R> Element type.
   * @return A filtering iterator based on {@code source} and {@code filter}.
   */
  public static <R> FilteringIterator<R> create(Iterator<R> source, Convertor<? super R, Boolean> filter) {
    return new FilteringIterator<R>(source, filter);
  }

  /**
   * Factory method taking a source iterable and a filter.
   * @param source The source {@link Iterable}.
   * @param filter The {@link Convertor} instance used as a filter.
   * @param <R> Element type.
   * @return A filtering iterator based on {@code source.iterator()} and {@code filter}.
   */
  public static <R> FilteringIterator<R> create(Iterable<R> source, Convertor<? super R, Boolean> filter) {
    return new FilteringIterator<R>(source.iterator(), filter);
  }

  /**
   * Factory method for filtering {@link Iterable}s.
   * @param source The source {@link Iterable}.
   * @param filter The {@link Convertor} instance used as a filter.
   * @param <R> Element type.
   * @return An {@link Iterable} that creates filtering iterators
   * using {@code source.iterator()} and {@code filter}.
   */
  @Nullable
  public static <R> Iterable<R> iterable(
    @Nullable final Iterable<R> source,
    @NotNull final Convertor<? super R, Boolean> filter)
  {
    if(source == null) {
      return null;
    }
    
    return new Iterable<R>() {
      public Iterator<R> iterator() {
        assert source != null;
        return create(source.iterator(), filter);
      }
    };
  }

  /** The source iterator. */
  private final Iterator<R> mySource;

  /** The convertor to Boolean that's used as a filter. */
  private final Convertor<? super R, Boolean> myFilter;

  /** Whether there is a satisfying next element. */
  private boolean myHasNext = false;

  /** Whether the iterator has run out of elements. */
  private boolean myFinished = false;

  /** The next element. */
  private R myNext;

  /**
   * The constructor.
   * @param source
   * @param filter
   */
  public FilteringIterator(Iterator<R> source, Convertor<? super R, Boolean> filter) {
    mySource = source;
    myFilter = filter;
  }

  /**
   * Advance the iterator, i.e. find out the next element,
   * if there's none. Pulls elements form the source iterator
   * until either a satisfying one is found or the source
   * runs out of elements.
   * Sets {@link #myHasNext}, {@link #myFinished}, and
   * {@link #myNext} fields as appropriate.
   */
  private void advanceAsNeeded() {
    if(myHasNext) {
      return;
    }

    while(!myFinished && mySource.hasNext()) {
      final R next = mySource.next();
      if(myFilter.convert(next)) {
        myHasNext = true;
        myNext = next;
        return;
      }
    }

    myFinished = true;
  }

  public boolean hasNext() {
    advanceAsNeeded();
    return myHasNext && !myFinished;
  }

  public R next() {
    advanceAsNeeded();

    if(myFinished) {
      throw new NoSuchElementException();
    }

    assert myHasNext;
    final R next = myNext;
    myNext = null;
    myHasNext = false;
    return next;
  }

  public void remove() {
    mySource.remove();
  }
}
