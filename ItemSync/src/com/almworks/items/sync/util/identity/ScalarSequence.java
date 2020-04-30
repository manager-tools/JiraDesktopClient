package com.almworks.items.sync.util.identity;

import com.almworks.items.api.DBIdentifiedObject;
import com.almworks.items.sync.DBDrain;
import com.almworks.items.sync.ItemProxy;
import com.almworks.util.LogHelper;
import com.almworks.util.collections.ByteArray;
import org.almworks.util.Collections15;
import org.almworks.util.Const;

import java.util.Collection;
import java.util.List;

public class ScalarSequence {
  public static final ScalarSequence EMPTY_SEQUENCE = new ScalarSequence(Const.EMPTY_OBJECTS);
  private final Object[] myData;

  private ScalarSequence(Object[] data) {
    myData = data;
  }

  public byte[] serialize(DBDrain drain) {
    ByteArray array = new ByteArray();
    for (Object o : myData) {
      Object value = DBStaticObject.toDBValue(drain, o);
      if (value instanceof String) array.addUTF8((String) value);
      else if (value instanceof Long) array.addLong((Long) value);
      else if (value instanceof Boolean) array.addBoolean((Boolean) value);
      else if (value instanceof Integer) array.addInt((Integer) value);
      else if (value instanceof Byte) array.addByte((Byte) value);
      else if (value instanceof byte[]) array.addSubarray((byte[])value);
      else LogHelper.error("Unknown data", o, value);
    }
    return array.toNativeArray();
  }

  public boolean isEmpty() {
    return myData.length == 0;
  }

  public static ScalarSequence create(Collection<? extends ItemProxy> proxies) {
    Builder builder = new Builder();
    for (ItemProxy proxy : proxies) builder.append(proxy);
    return builder.create();
  }

  public static ScalarSequence create(ItemProxy... objects) {
    final Builder builder = new Builder();
    for(final ItemProxy object : objects) {
      builder.append(object);
    }
    return builder.create();
  }

  public static class Builder {
    private final List<Object> myData = Collections15.arrayList();

    public ScalarSequence create() {
      return new ScalarSequence(myData.toArray());
    }

    public Builder append(ItemProxy object) {
      if (object == null) {
        LogHelper.error("Null object");
        myData.add((long) 0);
      }
      myData.add(object);
      return this;
    }

    public Builder append(DBIdentifiedObject object) {
      return append(DBIdentity.fromDBObject(object));
    }

    public Builder append(int value) {
      myData.add(value);
      return this;
    }

    public Builder appendLong(long value) {
      myData.add(value);
      return this;
    }

    public Builder append(boolean value) {
      myData.add(value);
      return this;
    }

    public Builder append(String value) {
      if (value == null) {
        LogHelper.error("Null values not supported. Replaced with empty string");
        value = "";
      }
      myData.add(value);
      return this;
    }

    public Builder appendSubsequence(ScalarSequence sequence) {
      if (sequence == null) {
        LogHelper.error("Null values not supported. Replaced with empty sequence");
        sequence = EMPTY_SEQUENCE;
      }
      myData.add(sequence);
      return this;
    }

    public Builder appendByte(int byteValue) {
      //noinspection UnnecessaryBoxing
      myData.add(Byte.valueOf((byte) byteValue));
      return this;
    }
  }
}
