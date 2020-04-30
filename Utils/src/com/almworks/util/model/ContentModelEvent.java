package com.almworks.util.model;

import com.almworks.util.events.EventDebugger;

/**
 * :todoc:
 *
 * @author sereda
 */
public class ContentModelEvent {
  private final ContentModel mySource;
  private final Throwable myCreatorTrace;

  public ContentModelEvent(ContentModel source) {
    mySource = source;
    myCreatorTrace = EventDebugger.isEnabled() ? new Throwable() : null;
  }

  public ContentModel getSource() {
    return mySource;
  }
}
