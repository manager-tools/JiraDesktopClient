package com.almworks.items.sync.util.identity;

import com.almworks.items.api.DBReader;
import com.almworks.util.LogHelper;
import org.almworks.util.Collections15;
import org.almworks.util.Const;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ResolvedIdentities {
  private static final ResolvedIdentities EMPTY = new ResolvedIdentities(Collections15.<DBIdentity>emptyList(), Const.EMPTY_LONGS);
  private final List<DBIdentity> myFindItems;
  private final long[] myItems;

  private ResolvedIdentities(List<DBIdentity> findItems, long[] items) {
    myFindItems = findItems;
    myItems = items;
  }

  public long getItem(DBStaticObject object) {
    return getItemAt(indexOf(object));
  }

  public int indexOf(DBStaticObject object) {
    if (object == null) return -1;
    int index = myFindItems.indexOf(object.getIdentity());
    if (index < 0) {
      LogHelper.error("Not requested identity", object, myFindItems);
      return -1;
    }
    return index;
  }

  @NotNull
  public static ResolvedIdentities findObjects(DBReader reader, List<DBStaticObject> objects) {
    return find(reader, DBStaticObject.GET_IDENTITY.collectList(objects));
  }

  @NotNull
  public static ResolvedIdentities find(DBReader reader, List<DBIdentity> identities) {
    if (identities == null || identities.isEmpty()) return EMPTY;
    long[] items = new long[identities.size()];
    for (int i = 0; i < identities.size(); i++) {
      DBIdentity identity = identities.get(i);
      items[i] = identity != null ? identity.findItem(reader) : 0;
    }
    return new ResolvedIdentities(identities, items);
  }

  public boolean isAllFound() {
    for (long item : myItems) if (item <= 0) return false;
    return true;
  }

  public int size() {
    return myFindItems.size();
  }

  public long getItemAt(int index) {
    if (index < 0 || index >= size()) return 0;
    long item = myItems[index];
    return item > 0 ? item : 0;
  }
}
