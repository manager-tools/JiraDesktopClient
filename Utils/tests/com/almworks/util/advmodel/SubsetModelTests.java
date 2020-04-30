package com.almworks.util.advmodel;

import com.almworks.util.collections.Convertor;
import com.almworks.util.commons.Condition;
import com.almworks.util.tests.CollectionsCompare;
import com.almworks.util.tests.GUITestCase;
import org.almworks.util.Collections15;
import org.almworks.util.detach.Lifespan;

import java.util.Collections;

import static com.almworks.util.collections.Functional.convert;
import static org.almworks.util.Collections15.arrayList;

/**
 * @author : Dyoma
 */
public class SubsetModelTests extends GUITestCase {
  private final CollectionsCompare CHECK = new CollectionsCompare();
  private final OrderListModel<String> myModel = OrderListModel.create();

  public void testAddingToSubset() {
    SubsetModel<String> subset = SubsetModel.create(Lifespan.FOREVER, myModel, true);
    myModel.addElement("1");
    CHECK.singleElement("1", subset.toList());
    CHECK.empty(subset.getComplementSet().toList());
  }

  public void testAddingToComplement() {
    SubsetModel<String> subset = SubsetModel.create(Lifespan.FOREVER, myModel, false);
    AListModel<String> complementSet = subset.getComplementSet();
    myModel.addElement("1");
    CHECK.singleElement("1", complementSet.toList());
    CHECK.empty(subset.toList());
  }

  public void testComplementSetLeftOverCausedBySortedListDecoratorBug() {
    myModel.addAll(new String[] {"5", "3", "4", "1", "2"});
    SubsetModel<String> model = SubsetModel.create(Lifespan.FOREVER, myModel, false);
    model.addFromFullSet(new int[] {1, 2, 3});
    AListModel<String> unselected =
      SortedListDecorator.createForComparables(Lifespan.FOREVER, model.getComplementSet());
    AListModel<String> selected = SortedListDecorator.createForComparables(Lifespan.FOREVER, model);
    CHECK.order(new String[] {"1", "3", "4"}, selected.toList());
    CHECK.order(new String[] {"2", "5"}, unselected.toList());
    model.removeAllAt(new int[] {0, 1, 2});
    CHECK.order(new String[] {}, selected.toList());
    CHECK.order(new String[] {"1", "2", "3", "4", "5"}, unselected.toList());
    model.addFromComplementSet(Collections.singletonList("2"));
    CHECK.singleElement("2", model.toList());
    CHECK.unordered(model.getComplementSet().toList(), new String[]{"1",  "3", "4", "5"});
    CHECK.order(new String[] {"2"}, selected.toList());
    CHECK.order(new String[] {"1",  "3", "4", "5"}, unselected.toList());
  }

  public void testSourceUpdate() {
    myModel.addAll("1", "2", "3");
    SubsetModel<String> model = SubsetModel.create(Lifespan.FOREVER, myModel, true);
    CHECK.order(model.toList(), "1", "2", "3");
    myModel.replaceAt(1, "5");
    CHECK.order(model.toList(), "1", "5", "3");
  }

// todo This test shouldn't work: source removes should not lead to subset removes, see javadoc for DefaultSubsetModel class
//  public void testSourceRemove() {
//    myModel.addAll("1", "2", "3");
//    SubsetModel<String> model = SubsetModel.create(Lifespan.FOREVER, myModel, true);
//    CHECK.order(model.toList(), "1", "2", "3");
//    myModel.remove("1");
//    CHECK.order(model.toList(), "2", "3");
//  }

  public void testSourceRearrange() {
    myModel.addAll("0", "1", "2", "3", "4", "5");
    SubsetModel<String> model = SubsetModel.create(Lifespan.FOREVER, myModel, false);
    model.addFromComplementSet(arrayList("1", "2", "5"));
    CHECK.order(model.toList(), "1", "2", "5");
    myModel.swap(3, 4); // not in subset
    CHECK.order(model.toList(), "1", "2", "5");
    myModel.swap(1, 5);
    // Subset should not be reordered: see class javadoc for DefaultSubsetModel
    CHECK.order(model.toList(), "1", "2", "5");
    myModel.replaceAt(1, "10");
    CHECK.order(model.toList(), "1", "2", "10");
    myModel.rearrange(0, Collections15.<String>arrayList("0", "1", "2", "3", "4", "10"));
    CHECK.order(model.toList(), "1", "2", "10");
    myModel.rearrange(0, Collections15.<String>arrayList("0", "2", "4", "1", "3", "10"));
    CHECK.order(model.toList(), "1", "2", "10");
    myModel.replaceAt(1, "20");
    model.removeAllAt(0);
    CHECK.order(model.toList(), "20", "10");
  }

