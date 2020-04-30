package com.almworks.items.gui.meta.schema.enums;

import com.almworks.api.application.ItemOrder;
import com.almworks.integers.LongList;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBReader;
import com.almworks.items.gui.meta.commons.SerializableFeature;
import com.almworks.items.util.BadUtil;
import com.almworks.util.LogHelper;
import com.almworks.util.collections.ByteArray;
import com.almworks.util.commons.Procedure;
import org.almworks.util.Util;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;

public abstract class OrderKind {
  public static final OrderKind BY_DISPLAY_NAME = new OrderKind() {
    @Override
    public ItemOrder create(DBReader reader, long item, String displayName) {
      return ItemOrder.byString(displayName);
    }

    @Override
    public Collection<? extends DBAttribute<?>> getAttributes() {
      return null;
    }
  };

  public static final OrderKind NO_ORDER = new OrderKind() {
    @Override
    public ItemOrder create(DBReader reader, long item, String displayName) {
      return ItemOrder.NO_ORDER;
    }

    @Override
    public Collection<? extends DBAttribute<?>> getAttributes() {
      return null;
    }
  };

  static final SerializableFeature<OrderKind> READ_BY_DISPLAY_NAME =
    SerializableFeature.NoParameters.create(BY_DISPLAY_NAME, OrderKind.class);
  static final SerializableFeature<OrderKind> READ_NO_ORDER =
    SerializableFeature.NoParameters.create(NO_ORDER, OrderKind.class);

  static final SerializableFeature<OrderKind> READ_BY_NUMBER_ORDER =
    new SerializableFeature<OrderKind>() {
      @Override
      public OrderKind restore(DBReader reader, ByteArray.Stream stream, @Nullable Procedure<LongList> invalidate) {
        long attributeItem = stream.nextLong();
        boolean direct = stream.nextBoolean();
        if (attributeItem <= 0 || ! stream.isSuccessfullyAtEnd()) {
          LogHelper.error("Failed to load attribute", stream);
          return BY_DISPLAY_NAME;
        }
        final DBAttribute<? extends Number> attribute = BadUtil.getScalarAttribute(reader, attributeItem, Number.class);
        return attribute != null ? new ByIntValue(attribute, direct) : null;
      }

      @Override
      public Class<OrderKind> getValueClass() {
        return OrderKind.class;
      }
    };

  public abstract ItemOrder create(DBReader reader, long item, String displayName);

  @Nullable
  public abstract Collection<? extends DBAttribute<?>> getAttributes();

  private static class ByIntValue extends OrderKind {
    private final DBAttribute<? extends Number> myAttribute;
    private final boolean myDirect;

    public ByIntValue(DBAttribute<? extends Number> attribute, boolean direct) {
      myAttribute = attribute;
      myDirect = direct;
    }

    @Override
    public ItemOrder create(DBReader reader, long item, String displayName) {
      Number order = myAttribute.getValue(item, reader);
      long longOrder = order != null ? order.longValue() : 0;
      if (!myDirect) longOrder = -longOrder;
      return ItemOrder.byOrderAndString(longOrder, displayName);
    }

    @Override
    public Collection<? extends DBAttribute<?>> getAttributes() {
      return Collections.singleton(myAttribute);
    }

    @Override
    public int hashCode() {
      return ByIntValue.class.hashCode() ^ Util.hashCode(myAttribute);
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      ByIntValue other = Util.castNullable(ByIntValue.class, obj);
      return other != null && Util.equals(myAttribute, other.myAttribute);
    }
  }
}
