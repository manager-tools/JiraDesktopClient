package com.almworks.restconnector.json.sax;

import com.almworks.util.LogHelper;
import org.almworks.util.Collections15;
import org.jetbrains.annotations.Nullable;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Wraps {@link LocationHandler another handler}. Tracks current parsed tree location and delegates all {@link LocationHandler#visit(com.almworks.restconnector.json.sax.LocationHandler.Location, boolean, String, Object) visit events}
 * to wrapped handler.<br><br>
 * Usually use case:<br>
 * <pre>
 * class Handler {
 *   private final PathTracker myTracker = new PathTracker(new LocationHandler() {
 *     public void visit(Location what, boolean start, String key, Object value) throws ParseException, IOException {
 *       // Use myTracker to get current location info
 *     }
 *   }
 *
 *   public LocationHandler getLocationHandler() { return myTracker; }
 * }
 * </pre>
 */
public class PathTracker implements LocationHandler {
  private final LocationHandler myHandler;
  private final ArrayList<LocationHandler.Location> myLocationStack = Collections15.arrayList();

  public PathTracker(LocationHandler handler) {
    myHandler = handler;
  }

  public final void visit(LocationHandler.Location what, boolean start, @Nullable String key, @Nullable Object value) throws ParseException, IOException {
    if (what == Location.TOP && start) myLocationStack.clear();
    if (start) myLocationStack.add(what);
    myHandler.visit(what, start, key, value);
    if (!start || what == Location.PRIMITIVE) {
      if (myLocationStack.isEmpty()) throw new ParseException(ParseException.ERROR_UNEXPECTED_TOKEN);
      LocationHandler.Location last = myLocationStack.remove(myLocationStack.size() - 1);
      if (last != what) {
        LogHelper.error("Expected:", last, "but ends:", what);
        throw new ParseException(ParseException.ERROR_UNEXPECTED_TOKEN);
      }
    }
    if (what == Location.TOP && !start) myLocationStack.clear(); // Ensure clean-up
  }

  public int getDepth() {
    return myLocationStack.size();
  }

  @Override
  public String toString() {
    return "PT." + myHandler;
  }

  /**
   * @return previous locations. 0 for current location, 1 - last location and so on.
   */
  public Location getPrevLocation(int backSteps) {
    return myLocationStack.get(myLocationStack.size() - 1 - backSteps);
  }
}
