package com.almworks.api.constraint;

import com.almworks.items.api.DBAttribute;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

/**
 * @author dyoma
 */
public interface FieldSubsetConstraint extends OneFieldConstraint {
  TypedKey<FieldSubsetConstraint> INTERSECTION = TypedKey.create("intersection");

  TypedKey<? extends FieldSubsetConstraint> getType();

  @NotNull
  Set<Long> getSubset();

  class Simple implements FieldSubsetConstraint {
    private final DBAttribute myAttribute;
    private final Set<Long> mySubset;

    private Simple(DBAttribute attribute, Collection<Long> subset) {
      myAttribute = attribute;
      mySubset = Collections.unmodifiableSet(Collections15.hashSet(subset));
    }

    public TypedKey<? extends FieldSubsetConstraint> getType() {
      return INTERSECTION;
    }

    @NotNull
    public Set<Long> getSubset() {
      return mySubset;
    }

    public DBAttribute getAttribute() {
      return myAttribute;
    }

    public String toString() {
      StringBuilder b = new StringBuilder();
      b.append(myAttribute.getId());
      b.append(" in {");
      String prefix = "";
      for (Long pointer : mySubset) {
        b.append(prefix).append(pointer);
        prefix = " ";
      }
      b.append("}");
      return b.toString();
    }

    public static FieldSubsetConstraint intersection(DBAttribute attribute, Collection<Long> subset) {
      return new Simple(attribute, subset);
    }
  }
}
