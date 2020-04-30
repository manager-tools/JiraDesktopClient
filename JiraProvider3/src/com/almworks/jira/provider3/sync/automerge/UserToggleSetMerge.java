package com.almworks.jira.provider3.sync.automerge;

import com.almworks.api.engine.Connection;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBReader;
import com.almworks.items.sync.AutoMergeData;
import com.almworks.items.sync.util.merge.SimpleAutoMerge;
import com.almworks.items.util.SyncAttributes;
import com.almworks.util.LogHelper;
import com.almworks.util.Pair;
import com.almworks.util.collections.Containers;
import org.almworks.util.Util;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import static org.almworks.util.Collections15.hashSet;

class UserToggleSetMerge extends SimpleAutoMerge {
  private final DBAttribute<Set<Long>> myUsers;
  private final DBAttribute<Integer> myCount;
  private final DBAttribute<Boolean> myMeToggled;

  UserToggleSetMerge(DBAttribute<Set<Long>> users, DBAttribute<Integer> count, DBAttribute<Boolean> meToggled) {
    myUsers = users;
    myCount = count;
    myMeToggled = meToggled;
  }

  @Override
  public void resolve(AutoMergeData data) {
    Set<Long> oldLocalSet = Util.NN(data.getLocal().getElderValue(myUsers), Collections.EMPTY_SET);
    Set<Long> newLocalSet = Util.NN(data.getLocal().getNewerValue(myUsers), Collections.EMPTY_SET);
    Pair<Collection<Long>,Collection<Long>> diff = Containers.diffSet(oldLocalSet, newLocalSet);
    Set<Long> newServerSet = hashSet(data.getServer().getNewerValue(myUsers));
    newServerSet.addAll(diff.getFirst());
    newServerSet.removeAll(diff.getSecond());
    data.setResolution(myUsers, newServerSet);
    Long me = getMe(data);
    // Toggled value is either TRUE or NULL
    data.setResolution(myMeToggled, me == null ? null : newServerSet.contains(me) ? true : null);
    data.setResolution(myCount, newServerSet.size());
  }

  @Nullable
  private static Long getMe(AutoMergeData data) {
    DBReader reader = data.getReader();
    Long connection = reader.getValue(data.getItem(), SyncAttributes.CONNECTION);
    if (connection == null) {
      LogHelper.error(data.getItem(), "no connection");
      return null;
    }
    return reader.getValue(connection, Connection.USER);
  }
}
