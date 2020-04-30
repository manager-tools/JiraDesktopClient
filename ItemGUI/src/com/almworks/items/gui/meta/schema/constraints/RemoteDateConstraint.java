package com.almworks.items.gui.meta.schema.constraints;

import com.almworks.api.application.qb.ConstraintDescriptor;
import com.almworks.explorer.qbuilder.filter.DateConstraintDescriptor;
import com.almworks.integers.LongList;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBReader;
import com.almworks.items.gui.meta.EnumTypesCollector;
import com.almworks.items.gui.meta.commons.SerializableFeature;
import com.almworks.items.util.BadUtil;
import com.almworks.util.LogHelper;
import com.almworks.util.collections.ByteArray;
import com.almworks.util.commons.Procedure;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Date;

class RemoteDateConstraint implements ConstraintKind {
  private static final ConstraintKind ACCEPT_NULL = new RemoteDateConstraint(true);
  private static final ConstraintKind NOT_ACCEPT_NULL = new RemoteDateConstraint(false);
  public static final SerializableFeature<ConstraintKind> FEATURE = new SerializableFeature<ConstraintKind>() {
    @Override
    public ConstraintKind restore(DBReader reader, ByteArray.Stream stream, @Nullable Procedure<LongList> invalidate) {
      boolean acceptNull = stream.nextBoolean();
      if (!stream.isSuccessfullyAtEnd()) return null;
      return acceptNull ? ACCEPT_NULL : NOT_ACCEPT_NULL;
    }

    @Override
    public Class<ConstraintKind> getValueClass() {
      return ConstraintKind.class;
    }
  };
  private final boolean myAcceptNullLaterThan;

  private RemoteDateConstraint(boolean acceptNullLaterThan) {
    myAcceptNullLaterThan = acceptNullLaterThan;
  }

  @Override
  public ConstraintDescriptor createDescriptor(DBAttribute<?> a, String displayName, String id, EnumTypesCollector.Loaded type, @Nullable Icon icon) {
    DBAttribute<Date> attribute = BadUtil.castScalar(Date.class, a);
    if (attribute == null) {
      LogHelper.error("Wrong attribute", a);
      return null;
    }
    return new DateConstraintDescriptor(displayName, id, attribute, myAcceptNullLaterThan);
  }
}
