package com.almworks.util.components;

import com.almworks.util.advmodel.OrderListModel;
import com.almworks.util.collections.ChangeCounter;
import com.almworks.util.collections.IntArray;
import com.almworks.util.commons.Condition;
import com.almworks.util.tests.AwtTestsGate;
import com.almworks.util.tests.BaseTestCase;
import com.almworks.util.tests.CollectionsCompare;
import org.almworks.util.Collections15;
import org.almworks.util.Const;
import org.almworks.util.detach.Lifespan;

import javax.swing.*;
import java.util.Arrays;
import java.util.List;

/**
 * @author : Dyoma
 */
public class ListSelectionAccessorTests extends BaseTestCase {
  private final DefaultListSelectionModel mySelection = new DefaultListSelectionModel();
  private final OrderListModel myList = new OrderListModel();
  private final ListSelectionAccessor myAccessor = new ListSelectionAccessor(mySelection, myList);
  private static final CollectionsCompare CHECK = new CollectionsCompare();

  public ListSelectionAccessorTests() {
    super(AwtTestsGate.AWT_FOR_TEST);
  }

  protected void setUp() throws Exception {
    super.setUp();
    myList.addAll(new Object[]{"0", "1", "2", "3"});
    assertTrue(mySelection.isSelectionEmpty());
  }

  public void testSetSelected() {
    myAccessor.setSelected("0");
    assertEquals(0, mySelection.getMinSelectionIndex());
    assertEquals(0, mySelection.getMaxSelectionIndex());

    myAccessor.setSelected(new Object[]{"1", "3"});
    checkSelection(new boolean[]{false, true, false, true});
  }

  public void testInvert() {
    myAccessor.setSelected("0");
    myAccessor.invertSelection();
    checkRangeSelected(1, 3);

    myAccessor.setSelected("3");
    myAccessor.invertSelection();
    checkRangeSelected(0, 2);

    myAccessor.setSelected(new Object[]{"0", "3"});
    myAccessor.invertSelection();
    checkSelection(new boolean[]{false, true, true, false});
  }

  public void testSelectedItemUpdated() {
    myAccessor.setSelected("0");
    myAccessor.invertSelection();
    ChangeCounter counter = new ChangeCounter();
    myAccessor.addSelectedItemsListener(counter);
    myList.updateElement("2");
    assertEquals(1, counter.getCount());
    myList.updateRange(2, 3);
    assertEquals(2, counter.getCount());
    myList.updateElement("0");
    assertEquals(2, counter.getCount());
  }

  public void testRemoveManyAndSelected() {
    myAccessor.setSelected("0");
    ChangeCounter changCounter = new ChangeCounter();
    myAccessor.addChangeListener(Lifespan.FOREVER, changCounter);
    SelectionChangeCounter selectedCounter = new SelectionChangeCounter();
    myAccessor.addListener(selectedCounter);
    myList.removeRange(0, myList.getSize() - 1);

    assertEquals(1, selectedCounter.getCount());
    assertEquals(1, changCounter.getCount());
    checkSelectionEmpty();
  }

  public void testRemoveLastSelected() {
    myList.removeAll(Condition.always());
    assertEquals(0, myList.getSize());
    myList.addElement("A");
    myAccessor.setSelected("A");
    ChangeCounter changeCounter = new ChangeCounter();
    myAccessor.addChangeListener(Lifespan.FOREVER, changeCounter);
    SelectionChangeCounter selectedCounter = new SelectionChangeCounter();
    myAccessor.addListener(selectedCounter);
    myList.remove("A");
    assertEquals(0, myList.getSize());
    assertEquals(1, changeCounter.getCount());
    assertEquals(1, selectedCounter.getCount());
    checkSelectionEmpty();
  }

  public void testAddRemoveSelection() {
    myAccessor.setSelectedIndexes((int[])null);
    checkSelectionEmpty();
    myAccessor.ensureSelectionExists();
    checkSelectedItems("0");
    myAccessor.addSelectionIndex(0);
    checkSelectedItems("0");
    myAccessor.addSelection("1");
    checkSelectedItems("0", "1");
    myAccessor.removeSelection("2");
    checkSelectedItems("0", "1");
    checkSelectedIndexes(0, 1);
    myAccessor.removeSelectedRange(1, 3);
    checkSelectedItems("0");
    myAccessor.addSelection("2");
    checkSelectedIndexes(0, 2);
    myAccessor.setSelected(new Object[]{"0", "1", "3"});
    checkSelectedIndexes(0, 1, 3);
    myAccessor.setSelectedIndexes(Const.EMPTY_INTS);
    checkSelectedIndexes();
  }

