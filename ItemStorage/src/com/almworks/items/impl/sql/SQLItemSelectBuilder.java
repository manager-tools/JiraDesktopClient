package com.almworks.items.impl.sql;

import com.almworks.integers.*;
import com.almworks.items.impl.dbadapter.DBColumn;
import com.almworks.items.impl.sqlite.SQLUtil;
import com.almworks.items.impl.sqlite.Schema;
import com.almworks.items.impl.sqlite.TransactionContext;
import com.almworks.items.impl.sqlite.filter.SQLPartsParameterized;
import com.almworks.sqlite4java.SQLParts;
import com.almworks.sqlite4java.SQLiteException;
import org.almworks.util.Collections15;
import org.almworks.util.Log;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class helps build filtering SQL for selecting items.
 * <p/>
 * By default, if you create a builder and don't add anything to its conditions, it will select
 * all issues.
 */
public final class SQLItemSelectBuilder implements Cloneable {
  private static final String TABLE_ALIAS_PREFIX = "_t";
  private static final String ITEMS_TABLE_ALIAS = "_ti";
  private static final String LONG_ARRAY_PREFIX = "_la";
  private static final String[] TABLE_ALIASES = generateTableAliases(30);
  private static final String ITEMID_COLUMN = DBColumn.ITEM.getName();
  private static final int MAX_BIND_IN_PARAMETERS = 10;

  /**
   * If not null, the query will check only the specified items.
   */
  @Nullable
  private Map<String, LongIterable> myLongArrayParameters;

  /**
   * List of the primary tables. Primary tables are joined using item id.
   */
  @Nullable
  private JoinList myPrimaryJoins;

  /**
   * Holds SQLParts that are used in WHERE part. The real number of used parts is defined by myWheresCount.
   */
  @Nullable
  private List<SQLPartsParameterized> myWheres;

  private int myWheresCount;

  private String myItemFilterArrayName;

  /**
   * Contains the next number for generating table alias. Should never decrease.
   */
  private int myJoinSequence;

  // what to select from the query - should point to a long column
  // if null, item ids from the first primary table are retrieved
  private String mySelectAlias;
  private String mySelectColumn;

  // the following fields are used in building

  private SQLParts myParts;
  private String myPrimaryJoinAlias;
  private String myPrimaryJoinColumn;
  private Join myFirstJoin;

  public String arrayParameter(LongIterable list) {
    if (list == null)
      throw new NullPointerException();
    Map<String, LongIterable> lap = myLongArrayParameters;
    if (lap == null)
      myLongArrayParameters = lap = Collections15.hashMap();
    // todo const tempName
    String tempName = LONG_ARRAY_PREFIX + lap.size();
    lap.put(tempName, list);
    return tempName;
  }

  public void whereColumnValueInList(LongIterable list, String tableAlias, String tableColumn) {
    SQLPartsParameterized where = addWhere();
    SQLParts parts = where.getParts();
    parts.append(tableAlias).append(".").append(tableColumn);

    List<Object> parameters = Collections15.arrayList();
    if (addOptimalListMatchingWhereClause(list, parts, parameters)) {
      where.addParametersList(parameters);
    } else {
      // bind a virtual array table if too many items
      String alias = arrayParameter(list);
      parts.append(" IN ").append(alias);
    }
  }

  private boolean addOptimalListMatchingWhereClause(LongIterable list, SQLParts parts, List<Object> parameters) {
    // depending on list size, use different bind strategies
    LongIterator ii = list.iterator();
    if (!ii.hasNext()) {
      Log.warn(this + ": looking for an empty list");
      // this is always false or null
      parts.append(" IN ()");
      return true;
    }
    long singleValue = ii.nextValue();
    if (!ii.hasNext()) {
      // binding single value
      parts.append(" = ?");
      parameters.add(singleValue);
      return true;
    }
    LongArray params = new LongArray(MAX_BIND_IN_PARAMETERS);
    params.add(singleValue);
    for (int i = 1; i < MAX_BIND_IN_PARAMETERS && ii.hasNext(); i++) {
      params.add(ii.nextValue());
    }
    if (!ii.hasNext()) {
      // binding up to MAX_BIND_IN_PARAMETERS values
      assert params.size() <= MAX_BIND_IN_PARAMETERS : params;
      parts.append(" IN (").appendParams(MAX_BIND_IN_PARAMETERS).append(")");
      int i = 0;
      while (i < params.size()) {
        parameters.add(params.get(i++));
      }
      while (i < MAX_BIND_IN_PARAMETERS) {
        parameters.add(null);
        i++;
      }
      return true;
    }
    // too many items
    return false;
  }

