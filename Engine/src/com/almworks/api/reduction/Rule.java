package com.almworks.api.reduction;

import org.jetbrains.annotations.Nullable;


public interface Rule {
  /**
   * @return result of rule or null if rule isn't applicable. Not null means that rule was succesfuly applied.
   */
  @Nullable
  ConstraintTreeElement process(ConstraintTreeElement element);
}
