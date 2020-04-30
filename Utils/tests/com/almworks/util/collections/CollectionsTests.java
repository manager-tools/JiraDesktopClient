package com.almworks.util.collections;

import com.almworks.util.tests.BaseTestCase;
import com.almworks.util.tests.CollectionsCompare;
import org.almworks.util.Collections15;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;

/**
 * @author : Dyoma
 */
public class CollectionsTests extends BaseTestCase {
  private final CollectionsCompare CHECK = new CollectionsCompare();

  public void testIterations() {
    FlattenIterator iterator = new FlattenIterator(Arrays.asList(new Iterator[]{
      Collections15.emptyList().iterator(),
      Arrays.asList(new String[]{"1", "2"}).iterator(), null, Collections15.emptyList().iterator()}).iterator());
    CHECK.order(new String[]{"1", "2"}, iterator);
  }

  public void testReverseComparator() {
    Comparator<String> comparator = String.CASE_INSENSITIVE_ORDER;
    assertTrue(comparator.compare("a", "b") < 0);
    Comparator<String> reverse = Containers.reverse(comparator);
    assertTrue(reverse.compare("a", "b") > 0);
    assertSame(comparator, Containers.reverse(reverse));

    assertEquals(0, Containers.checkSameComparators(comparator, comparator));
    assertEquals(1, Containers.checkSameComparators(reverse, String.CASE_INSENSITIVE_ORDER));
    Comparator<String> otherReversed = Containers.reverse(String.CASE_INSENSITIVE_ORDER);
    assertEquals(0, Containers.checkSameComparators(reverse, otherReversed));
    assertEquals(otherReversed, reverse);
    assertEquals(otherReversed.hashCode(), reverse.hashCode());
    assertEquals(-1, Containers.checkSameComparators(comparator, new Comparator() {
      public int compare(Object o, Object o1) {
        return 0;
      }
    }));
  }

  public void testIsOrderValid() {
    assertTrue(Containers.isOrderValidAt(new String[]{"1", "2", "2"}, 1, String.CASE_INSENSITIVE_ORDER));
    assertFalse(Containers.isOrderValidAt(new String[]{"2", "2", "1"}, 1, String.CASE_INSENSITIVE_ORDER));
    assertTrue(Containers.isOrderValidAt(new String[]{"2", "2"}, 1, String.CASE_INSENSITIVE_ORDER));
    assertTrue(Containers.isOrderValidAt(new String[]{"2", "2"}, 0, String.CASE_INSENSITIVE_ORDER));
    assertTrue(Containers.isOrderValidAt(new String[]{"2"}, 0, String.CASE_INSENSITIVE_ORDER));
  }
}
