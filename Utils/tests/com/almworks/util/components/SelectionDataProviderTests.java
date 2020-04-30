package com.almworks.util.components;

import com.almworks.util.advmodel.OrderListModel;
import com.almworks.util.collections.ChangeCounter;
import com.almworks.util.tests.AwtTestsGate;
import com.almworks.util.tests.BaseTestCase;
import com.almworks.util.tests.CollectionsCompare;
import com.almworks.util.ui.actions.DataRole;
import org.almworks.util.detach.DetachComposite;

import javax.swing.*;
import java.util.Collections;

/**
 * @author dyoma
 */
public class SelectionDataProviderTests extends BaseTestCase {
  private final CollectionsCompare CHECK = new CollectionsCompare();
  private final DefaultListSelectionModel mySelection = new DefaultListSelectionModel();
  private final OrderListModel<Object> myItems = OrderListModel.create();
  private final SelectionDataProvider myProvider = new SelectionDataProvider(new ListSelectionAccessor(mySelection, myItems), null);
  private static final DataRole<Object> ROLE = DataRole.createRole(Object.class);

  public SelectionDataProviderTests() {
    super(AwtTestsGate.AWT_FOR_TEST);
  }

  protected void setUp() throws Exception {
    super.setUp();
    myProvider.replaceRoles(Collections.singletonList(ROLE));
    myItems.addElement("1");
  }

  public void testData() {
    CHECK.empty(myProvider.getObjectsByRole(ROLE));
    assertNull(myProvider.getObjectsByRole(DataRole.createRole(Object.class)));

    mySelection.addSelectionInterval(0, 0);
    CHECK.singleElement("1", myProvider.getObjectsByRole(ROLE));
  }

  public void testUpdatingSelectedItem() {
    mySelection.addSelectionInterval(0, 0);
    ChangeCounter counter = new ChangeCounter();
    myProvider.addRoleListener(new DetachComposite(), ROLE, counter);
    myItems.updateAt(0);
    assertEquals(1, counter.getCount());
  }

}
