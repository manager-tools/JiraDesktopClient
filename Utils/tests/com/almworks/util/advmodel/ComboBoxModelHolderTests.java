package com.almworks.util.advmodel;

import com.almworks.util.tests.CollectionsCompare;
import com.almworks.util.tests.GUITestCase;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.Lifespan;

import java.util.Arrays;
import java.util.Collections;

public class ComboBoxModelHolderTests extends GUITestCase {
  private ComboBoxModelHolder<String> myHolder;
  private final CollectionsCompare myCompare = new CollectionsCompare();


  protected void setUp() throws Exception {
    super.setUp();
    myHolder = ComboBoxModelHolder.create();
  }

  protected void tearDown() throws Exception {
    myHolder = null;
    super.tearDown();
  }

  public void testModelChanges() {
    SelectionInListModel<String> cbModel1 = SelectionInListModel.create(Arrays.asList("1", "2", "3"), "2");
    Detach detach = myHolder.setModel(cbModel1);

    myCompare.order(new Object[] {"1", "2", "3"}, myHolder.toList());
    assertEquals("2", myHolder.getSelectedItem());
    myHolder.setSelectedItem("3");
    assertEquals("3", cbModel1.getSelectedItem());

    SelectionInListModel<String> cbModel2 = SelectionInListModel.create(Arrays.asList("2", "4", "3", "5"), "2");
    RemovedElementsTestLogger rlogger = new RemovedElementsTestLogger(myHolder);
    myHolder.addRemovedElementListener(rlogger);
    myHolder.setModel(cbModel2);
    rlogger.checkList(3, "1", "2", "3");

    myCompare.order(new Object[] {"2", "4", "3", "5"}, myHolder.toList());
    assertEquals("3", cbModel1.getSelectedItem());
  }

  public void testListeners() {
    final int[] update = new int[] {0};
    final int[] selection = new int[] {0};
    final int[] inserts = new int[] {0};
    ComboBoxModelHolder holder = new ComboBoxModelHolder();
    holder.addSelectionListener(Lifespan.FOREVER, new SelectionListener.Adapter() {
      public void onItemsUpdated(AListModel.UpdateEvent event) {
        update[0]++;
      }

      public void onSelectionChanged() {
        selection[0]++;
      }

      public void onInsert(int index, int length) {
        inserts[0]++;
      }   
    });
    final int[] removes = new int[] {0};
    holder.addRemovedElementListener(new AListModel.RemovedElementsListener() {
      public void onBeforeElementsRemoved(AListModel.RemoveNotice elements) {
        removes[0]++;
      }
    });
    OrderListModel<String> listModel = OrderListModel.create("1");
    SelectionInListModel<String> cbModel = SelectionInListModel.createForever(listModel, null);
    assertEquals(0, inserts[0]);
    holder.setModel(cbModel);
    assertEquals(1, inserts[0]);

    assertEquals(0, update[0]);
    cbModel.forceUpdateAt(0);
    assertEquals(1, update[0]);

    assertEquals(0, selection[0]);
    cbModel.setSelectedItem("2");
    assertEquals(1, selection[0]);

    assertEquals(0, removes[0]);
    listModel.removeAt(0);
    assertEquals(1, removes[0]);

    assertEquals(1, inserts[0]);
    listModel.addElement("3");
    assertEquals(2, inserts[0]);

    assertEquals(2, inserts[0]);
    assertEquals(1, removes[0]);
    holder.setModel(SelectionInListModel.create(Collections.singleton("A"), null));
    assertEquals(3, inserts[0]);
    assertEquals(2, removes[0]);
  }
}
