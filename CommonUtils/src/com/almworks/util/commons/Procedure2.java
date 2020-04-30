package com.almworks.util.commons;

import com.almworks.util.Pair;
import org.almworks.util.Collections15;

import java.util.List;

/**
 * @author dyoma
 */
public interface Procedure2<A, B> {
  void invoke(A a, B b);

  class ListCollector<A, B> implements Procedure2<A, B> {
    private final List<Pair<A, B>> myPairs = Collections15.arrayList();

    @Override
    public void invoke(A a, B b) {
      myPairs.add(Pair.create(a, b));
    }

    public List<Pair<A, B>> getPairs() {
      return myPairs;
    }
  }
}
