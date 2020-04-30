package com.almworks.items.gui.meta;

import com.almworks.api.application.BadItemException;
import com.almworks.api.application.ItemKeyCache;
import com.almworks.api.application.ItemOrder;
import com.almworks.api.application.ResolvedFactory;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBItemType;
import com.almworks.items.api.DBReader;
import com.almworks.items.gui.meta.schema.enums.IconLoader;
import com.almworks.items.gui.meta.schema.enums.OrderKind;
import com.almworks.items.sync.util.SyncUtils;
import com.almworks.items.util.BadUtil;
import com.almworks.items.util.SyncAttributes;
import com.almworks.util.LogHelper;
import gnu.trove.TLongObjectHashMap;
import org.almworks.util.Collections15;
import org.almworks.util.Util;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

class ItemKeyDescriptor {
  private final DBAttribute<?> myUniqueKey;
  private final IconLoader myIconLoader;
  private final ItemKeyDisplayName myRendererKind;
  private final OrderKind myOrderKind;
  private final List<ItemKeySubloader> mySubloaders;

  ItemKeyDescriptor(DBAttribute<?> uniqueKey, IconLoader iconLoader, ItemKeyDisplayName rendererKind, OrderKind orderKind, @Nullable List<ItemKeySubloader> subloaders) {
    myUniqueKey = uniqueKey;
    myIconLoader = iconLoader;
    myRendererKind = rendererKind;
    myOrderKind = orderKind;
    mySubloaders = Util.NN(subloaders, Collections.<ItemKeySubloader>emptyList());
  }

  public ResolvedFactory<LoadedItemKey> createFactory(Collection<? extends DBAttribute<?>> additional) {
    Collection<ItemKeySubloader> fullAdditional = Collections15.hashSet(mySubloaders);
    if (additional != null) {
      for(DBAttribute<?> attribute : additional) {
        fullAdditional.add(new ItemKeySubloader.Attribute(attribute));
      }
    }
    for (DBAttribute<?> attribute : Util.NN(myOrderKind.getAttributes(), Collections.<DBAttribute<?>>emptySet())) fullAdditional.add(new ItemKeySubloader.Attribute(attribute));
    final ItemKeySubloader[] subloaders;
    if(fullAdditional.isEmpty()) {
      subloaders = null;
    } else {
      subloaders = fullAdditional.toArray(new ItemKeySubloader[fullAdditional.size()]);
    }

    return new ResolvedFactory<LoadedItemKey>() {
      @Override
      public LoadedItemKey createResolvedItem(long item, DBReader reader, ItemKeyCache cache) throws BadItemException {
        String displayName = myRendererKind.getDisplayName(reader, item);
        if (displayName == null) {
          LogHelper.error("Missing display name", SyncUtils.readTrunk(reader, item), myRendererKind);
          displayName = "";
        }
        Object key = reader.getValue(item, myUniqueKey);
        if(key == null) {
          LogHelper.warning("Missing unique key for", item, reader.getAttributeMap(item), displayName, myUniqueKey);
          key = displayName;
        }

        Long connection = reader.getValue(item, SyncAttributes.CONNECTION);
        if(connection == null || connection <= 0L) {
          String id = reader.getValue(item, DBAttribute.ID);
          LogHelper.assertError(id != null, "Missing connection for", item, displayName, key); // No connection is allowed for DB singletons
          connection = 0L;
        }

        final ItemOrder order = getOrder(reader, item, displayName);
        final Object[] keys;
        final Object[] values;
        if(subloaders == null) {
          values = keys = null;
        } else {
          values = new Object[subloaders.length];
          keys = new Object[subloaders.length];
          int i = 0;
          for(final ItemKeySubloader s : subloaders) {
            if(s != null) {
              keys[i] = s.getKey();
              values[i++] = s.getValue(item, reader, cache, this);
            }
          }
        }

        Icon icon = myIconLoader.loadIcon(reader, item);
        DBItemType type = getItemType(reader, item);
        return new LoadedItemKey(item, type, displayName, order, String.valueOf(key), icon, connection, keys, values);
      }
    };
  }

  private final TLongObjectHashMap<DBItemType> myKnownTypes = new TLongObjectHashMap<>();
  private DBItemType getItemType(DBReader reader, long item) {
    Long typeItem = reader.getValue(item, DBAttribute.TYPE);
    if (typeItem == null) return null;
    synchronized (myKnownTypes) {
      if (myKnownTypes.containsKey(typeItem)) return myKnownTypes.get(typeItem);
    }
    DBItemType type = BadUtil.getItemType(reader, typeItem);
    if (type == null) return null;
    synchronized (myKnownTypes) {
      if (!myKnownTypes.containsKey(typeItem)) myKnownTypes.put(typeItem, type);
    }
    return type;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) return true;
    ItemKeyDescriptor other = Util.castNullable(ItemKeyDescriptor.class, obj);
    return other != null
      && Util.equals(myUniqueKey, other.myUniqueKey)
      && Util.equals(myRendererKind, other.myRendererKind)
      && Util.equals(myOrderKind, other.myOrderKind)
      && Util.equals(mySubloaders, other.mySubloaders);
  }

  @Override
  public int hashCode() {
    return ItemKeyDescriptor.class.hashCode()
      ^ Util.hashCode(myUniqueKey) ^ Util.hashCode(myRendererKind)
      ^ Util.hashCode(myOrderKind) ^ Util.hashCode(mySubloaders);
  }

  private ItemOrder getOrder(DBReader reader, long item, String displayName) {
    return myOrderKind.create(reader, item, displayName);
  }
}
