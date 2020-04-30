package com.almworks.api.constraint;

import com.almworks.items.api.DBAttribute;
import org.almworks.util.TypedKey;

public interface IsEmptyConstraint extends OneFieldConstraint {
  TypedKey<IsEmptyConstraint> IS_EMPTY = TypedKey.create("isEmpty");

  TypedKey<? extends IsEmptyConstraint> getType();

  class Simple implements IsEmptyConstraint {
    private final TypedKey<? extends IsEmptyConstraint> myType;
    private final DBAttribute myAttribute;

    Simple(TypedKey<? extends IsEmptyConstraint> type, DBAttribute attribute) {
      myType = type;
      myAttribute = attribute;
    }

    @Override
    public TypedKey<? extends IsEmptyConstraint> getType() {
      return myType;
    }

    @Override
    public DBAttribute getAttribute() {
      return myAttribute;
    }

    public static Simple isEmpty(DBAttribute attribute) {
      return new Simple(IS_EMPTY, attribute);
    }
  }
}
