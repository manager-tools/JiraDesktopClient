package com.almworks.util.model;

import com.almworks.util.events.EventSource;

/**
 * :todoc:
 *
 * @author sereda
 */
public interface ModelSetter <C extends ContentModel, M extends ModelSetter> {
  C getModel();

  EventSource<RequestConsumer<M>> getRequestEventSource();

  void setContentKnown();

  Object getLock();

  public static interface RequestConsumer <M> {
    void valueRequested(M model);
  }
}
