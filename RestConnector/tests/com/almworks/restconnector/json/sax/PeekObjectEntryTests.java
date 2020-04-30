package com.almworks.restconnector.json.sax;

import com.almworks.util.Pair;
import com.almworks.util.commons.Procedure2;
import com.almworks.util.tests.BaseTestCase;
import org.json.simple.parser.ParseException;

import java.util.List;

public class PeekObjectEntryTests extends BaseTestCase {
  private final CollectAllJSON myCollector = new CollectAllJSON();

  public void testSimple() throws ParseException {
    JSONCollectorTests.parse("[{'a':1}, {'a': {'a':'X'}}, {'a' : []}]", new PeekObjectEntry("a", 3, myCollector.getHandler()).getUpLink());
    assertEquals(1l, myCollector.getValue(0));
    assertEquals("X", myCollector.getObject(1).get("a"));
    assertEquals(0, myCollector.getArray(2).size());
    myCollector.assertSize(3);
  }

  public void testChain() throws ParseException {
    PeekObjectEntry inner = new PeekObjectEntry("b", 2, myCollector.getHandler());
    PeekObjectEntry outer = new PeekObjectEntry("a", 3, inner.getUpLink());
    JSONCollectorTests.parse("[{'a' : []}, {'a' : {'b' : 1}}]", outer.getUpLink());
    assertEquals(1l, myCollector.getValue(0));
    myCollector.assertSize(1);
  }

  public void testArrayEntry() throws ParseException {
    LocationHandler handler = PeekArrayElement.entryArray("a", 3, myCollector.getHandler());
    JSONCollectorTests.parse("[{'a' : []}, {'a' : [1, 2]}, {'a' : '[]'}, {'a' : [3]}, {'a' : 'X'}, {'b' : ['Y']}]", handler);
    assertEquals(1l, myCollector.getValue(0));
    assertEquals(2l, myCollector.getValue(1));
    assertEquals(3l, myCollector.getValue(2));
    myCollector.assertSize(3);
  }

  public void testEntryValue() throws ParseException {
    Procedure2.ListCollector<String, Object> collector = new Procedure2.ListCollector<String, Object>();
    JSONCollectorTests.parse("{'x' : {'a' : 0}, 'k' : {'a' : 1, 'b' : 2}}", new PeekEntryValue(collector).entryObject("k"));
    List<Pair<String,Object>> list = collector.getPairs();
    assertEquals(list.toString(), 2, list.size());
    assertEquals(Pair.create("a", 1l), list.get(0));
    assertEquals(Pair.create("b", 2l), list.get(1));
  }
}
