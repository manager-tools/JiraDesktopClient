package com.almworks.util.collections;

public interface BiMap<L, R> {
  void put(L left, R right);

  R getRight(L left);

  L getLeft(R right);
}
