package com.almworks.api.application;

import com.almworks.api.engine.Connection;
import com.almworks.api.engine.Engine;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.sync.ItemVersion;
import com.almworks.items.util.SyncAttributes;
import com.almworks.util.collections.Equality;
import com.almworks.util.commons.Condition;
import com.almworks.util.exec.Context;
import org.almworks.util.Log;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Comparator;

/**
 * @author : Dyoma
 */
public class ResolvedItem extends ItemKey {
  // > 0
  private final long myItem;
  @NotNull
  private final String myRepresentation;
  @NotNull
  private final ItemOrder myOrder;
  @NotNull
  private final String myId;
  // can be <= 0
  private final long myConnectionItem;
  @Nullable
  private final Icon myIcon;
  // order of enum elements
  public static final DBAttribute<Integer> attrOrder = Engine.NS.integer("order", "Enum Order", false);

  public static final Equality<ResolvedItem> BY_RESOLVED_ITEM = new Equality<ResolvedItem>() {
    @Override
    public boolean areEqual(ResolvedItem o1, ResolvedItem o2) {
      if(o1 == o2) {
        return true;
      }
      if(o1 == null || o2 == null) {
        return false;
      }
      if(o1.getResolvedItem() != o2.getResolvedItem()) {
        // not null
        return false;
      }
      if(o1.getResolvedItem() == NOT_RESOLVED_LONG) {
        // if both are non-resolved, fall back to usual equality
        return o1.equals(o2);
      }
      return true;
    }
  };

  @SuppressWarnings({"ConstantConditions"})
  public ResolvedItem(long item, @NotNull String representation, ItemOrder order, @Nullable String uniqueKey, @Nullable Icon icon, long connectionItem) {
    assert item > 0L;
    assert representation != null;
    myItem = item;
    String dn = hackFixHtmlValueName(representation);
    myRepresentation = dn == null ? dn : dn.intern();
    myId = uniqueKey == null ? (representation == null ? representation : representation.intern()) : uniqueKey.intern();
    myOrder = ItemOrder.adjustString(order, myRepresentation);
    myIcon = icon;
    myConnectionItem = connectionItem;
  }

  @NotNull
  public String getId() {
    return myId;
  }

  @NotNull
  public String getDisplayName() {
    return myRepresentation;
  }

  @NotNull
  public ItemOrder getOrder() {
    return myOrder;
  }

  @Nullable
  public Icon getIcon() {
    return myIcon;
  }

  public long getResolvedItem() {
    return myItem;
  }

  public long getConnectionItem() {
    return myConnectionItem;
  }

  public static ResolvedItem create(ItemVersion item, String text) {
    return create(item, text, null);
  }

  public static ResolvedItem create(ItemVersion item, @NotNull String displayName, @Nullable String uniqueKey) {
    Integer intOrder = item.getValue(attrOrder);
    ItemOrder order = intOrder != null ? ItemOrder.byOrder(intOrder) : null;
    return new ResolvedItem(item.getItem(), displayName, order, uniqueKey, null, item.getNNValue(SyncAttributes.CONNECTION, 0L));
  }

  public static Comparator<ResolvedItem> comparator() {
    return ItemKey.keyComparator();
  }

  public boolean isSame(ResolvedItem that) {
    if (this == that)
      return true;
    if (that == null || getClass() != that.getClass())
      return false;
    if (!super.equals(that))
      return false;

    if (myItem != that.myItem)
      return false;
    if (myConnectionItem != that.myConnectionItem)
      return false;
    if (!myId.equals(that.myId))
      return false;
    if (!myOrder.equals(that.myOrder))
      return false;
    if (!myRepresentation.equals(that.myRepresentation))
      return false;

    return true;
  }

  @Nullable
  public <T extends Connection> T getConnection(Class<T> clazz) {
    long connectionItem = getConnectionItem();
    if (connectionItem <= 0)
      return null;
    Engine engine = Context.require(Engine.class);
    Connection c = engine.getConnectionManager().findByItem(connectionItem);
    if (c == null) {
      Log.warn("no connection for " + this + ", probably it is disabled?");
      return null;
    }
    if (!clazz.isInstance(c)) {
      assert false : c + " " + clazz;
      Log.warn("wrong C " + c + " " + clazz);
      return null;
    }
    return (T)c;
  }

  public static class SelectConnection extends Condition<ResolvedItem> {
    private final long myConnection;

    public SelectConnection(long connection) {
      myConnection = connection;
    }

    @Override
    public boolean isAccepted(ResolvedItem value) {
      return value != null && value.myConnectionItem == myConnection;
    }
  }
}
