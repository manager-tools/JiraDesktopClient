package com.almworks.items.sync.edit;

import com.almworks.integers.LongArray;
import com.almworks.integers.LongList;
import com.almworks.integers.LongListIterator;
import com.almworks.integers.LongTreeSet;
import com.almworks.items.api.DBReader;
import com.almworks.items.sync.EditorLock;
import com.almworks.items.sync.util.SyncUtils;
import com.almworks.util.collections.SimpleModifiable;
import gnu.trove.TLongObjectHashMap;
import org.almworks.util.Collections15;
import org.almworks.util.Log;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

class EditLocks {
  // Guarded by myLocks
  private final TLongObjectHashMap<EditCounterpart> myLocks = new TLongObjectHashMap<>();
  private final ConcurrentHashMap<Object, ShortProcess> myProcesses = new ConcurrentHashMap<Object, ShortProcess>();
  private final SimpleModifiable myModifiable;

  public EditLocks(SimpleModifiable modifiable) {
    myModifiable = modifiable;
  }


  public EditCounterpart findLock(long item) {
    return findAnyLock(LongArray.create(item), null);
  }

  public EditCounterpart findAnyLock(LongList items, @Nullable EditCounterpart ignore) {
    while (true) {
      EditCounterpart lock;
      synchronized (myLocks) {
        lock = priFindLock(items, ignore, null);
      }
      if (lock == null) return null;
      if (ensureAlive(lock) != null) return lock;
    }
  }

  @Nullable
  private EditCounterpart priFindLock(LongList items, @Nullable EditorLock ignoreLock1, @Nullable EditorLock ignoreLock2) {
    assert Thread.holdsLock(myLocks);
    if (items == null) return null;
    for (int i = 0; i < items.size(); i++) {
      EditCounterpart lock = myLocks.get(items.get(i));
      if (lock != null && lock != ignoreLock1 && lock != ignoreLock2) return lock;
    }
    return null;
  }

  @Nullable
  private <L extends EditorLock> L ensureAlive(@Nullable L lock) {
    assert !Thread.holdsLock(myLocks); // Require for notifications (don't invoke listeners inside synchronized block)
    if (lock != null && !lock.isAlive()) {
      lock.release();
      boolean removed = false;
      synchronized (myLocks) {
        LongList items = lock.getItems();
        for (int i = 0; i < items.size(); i++) {
          long item = items.get(i);
          if (myLocks.get(item) == lock) {
            myLocks.remove(item);
            removed = true;
          }
        }
      }
      if (removed) {
        Log.warn("Dead lock not removed during release " + lock);
        myModifiable.fireChanged();
      }
      lock = null;
    }
    return lock;
  }

  public boolean lockEdit(LongList items, EditCounterpart counterpart) {
    return includeLock(counterpart, items, null);
  }

  public boolean includeLock(EditCounterpart target, LongList items, @Nullable EditCounterpart source) {
    try {
      while (true) {
        EditorLock lock;
        if (source != null && !source.isPreparing()) return false;
        synchronized (myLocks) {
          lock = priFindLock(items, target, source);
          if (lock == null) {
            if (source != null) source.removeLocks(items);
            for (int i = 0; i < items.size(); i++) {
              long item = items.get(i);
              EditCounterpart current = myLocks.get(item);
              if (current == target) continue;
              if (current == source) current = null;
              if (current != null) {
                Log.error("Happens locked " + current + " " + target);
                lock = current;
                break;
              }
              myLocks.put(item, target);
            }
            if (!target.itemsLocked(items)) {
              synchronized (myLocks) {
                LongList locked = target.getItems();
                for (int i = 0; i < items.size(); i++) {
                  long item = items.get(i);
                  if (!locked.contains(item)) myLocks.remove(item);
                }
              }
            }
            if (lock == null) break;
          }
        }
        if (ensureAlive(lock) != null) return false;
      }
      myModifiable.fireChanged();
      return true;
    } finally {
      if (target.isReleased()) {
        unlock(items, target);
        unlock(target.getItems(), target);
      }
    }
  }