  public void testRemoveThenAdd() {
    myModel.addAll("0", "1", "2", "3", "4", "5");
    SubsetModel<String> model = SubsetModel.create(Lifespan.FOREVER, myModel, false);
    model.addFromFullSet(4, 0, 1, 5, 2);
    CHECK.order(model.toList(), "4", "0", "1", "5", "2");
    myModel.removeRange(1, 3);
    CHECK.order(model.toList(), "4", "0", "1", "5", "2");
    myModel.replaceAt(0, "100");
    CHECK.order(model.toList(), "4", "100", "1", "5", "2");
    myModel.addElement("6");
    myModel.replaceAt(3, "60");
    CHECK.order(myModel.toList(), "100", "4", "5", "60");
    CHECK.order(model.toList(), "4", "100", "1", "5", "2");
    myModel.addElement("1");
    CHECK.order(model.toList(), "4", "100", "1", "5", "2");
    myModel.addElement("2");
    CHECK.order(model.toList(), "4", "100", "1", "5", "2");
    myModel.swap(4, 5);
    myModel.replaceAt(5, "-1");
    CHECK.order(model.toList(), "4", "100", "-1", "5", "2");
    myModel.addElement("3");
    CHECK.order(model.toList(), "4", "100", "-1", "5", "2");
    model.insertFromComplementSet(1, "3");
    myModel.replaceAt(6, "30");
    CHECK.order(model.toList(), "4", "30", "100", "-1", "5", "2");
    model.add("6");
    CHECK.order(model.toList(), "4", "30", "100", "-1", "5", "2", "6");
    myModel.insert(0, "6");
    CHECK.order(model.toList(), "4", "30", "100", "-1", "5", "2", "6");
    myModel.replaceAt(0, "60");
    CHECK.order(model.toList(), "4", "30", "100", "-1", "5", "2", "60");
  }

