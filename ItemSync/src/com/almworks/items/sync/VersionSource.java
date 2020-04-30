package com.almworks.items.sync;

import com.almworks.integers.LongList;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBIdentifiedObject;
import com.almworks.items.api.DBReader;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Available implementations:
 * <ul>
 * <li>{@link com.almworks.items.sync.util.ShadowVersionSource#base(com.almworks.items.api.DBReader) item BASE version} reads {@link com.almworks.items.sync.impl.SyncSchema#BASE BASE} or falls back to TRUNK</li>
 * <li>{@link com.almworks.items.sync.util.ShadowVersionSource#conflict(com.almworks.items.api.DBReader) item CONFLICT version} reads {@link com.almworks.items.sync.impl.SyncSchema#CONFLICT CONFLICT} or falls back to {@link com.almworks.items.sync.impl.SyncSchema#BASE BASE} or TRUNK</li>
 * <li>{@link com.almworks.items.sync.util.BranchSource#trunk(com.almworks.items.api.DBReader) item TRUNK version} always reads TRUNK</li>
 * <li>{@link com.almworks.items.sync.util.BranchSource#server(com.almworks.items.api.DBReader) most recent server version} chooses first existing version from
 * {@link com.almworks.items.sync.impl.SyncSchema#DOWNLOAD DOWNLOAD}, {@link com.almworks.items.sync.impl.SyncSchema#CONFLICT CONFLICT}, {@link com.almworks.items.sync.impl.SyncSchema#BASE BASE}</li>
 * </ul>
 */
public interface VersionSource {
  @NotNull
  DBReader getReader();

  /**
   * Creates ItemVersion for the same type of version
   * @param item item to read
   * @return item version from current branch (or trunk if no version in the branch)<br>
   * Return {@link com.almworks.items.sync.util.IllegalItem illegal item} if item is not positive
   * (doesn't corresponds to existing item)
   */
  @NotNull
  ItemVersion forItem(long item);

  @NotNull
  ItemVersion forItem(DBIdentifiedObject object);

  long findMaterialized(DBIdentifiedObject object);

  @NotNull
  List<ItemVersion> readItems(LongList items);

  <T> List<T> collectValues(DBAttribute<T> attribute, LongList items);
}
