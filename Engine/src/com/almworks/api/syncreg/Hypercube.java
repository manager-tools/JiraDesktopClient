package com.almworks.api.syncreg;

import com.almworks.util.Pair;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.SortedSet;

public interface Hypercube<A, V> {
  /**
   * @return sorted set of axes
   */
  Set<A> getAxes();

  @Nullable
  SortedSet<V> getIncludedValues(A axis);

  @Nullable
  SortedSet<V> getExcludedValues(A axis);

  @Nullable
  Pair<SortedSet<V>, SortedSet<V>> getValues(A axis);

  int getAxisCount();

  boolean allows(A axis, V value);

  boolean containsAnyAxis(A ... axes);

  boolean containsAxis(A axis);
}
