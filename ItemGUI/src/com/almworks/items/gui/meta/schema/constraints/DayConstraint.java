package com.almworks.items.gui.meta.schema.constraints;

import com.almworks.api.application.qb.AttributeConstraintDescriptor;
import com.almworks.api.application.qb.ConstraintDescriptor;
import com.almworks.explorer.qbuilder.filter.DateAttribute;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.gui.meta.EnumTypesCollector;
import com.almworks.util.LogHelper;
import com.almworks.util.collections.Modifiable;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class DayConstraint implements ConstraintKind {
  @Override
  public ConstraintDescriptor createDescriptor(DBAttribute<?> attribute, String displayName, String id, EnumTypesCollector.Loaded type, @Nullable Icon icon) {
    boolean isScalar = attribute.getComposition() == DBAttribute.ScalarComposition.SCALAR;
    LogHelper.assertError(isScalar, "Scalar attribute expected:", attribute.getComposition(), attribute);
    boolean isInt = attribute.getScalarClass() == Integer.class;
    LogHelper.assertError(isInt, "Integer attribute expected:", attribute.getScalarClass(), attribute);
    if (!(isScalar && isInt)) return null;
    return new AttributeConstraintDescriptor(DateAttribute.DAY, displayName, id, attribute, Modifiable.NEVER);
  }
}
