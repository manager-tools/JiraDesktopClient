package com.almworks.util.model;

/**
 * :todoc:
 *
 * @author sereda
 */
public interface ContentModelConsumer <E extends ContentModelEvent> {
  void onContentKnown(E event);
}
