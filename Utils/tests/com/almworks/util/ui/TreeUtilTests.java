package com.almworks.util.ui;

import com.almworks.util.collections.Convertor;
import com.almworks.util.tests.CollectionsCompare;
import com.almworks.util.tests.GUITestCase;

import javax.swing.tree.TreePath;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * @author dyoma
 */
public class TreeUtilTests extends GUITestCase {
  public void testTreeOrder() {
    Comparator<TreePath> pathOrder = TreeUtil.treeUserObjectOrder(String.CASE_INSENSITIVE_ORDER);
    TreePath root1 = new TreePath("1");
    TreePath root2 = new TreePath("2");
    checkLess(pathOrder, root1, root2);
    TreePath child11 = root1.pathByAddingChild("0");
    checkLess(pathOrder, root1, child11);
    checkLess(pathOrder, child11, root2);
  }

  private void checkLess(Comparator<TreePath> pathOrder, TreePath path1, TreePath path2) {
    assertTrue(pathOrder.compare(path1, path2) < 0);
    assertTrue(pathOrder.compare(path2, path1) > 0);
  }

  private static final Convertor<Integer, Integer> PARENT_FUNCTION = new Convertor<Integer, Integer>() {
    public Integer convert(Integer value) {
      if (value == null || value == 0)
        return null;
      else
        return value / 10;
    }
  };

  public void testExcludeDescendants() {
    checkExclude(new Integer[] {});
    checkExclude(new Integer[] {0}, 0);
    checkExclude(new Integer[] {1}, 1);
    checkExclude(new Integer[] {1, 1}, 1);
    checkExclude(new Integer[] {1, 2, 3}, 1, 2, 3);
    checkExclude(new Integer[] {1, 2, 3, 22, 33, 44}, 1, 2, 3, 44);
    checkExclude(new Integer[] {1, 2, 3, 22, 33, 44}, 1, 2, 3, 44);
    checkExclude(new Integer[] {111, 222, 11, 2}, 11, 2);
    checkExclude(new Integer[] {111, 222, 11, 2, 0}, 0);
  }

  private void checkExclude(Integer[] collection, Integer ... remaining) {
    List<Integer> list = Arrays.asList(collection);
    Set<Integer> result;
    result = TreeUtil.excludeDescendants(list, PARENT_FUNCTION);
    new CollectionsCompare().unordered(result, remaining);
    int length = collection.length;
    for (int i = 0; i < length - 1; i++) {
      int tmp = collection[0];
      System.arraycopy(collection, 1, collection, 0, length - 1);
      collection[length - 1] = tmp;
      result = TreeUtil.excludeDescendants(list, PARENT_FUNCTION);
      new CollectionsCompare().unordered(result, remaining);
    }
  }
}
