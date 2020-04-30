package com.almworks.items.gui.edit;

import com.almworks.api.syncreg.ItemHypercube;
import com.almworks.integers.LongList;
import com.almworks.items.api.DBAttribute;
import com.almworks.util.Pair;
import org.almworks.util.TypedKey;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

// todo document
public interface EditModelState {
  <T> void putValue(TypedKey<T> key, T value);

  <A, B> void putValues(TypedKey<A> key1, @Nullable A value1, TypedKey<B> key2, @Nullable B value2);

  <A, B, C> void putValues(TypedKey<A> key1, A value1, TypedKey<B> key2, B value2, TypedKey<C> key3, C value3);

  // todo what's the difference with putValue?
  <T> void putHint(TypedKey<T> key, @Nullable T hint);

  <T> T getValue(TypedKey<T> key);

  <T> T getInitialValue(TypedKey<T> key);

  boolean isNewItem();

  LongList getEditingItems();

  Long getSingleEnumValue(DBAttribute<Long> attribute);

  /**
   * @return current value for hypercube axis. Null if no value for the axis is provided (the axis is not restricted)<br>
   * Not null pair [included,excluded]. Included and excluded are not null (may be empty) and may contain 0-item - means no value is included/excluded.
   */
  @Nullable
  Pair<LongList, LongList> getCubeAxis(DBAttribute<?> axis);

  void fireChanged();

  ItemHypercube collectHypercube(Collection<? extends DBAttribute<?>> attributes);
}
