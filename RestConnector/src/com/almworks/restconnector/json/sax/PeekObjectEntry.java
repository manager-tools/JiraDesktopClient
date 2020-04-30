package com.almworks.restconnector.json.sax;

import com.almworks.util.LogHelper;
import org.jetbrains.annotations.Nullable;
import org.json.simple.parser.ParseException;

import java.io.IOException;

/**
 * Points object entry with specified name and at specified level. Then sends all inner events to next handler.<br>
 * Examples. Assume JSON [{'a' : []}, {'a' : {'b' : 1}}]<br>
 * 1. PeekObjectEntry("a", 3) - points value of 'a' in the whole JSON (3 = top, array, object, <strike>objectEntry</strike>)<br>
 * 2. PeekObjectEntry("b", 2) - points value of 'b' inside {'b' : 1} (2 = top, object, <strike>objectEntry</strike>)<br>
 * Sample 1 - points "[]" and "{'b' : 1}". Sample 2 - points nothing in given JSON. But sample 2 finds "1" when chained to sample 1.
 * @see #objectEntry(String, LocationHandler)
 */
public class PeekObjectEntry {
  private final PathTracker myTracker = new PathTracker(new LocationHandler() {
    @Override
    public void visit(Location what, boolean start, @Nullable String key, @Nullable Object value) throws ParseException, IOException {
      doVisit(what, start, key, value);
    }

    @Override
    public String toString() {
      return PeekObjectEntry.this.toString();
    }
  });

  private final LocationHandler myConsumer;
  private final String myKey;
  private final int myExpectedDepth;
  private boolean myDelegating;

  /**
   * @param key desired object entry key
   * @param expectedDepth desired depth - number of upperSyntax constructs. 2 for "{"a" : VALUE}" : top + object
   * @param consumer chained processor
   */
  public PeekObjectEntry(String key, int expectedDepth, LocationHandler consumer) {
    myConsumer = consumer;
    myKey = key;
    myExpectedDepth = expectedDepth + 1;
  }

  /**
   * Common constructor. When applied to an object extracts entry with given name and passes value to valueHandler.<br>
   * Example: assume JSON {a:{....}}<br>
   * objectEntry("a", handler);<br>
   * Creates handler such as when it is applied to sample JSON then handler gets value of "a".
   * @param key entry name
   * @param valueHandler handler for entry value
   */
  public static LocationHandler objectEntry(String key, LocationHandler valueHandler) {
    return new PeekObjectEntry(key, 2, valueHandler).getUpLink();
  }

  private void doVisit(LocationHandler.Location what, boolean start, String key, Object value) throws IOException, ParseException {
    if (what == LocationHandler.Location.TOP) myDelegating = false;
    boolean justStarted = false;
    if (start && !myDelegating && what == LocationHandler.Location.ENTRY && myKey.equals(key) && myExpectedDepth == myTracker.getDepth()) {
      myDelegating = true;
      LocationHandler.Location.startJSON(myConsumer);
      justStarted = true;
    }
    if (!start && myDelegating && myExpectedDepth == myTracker.getDepth()) {
      LogHelper.assertError(LocationHandler.Location.ENTRY == what, what);
      myDelegating = false;
      LocationHandler.Location.endJSON(myConsumer);
    }
    if (myDelegating && !justStarted) myConsumer.visit(what, start, key, value);
  }

  /**
   * @return handler to connect to outer processor. It may be root parser handler or previous processor in a chain.
   */
  public LocationHandler getUpLink() {
    return myTracker;
  }

  @Override
  public String toString() {
    return "PeekEntry(" + myKey + "@" + myExpectedDepth + ")->" + myConsumer;
  }
}