  public String whereItemInArray(LongIterable list) {
    Map<String, LongIterable> lap = myLongArrayParameters;
    if (myItemFilterArrayName == null || lap == null) {
      myItemFilterArrayName = arrayParameter(list);
    } else {
      LongIterable current = lap.get(myItemFilterArrayName);
      assert current != null;
      LongSetBuilder builder = new LongSetBuilder();
      builder.addAll(current.iterator());
      builder.addAll(list.iterator());
      lap.put(myItemFilterArrayName, builder.commitToArray());
    }
    return myItemFilterArrayName;
  }

  /**
   * Makes this query join a primary table. If table is already joined on this column, then existing join builder
   * will be used, probably escalating join type from outer to inner.
   * Adds a primary table to the selection (with item id)
   */
  @NotNull
  public Join joinPrimaryTable(@NotNull String table, @NotNull String itemColumn, boolean outerJoin) {
    return joinPrimaryTable(table, itemColumn, outerJoin, false);
  }

  @NotNull
  public Join joinPrimaryTable(@NotNull String table, @NotNull String itemColumn, boolean outerJoin, boolean separate) {
    JoinList joins = myPrimaryJoins;
    if (joins == null)
      myPrimaryJoins = joins = new JoinList(this);
    return separate
      ? joins.addJoin(ITEMID_COLUMN, table, itemColumn, outerJoin)
      : joins.join(ITEMID_COLUMN, table, itemColumn, outerJoin);
  }

  private String nextTableAlias() {
    int index = myJoinSequence++;
    if (index < TABLE_ALIASES.length)
      return TABLE_ALIASES[index];
    else
      return generateTableAlias(index);
  }

  private static String[] generateTableAliases(int count) {
    String[] r = new String[count];
    for (int i = 0; i < count; i++)
      r[i] = generateTableAlias(i);
    return r;
  }

  private static String generateTableAlias(int index) {
    return TABLE_ALIAS_PREFIX + index;
  }

  public SQLItemSelect build() {
    return build0(false);
  }

  public LongList loadItems(TransactionContext context) throws SQLiteException {
    LongSetBuilder r = new LongSetBuilder();
    build().loadItems(context, r);
    return r.commitToArray();
  }

  public long countDistinctIds(TransactionContext context) throws SQLiteException {
    SQLItemSelect select = build0(true);
    return select.count(context);
  }


  private SQLItemSelect build0(boolean countOnly) {
    SQLParts parts = myParts;
    if (parts == null)
      myParts = parts = new SQLParts();
    parts.clear();

    sqlStart(parts, countOnly);
    sqlJoin(parts, myPrimaryJoins, false, myPrimaryJoinAlias, myPrimaryJoinColumn);
    sqlJoin(parts, myPrimaryJoins, true, myPrimaryJoinAlias, myPrimaryJoinColumn);
    List<?> arguments = sqlWhere(parts);

    return new SQLItemSelect(parts, myLongArrayParameters, arguments);
  }


