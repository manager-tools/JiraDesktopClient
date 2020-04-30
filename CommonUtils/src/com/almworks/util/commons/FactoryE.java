package com.almworks.util.commons;

public interface FactoryE<T, E extends Exception> {
  T create() throws E;
}
