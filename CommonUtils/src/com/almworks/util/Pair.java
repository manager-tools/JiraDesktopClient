package com.almworks.util;

import com.almworks.util.collections.Convertor;
import org.almworks.util.Util;

import java.util.Comparator;

/**
 * @author : Dyoma
 */
public abstract class Pair <T1, T2> {
  @SuppressWarnings({"RawUseOfParameterizedType"})
  private static final Convertor<Pair, Object> TO_FIRST = new Convertor<Pair, Object>() {
    @SuppressWarnings({"RawUseOfParameterizedType"})
    public Object convert(Pair pair) {
      return pair.getFirst();
    }
  };
  private static final Convertor<Pair,Object> CONVERTOR_GET_FIRST = new Convertor<Pair, Object>() {
    public Object convert(Pair value) {
      return value.getFirst();
    }
  };

  private static final Convertor<Pair, Object> CONVERTOR_GET_SECOND  = new Convertor<Pair, Object>() {
    public Object convert(Pair value) {
      return value.getSecond();
    }
  };

  public abstract T1 getFirst();

  public abstract T2 getSecond();

  public static <T1, T2> Convertor<Pair<T1, ?>, T1> toFirst() {
    //noinspection RedundantCast,RawUseOfParameterizedType
    return (Convertor) TO_FIRST;
  }

  public int hashCode() {
    T1 first = getFirst();
    int code = first == null ? 9010391 : first.hashCode();
    T2 second = getSecond();
    code = code * 23 + (second == null ? 28719051 : second.hashCode());
    return code;
  }

  @SuppressWarnings({"RawUseOfParameterizedType"})
  public boolean equals(Object o) {
    if (!(o instanceof Pair))
      return false;
    Object thatFirst = ((Pair) o).getFirst();
    Object thatSecond = ((Pair) o).getSecond();
    return Util.equals(thatFirst, getFirst()) && Util.equals(thatSecond, getSecond());
  }

  public final Pair<T1, T2> copyWithFirst(T1 first) {
    return create(first, getSecond());
  }

  public final Pair<T1, T2> copyWithSecond(T2 second) {
    return create(getFirst(), second);
  }

  public String toString() {
    return "Pair<" + getFirst() + ", " + getSecond() + ">";
  }

  public static <T1, T2> Pair<T1, T2> create(final T1 first, final T2 second) {
    return new Pair<T1, T2>() {
      public T1 getFirst() {
        return first;
      }

      public T2 getSecond() {
        return second;
      }
    };
  }

  public static <T1, T2> Pair.Builder<T1, T2> create() {
    return new Builder<T1, T2>();
  }

  @SuppressWarnings({"RawUseOfParameterizedType"})
  private static final Pair NULL_NULL = new Pair() {
    @SuppressWarnings({"ConstantConditions"})
    public Object getFirst() {
      return null;
    }

    @SuppressWarnings({"ConstantConditions"})
    public Object getSecond() {
      return null;
    }
  };
  public static <T1, T2> Pair<T1, T2> nullNull() {
    return NULL_NULL;
  }

  public static <A> Convertor<Pair<A, ?>, A> convertorGetFirst() {
    //noinspection unchecked,RedundantCast
    return (Convertor<Pair<A, ?>, A>) (Convertor)CONVERTOR_GET_FIRST;
  }

  public static <B> Convertor<Pair<?, B>, B> convertorGetSecond() {
    //noinspection unchecked,RedundantCast
    return (Convertor<Pair<?, B>, B>) (Convertor)CONVERTOR_GET_SECOND;
  }

  public static <T> Comparator<Pair<T, ?>> compareFirst(final Comparator<T> comparator) {
    return new Comparator<Pair<T, ?>>() {
      public int compare(Pair<T, ?> o1, Pair<T, ?> o2) {
        T f1 = o1 != null ? o1.getFirst() : null;
        T f2 = o2 != null ? o2.getFirst() : null;
        return comparator.compare(f1, f2);
      }
    };
  }

  public static class Builder <T1, T2> extends Pair<T1, T2> {
    private T1 myFirst;
    private T2 mySecond;

    public void setFirst(T1 first) {
      myFirst = first;
    }

    public void setSecond(T2 second) {
      mySecond = second;
    }

    public T1 getFirst() {
      return myFirst;
    }

    public T2 getSecond() {
      return mySecond;
    }
  }
}
