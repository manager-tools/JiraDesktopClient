package com.almworks.util.commons;

import com.almworks.util.Pair;
import com.almworks.util.collections.Containers;
import com.almworks.util.collections.Convertor;
import org.almworks.util.Collections15;
import org.almworks.util.Log;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static org.almworks.util.Collections15.arrayList;

/**
 * @author : Dyoma
 */
public abstract class Condition <T> {
  public static final Condition<String> EMPTY_STRING = new Condition<String>() {
    @Override
    public boolean isAccepted(String value) {
      return value == null || value.length() == 0;
    }
  };

  public static final Condition<String> NOT_BLANK_STRING = new Condition<String>() {
    @Override
    public boolean isAccepted(String value) {
      String trimmed = Util.NN(value).trim();
      return !trimmed.isEmpty();
    }
  };

  public abstract boolean isAccepted(T value);

  @SuppressWarnings({"RawUseOfParameterizedType"})
  private static final Condition ALWAYS = new Condition() {
    public boolean isAccepted(Object o) {
      return true;
    }
  };

  public Condition<T> not() {
    return new NotCondition<T>(this);
  }

  public static <T> Condition<T> not(Condition<T> condition) {
    return condition.not();
  }

  public <X extends T> X detect(Iterator<? extends X> it, X ifNotFound) {
    while (it.hasNext()) {
      X t = it.next();
      if (isAccepted(t))
        return t;
    }
    return ifNotFound;
  }

  public <X extends T> X detect(Iterator<? extends X> it) {
    return detect(it, null);
  }

  public List<T> select(List<? extends T> original) {
    if (original == null || original.isEmpty()) return Collections15.emptyList();
    List<T> result = Collections15.arrayList();
    for (T t : original) {
      if (isAccepted(t))
        result.add(t);
    }
    return result;
  }

  public List<T> select(Collection<? extends T> original) {
    List<T> result = Collections15.arrayList();
    for (T t : original) {
      if (isAccepted(t))
        result.add(t);
    }
    return result;
  }

  public Set<T> selectSet(Collection<? extends T> original) {
    Set<T> result = Collections15.hashSet();
    for (T t : original) {
      if (isAccepted(t))
        result.add(t);
    }
    return result;
  }

  public static <T> ClassIsAssignable<T> assignable(Class<T> aClass) {
    return new ClassIsAssignable<T>(aClass);
  }

  public <R> Set<R> selectThenCollectSet(Collection<? extends T> collection, Convertor<? super T, R> convertor) {
    return selectThenCollect(collection, Collections15.<R>hashSet(), convertor);
  }

  private <R, C extends Collection<R>> C selectThenCollect(Collection<? extends T> collection, C result,
    Convertor<? super T, R> convertor) {
    for (T t : collection) {
      if (isAccepted(t))
        result.add(convertor.convert(t));
    }
    return result;
  }

  public <R> List<R> selectThenCollectList(List<? extends T> list, Convertor<? super T, R> convertor) {
    return selectThenCollect(list, Collections15.<R>arrayList(), convertor);
  }

  @NotNull
  public static <T> Condition<T> always() {
    return (Condition<T>) ALWAYS;
  }

  public static <T> Condition<T> and(final Condition<T> condition1, final Condition<T> condition2) {
    return new Condition<T>() {
      public boolean isAccepted(T item) {
        return condition1.isAccepted(item) && condition2.isAccepted(item);
      }
    };
  }

  public static <T> Condition<T> or(final Condition<T> condition1, final Condition<T> condition2) {
    return new Condition<T>() {
      public boolean isAccepted(T item) {
        return condition1.isAccepted(item) || condition2.isAccepted(item);
      }
    };
  }

  public static <T> Condition<T> compositeAnd(final Condition<T>[] conditions) {
    return new Condition<T>() {
      public boolean isAccepted(T item) {
        for (Condition<T> condition : conditions) {
          if (!condition.isAccepted(item))
            return false;
        }
        return true;
      }
    };
  }

  public static <T> Condition<T> compositeOr(final Condition<T>[] conditions) {
    return new Condition<T>() {
      public boolean isAccepted(T item) {
        for (Condition<T> condition : conditions) {
          if (condition.isAccepted(item))
            return true;
        }
        return false;
      }
    };
  }


