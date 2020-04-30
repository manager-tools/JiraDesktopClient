package com.almworks.restconnector.json.sax;

import com.almworks.util.tests.BaseTestCase;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;

public class JSONCollectorTests extends BaseTestCase {
  public void test() throws ParseException {
    JSONObject object = parse(JSONObject.class, "{'a' : 1, 'b' : [], 'c' : {'a' : 'X'}}");
    assertEquals(1l, object.get("a"));
    assertEquals(0, ((JSONArray) object.get("b")).size());
    object = (JSONObject) object.get("c");
    assertEquals("X", object.get("a"));

    JSONArray array = parse(JSONArray.class, "[1, 'a', {'b' : 2}]");
    assertEquals(3, array.size());
    assertEquals(1l, array.get(0));
    assertEquals("a", array.get(1));
    object = (JSONObject) array.get(2);
    assertEquals(2l, object.get("b"));

    assertEquals("a", parse(String.class, "'a'"));
    assertEquals(1l, parse(Long.class, "1").longValue());
  }

  public void testPeekEntry() throws ParseException {
    JSONCollector a = new JSONCollector(null);
    JSONCollector b = new JSONCollector(null);
    CompositeHandler handler = new CompositeHandler(a.peekObjectEntry("a"), b.peekObjectEntry("b"));
    parse("{'a' : 1, 'b' : 2, 'c' : 3}", handler);
    assertEquals(1l, a.getObject());
    assertEquals(2l, b.getObject());
  }

  public void testResetOnStartTop() throws ParseException {
    final JSONCollector a = new JSONCollector(null);
    final int[] countNull = {0};
    final int[] countNotNull = {0};
    // Comment out "a.resetOnStartTop()" and get exception because of observing value "2".
    CompositeHandler handler = new CompositeHandler(a.peekObjectEntry("a"), a.resetOnStartTop(), new PeekObjectEntry("b", 2, new LocationHandler() {
      @Override
      public void visit(Location what, boolean start, @Nullable String key, @Nullable Object value) throws ParseException, IOException {
        Object v = a.getObject();
        assertTrue(String.valueOf(v), v == null || v.equals(1l));
        if (what == Location.PRIMITIVE) {
          if (v == null) countNull[0]++;
          else countNotNull[0]++;
        }
      }
    }).getUpLink());
    parse("[{'a' : 1, 'b' : 0}, {'b' : 0, 'a' : 2}, {'b' : 0, 'a' : 2}]", new PeekArrayElement(handler).getUpLink());
    assertEquals(1, countNotNull[0]);
    assertEquals(2, countNull[0]);
  }

  @NotNull
  private <T> T parse(Class<T> expectedClass, String json) throws ParseException {
    JSONCollector collector = new JSONCollector(null);
    parse(json, collector);
    return Util.castNotNull(expectedClass, collector.getObject());
  }

  public static void parse(String json, LocationHandler handler) throws ParseException {
    json = json.replaceAll("'", "\"");
    JSONParser parser = new JSONParser();
    parser.parse(json, new LocationHandler.ContentAdapter(handler), true);
  }
}
