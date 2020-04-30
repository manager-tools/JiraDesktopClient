package com.almworks.items.sync.util;

import com.almworks.integers.LongArray;
import com.almworks.integers.LongList;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBIdentifiedObject;
import com.almworks.items.sync.*;
import com.almworks.items.sync.impl.SyncSchema;
import com.almworks.items.sync.util.identity.ScalarSequence;
import com.almworks.items.util.SyncAttributes;
import org.almworks.util.Collections15;
import org.almworks.util.Const;
import org.almworks.util.Log;

import java.util.*;

/**
 * Common implementation for methods of {@link VersionSource}, {@link DBDrain}, {@link ItemVersion}, {@link ItemVersionCreator}
 */
public class VersionWriterUtil {
  public static ItemVersionCreator setValue(ItemVersionCreator creator, DBAttribute<Long> attribute, DBIdentifiedObject value) {
    long item = creator.materialize(value);
    if (item > 0) creator.setValue(attribute, item);
    else creator.setValue(attribute, (Long) null);
    return creator;
  }

  public static void setValue(ItemVersionCreator creator, DBAttribute<Long> attribute, ItemProxy value) {
    long item = creator.materialize(value);
    if (item > 0) creator.setValue(attribute, item);
    else creator.setValue(attribute, (Long) null);
  }

  public static void setValue(ItemVersionCreator creator, DBAttribute<Long> attribute, ItemVersion value) {
    if (value != null) {
      long item = value.getItem();
      if (item > 0) {
        creator.setValue(attribute, item);
        return;
      }
    }
    creator.setValue(attribute, (Long) null);
  }

  public static void setList(ItemVersionCreator creator, DBAttribute<List<Long>> attribute, long[] value) {
    List<Long> list;
    if (value == null || value.length == 0) list = null;
    else {
      list = Collections15.arrayList(value.length);
      for (long l : value) list.add(l);
    }
    creator.setValue(attribute, list);
  }

  public static void setList(ItemVersionCreator creator, DBAttribute<List<Long>> attribute, LongList value) {
    List<Long> list;
    if (value == null || value.isEmpty()) list = null;
    else {
      list = Collections15.arrayList(value.size());
      for (int i = 0; i < value.size(); i++) list.add(value.get(i));
    }
    creator.setValue(attribute, list);
  }

  public static void setSet(ItemVersionCreator creator, DBAttribute<? extends Collection<? extends Long>> attribute, LongList value) {
    DBAttribute.ScalarComposition composition = attribute.getComposition();
    boolean composeSet;
    switch (composition) {
    case SET: composeSet = true; break;
    case LIST: composeSet = false; break;
    default:
      Log.error("Wrong composition " + composition + " " + attribute);
      return;
    }
    long[] array = value != null ? value.toNativeArray() : null;
    Collection<Long> set;
    if (value == null || value.isEmpty()) set = null;
    else {
      set = composeSet ? Collections15.<Long>hashSet(array.length) : Collections15.<Long>arrayList(array.length);
      for (int i = 0; i < value.size(); i++) {
        long item = value.get(i);
        if (item > 0) set.add(item);
      }
    }
    //noinspection unchecked
    creator.setValue((DBAttribute<Collection<Long>>) attribute, set);
  }

  public static void setSet(ItemVersionCreator creator, DBAttribute<? extends Collection<? extends Long>> attribute, Collection<? extends ItemProxy> value) {
    LongArray items = new LongArray();
    for (ItemProxy proxy : value) items.add(proxy.findOrCreate(creator));
    creator.setSet(attribute, items);
  }


  public static ItemVersionCreator setSequence(ItemVersionCreator creator, DBAttribute<byte[]> attribute, ScalarSequence sequence) {
    byte[] bytes = sequence != null ? sequence.serialize(creator) : Const.EMPTY_BYTES;
    if (bytes.length == 0) bytes = null;
    return creator.setValue(attribute, bytes);
  }

  public static <T> T getNNValue(ItemVersion version, DBAttribute<T> attribute, T nullValue) {
    T value = version.getValue(attribute);
    return value != null ? value : nullValue;
  }

  public static <T extends Collection<? extends Long>> void addValue(ItemVersionCreator creator, DBAttribute<T> attribute, DBIdentifiedObject value) {
    long item = creator.materialize(value);
    if (item > 0) addValue(creator, attribute, item);
  }

  private static  <T extends Collection<? extends Long>> void addValue(ItemVersionCreator creator, DBAttribute<T> attribute, long item) {
    T current = creator.getValue(attribute);
    if (current == null || current.isEmpty()) creator.setValue(attribute, (T) Collections.singleton(item));
    else {
      ((Collection<Long>) current).add(item);
      creator.setValue(attribute, current);
    }
  }

  public static void setAlive(ItemVersionCreator creator) {
    // Update INVISIBLE before EXISTING - allow internals to detect that the item is being revived
    creator.setValue(SyncSchema.INVISIBLE, null);
    creator.setValue(SyncAttributes.EXISTING, true);
  }

  public static void delete(ItemVersionCreator creator) {
    creator.setValue(SyncSchema.INVISIBLE, true);
  }

  public static List<ItemVersion> readItems(final VersionSource source, LongList items) {
    if (items == null || items.isEmpty()) return Collections.emptyList();
    final long[] array = items.toNativeArray();
    return new AbstractList<ItemVersion>() {
      @Override
      public ItemVersion get(int index) {
        return source.forItem(array[index]);
      }

      @Override
      public int size() {
        return array.length;
      }
    };
  }

  public static <T> List<T> collectValues(VersionSource source, DBAttribute<T> attribute, LongList items) {
    ArrayList<T> result = Collections15.arrayList();
    for (ItemVersion item : source.readItems(items)) result.add(item.getValue(attribute));
    return result;
  }


  public static ItemVersionCreator changeItem(DBDrain drain, DBIdentifiedObject object) {
    return drain.changeItem(drain.materialize(object));
  }

  public static ItemVersionCreator changeItem(DBDrain drain, ItemProxy proxy) {
    long item = drain.materialize(proxy);
    return drain.changeItem(item);
  }

  public static List<ItemVersionCreator> changeItems(final DBDrain drain, LongList items) {
    if (items == null || items.isEmpty()) return Collections.emptyList();
    final long[] array = items.toNativeArray();
    return new AbstractList<ItemVersionCreator>() {
      @Override
      public ItemVersionCreator get(int index) {
        return drain.changeItem(array[index]);
      }

      @Override
      public int size() {
        return array.length;
      }
    };
  }
}
