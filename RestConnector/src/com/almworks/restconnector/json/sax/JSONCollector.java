package com.almworks.restconnector.json.sax;

import com.almworks.restconnector.json.JSONKey;
import com.almworks.util.LogHelper;
import com.almworks.util.commons.Procedure;
import org.almworks.util.Collections15;
import org.almworks.util.Util;
import org.jetbrains.annotations.Nullable;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

/**
 * Collects subtree into JSON object presentation.It allows to:<br>
 * process subtrees in DOM-style during SAX-like processing. User target constructor parameter<br>
 * Peek the value and hold it for later processing. Use {@link #getObject()}.
 */
public class JSONCollector implements LocationHandler {
  @Nullable
  private final Procedure<Object> myTarget;
  private final ArrayList<Builder> myBuilderStack = Collections15.arrayList();
  private Object myObject = null;
  private final Builder myDefault = new Builder() {
    @Override
    public Builder start(LocationHandler.Location what, String key, Object value) throws ParseException {
      switch (what) {
      case OBJECT:
        JSONObject object = new JSONObject();
        myObject = object;
        return new BuildObject(object);
      case ARRAY:
        JSONArray array = new JSONArray();
        myObject = array;
        return new BuildArray(array);
      case PRIMITIVE:
        myObject = value;
        return null;
      default: throw unexpectedToken(what, key, value);
      }
    }
  };

  public JSONCollector(@Nullable Procedure<Object> target) {
    myTarget = target;
  }

  public static JSONCollector objectConsumer(final Procedure<JSONObject> consumer) {
    return new JSONCollector(new Procedure<Object>() {
      @Override
      public void invoke(Object arg) {
        JSONObject object = JSONKey.ROOT_OBJECT.getValue(arg);
        if (object != null) consumer.invoke(object);
      }
    });
  }

  /**
   * This method allows to use the handler as object holder. It catches last sent object and holds it until next object send is started.
   * @return last object collected.
   * @see #getArray()
   * @see #getInteger()
   * @see #getJsonObject()
   * @see #getString()
   * @see #cast(Class)
   */
  public Object getObject() {
    return myObject;
  }

  /**
   * Allows to extract held object casted to desired class
   * @see #getObject()
   */
  public <T> T cast(Class<T> aClass) {
    T casted = Util.castNullable(aClass, myObject);
    LogHelper.assertError(casted != null || myObject == null, "Wrong class", aClass, myObject, (myObject != null ? myObject.getClass() : null));
    return casted;
  }

  /**
   * @see #getObject()
   */
  public String getString() {
    return cast(String.class);
  }

  /**
   * @see #getObject()
   */
  public JSONObject getJsonObject() {
    return cast(JSONObject.class);
  }

  /**
   * @see #getObject()
   */
  public JSONArray getArray() {
    return cast(JSONArray.class);
  }

  /**
   * @see #getObject()
   */
  public Integer getInteger() {
    return JSONKey.INTEGER.convert(myObject);
  }

  /**
   * @return handler to extract object entry value directly from object (JSON: "{key : value}")
   */
  public LocationHandler peekObjectEntry(String key) {
    return PeekObjectEntry.objectEntry(key, this);
  }

  /**
   * {@link JSONCollector} collects value and holds it until new value starts.<br><br>
   * The purpose: Some code may assume that some key (Key1) is always goes before some other key (Key2) inside same object. This code may use this implementation to peek a value of
   * Key1 hold it and use when Key2 is reached. However the value for Key1 is held till next value of Key1 starts. So if the assumption that Key1 always precedes Key2 fails than the code
   * cannot detect the error and uses Key1 value from previous object.<br><br>
   * This handler is intended to guarantee that held value is reset when new object starts. This handle has to be chained to entire object start.
   * <pre>
   *   JSONCollector value1;
   *   LocationHandler object = new {@link CompositeHandler}( // The whole object processor
   *      value1.peekObjectEntry("key1"), // install to catch value
   *      value1.resetOnStartTop(),       // install to reset when new object starts
   *      new LocationHandler() {
   *        // Safely use value held in value1 - it never hold a value from previous processed object
   *      })
   *   // Sample use case:
   *   {@link PeekArrayElement}.{@link PeekArrayElement#entryArray(String, int, LocationHandler) entryArray}("objects", 2, object);
   * </pre>
   * @return
   */
  public LocationHandler resetOnStartTop() {
    return new LocationHandler() {
      @Override
      public void visit(Location what, boolean start, @Nullable String key, @Nullable Object value) throws ParseException, IOException {
        if (start && what == Location.TOP) reset();
      }

      @Override
      public String toString() {
        return "ResetWhenTop";
      }
    };
  }

  public void reset() {
    if (!myBuilderStack.isEmpty()) LogHelper.error(myBuilderStack.isEmpty(), "During build", myBuilderStack);
    myObject = null;
    myBuilderStack.clear();
  }

