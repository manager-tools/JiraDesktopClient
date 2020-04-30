package com.almworks.api.engine;

import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;

import java.util.Map;
import java.util.Set;

public final class SyncParameter <T> extends TypedKey<T> {
  public static final SyncParameter<SyncType> ALL_ITEMS =
    new SyncParameter<SyncType>("ALL_ITEMS");
  public static final SyncParameter<SyncType> CHANGED_ITEMS =
    new SyncParameter<SyncType>("CHANGED_ITEMS");
  public static final SyncParameter<Map<Long, SyncType>> EXACT_ITEMS =
    new SyncParameter<Map<Long, SyncType>>("EXACT_ITEMS");
  public static final SyncParameter<Map<Integer, SyncType>> EXACT_ITEMS_BY_ID =
    new SyncParameter<Map<Integer, SyncType>>("EXACT_ITEMS_BY_ID");
  public static final SyncParameter<Map<Connection, SyncType>> EXACT_CONNECTIONS =
    new SyncParameter<Map<Connection, SyncType>>("EXACT_CONNECTION");
  public static final SyncParameter<Boolean> UPDATE_CHANGES =
    new SyncParameter<Boolean>("UPDATE_CHANGES");
  public static final SyncParameter<Set<Connection>> INITIALIZE_CONNECTION =
    new SyncParameter<Set<Connection>>("INITIALIZE_CONNECTION");
  public static final SyncParameter<Boolean> DOWNLOAD_META =
    new SyncParameter<Boolean>("DOWNLOAD_META");

  private SyncParameter(String name) {
    super(name, null, null);
  }

  public Map<SyncParameter, ?> map(T value) {
    Map<SyncParameter, ?> r = Collections15.hashMap();
    putTo((Map) r, value);
    return r;
  }

}
