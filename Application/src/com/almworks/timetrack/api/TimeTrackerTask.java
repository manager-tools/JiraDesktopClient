package com.almworks.timetrack.api;

import com.almworks.api.application.LoadedItem;
import com.almworks.api.application.StateIcon;
import com.almworks.api.explorer.ItemModelRegistry;
import com.almworks.explorer.loader.LoadedItemImpl;
import com.almworks.items.api.DBReader;
import com.almworks.recentitems.RecentItemUtil;
import com.almworks.util.Terms;
import com.almworks.util.exec.Context;
import com.almworks.util.images.Icons;
import com.almworks.util.io.persist.FormatException;
import com.almworks.util.io.persist.LeafPersistable;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import util.external.CompactInt;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;

public final class TimeTrackerTask {
  public static final TypedKey<TimeTrackerTask> TIME_TRACKER_TASK = TypedKey.create("TimeTrackerTask");
  public static final TypedKey<Boolean> TIME_TRACKER_ACTIVE = TypedKey.create("TimeTrackerActive");

  public static final StateIcon TIME_TRACKING_STARTED =
    new StateIcon(Icons.TIME_TRACKING_STARTED, 0, "You are currently working on this " + Terms.ref_artifact);
  public static final StateIcon TIME_TRACKING_PAUSED = new StateIcon(Icons.TIME_TRACKING_PAUSED, 0,
    "You are currently working on this " + Terms.ref_artifact + " (time tracking paused)");
  public static final Set<StateIcon> TIME_TRACKING_ICONS =
    Collections.unmodifiableSet(Collections15.hashSet(TIME_TRACKING_PAUSED, TIME_TRACKING_STARTED));

  private final long myKey;

  public TimeTrackerTask(long key) {
    myKey = key;
  }

  @Nullable
  public LoadedItem load(ItemModelRegistry registry, DBReader reader) {
    return load(null, registry, reader);
  }

  @Nullable
  public LoadedItem loadLive(@NotNull Lifespan life, DBReader reader) {
    if(life.isEnded()) {
      return null;
    }
    return load(life, Context.require(ItemModelRegistry.class), reader);
  }

  @Nullable
  private LoadedItem load(@Nullable Lifespan life, ItemModelRegistry modelRegistry, DBReader reader) {
    if(!RecentItemUtil.checkItem(myKey, reader)) {
      return null;
    }
    return life != null
      ? LoadedItemImpl.createLive(life, modelRegistry, myKey, reader)
      : LoadedItemImpl.create(modelRegistry, myKey, reader);
  }

  @Override
  public String toString() {
    return "TTT[" + myKey + "]";
  }

  public long getKey() {
    return myKey;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;

    TimeTrackerTask task = (TimeTrackerTask) o;
    return myKey == task.myKey;
  }

  @Override
  public int hashCode() {
    return (int) (myKey ^ (myKey >>> 32));
  }


  public static class Persister extends LeafPersistable<TimeTrackerTask> {
    private long myKey;

    protected void doClear() {
      myKey = 0;
    }

    protected TimeTrackerTask doAccess() {
      return doCopy();
    }

    protected TimeTrackerTask doCopy() {
      return new TimeTrackerTask(myKey);
    }

    protected void doRestore(DataInput in) throws IOException, FormatException {
      myKey = CompactInt.readLong(in);
    }

    protected void doSet(TimeTrackerTask value) {
      myKey = value.getKey();
    }

    protected void doStore(DataOutput out) throws IOException {
      CompactInt.writeLong(out, myKey);
    }
  }
}
