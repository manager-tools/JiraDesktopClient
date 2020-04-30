package com.almworks.items.api;

import com.almworks.integers.LongArray;
import com.almworks.integers.LongCollector;
import com.almworks.integers.LongIterable;
import com.almworks.integers.LongList;
import com.almworks.util.bool.BoolExpr;
import com.almworks.util.commons.LongObjFunction2;
import org.jetbrains.annotations.NotNull;

public interface DBQuery {
  DBReader getReader();

  BoolExpr<DP> getExpr();

  DBQuery query(BoolExpr<DP> expr);

  long count();

  /**
   * Counts the number of issues in the query, grouped by given attributes (only attributes with INTEGER scalar
   * representation are allowed).
   *
   * @param groupAttributes a number of attributes to group the result by. Must be based on INTEGER scalar (however,
   * the attributes themselves may be non-scalar).
   *
   * @return a table consisting of X*(N + 1) items, where N is the number of attributes and X is the number of
   * groups. Each group contains a vector of N + 1 values, where [0] .. [N] elements are the values of the
   * grouping attributes, and [N + 1] value is the total for this group.
   */
  LongList distributionCount(DBAttribute<?>... groupAttributes);

  /**
   * Runs query and returns an array of matched items. The list is unique and sorted. It is also writable and
   * can be further altered by the caller.
   */
  @NotNull
  LongArray copyItemsSorted();

  /**
   * Runs query and retrieves matching items. The items put into the collector may
   * come in any order and have duplicates. To get a list of unique and sorted items,
   * use LongSetBuilder.
   *
   * @param collector items collector
   */
  <C extends LongCollector> C copyItems(C collector);

  boolean contains(long item);

  /**
   * Filters passed items and puts to the result all items that match the query's expression.
   * <p>
   * <strong>Careful:</strong> The items added to the result may come in any order (not necessary in the order
   * provided by items collection).
   * Furthermore, a single item may be added to the result several times!
   * <p>
   * The instance of LongCollector may throw {@link com.almworks.items.api.DBOperationCancelledException} to stop
   * the loading. This method will catch the exception and return false.
   *
   * @param items the source of items to be checked
   * @param result the receiver of filtered items
   * @return true false if filtering was cancelled with DBOperationCancelledException, true otherwise
   */
  boolean filterItems(LongIterable items, LongCollector result);

  /**
   * Filters passed items and returns an array of items that match the query's expression. The resulting array
   * is sorted and has no duplicates. It is also writable and owned by the caller.
   */
  LongArray filterItemsSorted(LongIterable items);

  /**
   * @return some item from the query or 0 if query result is empty
   * */
  long getItem();

  <T> long getItemByKey(DBAttribute<T> attribute, T value);

  /**
   * Runs the query and passes the result through fold function.
   * <p>
   * The function is called one time for each matching item.
   */
  <T> T fold(T seed, LongObjFunction2<T> f);
}
