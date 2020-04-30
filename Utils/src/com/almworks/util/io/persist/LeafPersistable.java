package com.almworks.util.io.persist;

import org.almworks.util.Collections15;

import java.util.List;

public abstract class LeafPersistable<V> extends AbstractPersistable<V> {
  public final List<Persistable> getChildren() {
    return Collections15.emptyList();
  }
}
