package com.almworks.util.components;

import com.almworks.util.advmodel.AListModel;
import com.almworks.util.advmodel.FixedListModel;
import com.almworks.util.components.plaf.LAFUtil;
import com.almworks.util.tests.AwtTestsGate;
import com.almworks.util.tests.BaseTestCase;
import com.almworks.util.tests.CollectionsCompare;

public class CompactSubsetEditorTests extends BaseTestCase {
  public CompactSubsetEditorTests() {
    super(AwtTestsGate.AWT_FOR_TEST);
  }

  private CompactSubsetEditor<String> myEditor;

  protected void setUp() throws Exception {
    super.setUp();
    LAFUtil.installExtensions();
    myEditor = new CompactSubsetEditor<String>();
  }

  protected void tearDown() throws Exception {
    myEditor = null;
    super.tearDown();
  }

  public void testFullModel() {
    AListModel<String> m = FixedListModel.create("x", "y", "z");
    myEditor.setFullModel(m);
    new CollectionsCompare().order(m.toList(), myEditor.getFullModel().toList());
  }
}
