package com.almworks.restconnector.json.sax;

import com.almworks.util.commons.Procedure;
import junit.framework.Assert;
import org.almworks.util.Collections15;
import org.almworks.util.Util;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.List;

public class CollectAllJSON implements Procedure<Object> {
  private final List<Object> myList = Collections15.arrayList();
  private final JSONCollector myCollector = new JSONCollector(this);

  public JSONCollector getHandler() {
    return myCollector;
  }

  @Override
  public void invoke(Object arg) {
    myList.add(arg);
  }

  public Object[] getCollected() {
    return myList.toArray(new Object[myList.size()]);
  }

  public void assertSize(int size) {
    Assert.assertEquals(myList.toString(), size, myList.size());
  }

  public JSONObject getObject(int index) {
    return cast(index, JSONObject.class);
  }

  public Object getValue(int index) {
    return myList.get(index);
  }

  public JSONArray getArray(int index) {
    return cast(index, JSONArray.class);
  }

  private <T> T cast(int index, Class<T> aClass) {
    Object obj = myList.get(index);
    if (obj == null) return null;
    T result = Util.castNullable(aClass, obj);
    Assert.assertNotNull(index + ": " + myList, result);
    return result;
  }

  @Override
  public String toString() {
    return "GatherValuesForTests";
  }
}
