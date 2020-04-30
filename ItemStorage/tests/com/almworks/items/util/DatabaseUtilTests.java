package com.almworks.items.util;

import com.almworks.util.tests.BaseTestCase;

import java.math.BigDecimal;

import static com.almworks.items.util.DatabaseUtil.decimalToString;

public class DatabaseUtilTests extends BaseTestCase {
  public void testDecimalToString() {
    assertNull(decimalToString(null));
    checkDecimalToString("0", "0");
    checkDecimalToString("0", "0.00");
    checkDecimalToStringPlusMinus("3", "3");
    checkDecimalToStringPlusMinus("3", "3.00");
    checkDecimalToStringPlusMinus("300", "300");
    checkDecimalToStringPlusMinus("300", "300.00");
    checkDecimalToStringPlusMinus("0.69", "0.69");
    checkDecimalToStringPlusMinus("0.69", "0.690");
    checkDecimalToStringPlusMinus("300.69", "300.69");
    checkDecimalToStringPlusMinus("300.69", "300.690");
  }

  private void checkDecimalToString(String expected, String decimal) {
    assertEquals(expected, decimalToString(new BigDecimal(decimal)));
  }

  private void checkDecimalToStringPlusMinus(String expected, String decimal) {
    checkDecimalToString(expected, decimal);
    checkDecimalToString("-" + expected, "-" + decimal);
  }
}
