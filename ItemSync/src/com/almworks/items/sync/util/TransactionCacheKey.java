package com.almworks.items.sync.util;

import com.almworks.items.api.DBReader;
import com.almworks.items.sync.VersionSource;
import org.almworks.util.TypedKey;
import org.jetbrains.annotations.Nullable;

public class TransactionCacheKey<T> {
  private final TypedKey<T> myKey;

  public TransactionCacheKey(String debugName) {
    myKey = TypedKey.create(debugName);
  }

  public static <T> TransactionCacheKey<T> create(String debugName) {
    return new TransactionCacheKey<T>(debugName);
  }

  @Nullable
  public T get(VersionSource source) {
    return get(source.getReader());
  }

  @Nullable
  @SuppressWarnings( {"unchecked"})
  public T get(DBReader reader) {
    return myKey.getFrom(reader.getTransactionCache());
  }

  public void put(VersionSource source, T value) {
    put(source.getReader(), value);
  }

  @SuppressWarnings( {"unchecked"})
  public void put(DBReader reader, T value) {
    myKey.putTo(reader.getTransactionCache(), value);
  }
}
