package com.almworks.items.api;

import com.almworks.items.util.AttributeMap;
import com.almworks.items.util.DatabaseUtil;
import org.almworks.util.Collections15;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AttributeTypeTests extends MemoryDatabaseFixture {
  private static final Map<DBAttribute, DBAttribute> LISTS = new HashMap();
  private static final AttributeMap SETS = new AttributeMap();

  static {
    for (DBAttribute<?> a : TestData.SCALAR_VALUES.keySet()) {
      Class<?> valueClass = a.getValueClass();
      if (valueClass.equals(byte[].class))
        continue;
      if (a.isPropagatingChange()) {
        LISTS.put(DBAttribute.LinkList("list:" + a.getId(), "List of " + a.getName(), true), a);
        SETS.put(DBAttribute.LinkSet("set:" + a.getId(), "Set of " + a.getName(), true),
          Collections15.<Long>hashSet(1L, 2L, 3L, 100L));
      } else {
        LISTS.put(DBAttribute.List("list:" + a.getId(), "List of " + a.getName(), valueClass), a);
      }
      if (valueClass.equals(String.class)) {
        SETS.put(DBAttribute.Set("set:" + a.getId(), "Set of " + a.getName(), String.class),
          Collections15.hashSet("a", "quick", "brown", "fox"));
      } 
    }
  }


  public void testSimpleValues() {
    db.writeForeground(new WriteTransaction<Object>() {
      @Override
      public Object transaction(DBWriter writer) throws DBOperationCancelledException {
        for (DBAttribute attribute : TestData.SCALAR_VALUES.keySet()) {
          writer.setValue(TestData.ITEM1, attribute, TestData.SCALAR_VALUES.get(attribute));
        }
        for (DBAttribute attribute : TestData.SCALAR_VALUES.keySet()) {
          assertTrue(attribute.toString(),
            DatabaseUtil.valueEquals(TestData.SCALAR_VALUES.get(attribute), writer.getValue(TestData.ITEM1, attribute)));
        }
        return null;
      }
    }).waitForCompletion();
    db.readForeground(new ReadTransaction<Object>() {
      @Override
      public Object transaction(DBReader reader) throws DBOperationCancelledException {
        for (DBAttribute attribute : TestData.SCALAR_VALUES.keySet()) {
          assertTrue(attribute.toString(),
            DatabaseUtil.valueEquals(TestData.SCALAR_VALUES.get(attribute), reader.getValue(TestData.ITEM1, attribute)));
        }
        return null;
      }
    });
  }

  public void testListValues() {
    db.writeForeground(new WriteTransaction<Object>() {
      @Override
      public Object transaction(DBWriter writer) throws DBOperationCancelledException {
        for (DBAttribute attribute : LISTS.keySet()) {
          List<Object> vvv = thrice(attribute);
          writer.setValue(TestData.ITEM1, attribute, vvv);
          assertEquals(attribute.toString(), vvv, writer.getValue(TestData.ITEM1, attribute));
        }
        return null;
      }
    }).waitForCompletion();
    db.readForeground(new ReadTransaction<Object>() {
      @Override
      public Object transaction(DBReader reader) throws DBOperationCancelledException {
        for (DBAttribute attribute : LISTS.keySet()) {
          List<Object> vvv = thrice(attribute);
          assertEquals(attribute.toString(), vvv, reader.getValue(TestData.ITEM1, attribute));
        }
        return null;
      }
    });
  }

  private static List<Object> thrice(DBAttribute attribute) {
    Object v = TestData.SCALAR_VALUES.get(LISTS.get(attribute));
    List<Object> vvv = Arrays.asList(v, v, v);
    return vvv;
  }

  public void testSetValues() {
    db.writeForeground(new WriteTransaction<Object>() {
      @Override
      public Object transaction(DBWriter writer) throws DBOperationCancelledException {
        for (DBAttribute attribute : SETS.keySet()) {
          writer.setValue(TestData.ITEM1, attribute, SETS.get(attribute));
          assertEquals(attribute.toString(), SETS.get(attribute), writer.getValue(TestData.ITEM1, attribute));
        }
        return null;
      }
    }).waitForCompletion();
    db.readForeground(new ReadTransaction<Object>() {
      @Override
      public Object transaction(DBReader reader) throws DBOperationCancelledException {
        for (DBAttribute attribute : SETS.keySet()) {
          assertEquals(attribute.toString(), SETS.get(attribute), reader.getValue(TestData.ITEM1, attribute));
        }
        return null;
      }
    });
  }

  public void testAttributeMap() {
    db.writeForeground(new WriteTransaction<Object>() {
      @Override
      public Object transaction(DBWriter writer) throws DBOperationCancelledException {
        writer.setValue(TestData.ITEM1, TestData.MAP, TestData.SCALAR_VALUES);
        assertEquals(TestData.SCALAR_VALUES, writer.getValue(TestData.ITEM1, TestData.MAP));
        return null;
      }
    }).waitForCompletion();
    db.readForeground(new ReadTransaction<Object>() {
      @Override
      public Object transaction(DBReader reader) throws DBOperationCancelledException {
        assertEquals(TestData.SCALAR_VALUES, reader.getValue(TestData.ITEM1, TestData.MAP));
        return null;
      }
    });
  }

  public void testAttributeMapLarge() {
    final AttributeMap map = new AttributeMap();
    map.putAll(TestData.SCALAR_VALUES);
    map.putAll(SETS);
    for (DBAttribute attribute : LISTS.keySet()) {
      map.put(attribute, thrice(attribute));
    }
    db.writeForeground(new WriteTransaction<Object>() {
      @Override
      public Object transaction(DBWriter writer) throws DBOperationCancelledException {
        writer.setValue(TestData.ITEM1, TestData.MAP, map);
        assertEquals(map, writer.getValue(TestData.ITEM1, TestData.MAP));
        return null;
      }
    }).waitForCompletion();
    db.readForeground(new ReadTransaction<Object>() {
      @Override
      public Object transaction(DBReader reader) throws DBOperationCancelledException {
        assertEquals(map, reader.getValue(TestData.ITEM1, TestData.MAP));
        return null;
      }
    });
  }


}
