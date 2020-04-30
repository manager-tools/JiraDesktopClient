package com.almworks.util.config;

import org.almworks.util.Collections15;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public interface SubMedium<T> {
  SubMedium EMPTY = new EmptySubMedium();


  /**
   * @param name null mean is there any element
   * @return
   */
  boolean isSet(String name);

  T get(String name);

  /**
   * @param name null means access all
   */
  List<T> getAll(String name);

  List<T> getAll(String name, List<T> buffer);

  Collection<String> getAllNames();

  List<T> getAll();


  public static class EmptySubMedium implements SubMedium {
    public boolean isSet(String name) {
      return false;
    }

    public Object get(String name) {
      return null;
    }

    public List getAll(String name) {
      return Collections.EMPTY_LIST;
    }

    public List getAll(String name, List buffer) {
      return Collections.EMPTY_LIST;
    }

    public Collection<String> getAllNames() {
      return Collections15.emptyCollection();
    }

    public List getAll() {
      return Collections.EMPTY_LIST;
    }
  }
}
