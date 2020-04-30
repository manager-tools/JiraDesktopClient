package com.almworks.util.advmodel;

import com.almworks.util.collections.Convertors;
import com.almworks.util.commons.Condition;
import com.almworks.util.tests.CollectionsCompare;
import com.almworks.util.tests.GUITestCase;
import org.almworks.util.detach.Lifespan;

public class FilteringConvertingListDecoratorTests extends GUITestCase {

  @Override
  protected void setUp() throws Exception {
    super.setUp();
  }

  public void testUpdate() {
    OrderListModel<Integer> ints = OrderListModel.create(0, 10, 20);
    FilteringConvertingListDecorator<Integer, String> strings = FilteringConvertingListDecorator.create(Lifespan.FOREVER, ints, Condition.always(), Convertors.TO_STRING);
    CollectionsCompare compare = new CollectionsCompare();
    compare.order(strings.toList(), "0", "10", "20");
    ints.replaceAt(0, 1);
    compare.order(strings.toList(), "1", "10", "20");
  }

  public void testUpdate2() {
    OrderListModel<Integer> ints = OrderListModel.create(0, 10, 20);
    FilteringConvertingListDecorator<Integer, String> strings = FilteringConvertingListDecorator.create(Lifespan.FOREVER, ints, new Condition<Integer>() {
      @Override
      public boolean isAccepted(Integer value) {
        return value != null && value % 10 == 0;
      }
    }, Convertors.TO_STRING);
    CollectionsCompare compare = new CollectionsCompare();
    compare.order(strings.toList(), "0", "10", "20");
    ints.replaceAt(0, 1);
    compare.order(strings.toList(), "10", "20");
  }
}
