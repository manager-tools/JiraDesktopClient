package com.almworks.util.commons;

/**
 * @author dyoma
 */
public interface Procedure<T> extends ProcedureE<T, Exception> {
  void invoke(T arg);

  class Stub implements Procedure {
    public static Procedure INSTANCE = new Stub();

    public static <T> Procedure<T> instance() { return INSTANCE; };

    private Stub() {}
    @Override
    public void invoke(Object ignored) {}
  }
}
