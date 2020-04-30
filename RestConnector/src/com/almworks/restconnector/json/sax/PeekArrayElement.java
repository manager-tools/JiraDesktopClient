package com.almworks.restconnector.json.sax;

import org.jetbrains.annotations.Nullable;
import org.json.simple.parser.ParseException;

import java.io.IOException;

/**
 * Peeks each array element from top array (JSON: "[elem1, elem2, ...]") and passes to the chained handler
 */
public class PeekArrayElement {
  private static final int DEPTH = 2;

  private final PathTracker myTracker = new PathTracker(new LocationHandler() {
    @Override
    public void visit(Location what, boolean start, @Nullable String key, @Nullable Object value) throws ParseException, IOException {
      doVisit(what, start, key, value);
    }

    @Override
    public String toString() {
      return PeekArrayElement.this.toString();
    }
  });

  private final LocationHandler myConsumer;
  private boolean myDelegating = false;

  public PeekArrayElement(LocationHandler elementHandler) {
    myConsumer = elementHandler;
  }

  /**
   * Shortcut to visit object entry and enumerate it's elements with elementHandler. (JSON: "{key : [elem1, ..]}").<br>
   * @param elementHandler array elements handler
   * @see PeekObjectEntry#PeekObjectEntry(String, int, LocationHandler)
   */
  public static LocationHandler entryArray(String key, int depth, LocationHandler elementHandler) {
    return new PeekObjectEntry(key, depth, new PeekArrayElement(elementHandler).getUpLink()).getUpLink();
  }

  public static LocationHandler entryArray(String key, LocationHandler elementHandler) {
    return entryArray(key, 2, elementHandler);
  }

  private void doVisit(LocationHandler.Location what, boolean start, String key, Object value) throws IOException, ParseException {
    if (what == LocationHandler.Location.TOP && start) myDelegating = false;
    if (myDelegating) {
      myConsumer.visit(what, start, key, value);
      if (!start && myTracker.getDepth() == DEPTH + 1) finishDelegate(); // end of element
    } else {
      if (start && myTracker.getDepth() == DEPTH + 1 && myTracker.getPrevLocation(1) == LocationHandler.Location.ARRAY) {
        myDelegating = true;
        LocationHandler.Location.startJSON(myConsumer);
        myConsumer.visit(what, start, key, value);
        if (what == LocationHandler.Location.PRIMITIVE) finishDelegate(); // single primitive value
      }
    }
  }

  private void finishDelegate() throws IOException, ParseException {
    LocationHandler.Location.endJSON(myConsumer);
    myDelegating = false;
  }

  public LocationHandler getUpLink() {
    return myTracker;
  }

  @Override
  public String toString() {
    return "PeekElement->" + myConsumer;
  }
}