  public int count(Collection<? extends T> collection) {
    int count = 0;
    for (T element : collection) {
      if (isAccepted(element))
        count++;
    }
    return count;
  }

  public <TT extends T> List<TT> filterList(List<TT> list) {
    if (list == null || list.isEmpty())
      return list;
    List<TT> result = null;
    for (int i = 0; i < list.size(); i++) {
      TT element = list.get(i);
      if (isAccepted(element)) {
        if (result != null) {
          result.add(element);
        }
      } else {
        if (result == null) {
          result = Collections15.arrayList();
          if (i > 0) {
            result.addAll(list.subList(0, i));
          }
        }
      }
    }
    return result == null ? list : result;
  }

  public Iterator<T> filterIterator(Collection<? extends T> collection) {
    return filterIterator(collection.iterator());
  }

  public Iterator<T> filterIterator(final Iterator<? extends T> iterator) {
    return new Iterator<T>() {
      private T myNextElement = null;
      private boolean myNextValid = false;
      private boolean myRemoveAllowed = false;

      public boolean hasNext() {
        if (myNextValid) return true;
        if (!iterator.hasNext()) return false;
        do {
          T t = iterator.next();
          myRemoveAllowed = false;
          if (isAccepted(t)) {
            myNextElement = t;
            myNextValid = true;
            return true;
          }
        } while (iterator.hasNext());
        return false;
      }

      public T next() {
        if (!hasNext()) throw new NoSuchElementException();
        if (myNextValid) {
          myNextValid = false;
          return myNextElement;
        }
        throw new NoSuchElementException();
      }

      public void remove() {
        if (!myRemoveAllowed) throw new UnsupportedOperationException("Wrong state");
        iterator.remove();
      }
    };
  }

  public Set<T> filterSet(Set<T> set) {
    return Containers.collectSet(filterIterator(set));
  }

  public int detectIndex(List<? extends T> elements) {
    for (int i = 0; i < elements.size(); i++) {
      T t = elements.get(i);
      if (isAccepted(t))
        return i;
    }
    return -1;
  }

  public int detectIndex(T[] elements) {
    for (int i = 0; i < elements.length; i++) {
      T element = elements[i];
      if (isAccepted(element))
        return i;
    }
    return -1;
  }

  public <X extends T> X detect(Collection<X> list) {
    return detect(list.iterator());
  }

  public T detectUntyped(Collection<?> collection) {
    return (T)detect((Iterator)collection.iterator());
  }

  public boolean hasAny(Iterable<? extends T> collection) {
    return detect(collection.iterator()) != null;
  }

  public boolean areAll(Collection<? extends T> collection) {
    return !not().hasAny(collection);
  }

  public static <T> Condition<T> isInstance(final Class<? extends T> aClass) {
    return new IsInstanceOf<T>(aClass);
  }

  @Nullable
  public <D extends T, R> T detect(Iterable<? extends D> collection, Convertor<? super D, R> convertor, R value) {
    for (D t : collection) {
      if (isAccepted(t)) {
        R converted = convertor.convert(t);
        if (Util.equals(converted, value))
          return t;
      }
    }
    return null;
  }

  public <K> Map<K, T> selectThenCollectAssignKeys(Collection<T> collection, Convertor<T, K> convertor) {
    Map<K, T> result = Collections15.hashMap();
    for (T t : collection) {
      if (!isAccepted(t))
        continue;
      result.put(convertor.convert(t), t);
    }
    return result;
  }

  public static <T> Condition<T> isNull() {
    return NULL;
  }

  public static <T> Condition<T> never() {
    return not(Condition.<T>always());
  }

  public List<? extends T> maybeFilterList(List<? extends T> list) {
    if (list == null)
      return null;
    List<T> result = null;
    int size = list.size();
    for (int i = 0; i < size; i++) {
      T e = list.get(i);
      if (!isAccepted(e)) {
        if (result == null) {
          result = Collections15.arrayList();
          if (i > 0)
            result.addAll(list.subList(0, i));
        }
      } else {
        if (result != null) {
          result.add(e);
        }
      }
    }
    return result == null ? list : result;
  }

