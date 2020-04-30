package com.almworks.api.application;

import com.almworks.api.application.util.PredefinedKey;
import com.almworks.api.engine.Connection;
import com.almworks.api.engine.Engine;
import com.almworks.api.syncreg.ItemHypercube;
import com.almworks.integers.LongList;
import com.almworks.items.sync.SyncState;
import com.almworks.util.properties.Role;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface LoadedItemServices {
  ModelKey<LoadedItemServices> VALUE_KEY = PredefinedKey.create("#loadedItemServices");

  Engine getEngine();

  @NotNull
  Connection requireConnection() throws ConnectionlessItemException;

  @Nullable
  Connection getConnection();

  @Nullable
  <C extends Connection> C getConnection(Class<C> c);

  ItemHypercube getConnectionCube();

  MetaInfo getMetaInfo();

  <T> T getActor(Role<T> role);

  ItemKeyCache getItemKeyCache();

  long getItem();

  @Nullable
  String getItemUrl();

  /**
   * @return true if the item is locally deleted
   * @see #isRemoteDeleted()
   */
  boolean isDeleted();

  boolean hasProblems();

  @NotNull
  LongList getEditableSlaves();

  boolean isLockedForUpload();

  @NotNull
  SyncState getSyncState();

  /**
   * The item may be locally not deleted because of unresolved conflict.
   * @return true if it is known that the item is deleted in local or server branch.
   * @see #isDeleted()
   */
  boolean isRemoteDeleted();
}
