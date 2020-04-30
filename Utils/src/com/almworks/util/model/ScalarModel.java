package com.almworks.util.model;

import com.almworks.util.commons.Condition;


/**
 * :todoc:
 *
 * @author sereda
 */
public interface ScalarModel <T> extends ContentModel<ScalarModelEvent<T>, ScalarModel.Consumer<T>> {
  T getValue(); //unsafe

  T getValueBlocking() throws InterruptedException;

  T waitValue(Condition<T> condition) throws InterruptedException;

  public static interface Consumer <T> extends ContentModelConsumer<ScalarModelEvent<T>> {
    void onScalarChanged(ScalarModelEvent<T> event);
  }

  public static abstract class Adapter <T> implements Consumer<T> {
    public void onScalarChanged(ScalarModelEvent<T> event) {
    }

    public void onContentKnown(ScalarModelEvent<T> event) {
    }
  }
}
