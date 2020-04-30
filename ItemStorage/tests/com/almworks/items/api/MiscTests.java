package com.almworks.items.api;

import com.almworks.integers.LongArray;
import com.almworks.integers.LongIterator;
import com.almworks.items.dp.DPNotNull;
import com.almworks.items.impl.SQLiteDatabase;
import org.almworks.util.Collections15;

import java.util.Map;

public class MiscTests extends DatabaseFixture {
  public void testMemStartStop() {
    SQLiteDatabase db = createSQLiteMemoryDatabase();

    final DBAttribute<String> COLOR = DBAttribute.String("color", "Color");
    final String[] COLORS = {"blue", "green", "velvet"};

    db.writeForeground(new WriteTransaction<Object>() {
      @Override
      public Object transaction(DBWriter writer) throws DBOperationCancelledException {
        for (String s : COLORS) {
          writer.setValue(writer.nextItem(), COLOR, s);
        }
        return null;
      }
    }).waitForCompletion();

    Map<String, Long> map = db.readForeground(new ReadTransaction<Map<String, Long>>() {
      @Override
      public Map<String, Long> transaction(DBReader reader) throws DBOperationCancelledException {
        LongArray items = reader.query(DPNotNull.create(COLOR)).copyItemsSorted();
        Map<String, Long> map = Collections15.hashMap();
        for (LongIterator ii = items.iterator(); ii.hasNext();) {
          long item = ii.nextValue();
          map.put(COLOR.getValue(item, reader), item);
        }
        return map;
      }
    }).waitForCompletion();

    assertEquals(3, map.size());
    assertEquals(Collections15.hashSet(COLORS), map.keySet());
  }
}
