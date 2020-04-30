package com.almworks.util.collections;

import com.almworks.util.tests.BaseTestCase;

import java.util.Arrays;
import java.util.List;

/**
 * @author : Dyoma
 */
public class ContainersTests extends BaseTestCase {
  public void testIsOrderValid() {
    List<String> ordered = Arrays.asList(new String[]{"1", "2", "3"});
    List<String> unordered = Arrays.asList(new String[]{"1", "3", "2"});
    assertTrue(Containers.isOrderValid(ordered, String.CASE_INSENSITIVE_ORDER));
    assertFalse(Containers.isOrderValid(unordered, String.CASE_INSENSITIVE_ORDER));
  }
}