  public void unlock(LongList items, EditCounterpart counterpart) {
    boolean changed = false;
    synchronized (myLocks) {
      for (int i = 0; i < items.size(); i++) {
        long item = items.get(i);
        if (myLocks.get(item) == counterpart) {
          myLocks.remove(item);
          changed = true;
        }
      }
    }
    if (changed) myModifiable.fireChanged();
  }

  public Collection<EditStart> clearShortLocks(Object process) {
    synchronized (myLocks) {
      ShortProcess shortProcess = myProcesses.remove(process);
      if (shortProcess == null) {
        Log.error("Unknown short process " + process);
        return Collections.emptyList();
      }
      shortProcess.myFinished = true;
      return shortProcess.myShortDeferred;
    }
  }

  public void registerShortLocker(Object process) {
    priRegisterLocker(process);
  }

  private ShortProcess priRegisterLocker(Object process) {
    ShortProcess shortProcess = new ShortProcess();
    ShortProcess prev = myProcesses.putIfAbsent(process, shortProcess);
    if (prev != null) Log.error("Duplicated process registration " + process);
    return prev == null ? shortProcess : prev;
  }

  public EditCounterpart shortLockAll(Object process, LongList items, EditorLock ignore) {
    ShortProcess shortProcess = myProcesses.get(process);
    if (shortProcess == null) {
      Log.error("Unknown process " + process);
      shortProcess = priRegisterLocker(process);
    }
    while (true) {
      EditCounterpart lock;
      synchronized (myLocks) {
        LongList notLocked = shortProcess.selectNotLocked(items);
        lock = priFindLock(notLocked, ignore, null);
        if (lock == null) {
          shortProcess.addLocks(notLocked);
          return null;
        }
      }
      if (ensureAlive(lock) != null) return lock;
    }
  }

  public boolean ensureCanEdit(DBReader reader, EditStart start) {
    EditCounterpart counterpart = start.getCounterpart();
    counterpart = ensureAlive(counterpart);
    if (counterpart == null) {
      Log.debug("EditStart not alive " + start.getCounterpart() + " " + start.getCounterpart().getItems());
      return false;
    }
    LongList items = counterpart.getItems();
    if (!allExisting(reader, items)) {
      counterpart.release();
      Log.warn("Edit start not possible: not existing " + items);
      return false;
    }
    synchronized (myLocks) {
      for (Map.Entry<Object, ShortProcess> entry : myProcesses.entrySet()) {
        ShortProcess process = entry.getValue();
        if (process.myFinished) continue;
        if (process.isAnyLocked(items)) {
          Log.debug("Edit start deferred " + items + ": " + entry.getKey());
          process.myShortDeferred.add(start);
          return false;
        }
      }
      for (int i = 0; i < items.size(); i++) {
        long item = items.get(i);
        EditCounterpart current = myLocks.get(item);
        if (current != counterpart) {
          counterpart.release();
          Log.error("Wrong lock found " + current + " " + counterpart);
          return false;
        }
      }
    }
    return true;
  }

  private boolean allExisting(DBReader reader, LongList items) {
    for (int i = 0; i < items.size(); i++) {
      if (SyncUtils.isRemoved(SyncUtils.readTrunk(reader, items.get(i)))) return false;
    }
    return true;
  }

  private class ShortProcess {
    // Guarded by myLocks
    private final LongTreeSet myShortLocks = new LongTreeSet();
    // Guarded by myLocks
    private final HashSet<EditStart> myShortDeferred = Collections15.hashSet();
    // Guarded by myLocks
    public boolean myFinished = false;

    public LongList selectNotLocked(LongList items) {
      assert Thread.holdsLock(myLocks);
      LongArray notLocked = null;
      for (int i = 0; i < items.size(); i++) {
        long item = items.get(i);
        if (!myShortLocks.contains(item)) {
          if (notLocked == null) notLocked = new LongArray();
          notLocked.add(item);
        }
      }
      return notLocked != null ? notLocked : items;
    }

    public void addLocks(LongList items) {
      assert Thread.holdsLock(myLocks);
      myShortLocks.addAll(items);
    }

    public boolean isAnyLocked(LongList items) {
      assert Thread.holdsLock(myLocks);
      for (LongListIterator i = items.iterator(); i.hasNext(); ) {
        if (myLocks.contains(i.nextValue())) return true;
      }
      return false;
    }
  }
}