  public void testUpdatesAfterUserManipulationsWithSubset() {
    myModel.addAll("0", "1", "2", "3", "4", "5");
    SubsetModel<String> model = SubsetModel.create(Lifespan.FOREVER, myModel, false);
    model.add("-2");
    model.insertFromComplementSet(0, "0");
    model.insertFromComplementSet(0, "4");
    model.insertFromComplementSet(1, "2");
    CHECK.order(model.toList(), "4", "2", "0", "-2");
    model.swap(0, 1);
    CHECK.order(model.toList(), "2", "4", "0", "-2");
    model.swap(1, 2);
    CHECK.order(model.toList(), "2", "0", "4", "-2");
    model.swap(2, 3);
    CHECK.order(model.toList(), "2", "0", "-2", "4");
    myModel.replaceAt(4, "40");
    CHECK.order(model.toList(), "2", "0", "-2", "40");
    model.removeAllAt(0, 2, 3);
    CHECK.order(model.toList(), "0");
    myModel.replaceAt(4, "4");
    CHECK.order(model.toList(), "0");
    model.setFull();
    CHECK.order(model.toList(), "0", "1", "2", "3", "4", "5");
    myModel.replaceAt(1, "10");
    CHECK.order(model.toList(), "0", "10", "2", "3", "4", "5");
    myModel.replaceAt(1, "1");
    model.setSubset(arrayList("5", "4", "3", "2", "1"));
    CHECK.order(model.toList(), "5", "4", "3", "2", "1");
    model.removeAll(arrayList("2", "0", "5", "4"));
    CHECK.order(model.toList(), "3", "1");
    model.insertFromComplementSet(0, "4");
    model.insertFromComplementSet(2, "2");
    model.insertFromComplementSet(4, "0");
    CHECK.order(model.toList(), "4", "3", "2", "1", "0");
    model.removeAll(new Condition<String>() {
      @Override
      public boolean isAccepted(String value) {
        return Integer.valueOf(value) % 2 == 0;
      }
    });
    CHECK.order(model.toList(), "3", "1");
    model.swap(0, 1);
    CHECK.order(model.toList(), "1", "3");
    myModel.replaceAt(1, "10");
    myModel.replaceAt(2, "20");
    myModel.replaceAt(3, "30");
    CHECK.order(model.toList(), "10", "30");
    model.setSubsetIndices(5, 3, 1, 2, 0);
    CHECK.order(model.toList(), "5", "30", "10", "20", "0");
    // Once more (no change)
    model.setSubsetIndices(5, 3, 1, 2, 0);
    CHECK.order(model.toList(), "5", "30", "10", "20", "0");
    model.setSubsetIndices(5, 1, 3, 2, 0);
    CHECK.order(model.toList(), "5", "10", "30", "20", "0");
    myModel.replaceAt(2, "2");
    myModel.replaceAt(1, "1");
    myModel.replaceAt(3, "3");
    model.setSubsetIndices(2, 3);
    CHECK.order(model.toList(), "2", "3");

    model.setFull();
    myModel.removeAll(new int[] {1, 2, 4});
    CHECK.order(model.toList(), "0", "1", "2", "3", "4", "5");
    model.removeAll(new Condition<String>() {
      @Override
      public boolean isAccepted(String value) {
        Integer i = Integer.valueOf(value);
        return i % 2 != 0 && i < 5;
      }
    });
    CHECK.order(model.toList(), "0", "2", "4", "5");
    myModel.replaceAt(1, "-3");
    CHECK.order(model.toList(), "0", "2", "4", "5");
    myModel.replaceAt(2, "-5");
    CHECK.order(model.toList(), "0", "2", "4", "-5");
    myModel.addAll("1", "2", "4");
    CHECK.order(model.toList(), "0", "2", "4", "-5");
    myModel.replaceAt(4, "-2");
    CHECK.order(model.toList(), "0", "-2", "4", "-5");
    myModel.replaceAt(5, "-4");
    CHECK.order(myModel.toList(), "0", "-3", "-5", "1", "-2", "-4");
    CHECK.order(model.toList(), "0", "-2", "-4", "-5");
    model.insertFromComplementSet(2, "-3");
    CHECK.order(model.toList(), "0", "-2", "-3", "-4", "-5");
    myModel.removeAll(arrayList("-3", "0", "-4"));
    CHECK.order(myModel.toList(), "-5", "1", "-2");
    CHECK.order(model.toList(), "0", "-2", "-3", "-4", "-5");

    model.removeAllAt(0, 1, 2);
    CHECK.order(model.toList(), "-4", "-5");
    myModel.replaceAt(0, "5");
    CHECK.order(model.toList(), "-4", "5");
    myModel.addAll("0", "-4");
    CHECK.order(myModel.toList(), "5", "1", "-2", "0", "-4");
    CHECK.order(model.toList(), "-4", "5");
    model.swap(0, 1);
    CHECK.order(model.toList(), "5", "-4");
    myModel.replaceAt(2, "2");
    CHECK.order(model.toList(), "5", "-4");
    myModel.replaceAt(4, "4");
    CHECK.order(model.toList(), "5", "4");

    myModel.remove("5");
    CHECK.order(myModel.toList(), "1", "2", "0", "4");
    model.addFromFullSet(0, 1, 2);
    CHECK.order(model.toList(), "5", "4", "1", "2", "0");
    model.removeAll(arrayList("5", "1", "0"));
    CHECK.order(model.toList(), "4", "2");
    myModel.replaceAt(1, "-2");
    CHECK.order(model.toList(), "4", "-2");
  }

  public void testAddElementTwice() {
    OrderListModel<Holder> full = new OrderListModel<Holder>();
    full.addAll(arrayList(convert(arrayList(110, 111, 112), FROM_INT)));
    SubsetModel<Holder> subset = new DefaultSubsetModel<Holder>(Lifespan.FOREVER, full, false);
    subset.addFromFullSet(0, 1, 2, 0);
    CHECK.order(arrayList(convert(arrayList(110, 111, 112), FROM_INT)), subset.toList());
  }

  public static Convertor<Integer, Holder> FROM_INT = new Convertor<Integer, Holder>() {
    @Override
    public Holder convert(Integer value) {
      return new Holder(value);
    }
  };

  public static class Holder {
    private final int myInt;

    public Holder(int anInt) {
      myInt = anInt;
    }

    @Override
    public String toString() {
      return String.valueOf(myInt);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o)
        return true;
      if (o == null || getClass() != o.getClass())
        return false;

      Holder holder = (Holder) o;

      if (myInt != holder.myInt)
        return false;

      return true;
    }

    @Override
    public int hashCode() {
      return myInt;
    }
  }
}
