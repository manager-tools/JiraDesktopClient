package com.almworks.util.commons;

import com.almworks.util.collections.Convertor;

public interface Function<A, R> extends FunctionE<A, R, Exception> {
  R invoke(A argument);

  class Const<R> implements Function<Object, R> {
    private final R myResult;

    public Const(R result) {
      myResult = result;
    }

    public static <R> Const<R> create(R result) {
      return new Const<R>(result);
    }

    public <A> Function<A, R> f() {
      return (Function)this;
    }

    @Override
    public R invoke(Object argument) {
      return myResult;
    }
  }

  class FromConvertor<A, R> implements Function<A, R> {
    private final Convertor<A, R> myConvertor;

    public FromConvertor(Convertor<A, R> convertor) {
      myConvertor = convertor;
    }

    @Override
    public R invoke(A argument) {
      return myConvertor.convert(argument);
    }
  }
}
