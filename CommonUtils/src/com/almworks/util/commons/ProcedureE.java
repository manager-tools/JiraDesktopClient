package com.almworks.util.commons;

public interface ProcedureE<T, E extends Exception> {
  void invoke(T arg) throws E;
}
