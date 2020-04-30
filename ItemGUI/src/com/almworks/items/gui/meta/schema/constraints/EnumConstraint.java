package com.almworks.items.gui.meta.schema.constraints;

import com.almworks.api.application.ItemKey;
import com.almworks.api.application.ItemKeyStub;
import com.almworks.api.application.ItemOrder;
import com.almworks.api.application.qb.ConstraintDescriptor;
import com.almworks.api.application.qb.EnumGrouping;
import com.almworks.explorer.qbuilder.filter.BaseEnumConstraintDescriptor;
import com.almworks.explorer.qbuilder.filter.EnumConstraintKind;
import com.almworks.integers.LongList;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBReader;
import com.almworks.items.gui.meta.EnumTypesCollector;
import com.almworks.items.gui.meta.commons.SerializableFeature;
import com.almworks.util.LogHelper;
import com.almworks.util.collections.ByteArray;
import com.almworks.util.collections.Convertor;
import com.almworks.util.commons.Procedure;
import org.almworks.util.Util;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.List;

class EnumConstraint implements ConstraintKind {
  public static final ConstraintKind NO_NULL = new EnumConstraint(null);
  public static final SerializableFeature<ConstraintKind> NULLABLE = new SerializableFeature<ConstraintKind>() {
    @Override
    public ConstraintKind restore(DBReader reader, ByteArray.Stream stream, @Nullable Procedure<LongList> invalidate) {
      String id = stream.nextUTF8();
      String displayName = stream.nextUTF8();
      if (!stream.isSuccessfullyAtEnd() || id == null || displayName == null) {
        LogHelper.error("Failed to read", id, displayName, stream);
        return null;
      }
      return new EnumConstraint(new ItemKeyStub(id, displayName, ItemOrder.byOrder(-1)));
    }

    @Override
    public Class<ConstraintKind> getValueClass() {
      return ConstraintKind.class;
    }
  };
  private final ItemKey myNotSet;

  EnumConstraint(ItemKey notSet) {
    myNotSet = notSet;
  }

  @Override
  public ConstraintDescriptor createDescriptor(DBAttribute<?> attribute, String displayName, String id, EnumTypesCollector.Loaded type, @Nullable Icon icon) {
    LogHelper.assertError(attribute.getScalarClass() == Long.class, "Attribute of type Long expected:", attribute);
    LogHelper.assertError(type != null, "Missing enum type:", attribute);
    if (type == null) return null;

    final EnumConstraintKind kind =
      attribute.getComposition() == DBAttribute.ScalarComposition.SCALAR ?
        EnumConstraintKind.INCLUSION : EnumConstraintKind.INTERSECTION;

    final List<EnumGrouping> grouping = null;
    final Convertor<ItemKey, String> filterConvertor = null;

    BaseEnumConstraintDescriptor descriptor = type.createEnumDescriptor(attribute, displayName, myNotSet, kind, grouping, filterConvertor, id);
    if (icon != null) descriptor.setIcon(icon);
    return descriptor;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    EnumConstraint other = Util.castNullable(EnumConstraint.class, obj);
    if (other == null) return false;
    return Util.equals(myNotSet, other.myNotSet);
  }

  @Override
  public int hashCode() {
    return Util.hashCode(myNotSet) ^ EnumConstraint.class.hashCode();
  }
}
