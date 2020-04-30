package com.almworks.util.models;

import com.almworks.util.tests.GUITestCase;

import javax.swing.table.TableColumn;

/**
 * @author dyoma
 */
public class ColumnSizePolicyTests extends GUITestCase {
  private final TableColumn myColumn = new TableColumn();

  protected void setUp() throws Exception {
    super.setUp();
    myColumn.setMinWidth(100);
    myColumn.setMaxWidth(100);
    myColumn.setPreferredWidth(100);
  }

  public void testSettingFixedSizes() {
    ColumnSizePolicy.FIXED.setWidthParameters(150, myColumn);
    checkColumnSizes(150, 150, 150);
  }

  private void checkColumnSizes(int min, int max, int pref) {
    assertEquals(min, myColumn.getMinWidth());
    assertEquals(max, myColumn.getMaxWidth());
    assertEquals(pref, myColumn.getPreferredWidth());
  }

  public void testSettingFreeSizes() {
    ColumnSizePolicy.FREE.setWidthParameters(150, myColumn);
    checkColumnSizes(ColumnSizePolicy.DefaultColumnSizePolicy.MIN_WIDTH, ColumnSizePolicy.DefaultColumnSizePolicy.MAX_WIDTH, 150);
  }

  public void testSettingShrinkableSizes() {
    ColumnSizePolicy.SRINKABLE.setWidthParameters(150, myColumn);
    checkColumnSizes(ColumnSizePolicy.DefaultColumnSizePolicy.MIN_WIDTH, 150, 150);
  }
}
