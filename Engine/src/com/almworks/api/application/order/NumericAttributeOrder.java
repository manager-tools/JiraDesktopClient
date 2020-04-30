package com.almworks.api.application.order;

import com.almworks.api.application.ItemWrapper;
import com.almworks.api.application.LoadedItem;
import com.almworks.api.application.ModelKey;
import com.almworks.api.application.ModelKeySetUtil;
import com.almworks.items.sync.DBDrain;
import com.almworks.items.sync.ItemVersionCreator;
import com.almworks.util.components.ATableModel;
import com.almworks.util.components.Canvas;
import com.almworks.util.components.CanvasRenderer;
import com.almworks.util.components.CollectionRenderer;
import com.almworks.util.components.renderer.CellState;
import com.almworks.util.components.renderer.Renderers;
import com.almworks.util.models.ColumnAccessor;
import com.almworks.util.models.TableColumnAccessor;
import org.almworks.util.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import util.external.BitSet2;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Comparator;
import java.util.List;

/**
 * @author dyoma
 */
public abstract class NumericAttributeOrder<T> {
  private static final BigDecimal TWO = new BigDecimal(2);
  private final TableColumnAccessor<ReorderItem, ?> myColumn;
  private final ModelKey<T> myKey;
  private final Comparator<LoadedItem> myComparator = new Comparator<LoadedItem>() {
    public int compare(LoadedItem o1, LoadedItem o2) {
      BigDecimal v1 = extractValue(o1);
      BigDecimal v2 = extractValue(o2);
      if (v1 == null)
        return v2 == null ? 0 : -1;
      if (v2 == null)
        return 1;
      return v1.compareTo(v2);
    }
  };

  public NumericAttributeOrder(ModelKey<T> key, TableColumnAccessor<LoadedItem, ?> column) {
    myKey = key;
    myColumn = new MyTableColumnAccessor(column, key);
  }

  public void updateOrder(List<ReorderItem> order, int[] indecies) {
    indecies = sortedCopy(indecies);
    List<BigDecimal> oldValues = Collections15.arrayList(order.size());
    for (ReorderItem item : order)
      oldValues.add(extractValue(item));
    List<BigDecimal> newValues = changeValues(oldValues, indecies);
    for (int i = 0; i < newValues.size(); i++) {
      BigDecimal v = newValues.get(i);
      BigDecimal old = oldValues.get(i);
      order.get(i).setNewOrderValue(Util.equals(v, old) ? null : v);
    }
  }

  protected ModelKey<T> getKey() {
    return myKey;
  }

  protected CanvasRenderer<BigDecimal> orderRenderer() {
    return Renderers.BIG_DECIMAL_RENDERER;
  }

  protected abstract BigDecimal extractValue(LoadedItem item);

  protected abstract void setDBValue(ItemVersionCreator creator, BigDecimal value);

  public Comparator<LoadedItem> getComparator() {
    return myComparator;
  }

  public void updateItems(DBDrain drain, List<ReorderItem> items) {
    for (ReorderItem item : items) {
      final ItemVersionCreator creator = drain.changeItem(item.getItem());
      final BigDecimal value = (BigDecimal) item.getNewOrderValue();
      assert value != null;
      setDBValue(creator, value);
    }
  }

  public String getId() {
    return myKey.getName();
  }

  public String toString() {
    return "Order [" + getId() + "]";
  }

  public TableColumnAccessor<ReorderItem, ?> getColumn() {
    return myColumn;
  }

  public boolean canOrder(ItemWrapper item) {
    BitSet2 allKeys = ModelKey.ALL_KEYS.getValue(item.getLastDBValues());
    return allKeys != null && ModelKeySetUtil.contains(allKeys, myKey);
  }

  static List<BigDecimal> changeValues(List<BigDecimal> oldValues, int[] indecies) {
    if (oldValues.isEmpty())
      return Collections15.emptyList();
    BigDecimal max = getMax(oldValues);
    if (max == null)
      max = new BigDecimal(1000 * 1000);
    indecies = sortedCopy(indecies);
    BitSet toChange = prepareChanges(oldValues, indecies);
    BigDecimal min = getMin(oldValues, toChange);
    List<BigDecimal> result = Collections15.arrayList(oldValues.size());
    for (int i = 0; i < oldValues.size(); i++) {
      assert result.size() == i;
      if (!toChange.get(i))
        result.add(oldValues.get(i));
      else {
        BigDecimal prev = i == 0 ? max.multiply(TWO) : result.get(i - 1);
        ChangeGroup group = new ChangeGroup(toChange);
        group.setInnerIndex(i);
        int lastIndex = group.getEnd();
        assert !toChange.get(lastIndex + 1);
        BigDecimal last = group.getEndValue(oldValues);
        if (last == null)
          last = min;
        assert checkGreaterThenTail(last, lastIndex + 1, oldValues, toChange);
        assert prev != null;
        for (int j = i; j <= lastIndex; j++) {
          BigDecimal diff = prev.subtract(last);
          assert diff.compareTo(BigDecimal.ZERO) > 0;
          prev = prev.subtract(diff.divide(BigDecimal.valueOf(lastIndex - j + 2), RoundingMode.DOWN));
          assert prev.compareTo(last) >= 0;
          assert result.size() == j;
          result.add(prev);
        }
        i = lastIndex;
      }
    }
    restoreOldValues(oldValues, result);
    return result;
  }

