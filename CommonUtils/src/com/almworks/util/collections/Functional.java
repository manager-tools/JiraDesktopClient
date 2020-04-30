package com.almworks.util.collections;

import com.almworks.util.commons.*;
import org.almworks.util.Collections15;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static java.util.Collections.emptyList;
import static org.almworks.util.Collections15.arrayList;
import static org.almworks.util.Collections15.hashSet;

/** For tests, see LazyCollectionTests (FunctionalTests would be misleading...) */
public class Functional {
  private Functional() {}
  
  public static final Function ONE = new Function() {    
    @Override
    public Object invoke(Object argument) {
      return argument;
    }
  }; 

  public static <K, V> Map<K, V> mapValues(Collection<? extends K> keys, Convertor<? super K, ? extends V> function) {
    HashMap<K, V> r = Collections15.hashMap();
    for (K key : keys) {
      V v = function.convert(key);
      r.put(key, v);
    }
    return r;
  }

  public static <K, V> Map<K, V> filterMapValues(Map<K, V> map, Condition<? super V> condition) {
    if (map == null || map.isEmpty())
      return map;
    Map<K, V> r = Collections15.hashMap();
    for (Map.Entry<K, V> e : map.entrySet()) {
      if (condition.isAccepted(e.getValue())) {
        r.put(e.getKey(), e.getValue());
      }
    }
    return r;
  }

  public static <K, V> K selectFirst(Map<K, V> map, Condition<? super V> valueCondition) {
    if (map == null || map.isEmpty())
      return null;
    for (Map.Entry<K, V> e : map.entrySet()) {
      if (valueCondition.isAccepted(e.getValue()))
        return e.getKey();
    }
    return null;
  }

  public static <T> List<T> filterToList(@Nullable Iterable<? extends T> iterable, @Nullable Condition<? super T> condition) {
    if (condition == null) {
      if (iterable instanceof List) {
        return (List<T>)iterable;
      } else {
        return arrayList(iterable);
      }
    }
    if (iterable == null || isEmpty(iterable))
      return emptyList();
    List<T> r = arrayList();
    for (T v : iterable) {
      if (condition.isAccepted(v))
        r.add(v);
    }
    return r;
  }

  public static <T> List<T> filterArray(Condition<? super T> condition, T... elements) {
    return filterToList(arrayList(elements), condition);
  }

  public static final <T> Function<Iterable<? extends T>, List<T>> filterToList(@Nullable final Condition<? super T> condition) {
    return new Function<Iterable<? extends T>, List<T>>() {
      @Override
      public List<T> invoke(Iterable<? extends T> value) {
        return filterToList(value, condition);
      }
    };
  }

  @NotNull
  public static <T> Iterable<T> filter(@Nullable Iterable<? extends T> source, Function<? super T, Boolean> cond) {
    return filter(source, Condition.cond(cond));
  }

  @NotNull
  public static <T> Iterable<T> filter(@Nullable final Iterable<? extends T> source, @Nullable final Condition<? super T> cond) {
    if (source == null) return emptyList();
    if (cond == null) return (Iterable)source;
    return new Iterable<T>() {
      @Override
      public Iterator<T> iterator() {
        return new Iterator<T>() {
          final Iterator<? extends T> i = source.iterator();
          Boolean hasNext = null;
          T next;

          @Override
          public boolean hasNext() {
            if (hasNext == null) {
              hasNext = calcHasNext();
            }
            return hasNext;
          }

          private boolean calcHasNext() {
            while (i.hasNext()) {
              next = i.next();
              if (cond.isAccepted(next)) return true;
            }
            return false;
          }

          @Override
          public T next() {
            if (!hasNext())
              throw new NoSuchElementException();
            hasNext = null;
            return next;
          }

          @Override
          public void remove() {
            throw new UnsupportedOperationException();
          }
        };
      }
    };
  }

  @NotNull
  public static <T> Function<Iterable<T>, Iterable<T>> filterIterable(@Nullable final Condition<T> cond) {
    return new Function<Iterable<T>, Iterable<T>>() {
      @Override
      public Iterable<T> invoke(Iterable<T> argument) {
        return filter(argument, cond);
      }
    };
  }

  /**
   * @return set union of the specified collections or empty set if at least one of the collections is empty or null
   */
  @NotNull
  public static <T> Set<T> unionOrEmptySet(@NotNull Collection<? extends Collection<? extends T>> src) {
    Set<T> result = hashSet();
    for (Collection<? extends T> cur : src) {
      if (cur == null || cur.isEmpty()) {
        return Collections.EMPTY_SET;
      } else {
        result.addAll(cur);
      }
    }
    return result;
  }

