package com.almworks.util.commons;

/**
 * @author dyoma
 */
public interface Function2<A, B, R> extends Function2E<A, B, R, Exception> {
  R invoke(A a, B b);

  class Bind1<A, R> implements Function2<A, Object, R> {
    private final Function<A, R> myFunction;

    public Bind1(Function<A, R> function) {
      myFunction = function;
    }

    @Override
    public R invoke(A a, Object o) {
      return myFunction.invoke(a);
    }
  }
}