  private List<?> sqlWhere(SQLParts parts) {
    String prefix = "\n  WHERE ";
    List<Object> params = null;
    if (myItemFilterArrayName != null) {
      parts.append(prefix).append(myPrimaryJoinAlias).append(".").append(myPrimaryJoinColumn);
      assert params == null;
      params = Collections15.arrayList();
      if (!addOptimalListMatchingWhereClauseForItems(parts, params)) {
        parts.append(" IN ").append(myItemFilterArrayName);
      }
      prefix = "\n  AND ";
    }
    List<SQLPartsParameterized> wheres = myWheres;
    if (myWheresCount > 0 && wheres != null) {
      for (SQLPartsParameterized where : wheres) {
        parts.append(prefix).append("(").append(where.getParts()).append(")");
        prefix = "\n  AND ";
        List<Object> whereParams = where.getParameters();
        if (whereParams != null) {
          if (params == null)
            params = Collections15.arrayList();
          params.addAll(whereParams);
        }
      }
    }
    return params;
  }

  private boolean addOptimalListMatchingWhereClauseForItems(SQLParts parts, List<Object> params) {
    String name = myItemFilterArrayName;
    Map<String, LongIterable> lap = myLongArrayParameters;
    assert name != null;
    assert lap != null;

    // check reference
    List<SQLPartsParameterized> wheres = myWheres;
    if (wheres != null) {
      for (SQLPartsParameterized where : wheres) {
        SQLParts p = where.getParts();
        for (String s : p.getParts()) {
          if (name.equals(s) || name.contains(s)) {
            // array (may be) referred in where
            return false;
          }
        }
      }
    }

    LongIterable list = lap.get(name);
    if (list == null) {
      assert false : name;
      return false;
    }
    if (addOptimalListMatchingWhereClause(list, parts, params)) {
      lap.remove(name);
      return true;
    } else {
      return false;
    }
  }

  private void sqlJoin(SQLParts parts, JoinList joinList, boolean outer, String parentAlias,
    @Nullable String parentColumn)
  {
    if (joinList == null)
      return;
    boolean hasSecondary = false;
    for (Join join : joinList.getList()) {
      if (join.isOuterJoin() == outer && join != myFirstJoin) {
        sqlJoinJoin(parts, outer ? "\n  LEFT OUTER JOIN " : "\n  INNER JOIN ", join, parentAlias, parentColumn);
      }
      if (!hasSecondary && join.getSecondaryJoins() != null) {
        hasSecondary = true;
      }
    }
    if (hasSecondary) {
      for (Join join : joinList.getList()) {
        sqlJoin(parts, join.getSecondaryJoins(), outer, join.getAlias(), null);
      }
    }
  }

  private void sqlStart(SQLParts parts, boolean countOnly) {
    myFirstJoin = getFirstJoin();
    String firstName;
    if (myFirstJoin != null) {
      myPrimaryJoinAlias = myFirstJoin.getAlias();
      myPrimaryJoinColumn = myFirstJoin.getJoinColumn();
      firstName = myFirstJoin.getTable();
    } else {
      myPrimaryJoinAlias = ITEMS_TABLE_ALIAS;
      myPrimaryJoinColumn = ITEMID_COLUMN;
      firstName = Schema.ITEMS;
    }
    String selectAlias = mySelectAlias == null ? myPrimaryJoinAlias : mySelectAlias;
    String selectColumn = mySelectColumn == null ? myPrimaryJoinColumn : mySelectColumn;
    parts.append("SELECT ");
    if (countOnly) {
      parts.append("COUNT(DISTINCT ").append(selectAlias).append(".").append(selectColumn).append(") FROM ");
    } else {
      parts.append(selectAlias).append(".").append(selectColumn).append(" r FROM ");
    }
    parts.append(firstName).append(" ").append(myPrimaryJoinAlias);
  }

  private void sqlJoinJoin(SQLParts parts, String joinPrefix, Join join, String parentAlias,
    @Nullable String parentColumn)
  {
    parts.append(joinPrefix).append(join.getTable()).append(" ").append(join.getAlias());
    String pc = parentColumn == null ? join.getParentColumn() : parentColumn;
    parts.append(" ON ").append(parentAlias).append(".").append(pc).append("=");
    parts.append(join.getAlias()).append(".").append(join.getJoinColumn());
  }

