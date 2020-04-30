package com.almworks.util.advmodel;

import com.almworks.util.tests.CollectionsCompare;
import com.almworks.util.tests.GUITestCase;
import org.almworks.util.detach.Lifespan;

import java.util.Arrays;

public class UniqueListDecoratorTests extends GUITestCase {
  private OrderListModel<String> mySource;
  private AListModel<String> myModel;
  private AListModel<String> mySorted;

  private AListLogger myLogger = new AListLogger();
  private CollectionsCompare CHECK = new CollectionsCompare();

  protected void setUp() throws Exception {
    mySource = new OrderListModel<String>();
    myModel = UniqueListDecorator.create(Lifespan.FOREVER, mySource);
    myModel.addListener(myLogger);
    mySorted = SortedListDecorator.createForComparables(Lifespan.FOREVER, myModel);
  }

  public void testBoundaryConditions() {
    verify();

    mySource.addElement("");
    myLogger.checkInsert(0, 1);
    verify("");

    mySource.addElement("");
    myLogger.checkSilence();
    verify("");
  }

  public void testAlreadyUnique() {
    mySource.addAll("a", "b", "c", "d");
    myLogger.checkLogSize(4, true);
    verify("a", "b", "c", "d");

    mySource.insert(0, "0");
    myLogger.checkInsert(0, 1);
    verify("0", "a", "b", "c", "d");

    mySource.remove("c");
    myLogger.checkRemove(3, 1);
    verify("0", "a", "b", "d");

    mySource.remove("d");
    myLogger.checkRemove(3, 1);
    verify("0", "a", "b");

    mySource.remove("0");
    myLogger.checkRemove(0, 1);
    verify("a", "b");

    mySource.insertAll(1, "x", "y");
    mySource.insertAll(4, "z");
    verify("a", "x", "y", "b", "z");
  }

  public void testAlreadyUniqueRearranged() {
    mySource.addAll("a", "x", "y", "b", "z");
    mySource.sort(String.CASE_INSENSITIVE_ORDER);
    verify("a", "b", "x", "y", "z");
  }

  public void testInsertSame() {
    mySource.addAll("a", "x", "y", "b", "z");
    myLogger.clear();

    mySource.addElement("x");
    myLogger.checkSilence();
    verify("a", "x", "y", "b", "z");

    mySource.insert(3, "y");
    myLogger.checkSilence();
    verify("a", "x", "y", "b", "z");

    mySource.insert(0, "z");
    myLogger.checkLogSize(1);
    AListModel.RearrangeEvent event = myLogger.popOneRearrageEvent();
    assertEquals(0, event.getLowAffectedIndex());
    assertEquals(4, event.getHighAffectedIndex());
    assertEquals(1, event.getNewIndex(0));
    assertEquals(2, event.getNewIndex(1));
    assertEquals(0, event.getNewIndex(4));
    verify("z", "a", "x", "y", "b");
  }

  public void testRemoveSame() {
    mySource.addAll("a", "x", "y", "b", "z");
    myLogger.clear();

    mySource.remove("y");
    myLogger.checkRemove(2, 1);
    verify("a", "x", "b", "z");

    mySource.addElement("x");
    myLogger.checkSilence();
    mySource.removeAt(4);
    myLogger.checkSilence();
    verify("a", "x", "b", "z");

    mySource.addElement("x");
    myLogger.checkSilence();
    mySource.remove("x");
    myLogger.checkLogSize(2); // remove & add
/*
    AListModel.RearrangeEvent event = myLogger.popOneRearrageEvent();
    assertEquals(1, event.getLowAffectedIndex());
    assertEquals(3, event.getHighAffectedIndex());
    assertEquals(3, event.getNewIndex(1));
    assertEquals(1, event.getNewIndex(2));
    assertEquals(2, event.getNewIndex(3));
*/
    verify("a", "b", "z", "x");
  }

  public void testRemoveRange() {
    mySource.addAll("a", "x", "y", "b", "z", "c");
    myLogger.clear();
    mySource.removeRange(1, 3);
    verify("a", "z", "c");
  }

  public void testRearrange() {
    mySource.addAll("a", "x", "y", "b", "z", "c");
    mySource.sort(String.CASE_INSENSITIVE_ORDER);
    verify("a", "b", "c", "x", "y", "z");
  }
  
  public void testAddSameBefore() {
    mySource.addAll("a", "b", "c");
    verify("a", "b", "c");
    mySource.insert(1, "b");
    verify("a", "b", "c");
  }

  private void verify(String ... values) {
    CHECK.order(values, myModel.toList());
    CHECK.order(values, UniqueListDecorator.create(Lifespan.FOREVER, mySource).toList());
    String[] copy = values.clone();
    Arrays.sort(copy);
    CHECK.order(copy, mySorted.toList());
  }
}