  public void testChangeSelection() {
    myAccessor.clearSelection();
    myAccessor.setSelected("0");
    checkSelectedIndexes(0);
    myAccessor.setSelectedIndex(1);
    checkSelectedIndexes(1);
    myAccessor.updateSelectionAt(IntArray.create(2, 1), true);
    checkSelectedIndexes(1, 2);
    myAccessor.updateSelectionAt(IntArray.create(0, 2, 3), false);
    checkSelectedIndexes(1);
    myAccessor.setSelected("1");
    checkSelectedIndexes(1);
    myAccessor.updateSelectionAt(IntArray.create(0, 3), true);
    checkSelectedIndexes(0, 1, 3);
    myAccessor.setSelected("2");
    checkSelectedIndexes(2);
    myAccessor.setSelectedIndexes(new int[]{2});
    checkSelectedIndexes(2);
    myAccessor.setSelectedIndexes(new int[]{0, 3});
    checkSelectedIndexes(0, 3);
  }
  
  public void testInvertSelection() {
    myAccessor.selectAll();
    myAccessor.invertSelection();
    checkSelectionEmpty();
    myAccessor.invertSelection();
    checkSelectedIndexes(0, 1, 2, 3);

    myAccessor.updateSelectionAt(IntArray.create(0, 2), false);
    myAccessor.invertSelection();
    checkSelectedIndexes(0, 2);
    myAccessor.invertSelection();
    checkSelectedIndexes(1, 3);

    myAccessor.setSelectedIndex(0);
    checkSelectedIndexes(0);
    myAccessor.invertSelection();
    checkSelectedIndexes(1, 2, 3);
    myAccessor.setSelectedIndex(3);
    myAccessor.invertSelection();
    checkSelectedIndexes(0, 1, 2);
    myAccessor.setSelectedIndex(1);
    myAccessor.invertSelection();
    checkSelectedIndexes(0, 2, 3);
  }

  private void checkSelectionEmpty() {
    assertEquals(0, myAccessor.getSelectedItems().size());
    assertSame(null, myAccessor.getSelection());
    assertEquals(0, myAccessor.getSelectedIndexes().length);
    assertEquals(-1, myAccessor.getSelectedIndex());
    assertFalse(myAccessor.hasSelection());
  }

  private void checkRangeSelected(int min, int max) {
    assertEquals(min, mySelection.getMinSelectionIndex());
    for (int i = min; i <= max; i++)
      assertTrue(mySelection.isSelectedIndex(i));
    assertEquals(max, mySelection.getMaxSelectionIndex());
  }

  private void checkSelection(boolean ... selected) {
    assertEquals(selected.length, myList.getSize());
    List<Boolean> expected = Collections15.arrayList();
    for (boolean b : selected) expected.add(b);
    List<Boolean> actual = Collections15.arrayList();
    for (int i = 0; i < myList.getSize(); i++) actual.add(mySelection.isSelectedIndex(i));
    CHECK.order(expected, actual);
  }

  private void checkSelectedItems(String ... items) {
    int[] expected = new int[items.length];
    for (int i = 0, itemsLength = items.length; i < itemsLength; i++) {
      String item = items[i];
      expected[i] = myList.indexOf(item);
    }
    checkSelectedIndexes(expected);
    for (String item : items) assertTrue(myAccessor.isSelected(item));
  }

  private void checkSelectedIndexes(int ... indexes) {
    boolean[] expected = new boolean[myList.getSize()];
    Arrays.fill(expected, false);
    for (int index : indexes) expected[index] = true;
    checkSelection(expected);
  }

  private static class SelectionChangeCounter implements SelectionAccessor.Listener {
    private int myCount = 0;

    public void onSelectionChanged(Object newSelection) {
      myCount++;
    }

    public int getCount() {
      return myCount;
    }
  }
}