  @Override
  public void visit(LocationHandler.Location what, boolean start, @Nullable String key, @Nullable Object value) throws ParseException, IOException {
    if (what == LocationHandler.Location.TOP) {
      if (start) myObject = null;
      else {
        if (myTarget != null) myTarget.invoke(myObject);
      }
      myBuilderStack.clear();
    } else {
      Builder top = myBuilderStack.isEmpty() ? myDefault : myBuilderStack.get(myBuilderStack.size() - 1);
      if (start) {
        Builder next = top.start(what, key, value);
        if (next != null) myBuilderStack.add(next);
      } else {
        if (!myBuilderStack.isEmpty()) myBuilderStack.remove(myBuilderStack.size() - 1);
        else {
          LogHelper.error("Not started", what, key, value);
          throw new ParseException(ParseException.ERROR_UNEXPECTED_EXCEPTION);
        }
      }
    }
  }

  @Override
  public String toString() {
    return "JSONCollector->" + myTarget;
  }

  private static ParseException unexpectedToken(LocationHandler.Location what, String key, Object value) {
    LogHelper.error("Unexpected", what, key, value);
    return new ParseException(ParseException.ERROR_UNEXPECTED_TOKEN);
  }

  /**
   * Adaptor methods. Allows to process parsed JSON objects in SAX style.
   * @param object object to be processed
   * @param handler SAX-like handler to process object
   */
  public static void sendObject(Object object, LocationHandler handler) {
    if (object == null) return;
    try {
      handler.visit(Location.TOP, true, null, null);
      sendValue(object, handler);
      handler.visit(Location.TOP, false, null, null);
    } catch (ParseException e) {
      LogHelper.error("Should not happen", e);
    } catch (IOException e) {
      LogHelper.error("Should not happen", e);
    }
  }

  private static void sendValue(Object o, LocationHandler handler) throws IOException, ParseException {
    Map<String, Object> obj = Util.castNullable(JSONObject.class, o);
    if (obj != null) {
      handler.visit(Location.OBJECT, true, null, null);
      for (Map.Entry<String, Object> entry : obj.entrySet()) {
        String key = entry.getKey();
        handler.visit(Location.ENTRY, true, key, null);
        sendValue(entry.getValue(), handler);
        handler.visit(Location.ENTRY, false, key, null);
      }
      handler.visit(Location.OBJECT, false, null, null);
    } else {
      JSONArray array = Util.castNullable(JSONArray.class, o);
      if (array != null) {
        handler.visit(Location.ARRAY, true, null, null);
        for (Object element : array) sendValue(element, handler);
        handler.visit(Location.ARRAY, false, null, null);
      } else handler.visit(Location.PRIMITIVE, true, null, o);
    }
  }

  private interface Builder {
    /**
     * Processes start of new entity and creates builder for it
     * @return null if entity does not require building (primitive)
     */
    Builder start(LocationHandler.Location what, String key, Object value) throws ParseException;
  }

  private static class BuildObject implements Builder {
    private final JSONObject myObject;

    private BuildObject(JSONObject object) {
      myObject = object;
    }

    @Override
    public Builder start(LocationHandler.Location what, String key, Object value) {
      if (what == LocationHandler.Location.ENTRY) return new PutValue(myObject, key);
      switch (what) {
      case ENTRY:
        break;
      case ARRAY:
        break;
      case PRIMITIVE:
        break;
      }
      return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
  }

  private static class BuildArray implements Builder {
    private final JSONArray myArray;

    public BuildArray(JSONArray array) {
      myArray = array;
    }

    @Override
    public Builder start(LocationHandler.Location what, String key, Object value) throws ParseException {
      Object addValue;
      Builder next;
      switch (what) {
      case OBJECT:
        JSONObject object = new JSONObject();
        addValue = object;
        next = new BuildObject(object);
        break;
      case ARRAY:
        JSONArray array = new JSONArray();
        addValue = array;
        next = new BuildArray(array);
        break;
      case PRIMITIVE:
        addValue = value;
        next = null;
        break;
      default: throw unexpectedToken(what, key, value);
      }
      //noinspection unchecked
      myArray.add(addValue);
      return next;
    }
  }

  private static class PutValue implements Builder {
    private final JSONObject myTarget;
    private final String myKey;

    private PutValue(JSONObject target, String key) {
      myTarget = target;
      myKey = key;
    }

    @Override
    public Builder start(LocationHandler.Location what, String key, Object value) throws ParseException {
      Builder next;
      Object putValue;
      switch (what) {
      case OBJECT:
        JSONObject object = new JSONObject();
        putValue = object;
        next = new BuildObject(object);
        break;
      case ARRAY:
        JSONArray array = new JSONArray();
        putValue = array;
        next = new BuildArray(array);
        break;
      case PRIMITIVE:
        putValue = value;
        next = null;
        break;
      default: throw unexpectedToken(what, key, value);
      }
      //noinspection unchecked
      myTarget.put(myKey, putValue);
      return next;
    }
  }
}
