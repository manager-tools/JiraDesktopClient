package com.almworks.api.engine;

import org.almworks.util.TypedKey;
import org.almworks.util.TypedKeyRegistry;

public final class SyncType extends TypedKey<SyncType> {
  public static final TypedKeyRegistry<SyncType> REGISTRY = TypedKeyRegistry.create();

  public static final SyncType RECEIVE_ONLY = new SyncType("RECEIVE_ONLY", 1);
  public static final SyncType RECEIVE_FULL = new SyncType("RECEIVE_FULL", 2);
  public static final SyncType RECEIVE_AND_SEND = new SyncType("RECEIVE_AND_SEND", 3);

  private final int myWeight;

  private SyncType(String name, int weight) {
    super(name, null, REGISTRY);
    myWeight = weight;
  }

  public int getWeight() {
    return myWeight;
  }

  public static SyncType heaviest(SyncType type1, SyncType type2) {
    // todo RECEIVE_AND_SEND may not be heaviest - if bug is not changed, we fall back to RECEIVE_ONLY, ignoring possible previous RECEIVE_HEAVY
    if (type2 == null)
      return type1;
    if (type1 == null)
      return type2;
    return type2.getWeight() < type1.getWeight() ? type1 : type2;
  }

  public boolean isUpload() {
    return this == RECEIVE_AND_SEND;
  }
}