  public Condition<T> removeAllFrom(Collection<T> collection) {
    for (Iterator<T> ii = collection.iterator(); ii.hasNext();) {
      T element = ii.next();
      if (isAccepted(element)) {
        ii.remove();
      }
    }
    return this;
  }

  public <S extends T> Pair<List<S>, List<S>> split(@Nullable Collection<S> src) {
    List<S> sat = arrayList();
    List<S> not = arrayList();
    if (src != null) {
      for (S s : src) {
        if (isAccepted(s)) sat.add(s);
        else not.add(s);
      }
    }
    return Pair.create(sat, not);
  }

  public Function<T, Boolean> fun() {
    return new Function<T, Boolean>() {
      @Override
      public Boolean invoke(T argument) {
        return isAccepted(argument);
      }
    };
  }

  public static <T> Condition<T> cond(final Function<T, Boolean> f) {
    return new Condition<T>() {
      @Override
      public boolean isAccepted(T value) {
        return Boolean.TRUE.equals(f.invoke(value));
      }
    };
  }

  public static class ClassIsAssignable <T> extends Condition<Class> {
    private final Class<T> myClass;

    public ClassIsAssignable(Class<T> aClass) {
      myClass = aClass;
    }

    public boolean isAccepted(Class aClass) {
      return myClass.isAssignableFrom(aClass);
    }

    public Class<T> detect(Iterator<Class> iterator, Class<? extends T> aClass) {
      return super.detect(iterator, aClass);
    }
  }

  @SuppressWarnings({"RawUseOfParameterizedType"})
  private static final Condition NOT_NULL = new Condition() {
    public boolean isAccepted(Object value) {
      return value != null;
    }
  };

  @SuppressWarnings({"RawUseOfParameterizedType"})
  private static final Condition NULL = new Condition<Object>() {
    public boolean isAccepted(Object o) {
      return o == null;
    }
  };

  public static <E> Condition<E> notNull() {
    return NOT_NULL;
  }

  /**
   * Condition&lt;T&gt; -> Condition&lt;V&gt;.
   * Use with caution: V should be castable to T
   */
  public static <T, V> Condition<V> cast(final Condition<T> src, final Class<T> cls) {
    return new Condition<V>() {
      @Override
      public boolean isAccepted(V value) {
        try {
          return src.isAccepted(cls.cast(value));
        } catch (ClassCastException ex) {
          assert false : value;
          Log.warn(ex);
          return false;
        }
      }
    };
  }

  private static class NotCondition <T> extends Condition<T> {
    private final Condition<T> myOriginal;

    public NotCondition(Condition<T> original) {
      myOriginal = original;
    }

    public boolean isAccepted(T t) {
      return !myOriginal.isAccepted(t);
    }

    public Condition<T> not() {
      return myOriginal;
    }
  }

  public static <E> Condition<E> inCollection(final Collection<?> collection) {
    return new Condition<E>() {
      public boolean isAccepted(E e) {
        return collection.contains(e);
      }
    };
  }

  public static <E> Condition<E> inCollection(Object ... variants) {
    return inCollection(Arrays.asList(variants));
  }

  public static <T> Condition<T> isEqual(final T sample) {
    return new Condition<T>() {
      public boolean isAccepted(T t) {
        return Util.equals(t, sample);
      }
    };
  }

  public static <T> Condition<T> refEqual(final T sample) {
    return new Condition<T>() {
      @Override
      public boolean isAccepted(T value) {
        return sample == value;
      }
    };
  }

  private static class IsInstanceOf<T> extends Condition<T> {
    private final Class<? extends T> myClass;

    public IsInstanceOf(Class<? extends T> aClass) {
      myClass = aClass;
    }

    public boolean isAccepted(T value) {
      return myClass.isInstance(value);
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      IsInstanceOf other = Util.castNullable(IsInstanceOf.class, obj);
      return other != null && Util.equals(myClass, other.myClass);
    }

    @Override
    public int hashCode() {
      return Util.hashCode(myClass);
    }
  }
}
