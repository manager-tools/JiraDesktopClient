package com.almworks.syncreg;

import com.almworks.util.tests.BaseTestCase;
import org.almworks.util.Collections15;
import org.almworks.util.Const;

import java.util.Arrays;
import java.util.Set;

public class EquidimensionalSetTests extends BaseTestCase {
  public void testSingleCubes() {
    checkSingleCube("1 +1");
    checkSingleCube("1 -1");
    checkSingleCube("1 +1 -2");
    checkSingleCube("1 +1,2,3");
    checkSingleCube("1 -1,2,3");
    checkSingleCube("1 +5,6,7 -1,2,3");

    checkSingleCube("1 +1; 2 +1");
    checkSingleCube("1 +1; 2 -1");
    checkSingleCube("1 -1; 2 +1");
    checkSingleCube("1 -1; 2 -1");
    checkSingleCube("1 -1,2,3; 2 -1,2,3");
    checkSingleCube("1 -1,2,3; 2 +1,2,3");
    checkSingleCube("1 +1,2,3 -5; 2 +1,2,3 -6,7");
  }

  public void testEncompassing() {
    checkEncompassing("1 +1,2,3", "1 +1", true, false);
    checkEncompassing("1 -1", "1 -1,2,3", true, false);
    checkEncompassing("1 -1", "1 -1,2,3", true, false);
    checkEncompassing("1 -1", "1 +1", false, false);
    checkEncompassing("1 -1", "1 +2", true, false);

    checkEncompassing("1 +1", "1 +1; 2 +2", true, false);
    checkEncompassing("1 +1,2", "1 +1; 2 -2", true, false);
    checkEncompassing("1 +1,2", "1 +1,2,3; 2 -2", false, false);

    checkEncompassing("1 +1,2; 2 +3,4", "1 +1,2; 2 +3,4,5", false, true);
    checkEncompassing("1 +1,2; 2 +3,4", "1 +1; 2 +3,4,5", false, false);
    checkEncompassing("1 +1,2; 2 +3,4", "1 +1; 2 +3", true, false);
    checkEncompassing("1 +1,2; 2 +3,4", "1 +1,2; 2 +3,4; 3 +5", true, false);

    checkEncompassing("1 -1; 2 -2", "1 -1,2; 2 -1,2", true, false);
    checkEncompassing("1 -1; 2 -2", "1 -1,2; 2 -1", false, false);
    checkEncompassing("1 -1; 2 +1,2,3", "1 -1,2; 2 +2,3", true, false);
  }

  public void testRemoval() {
    checkRemoveEncompassedBy(new String[] {"1 +1", "1 +2"}, "1 +1", new String[] {"1 +2"});
    checkRemoveEncompassedBy(new String[] {"1 +1; 2 +1", "1 +2; 2 +2", "1 +1; 2 +2"}, "1 +1", new String[] {"1 +2; 2 +2"});
    checkRemoveEncompassing(new String[] {"1 +1; 2 +1", "1 +2; 2 +2,3,4", "1 +1; 2 +2"}, "1 +2; 2 +2,3", new String[] {"1 +1; 2 +1", "1 +1; 2 +2"});
  }

  private void checkRemoveEncompassing(String[] initial, String removed, String[] mustRemain) {
    EquidimensionalSet set = createSet(initial);
    set.removeEncompassing(cube(removed));
    checkRemoved(set, initial, mustRemain);
  }

  private void checkRemoveEncompassedBy(String[] initial, String removed, String[] mustRemain) {
    EquidimensionalSet set = createSet(initial);
    set.removeEncompassedBy(cube(removed));
    checkRemoved(set, initial, mustRemain);
  }

  private void checkRemoved(EquidimensionalSet set, String[] initial, String[] mustRemain) {
    Set<String> r = Collections15.hashSet(mustRemain);
    for (String cube : initial) {
      boolean must = r.contains(cube);
      NumberedCube numberedCube = cube(cube);
      assertEquals(must, set.encompasses(numberedCube));
    }
  }

  private EquidimensionalSet createSet(String[] initial) {
    EquidimensionalSet set = cubeSet(initial[0]);
    for (int i = 1; i < initial.length; i++) {
      set.addCube(cube(initial[i]));
    }
    return set;
  }

  private void checkEncompassing(String superCube, String subCube, boolean forward, boolean backward) {
    checkEncompassing(superCube, subCube, forward);
    checkEncompassing(subCube, superCube, backward);
  }

  private void checkEncompassing(String superCube, String subCube, boolean forward) {
    EquidimensionalSet set = cubeSet(superCube);
    assertEquals(forward, set.encompasses(cube(subCube)));
  }

  private EquidimensionalSet cubeSet(String cubeText) {
    NumberedCube cube = cube(cubeText);
    EquidimensionalSet set = new EquidimensionalSet(cube.getAttributes().length);
    assertFalse(set.encompasses(cube));
    set.addCube(cube);
    assertTrue(set.encompasses(cube));
    return set;
  }

  private void checkSingleCube(String formula) {
    EquidimensionalSet set = cubeSet(formula);
    NumberedCube cube = cube(formula);
    set.removeEncompassedBy(cube);
    assertFalse(set.encompasses(cube));
    set.addCube(cube);
    assertTrue(set.encompasses(cube));
    set.removeEncompassing(cube);
    assertFalse(set.encompasses(cube));
  }

  static NumberedCube cube(String text) {
    if (text == null || text.length() == 0)
      return new NumberedCube(Const.EMPTY_STRINGS, null, null);
    String[] cubes = text.split("\\s*\\;\\s*");
    int count = cubes.length;
    String[] attributes = new String[count];
    long[][] includes = new long[count][];
    long[][] excludes = new long[count][];
    for (int i = 0; i < count; i++) {
      String[] strings = cubes[i].split("\\s+");
      assert strings.length == 2 || strings.length == 3;
      attributes[i] = strings[0];
      for (int j = 1; j < strings.length; j++) {
        String valueString = strings[j];
        char sign = valueString.charAt(0);
        assert sign == '+' || sign == '-';
        String[] values = valueString.substring(1).split(",");
        long[][] array = sign == '+' ? includes : excludes;
        array[i] = new long[values.length];
        for (int k = 0; k < values.length; k++) {
          array[i][k] = Long.parseLong(values[k]);
        }
        Arrays.sort(array[i]);
      }
    }
    return new NumberedCube(attributes, includes, excludes);
  }
}
