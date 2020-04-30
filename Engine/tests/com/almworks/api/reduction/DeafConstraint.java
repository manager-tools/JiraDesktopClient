package com.almworks.api.reduction;

import com.almworks.api.constraint.Constraint;
import org.almworks.util.TypedKey;

/**
 * @author dyoma
 */
public class DeafConstraint implements Constraint {
  private TypedKey<? extends Constraint> myType;

  public DeafConstraint(String name) {
    myType = TypedKey.create(name);
  }

  public TypedKey<? extends Constraint> getType() {
    return myType;
  }

  public String toString() {
    return myType.getName();
  }

  public boolean equals(Object obj) {
    if (!(obj instanceof DeafConstraint))
      return false;
    return myType.getName().equals(((DeafConstraint) obj).myType.getName());
  }

  public int hashCode() {
    return myType.getName().hashCode();
  }
}
