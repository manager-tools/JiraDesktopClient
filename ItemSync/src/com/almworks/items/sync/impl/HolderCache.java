package com.almworks.items.sync.impl;

import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBReader;
import com.almworks.items.api.DBWriter;
import com.almworks.items.util.AttributeMap;
import com.almworks.items.util.SyncAttributes;
import gnu.trove.TLongObjectHashMap;
import org.almworks.util.Collections15;
import org.almworks.util.Log;
import org.almworks.util.TypedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public class HolderCache {
  private static final TypedKey<HolderCache> KEY = TypedKey.create("versionHolderCache");
  public static final List<DBAttribute<AttributeMap>> SERVER_SHADOWS =
    Collections15.unmodifiableListCopy(SyncSchema.DOWNLOAD, SyncSchema.CONFLICT, SyncSchema.BASE);

  private final Map<DBAttribute<AttributeMap>, TLongObjectHashMap<VersionHolder>> myHolders = Collections15.hashMap();
  private final DBReader myReader;

  public HolderCache(DBReader reader) {
    myReader = reader;
  }

  @SuppressWarnings({"unchecked"})
  @NotNull
  public static HolderCache instance(DBReader reader) {
    HolderCache cache = KEY.getFrom(reader.getTransactionCache());
    if (cache == null) {
      cache = new HolderCache(reader);
      KEY.putTo(reader.getTransactionCache(), cache);
    }
    return cache;
  }

  /**
   * @param item item to read
   * @param shadow not null shadow or null for trunk
   * @param returnEmpty flag to return empty holder when missing shadow
   * @return not null if returnEmpty
   */
  public VersionHolder getHolder(long item, DBAttribute<AttributeMap> shadow, boolean returnEmpty) {
    TLongObjectHashMap<VersionHolder> cache = myHolders.get(shadow);
    if (cache != null) {
      VersionHolder holder = cache.get(item);
      if (holder != null) {
        if ((returnEmpty || !EmptyHolder.isEmpty(holder))) return holder;
        else return null;
      }
    }
    VersionHolder holder;
    if (shadow == null) holder = new VersionHolder.Trunk(myReader, item);
    else {
      AttributeMap values = myReader.getValue(item, shadow);
      if (values == null) {
        holder = new EmptyHolder(myReader, item, shadow);
        if (!returnEmpty) {
          addHolder(holder);
          return null;
        }
      } else holder = new VersionHolder.Shadow(myReader, item, values, shadow);
    }
    addHolder(holder);
    return holder;
  }

  @Nullable
  public VersionHolder getServerHolder(long item) {
    for (DBAttribute<AttributeMap> shadow : SERVER_SHADOWS) {
      VersionHolder holder = getHolder(item, shadow, false);
      if (holder != null) return holder;
    }
    return null;
  }

  public void addHolder(@NotNull VersionHolder holder) {
    DBAttribute<AttributeMap> shadow = holder.getShadow();
    TLongObjectHashMap<VersionHolder> cache = myHolders.get(shadow);
    if (cache == null) {
      cache = new TLongObjectHashMap<>();
      myHolders.put(shadow, cache);
    }
    cache.put(holder.getItem(), holder);
  }

  public void setBase(long item, @Nullable AttributeMap map) {
    if (map == null) ((DBWriter)myReader).setValue(item, SyncAttributes.CHANGE_HISTORY, null);
    setShadow(item, SyncSchema.BASE, map);
  }

  public void setConflict(long item, @Nullable AttributeMap map) {
    setShadow(item, SyncSchema.CONFLICT, map);
  }

  public void setDownload(long item, @Nullable AttributeMap map) {
    setShadow(item, SyncSchema.DOWNLOAD, map);
  }

  public void setDoneUpload(long item, @Nullable AttributeMap map, Integer historySteps) {
    setShadow(item, SyncSchema.DONE_UPLOAD, map);
    if (historySteps != null && historySteps <= 0) historySteps = null;
    ((DBWriter) myReader).setValue(item, SyncSchema.DONE_UPLOAD_HISTORY, historySteps);
  }

  public void setUploadTask(long item, @Nullable AttributeMap map) {
    setShadow(item, SyncSchema.UPLOAD_TASK, map);
  }

  private void setShadow(long item, @NotNull DBAttribute<AttributeMap> shadow, @Nullable AttributeMap map) {
    VersionHolder holder = getHolder(item, shadow, false);
    boolean noData = holder == null || EmptyHolder.isEmpty(holder);
    boolean emptyMap = map == null;
    DBWriter writer = (DBWriter) myReader;
    if (noData) {
      if (emptyMap) return;
      holder = new VersionHolder.Shadow(myReader, item, map, shadow);
      writer.setValue(item, shadow, map);
    } else {
      if (emptyMap) {
        holder = new EmptyHolder(myReader, item, shadow);
        writer.setValue(item, shadow, null);
      } else {
        if (holder.getClass() == VersionHolder.Shadow.class) ((VersionHolder.Shadow) holder).setValues(map);
        else if (holder.getClass() == VersionHolder.WriteShadow.class) ((VersionHolder.WriteShadow) holder).setValues(map);
        else Log.error("Unknown holder class " + holder);
        writer.setValue(item, shadow, map);
      }
    }
    addHolder(holder);
  }

  @Nullable
  public AttributeMap getBase(long item) {
    return getShadow(item, SyncSchema.BASE);
  }

  public AttributeMap getConflict(long item) {
    return getShadow(item, SyncSchema.CONFLICT);
  }

  public AttributeMap getUploadTask(long item) {
    return getShadow(item, SyncSchema.UPLOAD_TASK);
  }

  @Nullable
  private AttributeMap getShadow(long item, DBAttribute<AttributeMap> shadow) {
    VersionHolder holder = getHolder(item, shadow, true);
    if (holder == null || EmptyHolder.isEmpty(holder)) return null;
    return holder.getAllShadowableMap();
  }

  public boolean hasBase(long item) {
    return hasShadow(item, SyncSchema.BASE);
  }

  public boolean hasConflict(long item) {
    return hasShadow(item, SyncSchema.CONFLICT);
  }

  public boolean hasUploadTask(long item) {
    return hasShadow(item, SyncSchema.UPLOAD_TASK);
  }

  public boolean hasDoneUpload(long item) {
    return hasShadow(item, SyncSchema.DONE_UPLOAD);
  }

  public boolean hasShadow(long item, DBAttribute<AttributeMap> shadow) {
    VersionHolder holder = getHolder(item, shadow, true);
    return holder != null && !EmptyHolder.isEmpty(holder);
  }

  private static final class EmptyHolder extends VersionHolder {
    private final AttributeInfo myInfo;
    private final DBAttribute<AttributeMap> myShadow;

    public EmptyHolder(DBReader reader, long item, DBAttribute<AttributeMap> shadow) {
      super(item);
      myShadow = shadow;
      myInfo = AttributeInfo.instance(reader);
    }

    @Override
    @NotNull
    public DBReader getReader() {
      return myInfo.getReader();
    }

    @Override
    public <T> T getValue(@NotNull DBAttribute<T> attribute) {
      return myInfo.isWriteThrough(attribute) ? getTrunkValue(attribute) : null;
    }

    @Override
    @NotNull
    public AttributeMap getAllShadowableMap() {
      return getTrunkAllShadowable();
    }

    @Override
    public DBAttribute<AttributeMap> getShadow() {
      return myShadow;
    }

    public static boolean isEmpty(VersionHolder holder) {
      return holder != null && holder.getClass() == EmptyHolder.class;
    }
  }
}
