package com.almworks.items.dp;

import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBIdentifiedObject;
import com.almworks.items.api.DBReader;
import com.almworks.items.api.DP;
import org.almworks.util.Util;

import java.util.Collection;

public class DPResolvesTo extends DP {
  private final DBIdentifiedObject myObject;

  public DPResolvesTo(DBIdentifiedObject object) {
    myObject = object;
  }

  @Override
  public boolean accept(long item, DBReader reader) {
    return item != 0 && item == reader.findMaterialized(myObject);
  }

  @Override
  public boolean addAffectingAttributes(Collection<? super DBAttribute> target) {
    target.add(DBAttribute.ID);
    return true;
  }

  @Override
  public String toString() {
    return "is(" + myObject + ")";
  }

  @Override
  protected boolean equalDP(DP other) {
    return Util.equals(myObject, ((DPResolvesTo)other).myObject);
  }

  @Override
  protected int hashCodeDP() {
    return myObject == null ? 0 : myObject.hashCode();
  }
}
