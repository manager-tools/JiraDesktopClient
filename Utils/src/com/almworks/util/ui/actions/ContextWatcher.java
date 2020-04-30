package com.almworks.util.ui.actions;

import com.almworks.util.collections.Modifiable;
import com.almworks.util.model.ScalarModel;
import org.almworks.util.ArrayUtil;
import org.almworks.util.TypedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author dyoma
 */
public class ContextWatcher {
  @Nullable
  private Object myWatchers = null;
  private int myWatchersCount = 0;

  public void requestUpdates(UpdateRequest request) {
    synchronized (this) {
      Object watchers = myWatchers;
      if (watchers == null)
        return;
      if (watchers instanceof WatchType) {
        ((WatchType) watchers).watch(request);
      } else {
        assert watchers instanceof WatchType[] : watchers;
        assert myWatchersCount > 0 : myWatchersCount;
        WatchType[] watchersList = (WatchType[]) watchers;
        //noinspection ForLoopReplaceableByForEach
        for (int i = 0; i < myWatchersCount; i++) {
          WatchType watchType =  watchersList[i];
          if (watchType != null) {
            watchType.watch(request);
          }
        }
      }
    }
  }

  public void watchRole(TypedKey<?> role) {
    addWatchType(new WatchRole(role));
  }

  public void addWatchType(WatchType watchType) {
    synchronized(this) {
      if (myWatchers == null) {
        myWatchers = watchType;
        return;
      }
      @NotNull
      WatchType[] array;
      Object watchers = myWatchers;
      if (watchers instanceof WatchType) {
        assert myWatchersCount == 0;
        array = new WatchType[2];
        array[0] = (WatchType) watchers;
        array[1] = watchType;
        myWatchersCount = 2;
      } else {
        assert watchers instanceof WatchType[] : watchers;
        assert myWatchersCount >= 2 : myWatchersCount;
        array = (WatchType[]) watchers;
        array = ArrayUtil.ensureCapacity(array, myWatchersCount + 1);
        array[myWatchersCount] = watchType;
        myWatchersCount++;
        myWatchers = array;
      }
      myWatchers = array;
    }
  }

  public void watchModifiableRole(TypedKey<? extends Modifiable> role) {
    addWatchType(new WatchModifiableRole(role));
  }

  public void updateOnChange(final Modifiable modifiable) {
    addWatchType(new WatchModifiable(modifiable));
  }

  public void updateOnChange(final ScalarModel<?> model) {
    addWatchType(new WatchScalarModel(model));
  }

  public interface WatchType {
    void watch(UpdateRequest request);
  }

  private static class WatchRole implements WatchType {
    private final TypedKey<?> myRole;

    public WatchRole(TypedKey<?> role) {
      myRole = role;
    }

    public void watch(UpdateRequest request) {
      request.watchRole(myRole);
    }
  }

  private static class WatchModifiableRole implements WatchType {
    private final TypedKey<? extends Modifiable> myRole;

    public WatchModifiableRole(TypedKey<? extends Modifiable> role) {
      myRole = role;
    }

    public void watch(UpdateRequest request) {
      request.watchModifiableRole(myRole);
    }
  }

  private static class WatchModifiable implements WatchType {
    private final Modifiable myModifiable;

    public WatchModifiable(Modifiable modifiable) {
      myModifiable = modifiable;
    }

    public void watch(UpdateRequest request) {
      request.updateOnChange(myModifiable);
    }
  }

  private static class WatchScalarModel implements WatchType {
    private final ScalarModel<?> myModel;

    public WatchScalarModel(ScalarModel<?> model) {
      myModel = model;
    }

    public void watch(UpdateRequest request) {
      request.updateOnChange(myModel);
    }
  }
}