  @Nullable
  private Join getFirstJoin() {
    JoinList joins = myPrimaryJoins;
    if (joins == null)
      return null;
    for (Join join : joins.getList()) {
      if (!join.isOuterJoin())
        return join;
    }
    return null;
  }

  public SQLPartsParameterized addWhere() {
    List<SQLPartsParameterized> wheres = myWheres;
    if (wheres == null) {
      assert myWheresCount == 0 : this;
      myWheres = wheres = Collections15.arrayList();
      myWheresCount = 0;
    }
    int index = myWheresCount++;
    SQLPartsParameterized r;
    if (index < wheres.size()) {
      r = wheres.get(index);
      r.clear();
    } else {
      r = new SQLPartsParameterized();
      wheres.add(r);
    }
    return r;
  }

  public void setSelectedColumn(String selectAlias, String selectColumn) {
    mySelectAlias = selectAlias;
    mySelectColumn = selectColumn;
  }

  public SQLItemSelectBuilder clone() {
    try {
      SQLItemSelectBuilder r = (SQLItemSelectBuilder) super.clone();

      r.myParts = null;
      r.myPrimaryJoinAlias = null;
      r.myPrimaryJoinColumn = null;
      r.myFirstJoin = null;

      r.myPrimaryJoins = myPrimaryJoins == null ? null : myPrimaryJoins.clone(r);
      r.myWheres = myWheres == null ? null : (List<SQLPartsParameterized>) ((ArrayList) myWheres).clone();
      r.myLongArrayParameters = myLongArrayParameters == null ? null : (Map<String, LongIterable>)((HashMap)myLongArrayParameters).clone();

      return r;
    } catch (CloneNotSupportedException e) {
      throw new Error(e);
    }
  }

  private static final class JoinList implements Cloneable {
    private final List<Join> myList = Collections15.arrayList();
    private final SQLItemSelectBuilder myOwner;

    public JoinList(SQLItemSelectBuilder owner) {
      myOwner = owner;
    }

    public List<Join> getList() {
      return myList;
    }

    protected JoinList clone(SQLItemSelectBuilder newOwner) {
      JoinList r = new JoinList(newOwner);
      for (Join join : myList) {
        r.myList.add(join.clone(newOwner));
      }
      return r;
    }

    public Join join(String parentColumn, String table, String joinColumn, boolean outerJoin) {
      Join r = getJoin(parentColumn, table, joinColumn);
      if (r == null) {
        r = addJoin(parentColumn, table, joinColumn, outerJoin);
      } else {
        r.escalateType(outerJoin);
      }
      return r;
    }

    public Join addJoin(String parentColumn, String table, String joinColumn, boolean outerJoin) {
      String alias = myOwner.nextTableAlias();
      Join join = new Join(myOwner, parentColumn, table, joinColumn, alias, outerJoin);
      myList.add(join);
      return join;
    }

    @Nullable
    private Join getJoin(String parentColumn, String table, String joinColumn) {
      for (Join join : myList) {
        if (parentColumn.equals(join.getParentColumn()) && table.equals(join.getTable()) &&
          joinColumn.equals(join.getJoinColumn()))
        {
          return join;
        }
      }
      return null;
    }
  }


  public static final class Join implements Cloneable {
    private final SQLItemSelectBuilder myOwner;
    private final String myParentColumn;
    private final String myTable;
    private final String myAlias;
    private final String myJoinColumn;

    private boolean myOuterJoin;

    @Nullable
    private JoinList mySecondaryJoins;

    private Join(SQLItemSelectBuilder owner, String parentColumn, String table, String joinColumn, String alias,
      boolean outerJoin)
    {
      myOwner = owner;
      myTable = table;
      myJoinColumn = joinColumn;
      myAlias = alias;
      myOuterJoin = outerJoin;
      myParentColumn = parentColumn;
    }

    public Join clone(SQLItemSelectBuilder newOwner) {
      Join r = new Join(newOwner, myParentColumn, myTable, myJoinColumn, myAlias, myOuterJoin);
      r.mySecondaryJoins = mySecondaryJoins == null ? null : mySecondaryJoins.clone(newOwner);
      return r;
    }

