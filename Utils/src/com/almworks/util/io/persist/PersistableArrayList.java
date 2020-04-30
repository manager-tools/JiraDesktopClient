package com.almworks.util.io.persist;

import org.almworks.util.Collections15;

import java.util.List;

public class PersistableArrayList<T> extends PersistableCollection<T, List<T>> {
  public PersistableArrayList(Persistable<T> elementBuilder) {
    super(elementBuilder);
  }

  public static <T> PersistableArrayList<T> create(Persistable<T> elementBuilder) {
    return new PersistableArrayList<T>(elementBuilder);
  }

  protected List<T> createCollection() {
    return Collections15.arrayList();
  }
}
