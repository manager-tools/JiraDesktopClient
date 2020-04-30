package com.almworks.items.gui.meta.schema.constraints;

import com.almworks.api.application.qb.AttributeConstraintDescriptor;
import com.almworks.api.application.qb.ConstraintDescriptor;
import com.almworks.explorer.qbuilder.filter.DateAttribute;
import com.almworks.explorer.qbuilder.filter.NumericAttribute;
import com.almworks.explorer.qbuilder.filter.TextAttribute;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.gui.meta.EnumTypesCollector;
import com.almworks.util.LogHelper;
import com.almworks.util.collections.Modifiable;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Date;

class ScalarConstraint implements ConstraintKind {
  public static final ConstraintKind INSTANCE = new ScalarConstraint();

  private ScalarConstraint() {
  }

  @Override
  public ConstraintDescriptor createDescriptor(DBAttribute<?> attribute, String displayName, String id, EnumTypesCollector.Loaded type, @Nullable Icon icon) {
    LogHelper.assertError(attribute.getComposition() == DBAttribute.ScalarComposition.SCALAR, "Scalar attribute expected:", attribute);

    final Class<?> aClass = attribute.getScalarClass();

    if(String.class == aClass) {
      return new AttributeConstraintDescriptor<String>(
        TextAttribute.INSTANCE, displayName, id, (DBAttribute<String>)attribute, Modifiable.NEVER);
    }

    if(Date.class == aClass) {
      return new AttributeConstraintDescriptor<Date>(
        DateAttribute.DATE_TIME, displayName, id, (DBAttribute<Date>)attribute, Modifiable.NEVER);
    }

    if(Number.class.isAssignableFrom(aClass) && Comparable.class.isAssignableFrom(aClass)) {
      return new AttributeConstraintDescriptor<Number>(
        NumericAttribute.INSTANCE, displayName, id, (DBAttribute<Number>)attribute, Modifiable.NEVER);
    }

    LogHelper.error("Unknown value class", aClass, attribute);
    return null;
  }
}