  public static <A, B, C> Map<A, C> mapMap(Map<A, ? extends B> map1, Map<? super B, C> map2) {
    HashMap<A, C> map = Collections15.hashMap();
    for (Map.Entry<A, ? extends B> e : map1.entrySet()) {
      map.put(e.getKey(), map2.get(e.getValue()));
    }
    return map;
  }

  /**
   * @return a single lazy Iterable through the elements of many collections extracted from the source by the specified selector
   */
  @NotNull
  public static <S, T> Iterable<T> selectMany(@Nullable final Iterable<? extends S> from, @Nullable final Convertor<S, ? extends Iterable<T>> selector) {
    if (from == null || selector == null) return Collections.EMPTY_LIST;
    return new Iterable<T>() {
      @Override
      public Iterator<T> iterator() {
        return new Iterator<T>() {
          private final Iterator<? extends S> myOutIt = from.iterator();
          @Nullable
          private Iterator<T> myInIt;
          @Nullable
          private Iterator<T> myRemoveIt;

          /**
           * Postcondition: if returns true, myInIt.next() can be called (inItHasNext() == true)
           */
          @Override
          public boolean hasNext() {
            if (inItHasNext()) return true;
            while (myOutIt.hasNext()) {
              advanceOuter();
              if (inItHasNext()) return true;
            }
            return false;
          }

          private boolean inItHasNext() {
            return myInIt != null && myInIt.hasNext();
          }

          private void advanceOuter() {
            Iterable<T> nextCollection = selector.convert(myOutIt.next());
            // Removal will proceed in the collection that we're leaving now
            myRemoveIt = myInIt;
            myInIt = nextCollection == null ? null : nextCollection.iterator();
          }

          @Override
          public T next() {
            if (!hasNext()) throw new IndexOutOfBoundsException();
            assert inItHasNext();
            myRemoveIt = myInIt;
            //noinspection ConstantConditions
            return myInIt.next();
          }

          @Override
          public void remove() {
            if (myRemoveIt == null) throw new IllegalStateException();
            myRemoveIt.remove();
          }
        };
      }
    };
  }

  @NotNull
  public static <D, R> Collection<R> convertCollection(@Nullable final Collection<? extends D> source, @Nullable final Convertor<D, R> convertor) {
    if (source == null || convertor == null) return Collections.EMPTY_LIST;
    final Iterable<R> converted = convert((Iterable<D>) source, convertor);
    return new AbstractCollection<R>() {
      @Override
      public Iterator<R> iterator() {
        return converted.iterator();
      }

      @Override
      public int size() {
        return source.size();
      }
    };
  }

  public static <D, R> Function<Collection<D>, Collection<R>> convertCollection(@Nullable final Convertor<D, R> convertor) {
    return new Function<Collection<D>, Collection<R>>() {
      @Override
      public Collection<R> invoke(Collection<D> argument) {
        return convertCollection(argument, convertor);
      }
    };
  }

  @NotNull
  public static <D, R> List<R> convertList(@Nullable final List<? extends D> source, @Nullable final Convertor<D, R> convertor) {
    if (source == null || convertor == null) return Collections.EMPTY_LIST;
    return new AbstractList<R>() {
      @Override
      public R get(int index) {
        return convertor.convert(source.get(index));
      }

      @Override
      public int size() {
        return source.size();
      }
    };
  }

  public static <D, R> Function<List<D>, List<R>> convertList(@Nullable final Convertor<D, R> convertor) {
    return new Function<List<D>, List<R>>() {
      @Override
      public List<R> invoke(List<D> argument) {
        return convertList(argument, convertor);
      }
    };
  }

  @NotNull
  public static <D, R> Iterable<R> convert(@Nullable final Iterable<? extends D> source, @Nullable final Function<D, R> convertor) {
    return convert(source, Convertor.conv(convertor));
  }

  @NotNull
  public static <D, R> Iterable<R> convert(@Nullable final Iterable<? extends D> source, @Nullable final Convertor<D, R> convertor) {
    if (source == null || convertor == null) return Collections.EMPTY_LIST;
    return new Iterable<R>() {
      @Override
      public Iterator<R> iterator() {
        return new Iterator<R>() {
          Iterator<? extends D> it = source.iterator();
          @Override
          public boolean hasNext() {
            return it.hasNext();
          }

          @Override
          public R next() {
            return convertor.convert(it.next());
          }

          @Override
          public void remove() {
            throw new UnsupportedOperationException();
          }
        };
      }
    };
  }

