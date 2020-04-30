package com.almworks.items.entities.api.collector;

import com.almworks.items.entities.api.collector.typetable.EntityCollector2;
import com.almworks.util.tests.BaseTestCase;

import java.util.Arrays;
import java.util.List;

public class ValueRowTests extends BaseTestCase implements Collector2TestConsts {

  public void testGetFullRow() {
    ValueRow row = new ValueRow(new EntityCollector2());
    row.setColumns(Arrays.asList(iID1, iID2));
    row.setValue(0, "a");
    row.setValue(1, "b");
    assertSameValues(row, row.getFullRow(Arrays.asList(iID1, iID2)));
    assertSameValues(row, row.getFullRow(Arrays.asList(iID2, iID1)));
    assertSameValues(row, row.getFullRow(Arrays.asList(iID2)));
    assertSameValues(row, row.getFullRow(Arrays.asList(iID1)));
    assertNull(row.getFullRow(Arrays.asList(isID)));
    assertNull(row.getFullRow(Arrays.asList(isID, iID1)));
    assertNull(row.getFullRow(Arrays.asList(iID2, isID, iID1)));
  }

  private void assertSameValues(ValueRow superRow, ValueRow subRow) {
    assertNotNull("Null sub row, super: " + superRow, subRow);
    assertNotNull("Null super row, sub: " + subRow, superRow);
    List<KeyInfo> columns = subRow.getColumns();
    for (int i = 0, columnsSize = columns.size(); i < columnsSize; i++) {
      KeyInfo info = columns.get(i);
      int index = superRow.getColumnIndex(info);
      assertTrue("Missing column: " + info + " super: " + superRow + " sub: " + subRow, index >= 0);
      Object subValue = subRow.getValue(i);
      Object superValue = superRow.getValue(index);
      if (subValue == null) assertNull("Sub row missing value for " + info + " super: " + superRow + " sub: " + subRow, superValue);
      else {
        assertNotNull("Missing column value: " + info + " super: " + superRow + " sub: " + subRow, superValue);
        assertTrue("Different values: " + info + " super: " + superRow + " sub: " + subRow, info.equalValue(subValue, superValue));
      }
    }
  }
}
