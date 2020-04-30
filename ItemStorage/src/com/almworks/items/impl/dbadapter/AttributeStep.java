package com.almworks.items.impl.dbadapter;

import org.jetbrains.annotations.NotNull;

/**
 * Attribute step is a part of an attribute path in the FROM clause of the SQL SELECT statement.
 * Attribute path is the path in the DB that leads to a column C in table T which defines some attribute. It consists of several INNER JOINs, starting from the table that serves as a source of item IDs. This table and its column with IDs is not included in the path.
 * <p/>
 * Example (PrevTable and NextTable are the previous and next tables on the attrivute path):
 * <pre>
 *   SELECT ...
 *    FROM ...
 *      SOME_JOIN...
 *      INNER_JOIN Table ON PrevTable.FromColumn = Table.ToColumn
 *      INNER JOIN NextTable ON Table.FromColumn = NextTable.ToColumn
 *      SOME_JOIN ...
 *  ;
 * </pre>
 * The goal of the path is contained in the last step: that is, {@code lastStep.Table = T} and {@code lastStep.NextColumn = C}.
 *
 *
 * @author igor baltiyskiy
 */
public class AttributeStep {
  @NotNull
  private final String myTable;
  @NotNull
  private final String myPrevColumn;
  @NotNull
  private final String myNextColumn;
  private String myToStringCache;

  public AttributeStep(@NotNull String table, @NotNull String prevColumn, @NotNull String nextColumn) {
    assert table != null;
    assert nextColumn != null;
    assert prevColumn != null;
    myTable = table;
    myNextColumn = nextColumn;
    myPrevColumn = prevColumn;
  }
  
  @NotNull
  public String getTable() {
    return myTable;
  }

  @NotNull
  public String getNextColumn() {
    return myNextColumn;
  }

  @NotNull
  public String getPrevColumn() {
    return myPrevColumn;
  }

  @Override
  public String toString() {
    if(myToStringCache == null) {
      myToStringCache = "table " + myTable + " [prev col: " + myPrevColumn + ", next col: " + myNextColumn + ']';
    }

    return myToStringCache;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof AttributeStep)) return false;

    AttributeStep that = (AttributeStep) o;

    @SuppressWarnings({"NonFinalFieldReferenceInEquals"}) String thisStringCache = myToStringCache;
    @SuppressWarnings({"NonFinalFieldReferenceInEquals"}) String thatStringCache = that.myToStringCache;
    if (thisStringCache != null && thatStringCache != null) {
      return thisStringCache.equals(thatStringCache);
    }

    if (!myNextColumn.equals(that.myNextColumn)) return false;
    if (!myTable.equals(that.myTable)) return false;
    if (!myPrevColumn.equals(that.myPrevColumn)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = myTable.hashCode();
    result = 31 * result + myNextColumn.hashCode();
    result = 31 * result + myPrevColumn.hashCode();
    return result;
  }
}