  @NotNull
  public static <D, R> Function<Iterable<D>, Iterable<R>> convertIterable(@Nullable final Convertor<D, R> convertor) {
    return new Function<Iterable<D>, Iterable<R>>() {
      @Override
      public Iterable<R> invoke(Iterable<D> argument) {
        return convert(argument, convertor);
      }
    };
  }

  public static <E> Collection<E> repeat(final E element, final int size) {
    return new AbstractCollection<E>() {
      @Override
      public Iterator<E> iterator() {
        return new Iterator<E>() {
          int i = 0;
          @Override
          public boolean hasNext() {
            return i < size;
          }

          @Override
          public E next() {
            if (!hasNext()) throw new NoSuchElementException();
            i += 1;
            return element;
          }

          @Override
          public void remove() {
            throw new UnsupportedOperationException();
          }
        };
      }

      @Override
      public int size() {
        return size;
      }
    };
  }

  /**
   * @return a never-ending iterable that first returns all specified values, when they are over, returns the last value. Does not support remove.<br>
   * Returns iterable consisting of <tt>null</tt>s if values is empty or null
   * */
  public static <T> Iterable<T> infiniteSaturated(@Nullable final T... values) {
    if (values == null || values.length == 0) return infiniteRepeat(null);
    return new Iterable<T>() {
      @Override
      public Iterator<T> iterator() {
        return new Iterator<T>() {
          int pos = -1;
          @Override
          public boolean hasNext() {
            return true;
          }

          @Override
          public T next() {
            return values[pos = Math.min(pos + 1, values.length - 1)];
          }

          @Override
          public void remove() {
            throw new UnsupportedOperationException();
          }
        };
      }
    };
  }

  public static <T> Iterable<T> infiniteRepeat(final T sample) {
    return new Iterable<T>() {
      @Override
      public Iterator<T> iterator() {
        return new Iterator<T>() {
          @Override
          public boolean hasNext() {
            return true;
          }

          @Override
          public T next() {
            return sample;
          }

          @Override
          public void remove() {
            throw new UnsupportedOperationException();
          }
        };
      }
    };
  }

  public static <K, V> void map(Map<K, V> map, Procedure2<K, V> f) {
    for (Map.Entry<K, V> e : map.entrySet()) {
      f.invoke(e.getKey(), e.getValue());
    }
  }

  /** @return either first element of {@code ts} or null if {@code ts} is null or empty. */
  @Nullable
  public static <T> T first(@Nullable Iterable<T> ts) {
    if (ts == null) return null;
    Iterator<T> it = ts.iterator();
    return it.hasNext() ? it.next() : null;
  }

  @Nullable
  public static <T> T next(@Nullable Iterator<T> it) {
    if (it == null) return null;
    return it.hasNext() ? it.next() : null;
  }

  /**
   * @param n starting from 0 (first element)
   * */
  public static <T> boolean hasNth(@Nullable Iterable<T> ts, int n) {
    if (n < 0 || ts == null) return false;
    Iterator<T> it = ts.iterator();
    for (int i = 0; i < n && it.hasNext(); ++i) {
      it.next();
    }
    return it.hasNext();
  }

  public static boolean isEmpty(@Nullable Iterable<?> ts) {
    return ts == null ? true : !ts.iterator().hasNext();
  }

  /** Concatenates iterables. */
  @NotNull
  public static <T> Iterable<T> cat(@Nullable Iterable<? extends T>... iterables) {
    return iterables == null
      ? Collections.emptyList()
      : selectMany((List) Arrays.asList(iterables), Convertor.<Iterable<T>>identity());
  }

  public static <T0, T1, T2> Function<T0, Function<T1, T2>> curry(final Function2<T0, T1, T2> f) {
    return a0 -> (Function<T1, T2>) a1 -> f.invoke(a0, a1);
  }

  public static <T0, T1> Function<T0, Procedure<T1>> curry(final Procedure2<T0, T1> p) {
    return new Function<T0, Procedure<T1>>() {
      @Override
      public Procedure<T1> invoke(final T0 arg1) {
        return new Procedure<T1>() {
          @Override
          public void invoke(T1 arg2) {
            p.invoke(arg1, arg2);
          }
        };
      }
    };
  }

  public static <T0, T1, T2> Function2<T0, T1, T2> uncurry(final Function<T0, Function<T1, T2>> f) {
    return new Function2<T0, T1, T2>() {
      @Override
      public T2 invoke(T0 t0, T1 t1) {
        return f.invoke(t0).invoke(t1);
      }
    };
  }

