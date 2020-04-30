package com.almworks.util.components;

import com.almworks.util.models.TableColumnAccessor;

/**
 * @author dyoma
 */
public interface SortingListener {
  void onSortedBy(TableColumnAccessor<?, ?> column, boolean reverse);
}
