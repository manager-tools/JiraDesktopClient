package com.almworks.util.exec;

import com.almworks.util.tests.BaseTestCase;
import org.almworks.util.TypedKey;
import org.almworks.util.Util;

public class ContextComplexTests extends BaseTestCase {
  public void testReentrancyBreaks() {
    Context.add(InstanceProvider.instance("qqq"), null);

    Context.add(new ContextDataProvider() {
      public <T> T getObject(Class<T> objectClass, int depth) {
        if (StringBuilder.class.equals(objectClass)) {
          return (T) new StringBuilder(Util.NN(Context.get(String.class)));
        } else {
          return null;
        }
      }

      public <T> T getObject(TypedKey<T> key, int depth) {
        return null;
      }
    }, null);

    assertEquals("qqq", Context.require(String.class));
    try {
      StringBuilder builder = Context.get(StringBuilder.class);
      fail("got builder: " + builder);
    } catch (IllegalStateException e) {
      // normal!
    }
  }
}
