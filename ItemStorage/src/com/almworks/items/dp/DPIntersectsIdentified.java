package com.almworks.items.dp;

import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBIdentifiedObject;
import com.almworks.items.api.DBReader;
import com.almworks.items.api.DP;
import com.almworks.util.bool.BoolExpr;
import org.almworks.util.Collections15;

import java.util.Collection;
import java.util.Set;

public class DPIntersectsIdentified extends DPAttribute<Collection<Long>> {
  private final Set<DBIdentifiedObject> myValues;

  private DPIntersectsIdentified(DBAttribute<Collection<Long>> attribute, Set<DBIdentifiedObject> values) {
    super(attribute);
    myValues = values;
  }

  public Set<DBIdentifiedObject> getValues() {
    return myValues;
  }

  public static BoolExpr<DP> create(DBAttribute<? extends Collection<Long>> attribute, Collection<? extends DBIdentifiedObject> value) {
    return new DPIntersectsIdentified((DBAttribute<Collection<Long>>) attribute, Collections15.hashSet(value)).term();
  }

  @Override
  protected boolean acceptValue(Collection<Long> value, DBReader reader) {
    if (value == null || value.isEmpty()) return false;
    for (DBIdentifiedObject v : myValues) {
      long a = reader.findMaterialized(v);
      if (a == 0) continue;
      if (value.contains(a)) return true;
    }
    return false;
  }

  @Override
  public String toString() {
    return getAttribute() + " X " + myValues;
  }

  @Override
  protected boolean equalDPA(DPAttribute other) {
    return myValues.equals(((DPIntersectsIdentified) other).myValues);
  }

  @Override
  protected int hashCodeDPA() {
    return myValues.hashCode();
  }
}