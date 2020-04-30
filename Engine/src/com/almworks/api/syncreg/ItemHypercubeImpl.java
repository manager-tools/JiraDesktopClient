package com.almworks.api.syncreg;

import com.almworks.integers.LongIterator;
import com.almworks.integers.LongList;
import com.almworks.items.api.DBAttribute;
import com.almworks.util.Pair;
import com.almworks.util.collections.Containers;
import org.almworks.util.Collections15;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.SortedSet;

public class ItemHypercubeImpl implements ItemHypercube {
  /**
   * contains references to DBAttributes and AxisValues:
   * [k*2] instanceof DBAttribute, [k*2+1] instanceof AxisValues
   * DBAttributes are sorted by id
   */
  private Object[] myData;

  /**
   * This method is not equals() because this class is not immutable.
   */
  @Override
  public boolean isSame(ItemHypercube obj) {
    if (!(obj instanceof ItemHypercubeImpl))
      return false;
    ItemHypercubeImpl that = ((ItemHypercubeImpl) obj);
    if (myData == null || myData.length == 0 || myData[0] == null)
      return that.myData == null || that.myData.length == 0 || that.myData[0] == null;
    if (that.myData == null || that.myData.length == 0 || that.myData[0] == null)
      return false;
    int thatLength = that.myData.length;
    int i;
    for (i = 0; i < myData.length; i++) {
      Object my = myData[i];
      Object their = i >= thatLength ? null : that.myData[i];
      boolean eq;
      if (i % 2 == 0) {
        eq = Util.equals(my, their);
      } else {
        assert my != null : this;
        if (my == null)
          return false;
        eq = ((AxisValues) my).isSame((AxisValues) their);
      }
      if (!eq)
        return false;
      if (my == null)
        break;
    }
    if (i < thatLength && that.myData[i] != null)
      return false;
    return true;
  }

  @Override
  public Set<DBAttribute<?>> getAxes() {
    if (myData == null || myData.length == 0)
      return Collections15.emptySet();
    SortedSet<DBAttribute<?>> result = Collections15.treeSet(Containers.convertingComparator(DBAttribute.TO_ID));
    for (int i = 0; i < myData.length - 1; i += 2) {
      if (myData[i] == null)
        break;
      result.add((DBAttribute<?>) myData[i]);
    }
    return result;
  }

  @Override
  public SortedSet<Long> getIncludedValues(DBAttribute<?> axis) {
    AxisValues values = getAxis(axis);
    return values == null ? null : values.myIncludedReadonly;
  }

  @Override
  public SortedSet<Long> getExcludedValues(DBAttribute<?> axis) {
    AxisValues values = getAxis(axis);
    return values == null ? null : values.myExcludedReadonly;
  }

  @Override
  public Pair<SortedSet<Long>, SortedSet<Long>> getValues(DBAttribute<?> axis) {
    AxisValues values = getAxis(axis);
    if (values == null)
      return null;
    else
      return Pair.create(values.myIncludedReadonly, values.myExcludedReadonly);
  }

  @Override
  public int getAxisCount() {
    if (myData == null)
      return 0;
    int p = myData.length - 1;
    for (; p >= 0; p--)
      if (myData[p] != null)
        break;
    return (p + 1) / 2;
  }

  @Override
  public boolean allows(DBAttribute<?> axis, Long value) {
    AxisValues values = getAxis(axis);
    if (values == null)
      return true;
    SortedSet<Long> excluded = values.myExcluded;
    if (excluded != null && excluded.contains(value))
      return false;
    SortedSet<Long> included = values.myIncluded;
    if (included != null && !included.contains(value))
      return false;
    return true;
  }

  @Override
  public boolean containsAnyAxis(DBAttribute<?>... axes) {
    Object[] data = myData;
    if (data == null)
      return false;
    int length = data.length;
    for (int i = 0; i < length - 1; i += 2) {
      DBAttribute<?> p = (DBAttribute<?>) data[i];
      if (p == null)
        break;
      for (DBAttribute<?> axis : axes) {
        if (Util.equals(axis, p))
          return true;
      }
    }
    return false;
  }

  @Override
  public boolean containsAxis(@NotNull DBAttribute<?> axis) {
    return getAxis(axis) != null;
  }

  private AxisValues getAxis(DBAttribute<?> axis) {
    Object[] data = myData;
    if (data == null)
      return null;
    int length = data.length;
    for (int i = 0; i < length - 1; i += 2) {
      Object p = data[i];
      if (p == null)
        break;
      if (Util.equals(axis, p))
        return (AxisValues) data[i + 1];
    }
    return null;
  }

