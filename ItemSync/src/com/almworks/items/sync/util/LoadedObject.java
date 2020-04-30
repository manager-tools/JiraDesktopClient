package com.almworks.items.sync.util;

import com.almworks.integers.LongArray;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBItemType;
import com.almworks.items.api.DBReader;
import com.almworks.items.sync.DBDrain;
import com.almworks.items.sync.ItemVersion;
import com.almworks.items.sync.ItemVersionCreator;
import com.almworks.items.sync.SyncState;
import com.almworks.items.sync.impl.SyncSchema;
import com.almworks.items.util.SyncAttributes;
import org.almworks.util.Collections15;
import org.almworks.util.Util;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * @author dyoma
 */
public abstract class LoadedObject {
  protected final AttributeSet myAttributeSet;
  protected final Object[] myValues;
  protected int myLoadedMask = 0;
  protected Boolean myDeleted = null;

  public LoadedObject(AttributeSet attributes) {
    if (attributes == null) throw new NullPointerException("attributes");
    myAttributeSet = attributes;
    myValues = new Object[attributes.size()];
  }

  public <T> T getValue(DBAttribute<T> attribute) {
    AttrWrapper<T> wrapper = myAttributeSet.getWrapper(attribute);
    if (wrapper == null) return loadValue(attribute);
    Object value = myValues[wrapper.getIndex()];
    if (value != null) return (T) value;
    int attrMask = 1 << wrapper.getIndex();
    if ((myLoadedMask & attrMask) != 0) return wrapper.getDefault();
    T dbValue = loadValue(attribute);
    myValues[wrapper.getIndex()] = dbValue;
    myLoadedMask = myLoadedMask | attrMask;
    return dbValue != null ? dbValue : wrapper.getDefault();
  }

  public boolean isDeleted() {
    if (myDeleted == null) myDeleted = loadInvisible();
    return myDeleted;
  }

  protected abstract Boolean loadInvisible();

  protected abstract <T> T loadValue(DBAttribute<T> attribute);

  public abstract long getItem();

  public abstract ItemVersion forItem(Long item);

  public abstract DBReader getReader();

  public abstract SyncState getSyncState();

  public static void deleteAll(List<Writable> writables) {
    for (Writable writable : writables) writable.delete();
  }

  public static <T> int findBy(int fromIndex, List<? extends LoadedObject> list, DBAttribute<T> attribute, T value) {
    if (fromIndex < 0) fromIndex = -1;
    for (int i = fromIndex + 1; i < list.size(); i++) {
      LoadedObject obj = list.get(i);
      T v = obj.getValue(attribute);
      if (Util.equals(v, value)) return i;
    }
    return -1;
  }

  public static <T extends LoadedObject> List<T> selectNew(List<T> list) {
    List<T> result = Collections15.arrayList();
    for (T obj : list) if (obj.getSyncState() == SyncState.NEW) result.add(obj);
    return result;
  }

  public static class Writable extends LoadedObject {
    private final ItemVersionCreator myCreator;
    @Nullable
    private final ItemVersion myRead;
    private final SyncState mySyncState;

    public Writable(AttributeSet attributes, ItemVersionCreator creator, @Nullable ItemVersion read,
      SyncState syncState)
    {
      super(attributes);
      if (creator == null) throw new NullPointerException("Creator");
      myCreator = creator;
      myRead = read != null && read != creator ? read : null;
      mySyncState = syncState;
    }

    public <T> void setValueIfNotNull(DBAttribute<T> attribute, T value) {
      if (value != null) setValue(attribute, value);
    }

    public <T> void setValue(DBAttribute<T> attribute, T value) {
      AttrWrapper<T> wrapper = myAttributeSet.getWrapper(attribute);
      if (wrapper == null) {
        myCreator.setValue(attribute, value);
        return;
      }
      value = wrapper.correctWriteValue(value);
      int attrMask = 1 << wrapper.getIndex();
      if (myRead == null && (myLoadedMask & attrMask) != 0) {
        T current = (T) myValues[wrapper.getIndex()];
        if (Util.equals(current, value)) return;
      }
      myCreator.setValue(attribute, value);
      myValues[wrapper.getIndex()] = value;
      myLoadedMask = myLoadedMask | attrMask;
    }

    public void delete() {
      if (myDeleted != null && myDeleted) return;
      myCreator.delete();
      myDeleted = true;
    }

    public void setAlive() {
      myCreator.setAlive();
      myDeleted = false;
    }

    public DBDrain getDrain() {
      return myCreator;
    }

    protected <T> T loadValue(DBAttribute<T> attribute) {
      T value = myCreator.getValue(attribute);
      if (value == null && myRead != null) value = myRead.getValue(attribute);
      return value;
    }

    public long getItem() {
      return myCreator.getItem();
    }

    public ItemVersion forItem(Long item) {
      return myCreator.forItem(item);
    }

    @Override
    public DBReader getReader() {
      return myCreator.getReader();
    }

    public SyncState getSyncState() {
      return mySyncState;
    }

    protected Boolean loadInvisible() {
      return myCreator.getNNValue(SyncSchema.INVISIBLE, false);
    }
  }

  private static class ReadOnly extends LoadedObject {
    private final ItemVersion myItem;
    private SyncState mySyncState = null;

