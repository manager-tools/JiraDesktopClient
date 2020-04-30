package com.almworks.items.sync;

import com.almworks.integers.LongArray;
import com.almworks.integers.LongList;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBIdentifiedObject;
import com.almworks.items.api.ItemReference;
import com.almworks.items.util.AttributeMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;

/**
 * Represents actual branch state of an item. This object can be used as factory to access same branch of other items.<br>
 * Any item always has TRUNK branch, and may have Server branch. TRUNK branch is a "primary" values, Server branch is
 * a set of value shadows (BASE, CONFLICT, DOWNLOAD).<br>
 * While this object attached to TRUNK branch always reads "primary" values, the one attached to Server branch chooses most
 * recent values available at the moment when getValue() method is invoked.<br>
 * This means that if this object is attached to Server branch it isn't fixed at particular shadow and may switch to more
 * recent shadow when it appears (can switch to DOWNLOAD shadow, right after DOWNLOAD shadow is created).<br>
 * TBD: (current implementation)
 * The object may be attached to the particular shadow ({@link com.almworks.items.sync.util.SyncUtils#readBaseIfExists(com.almworks.items.api.DBReader, long)}
 * and other methods). Such objects always read the specified shadow and never switch to another. However, if such object
 * is used as factory the new object is not attached to a shadow but is attached to the Server branch. This implementation
 * probably should be changed to attach factory methods to the shadow too
 * (methods: {@link #forItem(long)}, {@link #readValue(com.almworks.items.api.DBAttribute)})
 */
public interface ItemVersion extends VersionSource {
  long getItem();

  @Nullable
  <T> T getValue(DBAttribute<T> attribute);

  <T> T getNNValue(DBAttribute<T> attribute, T nullValue);

  AttributeMap getAllShadowableMap();

  /**
   * Experimental -- contract is vague:
   * 1) may include or not include shadows
   * 2) used for local version only, no understanding on what it does for other versions<br>
   * Current implementation:<br>
   * 1. Returns values for all not shadowable attributes, reads from TRUNK<br>
   * 2. Returns all shadowable values corresponding to the version. (same as {@link #getAllShadowableMap()})
   */
  AttributeMap getAllValues();

  /**
   * @return last ICN for this item
   */
  long getIcn();

  /**
   * @return the item and all slave subtree (all own slaves, all slaves of slaves, etc)
   */
  LongList getSlavesRecursive();

  /**
   * @return long list as sorted unique array
   */
  @NotNull
  LongList getLongSet(DBAttribute<? extends Collection<? extends Long>> attribute);

  /**
   * @return a copy of all direct slaves of the items that refers to it via masterReference
   */
  @NotNull
  LongArray getSlaves(DBAttribute<Long> masterReference);

  /**
   * Returns sync state of a separate item <b>regardless</b> of its slaves. <br>
   * The result doesnt depend on this version shadow, the same result is provided by any shadow version
   * @return Sync state of the item.
   */
  @NotNull
  SyncState getSyncState();

  /**
   * @return true iff this version is invisible, the item may be visible in some shadows and invisible in others
   */
  boolean isInvisible();

  /**
   * @return true iff this version is not invisible and the item is not deleted
   * @see com.almworks.items.util.SyncAttributes#EXISTING
   */
  boolean isAlive();

  /**
   * Obtain {@link com.almworks.items.sync.ItemVersion} to read trunk of the specified item.
   * @param item the item to read
   * @return trunk version of the specified item. All versions obtained from returned value read trunk by default.
   * @see #forItem(long)
   */
  @NotNull
  ItemVersion readTrunk(long item);

  boolean equalValue(DBAttribute<Long> attribute, DBIdentifiedObject object);

  <T> T mapValue(DBAttribute<Long> attribute, Map<? extends DBIdentifiedObject, T> map);

  @Nullable
  ItemVersion readValue(DBAttribute<Long> attribute);

  @NotNull
  ItemVersion switchToServer();

  @NotNull
  ItemVersion switchToTrunk();

  @NotNull
  HistoryRecord[] getHistory();

  boolean equalItem(ItemReference reference);
}
