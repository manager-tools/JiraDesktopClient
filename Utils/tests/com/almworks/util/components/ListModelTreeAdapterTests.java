package com.almworks.util.components;

import com.almworks.util.Pair;
import com.almworks.util.advmodel.OrderListModel;
import com.almworks.util.collections.Convertor;
import com.almworks.util.collections.FactoryWithParameter;
import com.almworks.util.tests.GUITestCase;
import org.almworks.util.Collections15;
import org.almworks.util.detach.DetachComposite;

import java.util.Comparator;
import java.util.List;

/**
 * @author Vasya
 */
public class ListModelTreeAdapterTests extends GUITestCase {
  final OrderListModel<Pair<Integer, String>> myListModel = new OrderListModel<Pair<Integer, String>>();

  final FactoryWithParameter<TreeModelBridge<Pair<Integer, String>>, Pair<Integer, String>> myFactory = new FactoryWithParameter<TreeModelBridge<Pair<Integer, String>>, Pair<Integer, String>>() {
    public TreeModelBridge<Pair<Integer, String>> create(Pair<Integer, String> pair) {
      return new TreeModelBridge<Pair<Integer, String>>(pair);
    }
  };

  final Convertor<Pair<Integer, String>, Pair<?, ?>> myTreeFunction = new Convertor<Pair<Integer, String>, Pair<?, ?>>() {
    public Pair<?, ?> convert(Pair<Integer, String> pair) {
      if (pair == null) {
        //noinspection ConstantConditions
        return null;
      } else {
        return Pair.create(pair.getFirst(), detectParentIndex(pair));
      }
    }

    private Object detectParentIndex(Pair<Integer, String> pair) {
      String str = pair.getSecond();
      int pos = str.indexOf("#");
      if (pos > -1) {
        StringBuffer sb = new StringBuffer();
        int i = pos + 1;
        while (Character.isDigit(str.charAt(i))) {
          sb.append(str.charAt(i));
          i++;
        }
        try {
          return Integer.parseInt(sb.toString());
        } catch (NumberFormatException e) {
          return null;
        }
      }
      return null;
    }
  };
  final Comparator<? super Pair<Integer, String>> myCompataror = new Comparator<Pair<Integer, String>>() {
    public int compare(Pair<Integer, String> pair, Pair<Integer, String> pair1) {
      return pair.getFirst().compareTo(pair1.getFirst());
    }
  };

  ListModelTreeAdapter<Pair<Integer, String>, TreeModelBridge<Pair<Integer, String>>> myAdapter =
    ListModelTreeAdapter.create(myListModel, myFactory, myTreeFunction, myCompataror);
  private DetachComposite myDetach;

  private Pair<Integer, String> description = Pair.create(0, "Description");
  private Pair<Integer, String> comment1 = Pair.create(1, "text1");
  private Pair<Integer, String> comment2 = Pair.create(2, "(In reply to comment #1)\n> text1\ntext2");
  private Pair<Integer, String> comment3 = Pair.create(3,
    "(In reply to comment #2)\n> (In reply to comment #1)\ntext3");
  private Pair<Integer, String> comment4 = Pair.create(4, "(In reply to comment #3)\n> (In reply to comment #2)\n" +
    "> > (In reply to comment #1)\n> text3\n\ntext4");
  private Pair<Integer, String> comment5 = Pair.create(5, "(In reply to comment #4)\n" +
    "> (In reply to comment #3)\n" +
    "> > (In reply to comment #2)\n" +
    "> > > (In reply to comment #1)\n" +
    "> > text3\n" +
    "> \n" +
    "> text4\n" +
    "\n" +
    "text5");
  private final Pair<Integer, String> comment6 = Pair.create(6, ">text3\ntext6");
  List<Pair<Integer, String>> elements = Collections15.arrayList();

  {
    elements.add(description);
    elements.add(comment1);
    elements.add(comment2);
    elements.add(comment3);
    elements.add(comment4);
    elements.add(comment5);
    elements.add(comment6);
  }

  protected void setUp() throws Exception {
    super.setUp();
    myListModel.clear();
    myDetach = new DetachComposite();
    myAdapter.attach(myDetach);
  }

  protected void tearDown() throws Exception {
    super.tearDown();
    myDetach.detach();
    myListModel.clear();
  }

//  public void testRootElement() {
//    assertEquals(description, myAdapter.getRootNode().getUserObject());
//  }

  public void testSingleElement() {
    myListModel.addElement(comment1);
    assertEquals(1, myAdapter.getRootNode().getChildCount());
//    assertEquals(comment1, myAdapter.getRootNode().getChildAt(0).getUserObject());
  }

  public void testTree() {
    myListModel.addElement(comment1);
    assertEquals(1, myAdapter.getRootNode().getChildCount());
    myListModel.addElement(comment2);
    myListModel.addElement(comment3);
    myListModel.addElement(comment4);
    myListModel.addElement(comment5);
    myListModel.addElement(comment6);
//    assertEquals(2, myAdapter.getRootNode().getChildCount());
//    myListModel.setElements(elements);
//    assertEquals(2, myAdapter.getRootNode().getChildCount());
  }
}