  private static boolean checkGreaterThenTail(BigDecimal n, int index, List<BigDecimal> values, BitSet toChange) {
    assert n != null;
    for (int i = index; i < values.size(); i++) {
      BigDecimal v = values.get(i);
      assert v == null || toChange.get(i) || n.compareTo(v) >= 0 : n + " " + index + " " + values + " " + toChange;
    }
    return true;
  }

  private static void restoreOldValues(List<BigDecimal> oldValues, List<BigDecimal> result) {
    assert oldValues.size() == result.size();
    if (result.isEmpty())
      return;
    if (result.size() == 1) {
      if (oldValues.get(0) != null)
        result.set(0, oldValues.get(0));
      return;
    }
    if (!Util.equals(result.get(0), oldValues.get(0))) {
      BigDecimal oldValue = oldValues.get(0);
      BigDecimal rValue2 = result.get(1);
      if (oldValue != null && (rValue2 == null || oldValue.compareTo(rValue2) > 0))
        result.set(0, oldValue);
    }
    BigDecimal prev = result.get(0);
    for (int i = 1; i < result.size() - 1; i++) {
      BigDecimal oldValue = oldValues.get(i);
      if (oldValue != null && !Util.equals(result.get(i), oldValue)) {
        BigDecimal next = result.get(i + 1);
        if ((prev != null && prev.compareTo(oldValue) > 0) && (next == null || oldValue.compareTo(next) > 0))
          result.set(i, oldValue);
      }
      prev = result.get(i);
    }
    BigDecimal oldValue = oldValues.get(result.size() - 1);
    BigDecimal last = result.get(result.size() - 1);
    if (oldValue != null && prev != null && !Util.equals(oldValue, last) && prev.compareTo(oldValue) > 0)
      result.set(result.size() - 1, oldValue);
  }

  private static int[] sortedCopy(int[] indecies) {
    indecies = ArrayUtil.arrayCopy(indecies);
    Arrays.sort(indecies);
    return indecies;
  }

  private static BigDecimal getMax(List<BigDecimal> values) {
    BigDecimal max = values.get(0);
    for (int i = 1; i < values.size(); i++) {
      BigDecimal n = values.get(i);
      if (max == null)
        max = n;
      else if (n != null && n.compareTo(max) > 0)
        max = n;
    }
    return max;
  }

  @NotNull
  private static BigDecimal getMin(List<BigDecimal> values, BitSet toChange) {
    BigDecimal min = null;
    boolean lastNull = false;
    for (int i = 0; i < values.size(); i++) {
      if (toChange.get(i)) {
        lastNull = true;
        continue;
      }
      BigDecimal n = values.get(i);
      assert min == null || n == null || min.compareTo(n) >= 0;
      if (n == null) {
        lastNull = true;
        continue;
      }
      lastNull = false;
      min = n;
    }
    if (lastNull)
      if (min == null)
        min = BigDecimal.ZERO;
      else {
        int caridinality = toChange.cardinality();
        min = min.subtract(BigDecimal.valueOf(caridinality + 1));
        if (min.compareTo(BigDecimal.ZERO) >= 0)
          min = BigDecimal.ZERO;
        min = min.subtract(BigDecimal.valueOf(3 * caridinality));
      }
    else
      assert min != null;
    return min;
  }

  static BitSet prepareChanges(List<BigDecimal> oldValues, int[] indecies) {
    BitSet change = new BitSet(oldValues.size());
    if (indecies.length == 0)
      return change;
    for (int index : indecies)
      change.set(index);
    for (int i = indecies[indecies.length - 1] - 1; i >= 0; i--)
      if (oldValues.get(i) == null)
        change.set(i);
    int processed = 0;
    ChangeGroup group = new ChangeGroup(change);
    while (processed < oldValues.size()) {
      while (processed < oldValues.size() && !change.get(processed))
        processed++;
      if (processed >= oldValues.size())
        break;
      group.setInnerIndex(processed);
      BigDecimal last = group.getEndValue(oldValues);
      int start = group.getStart();
      if (start == 0 || last == null ||
        group.getStartValue(oldValues).subtract(last).compareTo(BigDecimal.valueOf(group.getValueCount())) >= 0)
      {
        processed = group.getEnd() + 1;
        continue;
      }
      bestExpand(group, change, oldValues);
      processed = group.getStart();
    }
    return change;
  }

