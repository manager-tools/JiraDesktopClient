package com.almworks.util.model;

import org.almworks.util.Const;

import java.util.Arrays;
import java.util.Collection;

/**
 * :todoc:
 *
 * @author sereda
 */
public class CollectionModelEvent <E> extends ContentModelEvent {
  private final Object[] myScalars;

  public CollectionModelEvent(ContentModel source, E[] scalars) {
    super(source);
    myScalars = scalars == null ? Const.EMPTY_OBJECTS : scalars;
  }

  public Object[] getScalars() {
    return myScalars;
  }

  public int size() {
    return myScalars.length;
  }

  public E get(int index) {
    return (E) myScalars[index];
  }

  public Collection<E> getCollection() {
    return (Collection) Arrays.asList(myScalars);
  }

  public static <E> CollectionModelEvent<E> create(CollectionModel<E> source, E[] scalars) {
    return new CollectionModelEvent<E>(source, scalars);
  }

  public String toString() {
    return "CollectionModelEvent:S" + myScalars.length;
  }
}