  public void addAxisExcluded(DBAttribute<?> attribute, SortedSet<Long> excluded) {
    AxisValues values = getOrCreateAxisValues(attribute);
    values.addExcluded(excluded);
  }

  public void addAxisExcluded(DBAttribute<?> attribute, LongList excluded) {
    AxisValues values = getOrCreateAxisValues(attribute);
    values.addExcluded(excluded);
  }

  public void addAxisIncluded(DBAttribute<?> attribute, SortedSet<Long> included) {
    AxisValues axisValues = getOrCreateAxisValues(attribute);
    axisValues.addIncluded(included);
  }

  public void addAxisIncluded(DBAttribute<?> attribute, LongList included) {
    AxisValues axisValues = getOrCreateAxisValues(attribute);
    axisValues.addIncluded(included);
  }

  public void addValues(DBAttribute<?> attribute, Collection<Long> expectedValues, boolean sign) {
    for (Long expectedValue : expectedValues) {
      addValue(attribute, expectedValue, sign);
    }
  }

  public void addValues(DBAttribute<?> attribute, LongList expectedValues, boolean sign) {
    if (expectedValues == null) return;
    for (LongIterator cursor : expectedValues) addValue(attribute, cursor.value(), sign);
  }

  public void addValue(DBAttribute<?> attribute, Long expectedValue, boolean sign) {
    AxisValues values = getOrCreateAxisValues(attribute);
    if (sign)
      values.addIncluded(expectedValue);
    else
      values.addExcluded(expectedValue);
  }

  private AxisValues getOrCreateAxisValues(@NotNull DBAttribute<?> attribute) {
    Object[] data = myData;
    int place = 0;
    if (data != null) {
      final String attrId = attribute.getId();
      int length = data.length;
      int i;
      for (i = 0; i < length - 1; i += 2) {
        if (data[i] == null)
          break;
        DBAttribute<?> key = ((DBAttribute<?>) data[i]);
        if (attribute.equals(key)) {
          AxisValues result = (AxisValues) data[i + 1];
          assert result != null;
          return result;
        } else if (String.CASE_INSENSITIVE_ORDER.compare(key.getId(), attrId) > 0) {
          // the array is sorted
          break;
        }
      }
      place = i;
    }
    AxisValues values = new AxisValues();
    if (data == null) {
      data = new Object[2];
      data[0] = attribute;
      data[1] = values;
      myData = data;
    } else {
      assert place >= 0 && place <= data.length;
      int length = data.length;
      if (length == 0 || data[length - 1] != null) {
        Object[] newData = new Object[((length < 5) ? 10 : (length - 1) * 2)];
        for (int i = 0; i < place; i++)
          newData[i] = data[i];
        newData[place] = attribute;
        newData[place + 1] = values;
        for (int i = place; i < length; i++)
          newData[i + 2] = data[i];
        myData = newData;
      } else {
        assert length % 2 == 0;
        assert data[length - 2] == null;
        assert place >= 0 && place <= length - 2;
        for (int i = length - 3; i >= place; i--) {
          Object o = data[i];
          if (o != null)
            data[i + 2] = o;
        }
        data[place] = attribute;
        data[place + 1] = values;
      }
    }
    return values;
  }

  public void removeAxis(DBAttribute<?> axis) {
    if (myData == null || myData.length == 0 || axis == null)
      return;
    int length = myData.length;
    for (int i = 0; i < length; i += 2) {
      DBAttribute<?> a = (DBAttribute<?>) myData[i];
      if (a == null)
        break;
      if (Util.equals(a, axis)) {
        if (i + 2 < length) {
          System.arraycopy(myData, i + 2, myData, i, length - (i + 2));
        }
        myData[length - 1] = null;
        myData[length - 2] = null;
        break;
      }
    }
  }

  public ItemHypercubeImpl copy() {
    ItemHypercubeImpl result = new ItemHypercubeImpl();
    if (myData != null) {
      int length = myData.length;
      result.myData = new Object[length];
      for (int i = 0; i < length - 1; i += 2) {
        if (myData[i] == null)
          break;
        assert myData[i + 1] != null;
        result.myData[i] = myData[i];
        result.myData[i + 1] = ((AxisValues) myData[i + 1]).copy();
      }
    }
    return result;
  }

