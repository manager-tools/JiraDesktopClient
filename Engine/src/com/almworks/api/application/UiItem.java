package com.almworks.api.application;

import com.almworks.util.collections.Convertor;

/**
 * DB item with loaded values. It is not necessarily a primary item, it can be a slave.
 */
public interface UiItem {
  Convertor<UiItem, Long> GET_ITEM = new Convertor<UiItem, Long>() {
    public Long convert(UiItem value) {
      return value.getItem();
    }
  };

  /**
   * @return ID of the DB item from which this UiItem was loaded.
   */
  long getItem();
}