  public static <T0, T1, T2> Function2<T1, T0, T2> flip(final Function2<T0, T1, T2> f) {
    return new Function2<T1, T0, T2>() {
      @Override
      public T2 invoke(T1 t1, T0 t0) {
        return f.invoke(t0, t1);
      }
    };
  }

  public static <T0, T1> Procedure2<T1, T0> flip(final Procedure2<T0, T1> p) {
    return new Procedure2<T1, T0>() {
      @Override
      public void invoke(T1 t1, T0 t0) {
        p.invoke(t0, t1);
      }
    };
  }

  public static <T0, T1, T2> Function<T1, Function<T0, T2>> flip(final Function<T0, Function<T1, T2>> f) {
    return new Function<T1, Function<T0, T2>>() {
      @Override
      public Function<T0, T2> invoke(final T1 t1) {
        return new Function<T0, T2>() {
          @Override
          public T2 invoke(T0 t0) {
            return f.invoke(t0).invoke(t1);
          }
        };
      }
    };
  }

  public static <T0, T1, T2> Function<T1, T2> apply2(final T0 arg, final Function<T0, Function<T1, T2>> f) {
    return new Function<T1, T2>() {
      @Override
      public T2 invoke(T1 t1) {
        return f.invoke(arg).invoke(t1);
      }
    };
  }

  public static <T0, T1> Procedure<T1> apply(final T0 arg0, final Function<T0, Procedure<T1>> p) {
    return new Procedure<T1>() {
      @Override
      public void invoke(T1 arg1) {
        p.invoke(arg0).invoke(arg1);
      }
    };
  }

  public static <T> Runnable apply(final T arg, final Procedure<T> p) {
    return new Runnable() {
      @Override
      public void run() {
        p.invoke(arg);
      }
    };
  }

  public static <T0, T1, T2> Function<T0, T2> compose(final Function<T1, T2> f2, final Function<T0, ? extends T1> f1) {
    return new Function<T0, T2>() {
      @Override
      public T2 invoke(T0 argument) {
        return f2.invoke(f1.invoke(argument));
      }
    };
  }
  
  public static <T0, T1> Function<T0, T1> I() {
    return ONE;
  }

  public static <T0, T1> Function<Function<T0, T1>, Function<List<T0>, List<T1>>> map() {
    return new Function<Function<T0, T1>, Function<List<T0>, List<T1>>>() {
      @Override
      public Function<List<T0>, List<T1>> invoke(final Function<T0, T1> f) {
        return new Function<List<T0>, List<T1>>() {
          @Override
          public List<T1> invoke(List<T0> argument) {
            return convertList(argument, Convertor.conv(f));
          }
        };
      }
    };
  }

  public static <E, R> R foldl(@Nullable Iterable<E> elements, R init, Function2<R, E, R> f) {
    R res = init;
    if (elements != null) {
      for (E e : elements) {
        res = f.invoke(res, e);
      }
    }
    return res;
  }

  public static <T0, T1, T2> Function2<T0, T1, T2> ignore2ndArg(final Function<T0, T2> src) {
    return new Function2<T0, T1, T2>() {
      @Override
      public T2 invoke(T0 arg1, T1 arg2) {
        return src.invoke(arg1);
      }
    };
  }

  public static <T1, T2> Procedure2<T1, T2> ignore2ndArg(final Procedure<T1> src) {
    return new Procedure2<T1, T2>() {
      @Override
      public void invoke(T1 arg1, T2 arg2) {
        src.invoke(arg1);
      }
    };
  }

  public static <A, R> Function<A, R> reduceWithLast(final A init, final Function2<A, A, R> f) {
    return new Function<A, R>() {
      A prev = init;
      @Override
      public R invoke(A argument) {
        R ret = f.invoke(prev, argument);
        prev = argument;
        return ret;
      }
    };
  }
  
  @NotNull
  public static <T> Procedure<T> ignore(@NotNull final Runnable r) {
    if (r == null) throw new NullPointerException();
    return new Procedure<T>() {
      @Override
      public void invoke(T arg) {
        r.run();
      }
    };
  }

  public static <T, C extends Collection<T>> Function2<C, T, C> add() {
    return new Function2<C, T, C>() {
      @Override
      public C invoke(C ts, T t) {
        ts.add(t);
        return ts;
      }
    };
  }

  public static <B, D extends B, R> Function<D, R> cov(Function<B, R> f) {
    return (Function)f;
  }
}
