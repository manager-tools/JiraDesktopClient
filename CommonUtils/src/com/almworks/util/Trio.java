package com.almworks.util;

import org.almworks.util.Util;

/**
 * @author : Dyoma
 */
public abstract class Trio<T1, T2, T3> {
  public abstract T1 getFirst();

  public abstract T2 getSecond();

  public abstract T3 getThird();

  public String toString() {
    return "Trio<" + getFirst() + ", " + getSecond() + ", " + getThird() + ">";
  }

  public int hashCode() {
    T1 first = getFirst();
    T2 second = getSecond();
    T3 third = getThird();
    int result = 0;
    if (first != null)
      result = first.hashCode();
    if (second != null)
      result = result * 29 + second.hashCode();
    if (third != null)
      result = result * 29 + third.hashCode();
    return result;
  }

  public boolean equals(Object obj) {
    if (!(obj instanceof Trio))
      return false;
    Trio that = ((Trio) obj);
    return Util.equals(getFirst(), that.getFirst()) && Util.equals(getSecond(), that.getSecond()) &&
      Util.equals(getThird(), that.getThird());
  }

  public static <T1, T2, T3> Trio<T1, T2, T3> create(final T1 first, final T2 second, final T3 third) {
    return new Trio<T1, T2, T3>() {
      public T1 getFirst() {
        return first;
      }

      public T2 getSecond() {
        return second;
      }

      public T3 getThird() {
        return third;
      }
    };
  }

  public static <T1, T2, T3> Trio.Builder<T1, T2, T3> create() {
    return new Builder<T1, T2, T3>();
  }

  public static class Builder<T1, T2, T3> extends Trio<T1, T2, T3> {
    private T1 myFirst;
    private T2 mySecond;
    private T3 myThird;

    public void setFirst(T1 first) {
      myFirst = first;
    }

    public void setSecond(T2 second) {
      mySecond = second;
    }

    public void setThird(T3 third) {
      myThird = third;
    }

    public T1 getFirst() {
      return myFirst;
    }

    public T2 getSecond() {
      return mySecond;
    }

    public T3 getThird() {
      return myThird;
    }
  }
}
