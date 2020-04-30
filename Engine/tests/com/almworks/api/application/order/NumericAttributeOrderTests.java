package com.almworks.api.application.order;

import com.almworks.util.collections.IntArray;
import com.almworks.util.tests.BaseTestCase;
import com.almworks.util.tests.CollectionsCompare;
import org.almworks.util.Collections15;

import java.math.BigDecimal;
import java.util.List;

/**
 * @author dyoma
 */
public class NumericAttributeOrderTests extends BaseTestCase {
  private static final int NA = Integer.MIN_VALUE;
  private static final int V = 1000 * 1000;
  private static final CollectionsCompare CHECK = new CollectionsCompare();

  public void testEmpty() {
    int[] allNA = {NA, NA, NA};
    check(allNA, new int[]{0}, V, NA, NA);
    check(allNA, new int[]{0, 1}, V*4/3 + 1, V*2/3 + 1, NA);
  }

  public void testOnlyMoved() {
    check(new int[]{6, NA, NA, 0}, new int[]{1, 2}, 6, 4, 2, 0);
    check(new int[]{5, NA, NA, 2}, new int[]{1, 2}, 5, 4, 3, 2);
  }

  public void testExpandGroup() {
    check(new int[] {9, 4, NA, 3, 0}, 9, 7, 5, 3, 0);
    checkBits(new int[] {11, 10, NA, 8, NA, 7, 6, 0}, 1, 1, 1, 1, 1);
    check(new int[] {11, 10, NA, 8, NA, 7, 6, -2}, 20, 18, 16, 13, 10, 7, 6, -2);
  }

  public void testBug() {
    check(new String[]{"9.1", null, null, null, "4.1"}, "9.1", "7.9", "6.7", "5.4", "4.1");
  }

  public void testNegativeBug() {
    check(new int[]{1, 0, NA, NA, NA}, 1, 0, -3, -6, -9);
    check(new int[] {2, 1, NA, NA, 0}, 2, 1, -2, -5, -8);
    check(new int[]{-1, -2, NA, NA, -3}, -1, -2, -5, -8, -11);
  }

  public void testMoveOrdered() {
    check(new int[]{NA, 10, NA}, new int[]{0, 1}, 14, 10, NA);
  }
  
  public void testSingleMoveBug() {
    check(new int[] {2, NA, NA}, new int[] {1}, 2, 0, NA);
    check(new int[] {1, NA, NA}, new int[] {1}, 1, -1, NA);
    check(new int[] {0, NA, NA}, new int[] {1}, 0, -2, NA);
    check(new int[] {-1, NA, NA}, new int[] {1}, -1, -3, NA);
  }

  public void testUpToZeroDistribution() {
    check(new int[] {100, NA, NA}, new int[] {1}, 100, 49, NA);
    check(new int[] {3, NA, NA}, new int[] {1}, 3, 0, NA);
  }

  private void check(String[] oldValues, String ... expected) {
    List<BigDecimal> expectedList = toDecimalList(expected);
    List<BigDecimal> oldList = toDecimalList(oldValues);
    int[] indecies = selectIndecies(oldList);
    CHECK.order(expectedList, NumericAttributeOrder.changeValues(oldList, indecies));
  }

  private void checkBits(int[] oldValues, int ... expectedBits) {
    CHECK.bits(NumericAttributeOrder.prepareChanges(toDecimalList(oldValues), selectIndecies(oldValues)), expectedBits);
  }

  private void check(int[] oldValues, int[] indecies, int ... expected) {
    List<BigDecimal> expectedList = toDecimalList(expected);
    List<BigDecimal> oldList = toDecimalList(oldValues);
    CHECK.order(expectedList, NumericAttributeOrder.changeValues(oldList, indecies));
  }

  private List<BigDecimal> toDecimalList(int... expected) {
    List<BigDecimal> expectedList = Collections15.arrayList();
    for (int n : expected)
      expectedList.add(n == NA ? null : new BigDecimal(n));
    return expectedList;
  }

  private List<BigDecimal> toDecimalList(String... expected) {
    List<BigDecimal> result = Collections15.arrayList();
    for (String str : expected)
      result.add(str != null ? new BigDecimal(str) : null);
    return result;
  }

  private void check(int[] oldValues, int ... expected) {
    check(oldValues, selectIndecies(oldValues), expected);
  }

  private int[] selectIndecies(int[] oldValues) {
    IntArray indeces = new IntArray();
    for (int i = 0; i < oldValues.length; i++) {
      int value = oldValues[i];
      if (value == NA)
        indeces.add(i);
    }
    return indeces.toNativeArray();
  }

  private int[] selectIndecies(List<?> oldValues) {
    IntArray indeces = new IntArray();
    for (int i = 0; i < oldValues.size(); i++)
      if (oldValues.get(i) == null)
        indeces.add(i);
    return indeces.toNativeArray();
  }
}
