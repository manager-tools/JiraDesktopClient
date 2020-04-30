package com.almworks.util.advmodel;

import com.almworks.util.collections.Containers;
import com.almworks.util.commons.Condition;
import com.almworks.util.tests.CollectionsCompare;
import com.almworks.util.tests.GUITestCase;
import org.almworks.util.detach.Lifecycle;

import java.util.Collections;

/**
 * :todoc:
 *
 * @author sereda
 */
public class RegressionTests extends GUITestCase {
  public void testFilteringDecoratorRemoveBug() {
    OrderListModel<String> source = OrderListModel.create();
    FilteringListDecorator<String> filter = FilteringListDecorator.create(source);
    filter.setFilter(Condition.<String>always());
    source.addElement("a");
    source.clear();
  }

  public void testReplaceElementsSetIterationBug() {
    OrderListModel<String> model = OrderListModel.create();
    model.addAll(new String[] {"A", "B", "C"});
    model.replaceElementsSet(Collections.singleton("C"));
    new CollectionsCompare().order(new String[] {"C"}, model.toList());
  }

  public void testResyncFilteredModelGetsOutOfBounds() {
    OrderListModel<String> mySource = new OrderListModel<String>();
    FilteringListDecorator<String> myModel = FilteringListDecorator.create(mySource);
    mySource.addElement("1");
    mySource.addElement("2");
    mySource.addElement("3");
    mySource.addElement("100");
    myModel.setFilter(new Condition<String>() {
      public boolean isAccepted(String s) {
        return s.length() > 2;
      }
    });
    myModel.setFilter(Condition.never());
    assertEquals(0, myModel.getSize());
  }

  public void testResyncFilteredModelGetsOutOfBounds2() {
    // BaseFilteringDecorator did not decrease the source indexes after removing
    OrderListModel<String> mySource = new OrderListModel<String>();
    FilteringListDecorator<String> myModel = FilteringListDecorator.create(mySource);
    mySource.addElement("1");
    mySource.addElement("2");
    mySource.addElement("3");
    mySource.addElement("100");
    mySource.addElement("4");
    mySource.addElement("5");
    // ==== here is the important point ====
    mySource.removeAt(2);
    // ==== ====
    myModel.setFilter(new Condition<String>() {
      public boolean isAccepted(String s) {
        return s.length() > 2;
      }
    });
    assertEquals(1, myModel.getSize());
    myModel.setFilter(Condition.never());
    assertEquals(0, myModel.getSize());
    myModel.setFilter(null);
    assertEquals(5, myModel.getSize());
  }

  public void testSortedListDecoratorLosesLifeAfterFirstDetach() {
    SortedListDecorator<String> sorted = SortedListDecorator.createEmpty();
    sorted.setComparator(Containers.<String>comparablesComparator());
    OrderListModel<String> modelA = new OrderListModel<String>();
    Lifecycle cycle = new Lifecycle();
    cycle.lifespan().add(sorted.setSource(modelA));
    modelA.addElement("A");
    assertEquals("A", sorted.getAt(0));
    cycle.cycle();
    OrderListModel<String> modelB = new OrderListModel<String>();
    cycle.lifespan().add(sorted.setSource(modelB));
    modelB.addElement("B");
    assertEquals("B", sorted.getAt(0));
  }

  public void testFilteringDecoratorIgnoresUpdates() {
    OrderListModel<StringBuffer> source = OrderListModel.create();
    source.addElement(new StringBuffer("x"));
    FilteringListDecorator<StringBuffer> decorator = FilteringListDecorator.create(source);
    decorator.setFilter(new Condition<StringBuffer>() {
      public boolean isAccepted(StringBuffer value) {
        return value.length() % 2 == 0;
      }
    });

    assertEquals(0, decorator.getSize());

    source.getAt(0).append('y');
    source.updateAt(0);

    assertEquals(1, decorator.getSize());
  }

  public void testFilteringDecoratorDoesNotFireUpdates() {
    OrderListModel<StringBuffer> source = OrderListModel.create();
    source.addElement(new StringBuffer("xz"));
    FilteringListDecorator<StringBuffer> decorator = FilteringListDecorator.create(source);
    decorator.setFilter(new Condition<StringBuffer>() {
      public boolean isAccepted(StringBuffer value) {
        return value.length() % 2 == 0;
      }
    });

    assertEquals(1, decorator.getSize());
    AListLogger logger = new AListLogger();
    decorator.addListener(logger);

    source.getAt(0).append("yx");
    source.updateAt(0);

    logger.checkUpdate(0, 1);
  }
}
