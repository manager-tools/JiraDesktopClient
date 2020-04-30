package com.almworks.syncreg;

import com.almworks.api.syncreg.Hypercube;
import com.almworks.api.syncreg.SyncCubeRegistry;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBReader;
import com.almworks.items.api.DBWriter;
import com.almworks.util.Env;
import com.almworks.util.LogHelper;
import com.almworks.util.io.persist.FormatException;
import com.almworks.util.threads.ThreadSafe;
import org.almworks.util.Log;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import util.external.CompactInt;

import java.io.*;

class SyncCubeRegistryImpl implements SyncCubeRegistry {
  public static final DBAttribute<byte[]> CUBE_SYNC = SyncRegistryComponent.NS.bytes("cubeSync");

  private static final int MAX_DIMENSIONS = 8;
  private static final int FORMAT_SIGNATURE = 0xC0BE50FF;

  private final EquidimensionalSet[] mySets = new EquidimensionalSet[MAX_DIMENSIONS];
  private boolean myAllSynchronized = false;
  private final Object myLock = new Object();
  @Nullable
  private final SyncRegistryImpl myRegistry;

  private boolean myDump = Env.getBoolean("cube.dump");

  SyncCubeRegistryImpl() {
    this(null);
  }

  public SyncCubeRegistryImpl(@Nullable SyncRegistryImpl registry) {
    myRegistry = registry;
    for (int i = 0; i < mySets.length; i++)
      mySets[i] = new EquidimensionalSet(i + 1);
  }

  @ThreadSafe
  public boolean isSynced(@Nullable Hypercube<DBAttribute<?>, Long> cube) {
    if (cube == null) return false;
    synchronized (myLock) {
      if (myAllSynchronized)
        return true;
      int axes = cube.getAxisCount();
      if (axes == 0)
        return false;
      NumberedCube numberedCube = SyncCubeUtils.convert(cube);
      for (int i = 1; i <= axes && i <= MAX_DIMENSIONS; i++) {
        EquidimensionalSet set = mySets[i - 1];
        if (set.encompasses(numberedCube))
          return true;
      }
      return false;
    }
  }

  public void setSynced(@NotNull Hypercube<DBAttribute<?>, Long> cube) {
    if (cube == null) return;
    int axisCount = cube.getAxisCount();
    if (axisCount == 0) {
      boolean already;
      synchronized (myLock) {
        already = myAllSynchronized;
        myAllSynchronized = true;
        for (EquidimensionalSet set : mySets) {
          set.clear();
        }
      }
      if (!already)
        notifyChanged(true, false);
    } else {
      synchronized (myLock) {
        if (myAllSynchronized)
          return;
        NumberedCube numberedCube = SyncCubeUtils.convert(cube);
        for (int i = 0; i < MAX_DIMENSIONS; i++) {
          EquidimensionalSet set = mySets[i];
          if (i + 1 <= axisCount) {
            // for same-dim or higher-dim sets, check if the cube is contained already
            if (set.encompasses(numberedCube))
              return;
          }
          if (i + 1 >= axisCount) {
            // for same-dim or lower-dim sets, check if they are outdated
            set.removeEncompassedBy(numberedCube);
          }
        }
        if (axisCount <= MAX_DIMENSIONS)
          mySets[axisCount - 1].addCube(numberedCube);
      }
      dump();
      notifyChanged(true, false);
    }
  }

  private void notifyChanged(boolean moreSynchronized, boolean lessSynchronized) {
    if (myRegistry != null) myRegistry.onSyncRegistryChanged(moreSynchronized, lessSynchronized);
  }

  public void setUnsynced(@NotNull Hypercube<DBAttribute<?>, Long> cube) {
    synchronized (myLock) {
      myAllSynchronized = false;
      int axisCount = cube.getAxisCount();
      if (axisCount == 0) {
        for (EquidimensionalSet set : mySets) {
          set.clear();
        }
      } else {
        NumberedCube numberedCube = SyncCubeUtils.convert(cube);
        for (int i = 0; i < MAX_DIMENSIONS; i++) {
          EquidimensionalSet set = mySets[i];
          if (i + 1 <= axisCount) {
            set.removeEncompassing(numberedCube);
          }
          if (i + 1 >= axisCount) {
            set.removeEncompassedBy(numberedCube);
          }
        }
      }
    }
    dump();
    notifyChanged(false, true);
  }

  private void dump() {
    if (!myDump)
      return;
    synchronized (myLock) {
      Log.debug("=========== cubesync.dump ===========");
      Log.debug("=== full sync: " + myAllSynchronized);
      for (int i = 0; i < MAX_DIMENSIONS; i++) {
        mySets[i].dump();
      }
      Log.debug("=========== cubesync.dump ===========");
    }
  }
  
  public void load(DBReader reader) {
    long holder = reader.findMaterialized(SyncRegistryComponent.SYNC_HOLDER);
    if (holder <= 0) return;
    byte[] bytes = reader.getValue(holder, CUBE_SYNC);
    if (bytes != null) loadBytes(bytes);
  }
  
  public void save(DBWriter writer) {
    long holder = writer.materialize(SyncRegistryComponent.SYNC_HOLDER);
    byte[] bytes = storeToBytes();
    writer.setValue(holder, CUBE_SYNC, bytes);
  }

  private void loadBytes(byte[] bytes) {
    ByteArrayInputStream stream = new ByteArrayInputStream(bytes);
    DataInputStream in = new DataInputStream(stream);
    try {
      load(in);
      LogHelper.debug("SyncCubeRegistry loaded");
    } catch (IOException e) {
      Log.debug("cannot restore cubes", e);
    }
  }

  private byte[] storeToBytes() {
    byte[] bytes = null;
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    DataOutputStream out = new DataOutputStream(stream);
    try {
      save(out);
      out.close();
      bytes = stream.toByteArray();
    } catch (IOException e) {
      // weird
      assert false : e;
      Log.warn("cannot write cubes");
    }
    return bytes;
  }

  /**
   * needed for testing. we don't override equals() because this class is not used for
   * hashable objects
   */
  boolean equalRegistry(SyncCubeRegistryImpl that) {
    if (that == null)
      return false;
    if (myAllSynchronized != that.myAllSynchronized)
      return false;
    if (mySets.length != that.mySets.length)
      return false;
    for (int i = 0; i < mySets.length; i++) {
      if (!mySets[i].equalSet(that.mySets[i]))
        return false;
    }
    return true;
  }

  private void load(DataInput in) throws IOException {
    synchronized (myLock) {
      int signature = in.readInt();
      if (signature != FORMAT_SIGNATURE)
        throw new FormatException(Long.toHexString(signature));
      boolean allSynchronized = in.readBoolean();
      int setCount = CompactInt.readInt(in);
      if (setCount <= 0 || setCount > 100)
        throw new FormatException("" + setCount);
      EquidimensionalSet[] sets = new EquidimensionalSet[MAX_DIMENSIONS];
      for (int i = 0; i < MAX_DIMENSIONS; i++) {
        sets[i] = new EquidimensionalSet(i + 1);
        if (i < setCount) {
          sets[i].load(in);
        }
      }
      myAllSynchronized = allSynchronized;
      System.arraycopy(sets, 0, mySets, 0, Math.min(mySets.length, sets.length));
      dump();
    }
  }

  private void save(DataOutput out) throws IOException {
    synchronized (myLock) {
      out.writeInt(FORMAT_SIGNATURE);
      out.writeBoolean(myAllSynchronized);
      CompactInt.writeInt(out, mySets.length);
      for (EquidimensionalSet set : mySets) {
        set.save(out);
      }
    }
  }
}