    public String getTable() {
      return myTable;
    }

    public String getJoinColumn() {
      return myJoinColumn;
    }

    public String getAlias() {
      return myAlias;
    }

    public boolean isOuterJoin() {
      return myOuterJoin;
    }

    public String getParentColumn() {
      return myParentColumn;
    }

    private void escalateType(boolean outerJoin) {
      // inner join is more restrictive => inner join takes preference
      myOuterJoin = myOuterJoin && outerJoin;
    }

    public Join joinSecondaryTable(String parentColumn, String table, String joinColumn, boolean outerJoin) {
      JoinList joins = mySecondaryJoins;
      if (joins == null)
        mySecondaryJoins = joins = new JoinList(myOwner);
      return joins.join(parentColumn, table, joinColumn, outerJoin);
    }

    // returns mapping from old aliases to new aliases

    public void joinSecondaryQueryInner(String parentColumn, SQLItemSelectBuilder subqueryBuilder) {
      JoinList joins = subqueryBuilder.myPrimaryJoins;
      if (joins == null)
        return;
      Map<String, String> arrayAliasMap = null;
      if (subqueryBuilder.myLongArrayParameters != null) {
        arrayAliasMap = joinSecondaryQueryArrayParameters(subqueryBuilder);
      }
      Map<String, String> aliasMap = Collections15.hashMap();
      for (Join join : joins.myList) {
        rejoin(parentColumn, join, aliasMap, arrayAliasMap);
      }
      if (subqueryBuilder.myWheres != null) {
        if (arrayAliasMap != null) {
          aliasMap.putAll(arrayAliasMap);
        }
        for (SQLPartsParameterized where : subqueryBuilder.myWheres) {
          SQLPartsParameterized newWhere = myOwner.addWhere();
          newWhere.addParametersList(where.getParameters());
          SQLUtil.replaceParts(where.getParts(), newWhere.getParts(), aliasMap);
        }
      }
    }

    private Map<String, String> joinSecondaryQueryArrayParameters(SQLItemSelectBuilder subqueryBuilder) {
      Map<String, String> arrayAliasMap = Collections15.hashMap();
      for (Map.Entry<String, LongIterable> e : subqueryBuilder.myLongArrayParameters.entrySet()) {
        String name = e.getKey();
        String newAlias = myOwner.arrayParameter(e.getValue());
        arrayAliasMap.put(name, newAlias);
        if (name.equals(subqueryBuilder.myItemFilterArrayName)) {
          // add where for sub-query's item filter
          SQLPartsParameterized where = myOwner.addWhere();
          SQLParts parts = where.getParts();
          Join fj = subqueryBuilder.getFirstJoin();
          if (fj != null) {
            parts.append(fj.getAlias()).append(".").append(fj.getJoinColumn());
          } else {
            assert false;
            // what to do? the following is most probably a bug
            parts.append(ITEMS_TABLE_ALIAS).append(".").append(ITEMID_COLUMN);
          }
          parts.append(" IN ").append(newAlias);
        }
      }
      return arrayAliasMap;
    }

    private void rejoin(String parentColumn, Join join, Map<String, String> aliasMap, Map<String, String> arrayMap) {
      String table;
      if (arrayMap == null) {
        table = join.myTable;
      } else {
        table = arrayMap.get(join.myTable);
        if (table == null)
          table = join.myTable;
      }
      Join newJoin = joinSecondaryTable(parentColumn, table, join.myJoinColumn, join.myOuterJoin);
      aliasMap.put(join.myAlias, newJoin.myAlias);
      if (join.mySecondaryJoins != null) {
        for (Join j : join.mySecondaryJoins.myList) {
          newJoin.rejoin(j.myParentColumn, j, aliasMap, arrayMap);
        }
      }
    }

    @Nullable
    public JoinList getSecondaryJoins() {
      return mySecondaryJoins;
    }
  }
}
