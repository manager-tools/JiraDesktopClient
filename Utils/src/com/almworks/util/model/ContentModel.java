package com.almworks.util.model;

import com.almworks.util.events.EventSource;

/**
 * :todoc:
 *
 * @author sereda
 */
public interface ContentModel <CE extends ContentModelEvent, CL extends ContentModelConsumer<CE>> {
  boolean isContentKnown();

  void requestContent();

  boolean isContentChangeable();

  EventSource<CL> getEventSource();
}
