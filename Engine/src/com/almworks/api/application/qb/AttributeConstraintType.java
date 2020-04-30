package com.almworks.api.application.qb;

import com.almworks.api.constraint.Constraint;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DP;
import com.almworks.util.bool.BoolExpr;
import com.almworks.util.properties.PropertyMap;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public interface AttributeConstraintType<V> extends ConstraintType {
  @Nullable
  BoolExpr<DP> createFilter(DBAttribute<V> attribute, PropertyMap data);

  @Nullable
  Constraint createConstraint(DBAttribute<V> attribute, PropertyMap data);

  boolean isSameData(PropertyMap data1, PropertyMap data2);

  Icon getDescriptorIcon();
}
