package com.almworks.util.collections;

public interface FactoryWithParameter<T, P> {
  T create(P parameter);
}
