package com.almworks.restconnector.json.sax;

import com.almworks.util.LogHelper;
import com.almworks.util.commons.Procedure2;
import org.almworks.util.Util;
import org.jetbrains.annotations.Nullable;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import java.io.IOException;

/**
 * Peeks each object entry with fully loaded value and passes to consumer procedure.<br>
 * Example. Assume JSON: "{"a" : 1, "b" : [2, 3]}"<br>
 * The consumer procedure will be called twice:<br>
 * 1. ("a", 1)<br>
 * 2. ("b", JSONArray[2, 3]) <br>
 * Instances must be chained to get OBJECT right after TOP. The following ENTRYs are passed to consumer.
 */
public class PeekEntryValue {
  private final PathTracker myTracker = new PathTracker(new LocationHandler() {
    @Override
    public void visit(Location what, boolean start, @Nullable String key, @Nullable Object value) throws ParseException, IOException {
      doVisit(what, start, key, value);
    }
  });
  private final Procedure2<String, Object> myConsumer;
  private String myCurrentKey = null;
  private final JSONCollector myValue = new JSONCollector(null);

  public PeekEntryValue(Procedure2<String, Object> consumer) {
    myConsumer = consumer;
  }

  /**
   * Constructor method to process entries with object value ({a: {...}, b: {...}}.<br>
   * Consumer gets only not null value. If an entry has not null, not object value then error is logged.
   * @param consumer procedure to consume [id, object] entries.
   * @return handler to process outer object
   */
  public static LocationHandler objectValue(final Procedure2<String, JSONObject> consumer) {
    return new PeekEntryValue(new Procedure2<String, Object>() {
      @Override
      public void invoke(String s, Object o) {
        JSONObject object = Util.castNullable(JSONObject.class, o);
        if (object == null) LogHelper.assertError(o == null, "Expected object, but was: ", o == null ? "null" : o.getClass());
        else consumer.invoke(s, object);
      }
    }).getUpLink();
  }

  public LocationHandler getUpLink() {
    return myTracker;
  }

  public LocationHandler entryObject(String key) {
    return new PeekObjectEntry(key, 2, myTracker).getUpLink();
  }

  private void doVisit(LocationHandler.Location what, boolean start, @Nullable String key, @Nullable Object value) throws ParseException, IOException {
    if (myTracker.getDepth() == 1) ensure(LocationHandler.Location.TOP, what, start, key, value);
    else if (myTracker.getDepth() == 2) ensure(LocationHandler.Location.OBJECT, what, start, key, value);
    else if (myTracker.getDepth() == 3) {
      ensure(LocationHandler.Location.ENTRY, what, start, key, value);
      if (start) {
        LogHelper.assertError(myCurrentKey == null, "Not ended", myCurrentKey);
        myCurrentKey = key;
        myValue.reset();
        LocationHandler.Location.startJSON(myValue);
      } else {
        LocationHandler.Location.endJSON(myValue);
        Object object = myValue.getObject();
        String currentKey = myCurrentKey;
        myValue.reset();
        myCurrentKey = null;
        if (currentKey == null) {
          LogHelper.error("Not started");
          return;
        }
        myConsumer.invoke(currentKey, object);
      }
    } else myValue.visit(what, start, key, value);
  }

  private void ensure(LocationHandler.Location expected, LocationHandler.Location what, boolean start, String key, Object value) throws ParseException {
    if (what != expected) {
      LogHelper.error("Expected", expected, what, start, key, value);
      throw new ParseException(ParseException.ERROR_UNEXPECTED_TOKEN);
    }
  }

  @Override
  public String toString() {
    return "EntryValue->" + myConsumer;
  }
}
