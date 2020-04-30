package com.almworks.items.sync.edit;

import com.almworks.integers.LongArray;
import com.almworks.integers.LongList;
import com.almworks.items.sync.SyncManager;
import com.almworks.items.sync.SyncState;
import gnu.trove.TLongObjectHashMap;
import gnu.trove.TLongObjectProcedure;
import org.almworks.util.Collections15;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class MergeEventImpl implements SyncManager.MergedEvent {
  private final TLongObjectHashMap<SyncState> myStates = new TLongObjectHashMap<>();

  @Override
  public LongList getItems() {
    return LongArray.create(myStates.keys());
  }

  @Override
  public LongList selectItems(final SyncState... states) {
    final LongArray result = new LongArray();
    final ArrayList<SyncState> fixedStates = Collections15.arrayList();
    for (SyncState state : states) {
      state = fixState(state);
      if (!fixedStates.contains(state)) fixedStates.add(state);
    }
    myStates.forEachEntry(new TLongObjectProcedure<SyncState>() {
      @Override
      public boolean execute(long item, SyncState state) {
        if (fixedStates.contains(state)) result.add(item);
        return true;
      }
    });
    result.sortUnique();
    return result;
  }

  @Override
  public SyncState getState(long item) {
    return myStates.get(item);
  }

  public boolean isEmpty() {
    return myStates.isEmpty();
  }

  public static MergeEventImpl sync(long item) {
    return new MergeEventImpl().addItem(item, SyncState.SYNC);
  }

  @SuppressWarnings({"ConstantConditions"})
  public MergeEventImpl addItem(long item, @NotNull SyncState state) {
    if (state == null) state = SyncState.SYNC;
    state = fixState(state);
    myStates.put(item, state);
    return this;
  }

  public MergeEventImpl addItems(LongList items, SyncState state) {
    for (int i = 0; i < items.size(); i++) addItem(items.get(i), state);
    return this;
  }

  private SyncState fixState(SyncState state) {
    if (state != SyncState.SYNC && state != SyncState.EDITED && state != SyncState.CONFLICT) state = state.isConflict() ? SyncState.CONFLICT : SyncState.EDITED;
    return state;
  }

  public void addEvent(MergeEventImpl other) {
    if (other == null || other.isEmpty()) return;
    other.myStates.forEachEntry(new TLongObjectProcedure<SyncState>() {
      @Override
      public boolean execute(long a, SyncState b) {
        myStates.put(a, b);
        return true;
      }
    });
  }
}
