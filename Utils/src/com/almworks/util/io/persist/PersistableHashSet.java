package com.almworks.util.io.persist;

import org.almworks.util.Collections15;

import java.util.Set;

public class PersistableHashSet<T> extends PersistableCollection<T, Set<T>> {
  public PersistableHashSet(Persistable<T> elementBuilder) {
    super(elementBuilder);
  }

  public static <T> PersistableHashSet<T> create(Persistable<T> elementBuilder) {
    return new PersistableHashSet<T>(elementBuilder);
  }

  protected Set<T> createCollection() {
    return Collections15.linkedHashSet();
  }
}