  public ItemHypercubeImpl intersect(ItemHypercube cube, boolean precise) {
    ItemHypercubeImpl result = copy();
    if (cube == null)
      return result;

    Set<DBAttribute<?>> axes = cube.getAxes();
    for (DBAttribute<?> axis : axes) {
      AxisValues resultAxis = result.getAxis(axis);
      SortedSet<Long> excluded = cube.getExcludedValues(axis);
      SortedSet<Long> included = cube.getIncludedValues(axis);
      if (resultAxis == null) {
        // copy axis
        resultAxis = result.getOrCreateAxisValues(axis);
        if (excluded != null)
          resultAxis.addExcluded(excluded);
        if (included != null)
          resultAxis.addIncluded(included);
      } else {
        if (included != null) {
          if (resultAxis.myIncluded != null) {
            resultAxis.myIncluded.retainAll(included);
            if (resultAxis.myIncluded.isEmpty()) {
              if (precise) {
                return null;
              } else {
                result.removeAxis(axis);
                continue;
              }
            }
          } else {
            resultAxis.createIncluded();
            resultAxis.myIncluded.addAll(included);
            if (resultAxis.myExcluded != null) {
              resultAxis.myIncluded.removeAll(resultAxis.myExcluded);
              if (resultAxis.myIncluded.isEmpty()) {
                if (precise)
                  return null;
                else {
                  result.removeAxis(axis);
                  continue;
                }
              }
            }
          }
        }

        if (excluded != null) {
          resultAxis.createExcluded();
          resultAxis.myExcluded.addAll(excluded);
          if (resultAxis.myIncluded != null) {
            resultAxis.myIncluded.removeAll(excluded);
            if (resultAxis.myIncluded.isEmpty()) {
              if (precise)
                return null;
              else {
                result.removeAxis(axis);
                continue;
              }
            }
          }
        }
      }
    }
    return result;
  }

  public String toString() {
    StringBuffer buf = new StringBuffer();
    Set<DBAttribute<?>> axes = getAxes();
    for (DBAttribute<?> attr : axes) {
      if (buf.length() > 0)
        buf.append("; ");
      buf.append(attr.getName());
      AxisValues axis = getAxis(attr);
      if (axis == null)
        continue;
      axis.toStringBuffer(buf);
    }
    buf.append(")");
    buf.insert(0, "cube(");
    return buf.toString();
  }

  private static final class AxisValues {
    private SortedSet<Long> myExcluded;
    private SortedSet<Long> myExcludedReadonly;
    private SortedSet<Long> myIncluded;
    private SortedSet<Long> myIncludedReadonly;

    public void addExcluded(SortedSet<Long> values) {
      createExcluded();
      myExcluded.addAll(values);
    }

    public void addExcluded(LongList values) {
      if (values == null || values.isEmpty()) return;
      createExcluded();
      for (int i = 0; i < values.size(); i++) myExcluded.add(Math.max(0, values.get(i)));
    }

    public void addExcluded(Long value) {
      createExcluded();
      myExcluded.add(value);
    }

    private void createExcluded() {
      if (myExcluded == null) {
        myExcluded = Collections15.treeSet();
        assert myExcludedReadonly == null;
        myExcludedReadonly = Collections.unmodifiableSortedSet(myExcluded);
      }
    }

    public void addIncluded(SortedSet<Long> values) {
      createIncluded();
      myIncluded.addAll(values);
    }

    public void addIncluded(LongList values) {
      if (values == null || values.isEmpty()) return;
      createIncluded();
      for (int i = 0; i < values.size(); i++) myIncluded.add(Math.max(0, values.get(i)));
    }

    private void createIncluded() {
      if (myIncluded == null) {
        myIncluded = Collections15.treeSet();
        assert myIncludedReadonly == null;
        myIncludedReadonly = Collections.unmodifiableSortedSet(myIncluded);
      }
    }

    public void addIncluded(Long value) {
      createIncluded();
      myIncluded.add(value);
    }

    public AxisValues copy() {
      AxisValues values = new AxisValues();
      if (myExcluded != null) {
        values.myExcluded = Collections15.treeSet(myExcluded);
        values.myExcludedReadonly = Collections.unmodifiableSortedSet(values.myExcluded);
      }
      if (myIncluded != null) {
        values.myIncluded = Collections15.treeSet(myIncluded);
        values.myIncludedReadonly = Collections.unmodifiableSortedSet(values.myIncluded);
      }
      return values;
    }

    public boolean isSame(AxisValues thatValue) {
      if (thatValue == null)
        return false;
      return Util.equals(myExcluded, thatValue.myExcluded) && Util.equals(myIncluded, thatValue.myIncluded);
    }

    public void toStringBuffer(StringBuffer buf) {
      toStringBuffer(buf, myIncluded, '+');
      toStringBuffer(buf, myExcluded, '-');
    }

    private void toStringBuffer(StringBuffer buf, SortedSet<Long> set, char sign) {
      if (set != null && set.size() > 0) {
        buf.append(sign);
        boolean comma = false;
        for (Long pointer : set) {
          if (comma) {
            buf.append(',');
          } else {
            comma = true;
          }
          buf.append(pointer);
        }
      }
    }
  }
}
