package com.almworks.util.advmodel;

import org.almworks.util.detach.Detach;

/**
 * @author : Dyoma
 */
public interface AListModelDecorator<T> extends AListModel<T> {
  Detach setSource(AListModel<? extends T> source);

  AListModel<? extends T> getSource();
}
