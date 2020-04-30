package com.almworks.restconnector.jql;

import com.almworks.api.constraint.Constraint;
import org.jetbrains.annotations.Nullable;

public interface JQLConstraint extends Constraint {

  void appendTo(StringBuilder builder);

  String getFieldId();

  @Nullable
  String getDisplayName();
}
