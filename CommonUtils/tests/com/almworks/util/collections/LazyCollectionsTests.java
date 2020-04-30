package com.almworks.util.collections;

import com.almworks.util.commons.Condition;
import com.almworks.util.commons.Function;
import com.almworks.util.tests.BaseTestCase;
import com.almworks.util.tests.CollectionsCompare;

import java.util.*;

import static com.almworks.util.collections.Functional.*;
import static org.almworks.util.Collections15.arrayList;

public class LazyCollectionsTests extends BaseTestCase {
  private final CollectionsCompare compare = new CollectionsCompare();
  private static final Condition<Integer> even = new Condition<Integer>() {
    @Override
    public boolean isAccepted(Integer value) {
      return value % 2 == 0;
    }
  };
  private static final Convertor<Integer, Integer> minus = new Convertor<Integer, Integer>() {
    @Override
    public Integer convert(Integer value) {
      return -value;
    }
  };
  public static final Convertor<Iterable<Integer>, Iterable<Integer>> I = Convertor.<Iterable<Integer>>identity();

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    setWriteToStdout(true);
  }

  public void testFilterIterable() {
    assertEquals(Collections.<Object>emptyList(), arrayList(filter(Collections.emptyList(), Condition.always())));
    assertEquals(Collections.<Object>emptyList(), arrayList(filter(arrayList(2), Condition.never())));
    compare.order(filt(even));
    compare.order(filt(even, 1, 3, 4), 4);
    for (int i = 0; i < 10; ++i) {
      for (Integer elem : filter(randomList(10), even)) {
        assertTrue(randomList(10) + "", even.isAccepted(elem));
      }
    }
  }

  private List<Integer> filt(Condition<? super Integer> cond, Integer ... elements) {
    return arrayList(filter(arrayList(elements), cond));
  }

  private List<Integer> randomList(int maxLen) {
    Random rand = new Random(System.currentTimeMillis());
    int length = Math.abs(rand.nextInt(maxLen));
    List<Integer> l = arrayList(length);
    for (int i = 0; i < length; ++i) {
      l.add(rand.nextInt());
    }
    return l;
  }

  public void testConvert() {
    testConvert(Functional.convertIterable(minus));
    testConvert(Functional.convertList(minus));
    testConvert(Functional.convertCollection(minus));
  }

  private <C extends Iterable<Integer>> void testConvert(Function<C, C> convertF) {
    assertEquals(Collections.<Object>emptyList(), arrayList(convertF.invoke((C)Collections.EMPTY_LIST)));
    assertEquals(Collections.<Object>emptyList(), arrayList(convertList(null, null)));
    compare.order(arrayList(convertF.invoke((C)arrayList(1, -1, 0))), -1, 1, 0);
    for (int i = 0; i < 10; ++i) {
      testRandConvert(10, convertF);
    }
  }

  private <C extends Iterable<Integer>> void testRandConvert(int times, Function<C, C> convertF) {
    List<Integer> l = randomList(times);
    Iterator<Integer> i = convertF.invoke((C)l).iterator();
    Iterator<Integer> j = l.iterator();
    while (i.hasNext() && j.hasNext()) {
      assertEquals(l + "", i.next(), minus.convert(j.next()));
    }
    assertTrue(l + "", !i.hasNext() && !j.hasNext());
  }

  public void testFirst() {
    assertEquals(null, first(null));
    assertEquals(null, first(Collections.<Object>emptyList()));
    fst(null);
    fst(null, null);
    fst(null, null, 1);
    fst(1, 1);
    fst(1, 1, 2);
    fst(1, 1, null);
  }

  private void fst(Integer exp, Integer... el) {
    assertEquals(exp, first(arrayList(el)));
  }

  public void testHasNth() {
    for (int i = 0; i < 3; ++i) {
      List<Integer> l = randomList(5);
      int sz = l.size();
      for (int j = 0; j < 3; ++j) {
        assertEquals(l + " " + j, j < sz, hasNth(l, j));
      }
    }
  }

  public void testIsEmpty() {
    for (int i = 0; i < 3; ++i) {
      List<Integer> l = randomList(2);
      assertEquals(l + "", l.isEmpty(), isEmpty(l));
    }
    assertTrue(isEmpty(null));
    assertTrue(isEmpty(Collections.emptyList()));
  }

  public void testRemoveFromSelectMany() {
    ArrayList<List<Integer>> lists = arrayList(arrayList(1, 2, 3), arrayList(4, 5), Arrays.asList(6));
    Iterable<Integer> sm = selectMany(lists, I);
    compare.order(arrayList(sm), 1, 2, 3, 4, 5, 6);
    for (Iterator<Integer> it = sm.iterator(); it.hasNext();) {
      if (it.next() % 2 == 1) it.remove();
    }
    compare.order(arrayList(sm), 2, 4, 6);
  }
}
