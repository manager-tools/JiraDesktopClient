package com.almworks.items.dp;

import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBQuery;
import com.almworks.items.api.DBReader;
import com.almworks.items.api.DP;
import com.almworks.util.bool.BoolExpr;

public class DPReferredBy extends DP {
  private final BoolExpr<DP> myRefereeQuery;
  private final DBAttribute<Long> myReferenceAttribute;

  public DPReferredBy(DBAttribute<Long> referenceAttribute, BoolExpr<DP> refereeQuery) {
    myRefereeQuery = refereeQuery;
    myReferenceAttribute = referenceAttribute;
    assert myReferenceAttribute.isPropagatingChange() : "todo support capturing changes from non-propagating attributes";
  }

  public static BoolExpr<DP> create(DBAttribute<Long> referenceAttribute, BoolExpr<DP> refereeQuery) {
    return new DPReferredBy(referenceAttribute, refereeQuery).term();
  }

  public BoolExpr<DP> getRefereeQuery() {
    return myRefereeQuery;
  }

  public DBAttribute<Long> getReferenceAttribute() {
    return myReferenceAttribute;
  }

  @Override
  public boolean accept(long item, DBReader reader) {
    DBQuery q = reader.query(myRefereeQuery.and(DPEquals.create(myReferenceAttribute, item)));
    return q.getItem() != 0;
  }

  @Override
  protected boolean equalDP(DP other) {
    DPReferredBy that = (DPReferredBy) other;

    if (!myRefereeQuery.equals(that.myRefereeQuery))
      return false;
    if (!myReferenceAttribute.equals(that.myReferenceAttribute))
      return false;

    return true;
  }

  @Override
  protected int hashCodeDP() {
    int result = myRefereeQuery.hashCode();
    result = 31 * result + myReferenceAttribute.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return "@" + myReferenceAttribute + "(" + myRefereeQuery + ")"; 
  }
}
