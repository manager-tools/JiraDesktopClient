package com.almworks.items.sync;

import com.almworks.items.api.DBReader;
import com.almworks.items.api.ItemReference;
import com.almworks.util.collections.ByteArray;
import org.almworks.util.ArrayUtil;
import org.almworks.util.Collections15;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public final class HistoryRecord {
  public static final HistoryRecord[] EMPTY_ARRAY = new HistoryRecord[0];

  private final long myKind;
  private final int myRecordId;
  private final byte[] myBytes;

  public HistoryRecord(long kind, int recordId, byte[] bytes) {
    myKind = kind;
    myRecordId = recordId;
    myBytes = bytes;
  }

  public int getRecordId() {
    return myRecordId;
  }

  public long getKind() {
    return myKind;
  }

  @Nullable
  public static HistoryRecord[] restore(byte[] bytes) {
    if (bytes == null || bytes.length == 0) return EMPTY_ARRAY;
    ByteArray.Stream stream = new ByteArray.Stream(bytes);
    List<HistoryRecord> result = Collections15.arrayList();
    while (!stream.isAtEnd()) {
      long kind = stream.nextLong();
      int id = stream.nextInt();
      byte[] data = stream.nextBytes(stream.nextInt());
      if (data == null || stream.isErrorOccurred()) return null;
      result.add(new HistoryRecord(kind, id, data));
    }
    return result.toArray(new HistoryRecord[result.size()]);
  }

  public void serializeTo(ByteArray bytes) {
    bytes.addLong(myKind);
    bytes.addInt(myRecordId);
    bytes.addInt(myBytes.length);
    bytes.add(myBytes);
  }

  @Nullable
  public static byte[] serialize(List<HistoryRecord> history) {
    if (history == null || history.isEmpty()) return null;
    ByteArray bytes = new ByteArray();
    for (HistoryRecord record : history) record.serializeTo(bytes);
    return bytes.toNativeArray();
  }

  @Nullable
  public <T> T mapKind(DBReader reader, Map<ItemReference, T> map) {
    if (map == null) return null;
    for (Map.Entry<ItemReference, T> entry : map.entrySet()) {
      long item = entry.getKey().findItem(reader);
      if (myKind == item) return entry.getValue();
    }
    return null;
  }

  public boolean isOfKind(DBReader reader, ItemReference kind) {
    return kind != null && myKind == kind.findItem(reader);
  }

  public ByteArray.Stream getDataStream() {
    return new ByteArray.Stream(myBytes);
  }

  @Override
  public String toString() {
    return new StringBuilder().append("HR(").append(myKind).append(" ").append(myRecordId).append(" ").append(ArrayUtil.toString(myBytes)).append(')').toString(); 
  }
}