  private static void bestExpand(ChangeGroup group, BitSet change, List<BigDecimal> oldValues) {
    int start = group.getStart();
    assert start > 0;
    if (start == 1 || change.get(start - 2)) {
      group.expandStart();
      return;
    }
    BigDecimal prev1 = oldValues.get(start - 1);
    BigDecimal prev2 = oldValues.get(start - 2);
    assert prev1 != null;
    assert prev2 != null;
    assert prev2.compareTo(prev1) >= 0 : prev2 + " " + prev1;
    BigDecimal endValue = group.getEndValue(oldValues);
    assert endValue != null;
    if (prev2.subtract(BigDecimal.ONE).subtract(endValue).compareTo(BigDecimal.valueOf(group.getValueCount())) >= 0) {
      group.expandStart();
      return;
    }
    int end = group.getEnd();
    assert end < oldValues.size() - 1;
    BigDecimal next = oldValues.get(end + 1);
    if (end == oldValues.size() - 2 || change.get(end + 1) || next == null) {
      group.expandEnd();
      return;
    }
    BigDecimal next2 = oldValues.get(end + 2);
    if (change.get(end + 2) || next2 == null) {
      group.expandEnd();
      return;
    }
    assert next.compareTo(next2) >= 0;
    if (prev2.subtract(prev1).compareTo(next.subtract(next2)) >= 0)
      group.expandStart();
    else
      group.expandEnd();
  }

  private static class ChangeGroup {
    private final BitSet myChange;
    private int myStart = -1;
    private int myEnd = -1;
    private int myInnerIndex = -1;

    public ChangeGroup(BitSet change) {
      myChange = change;
    }

    public void setInnerIndex(int index) {
      assert myChange.get(index);
      myInnerIndex = index;
      myStart = -1;
      myEnd = -1;
    }

    public int getEnd() {
      if (myEnd < 0) {
        assert myInnerIndex >= 0;
        int index = myInnerIndex;
        while (myChange.get(index))
          index++;
        myEnd = index - 1;
      }
      return myEnd;
    }

    public int getStart() {
      if (myStart < 0) {
        assert myInnerIndex >= 0;
        int index = myInnerIndex;
        while (index >= 0 && myChange.get(index))
          index--;
        myStart = index + 1;
      }
      return myStart;
    }

    @Nullable
    public BigDecimal getEndValue(List<BigDecimal> values) {
      int index = getEnd();
      if (index < values.size() - 1) {
        return values.get(index + 1);
      } else
        return null;
    }

    public BigDecimal getStartValue(List<BigDecimal> values) {
      int index = getStart();
      if (index == 0)
        throw new Failure("Not applicable");
      BigDecimal decimal = values.get(index - 1);
      assert decimal != null : "Not applicable";
      return decimal.subtract(BigDecimal.ONE);
    }

    public int getValueCount() {
      return getEnd() - getStart() + 1;
    }

    public void expandStart() {
      int index = getStart();
      assert index > 0;
      index--;
      assert !myChange.get(index);
      myChange.set(index);
      myStart = -1;
    }

    public void expandEnd() {
      int index = getEnd();
      index++;
      assert !myChange.get(index);
      myChange.set(index);
      myEnd = -1;
    }
  }


  protected class MyTableColumnAccessor extends TableColumnAccessor.DelegatingColumn<ReorderItem, Object> {
    private final CollectionRenderer<ReorderItem> myRenderer;

    public MyTableColumnAccessor(TableColumnAccessor<LoadedItem, ?> column, final ModelKey<T> key) {
      super(column);
      myRenderer = Renderers.createRenderer(new CanvasRenderer<ReorderItem>() {
        public void renderStateOn(CellState state, Canvas canvas, ReorderItem item) {
          Object newValue = item.getNewOrderValue();
          Object value;
          if (newValue == null) {
            value = extractValue(item);
          } else {
            canvas.setForeground(Color.BLUE);
            value = newValue;
          }
          if (value == null || value instanceof BigDecimal) {
            orderRenderer().renderStateOn(state, canvas, (BigDecimal) value);
          } else {
            Log.warn("non-renderable [" + value + "][" + value.getClass() + "]");
            // assert value instanceof T
//            valueRenderer.renderStateOn(state, canvas, (T) value);
          }
        }
      });
    }

    public CollectionRenderer<ReorderItem> getDataRenderer() {
      return myRenderer;
    }

    public int getPreferredWidth(JTable table, ATableModel<ReorderItem> aTableModel,
      ColumnAccessor<ReorderItem> renderingAccessor, int columnIndex)
    {
      //noinspection unchecked
      return ((TableColumnAccessor<ReorderItem, ?>) getDelegate()).getPreferredWidth(table, aTableModel,
        renderingAccessor, columnIndex);
    }
  }
}
