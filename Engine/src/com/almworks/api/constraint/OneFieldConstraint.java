package com.almworks.api.constraint;

import com.almworks.items.api.DBAttribute;
import org.almworks.util.TypedKey;

/**
 * @author dyoma
 */
public interface OneFieldConstraint extends Constraint {
  TypedKey<? extends OneFieldConstraint> getType();

  /**
   * @return valid constraint returns not null
   */
  DBAttribute getAttribute();
}
