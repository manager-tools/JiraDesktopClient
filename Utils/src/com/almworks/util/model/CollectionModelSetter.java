package com.almworks.util.model;

import java.util.Collection;

/**
 * :todoc:
 *
 * @author sereda
 */
public interface CollectionModelSetter <E> extends ModelSetter<CollectionModel<E>, CollectionModelSetter<E>> {
  Collection<E> getWritableCollection();
}
