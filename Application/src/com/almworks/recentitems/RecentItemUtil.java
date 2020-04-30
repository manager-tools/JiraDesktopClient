package com.almworks.recentitems;

import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBReader;
import com.almworks.items.util.SyncAttributes;

public class RecentItemUtil {
  public static boolean checkItem(long item, DBReader reader) {
    final Long itemType = reader.getValue(item, DBAttribute.TYPE);
    if(itemType == null || !Boolean.TRUE.equals(reader.getValue(itemType, SyncAttributes.IS_PRIMARY_TYPE))) {
      return false;
    }
    if(Boolean.TRUE.equals(reader.getValue(item, SyncAttributes.INVISIBLE))) {
      return false;
    }
    return true;
  }
}
