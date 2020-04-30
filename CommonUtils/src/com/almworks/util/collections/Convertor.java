package com.almworks.util.collections;

import com.almworks.util.commons.Condition;
import com.almworks.util.commons.Function;
import org.almworks.util.Collections15;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Array;
import java.util.*;

/**
 * @author : Dyoma
 */
public abstract class Convertor<D, R> {
  public abstract R convert(D value);

  public final Set<R> collectSet(Collection<? extends D> elements) {
    return collectSet(elements, Collections15.<R>linkedHashSet());
  }

  public final Set<R> collectSet(Collection<? extends D> elements, Set<R> target) {
    if (elements != null && !elements.isEmpty()) {
      for (D element : elements) target.add(convert(element));
    }
    return target;
  }

  public final Collection<R> collectOrderedSet(Collection<? extends D> elements) {
    return collectSet(elements, Collections15.<R>linkedHashSet());
  }

  public final Set<R> collectSet(D[] elements) {
    if (elements == null)
      return Collections15.emptySet();
    Set<R> result = Collections15.linkedHashSet();
    for (D element : elements)
      result.add(convert(element));
    return result;
  }

  public <T extends D> T detectEqual(Iterable<T> collection, R value) {
    for (T element : collection)
      if (isEqual(element, value))
        return element;
    return null;
  }

  public <T extends D> boolean isEqual(T element, R value) {
    return Util.equals(value, convert(element));
  }

  public List<R> collectList(Collection<? extends D> collection) {
    List<R> result = Collections15.arrayList(collection.size());
    for (D d : collection)
      result.add(convert(d));
    return result;
  }

  public Collection<R> lazyCollection(Collection<? extends D> source) {
    return Functional.convertCollection(source, this);
  }

  public List<R> lazyList(List<? extends D> source) {
    return Functional.convertList(source, this);
  }

  public static <D, R> Convertor<D, R> conv(final Function<D, R> f) {
    return new Convertor<D, R>() {
      @Override
      public R convert(D value) {
        return f.invoke(value);
      }
    };
  }

  public List<R> collectListWithoutNulls(Collection<? extends D> list) {
    List<R> result = Collections15.arrayList(list.size());
    for (D d : list) {
      R r = convert(d);
      if (r != null)
        result.add(r);
    }
    return result;
  }

  public <DD extends D> Map<R, DD> assignKeys(Collection<DD> collection) {
    HashMap<R, DD> result = Collections15.hashMap();
    for (DD d : collection)
      result.put(convert(d), d);
    return result;
  }

  public R[] collectArray(D[] array, Class<R> clazz) {
    //noinspection unchecked
    R[] result = (R[]) Array.newInstance(clazz, array.length);
    for (int i = 0; i < array.length; i++) {
      D obj = array[i];
      result[i] = convert(obj);
    }
    return result;
  }

  public R[] collectArray(Collection<? extends D> collection, Class<R> clazz) {
    //noinspection unchecked
    R[] result = (R[]) Array.newInstance(clazz, collection.size());
    int i = 0;
    for (D obj : collection) {
      result[i] = convert(obj);
      i++;
    }
    return result;
  }

  public Condition<D> equalsTo(final R rangeValue) {
    return new Condition<D>() {
      public boolean isAccepted(D domainValue) {
        return Util.equals(convert(domainValue), rangeValue);
      }
    };
  }

  public <T> Convertor<D, T> composition(final Convertor<? super R, ? extends T> other) {
    return new Convertor<D, T>() {
      public T convert(D value) {
        return other.convert(Convertor.this.convert(value));
      }
    };
  }

  @NotNull
  public static <T> Convertor<T, Boolean> equality(@Nullable final T sample) {
    return new Convertor<T, Boolean>() {
      public Boolean convert(T value) {
        return Util.equals(value, sample);
      }
    };
  }

  public Equality<D> convertEquality(final Equality<? super R> eq) {
    if (eq == null) throw new NullPointerException();
    return new Equality<D>() {
      @Override
      public boolean areEqual(D o1, D o2) {
        return eq.areEqual(convert(o1), convert(o2));
      }
    };
  }

  @NotNull
  public Comparator<D> comparator(@NotNull final Comparator<R> rangeComparator) {
    return new Comparator<D>() {
      public int compare(D o1, D o2) {
        return rangeComparator.compare(convert(o1), convert(o2));
      }
    };
  }

  public ReadAccessor<D, R> toReadAccessor() {
    return new ToReadAccessor<D, R>(this);
  }

  public Condition<D> elementOf(final Collection<R> set) {
    assert set != null;
    return new Condition<D>() {
      public boolean isAccepted(D value) {
        return set.contains(convert(value));
      }
    };
  }

  public static <D, R extends D> Convertor<D, R> downCastOrNull(final Class<R> childClass) {
    return new Convertor<D, R>() {
      @Override
      public R convert(D value) {
        return childClass.isInstance(value) ? (R)value : null;
      }
    };
  }

  public Function<D, R> fun() {
    return new Function<D, R>() {
      @Override
      public R invoke(D argument) {
        return convert(argument);
      }
    };
  }

  public static <T> Convertor<T, T> identity() {
    return Identity.INSTANCE;
  }

  public static class Identity<E> extends Convertor<E, E> {
    private static final Identity INSTANCE = new Identity();

    public E convert(E t) {
      return t;
    }

    public static <T> Identity<T> create() {
      return INSTANCE;
    }
  }


  private static class ToReadAccessor<D, R> implements ReadAccessor<D, R> {
    private final Convertor<D, R> myConvertor;

    private ToReadAccessor(Convertor<D, R> convertor) {
      myConvertor = convertor;
    }

    public R getValue(D object) {
      return myConvertor.convert(object);
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      ToReadAccessor other = Util.castNullable(ToReadAccessor.class, obj);
      return other != null && Util.equals(myConvertor, other.myConvertor);
    }

    @Override
    public int hashCode() {
      return Util.hashCode(myConvertor) ^ ToReadAccessor.class.hashCode();
    }
  }
}