    private ReadOnly(AttributeSet attributes, ItemVersion item) {
      super(attributes);
      myItem = item;
    }

    @Override
    protected Boolean loadInvisible() {
      return myItem.getNNValue(SyncSchema.INVISIBLE, false);
    }

    @Override
    protected <T> T loadValue(DBAttribute<T> attribute) {
      return myItem.getValue(attribute);
    }

    @Override
    public long getItem() {
      return myItem.getItem();
    }

    @Override
    public ItemVersion forItem(Long item) {
      return myItem.forItem(item);
    }

    @Override
    public DBReader getReader() {
      return myItem.getReader();
    }

    @Override
    public SyncState getSyncState() {
      if (mySyncState == null) mySyncState = myItem.getSyncState();
      return mySyncState;
    }
  }

  private static abstract class AttrWrapper<T> {
    private final DBAttribute<T> myAttribute;
    private final T myDefaultRead;
    private final int myIndex;

    protected AttrWrapper(DBAttribute<T> attribute, T defaultRead, int index) {
      myAttribute = attribute;
      myDefaultRead = defaultRead;
      myIndex = index;
    }

    public final boolean equalAttribute(DBAttribute<?> attribute) {
      return Util.equals(myAttribute, attribute);
    }

    public final int getIndex() {
      return myIndex;
    }

    public final T getDefault() {
      return myDefaultRead;
    }

    public abstract T correctWriteValue(T value);
  }

  private static class SimpleWrapper<T> extends AttrWrapper<T> {
    private SimpleWrapper(DBAttribute<T> attribute, T defaultRead, int index) {
      super(attribute, defaultRead, index);
    }

    @Override
    public T correctWriteValue(T value) {
      return value;
    }
  }

  private static class ItemAttrWrapper extends AttrWrapper<Long> {
    protected ItemAttrWrapper(DBAttribute<Long> longDBAttribute, Long defaultRead, int index) {
      super(longDBAttribute, defaultRead, index);
    }

    @Override
    public Long correctWriteValue(Long value) {
      return value == null || value <= 0 ? null : value;
    }
  }

  public static class AttributeSet {
    private final List<AttrWrapper<?>> myWrappers = Collections15.arrayList();
    private volatile boolean myFixed = false;
    private final DBItemType myType;
    private final DBAttribute<Long> myMasterRef;

    public AttributeSet(DBItemType type, DBAttribute<Long> masterRef) {
      myType = type;
      myMasterRef = masterRef;
    }

    public int size() {
      return myWrappers.size();
    }

    public <T> AttributeSet addAttribute(DBAttribute<T> attribute, T defaultValue) {
      return addWrapper(new SimpleWrapper<T>(attribute, defaultValue, myWrappers.size()));
    }

    private AttributeSet addWrapper(AttrWrapper<?> wrapper) {
      if (myFixed) throw new IllegalStateException();
      myWrappers.add(wrapper);
      return this;
    }

    public AttributeSet addItemReference(DBAttribute<Long> attribute) {
      return addWrapper(new ItemAttrWrapper(attribute, -1l, myWrappers.size()));
    }

    public AttributeSet fix() {
      myFixed = true;
      return this;
    }

    @SuppressWarnings({"unchecked"})
    @Nullable
    public <T> AttrWrapper<T> getWrapper(DBAttribute<T> attribute) {
      for (int i = 0; i < myWrappers.size(); i++) {
        AttrWrapper<?> attrWrapper =  myWrappers.get(i);
        if (attrWrapper.equalAttribute(attribute)) {
          assert attrWrapper.myIndex == i;
          return (AttrWrapper<T>) attrWrapper;
        }
      }
      return null;
    }

    public Writable changeServer(ItemVersionCreator creator) {
      SyncState state = creator.getSyncState();
      ItemVersion read = state == SyncState.NEW ? creator.switchToTrunk() : creator;
      return new Writable(this, creator, read, state);
    }

    public List<Writable> changeServerSlaves(ItemVersionCreator master) {
      LongArray slaves = master.getSlaves(myMasterRef);
      if (slaves.isEmpty()) return Collections.emptyList();
      List<Writable> result = Collections15.arrayList();
      for (ItemVersionCreator slave : master.changeItems(slaves)) result.add(changeServer(slave));
      return result;
    }

    public Writable newSlave(ItemVersionCreator master) {
      ItemVersionCreator creator = master.createItem();
      creator.setValue(SyncAttributes.CONNECTION, master.getValue(SyncAttributes.CONNECTION));
      creator.setValue(DBAttribute.TYPE, myType);
      creator.setValue(myMasterRef, master);
      return new Writable(this, creator, creator, SyncState.SYNC);
    }

    public LoadedObject readTrunk(DBReader reader, long item) {
      return new ReadOnly(this, SyncUtils.readTrunk(reader, item));
    }

    public List<LoadedObject> slavesSnapshot(ItemVersion master, boolean includeDeleted) {
      LongArray slaves = master.getSlaves(myMasterRef);
      if (slaves.isEmpty()) return Collections.emptyList();
      List<LoadedObject> result = Collections15.arrayList(slaves.size());
      for (ItemVersion slave : master.readItems(slaves)) {
        ReadOnly loaded = new ReadOnly(this, slave);
        if (!includeDeleted && loaded.isDeleted()) continue;
        result.add(loaded);
      }
      return result;
    }
  }
}
