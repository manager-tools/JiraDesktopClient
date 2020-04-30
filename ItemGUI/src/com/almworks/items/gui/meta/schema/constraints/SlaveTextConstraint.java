package com.almworks.items.gui.meta.schema.constraints;

import com.almworks.api.application.qb.CommentsDescriptor;
import com.almworks.api.application.qb.ConstraintDescriptor;
import com.almworks.integers.LongList;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBReader;
import com.almworks.items.gui.meta.EnumTypesCollector;
import com.almworks.items.gui.meta.commons.SerializableFeature;
import com.almworks.items.util.BadUtil;
import com.almworks.util.LogHelper;
import com.almworks.util.collections.ByteArray;
import com.almworks.util.commons.Procedure;
import org.almworks.util.Util;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

class SlaveTextConstraint implements ConstraintKind {
  public static final SerializableFeature<ConstraintKind> FEATURE = new SerializableFeature<ConstraintKind>() {
    @Override
    public ConstraintKind restore(DBReader reader, ByteArray.Stream stream, @Nullable Procedure<LongList> invalidate) {
      DBAttribute<Long> master = BadUtil.getScalarAttribute(reader, stream.nextLong(), Long.class);
      if (master == null || !stream.isSuccessfullyAtEnd()) {
        LogHelper.error("Failed to load", stream);
        return null;
      }
      return new SlaveTextConstraint(master);
    }

    @Override
    public Class<ConstraintKind> getValueClass() {
      return ConstraintKind.class;
    }
  };

  private final DBAttribute<Long> myMaster;

  private SlaveTextConstraint(DBAttribute<Long> master) {
    myMaster = master;
  }

  @Override
  public ConstraintDescriptor createDescriptor(DBAttribute<?> a, String displayName, String id, EnumTypesCollector.Loaded type, @Nullable Icon icon) {
    DBAttribute<String> attribute = BadUtil.castScalar(String.class, a);
    if (attribute == null) LogHelper.error("Wrong attribute", a);
    else return new CommentsDescriptor(myMaster, displayName, id, attribute);
    return null;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    SlaveTextConstraint other = Util.castNullable(SlaveTextConstraint.class, obj);
    return other != null && Util.equals(myMaster, other.myMaster);
  }

  @Override
  public int hashCode() {
    return myMaster.hashCode();
  }
}
