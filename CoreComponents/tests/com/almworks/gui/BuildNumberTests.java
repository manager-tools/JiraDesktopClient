package com.almworks.gui;

import com.almworks.api.gui.BuildNumber;
import com.almworks.util.tests.BaseTestCase;

public class BuildNumberTests extends BaseTestCase {
  public void testPresentation() {
    checkBuild("100", 100, 0, "100");
    checkBuild("0", 0, 0, "0");
    checkBuild("1001331", 1001, 331, "1001.331");
    checkBuild("844003", 844, 3, "844.3");
    checkBuild("DEBUG", 0, 0, "DEBUG");
    checkBuild("844.003", 0, 0, "844.003");
  }

  public void testCompare() {
    checkCompare("100", "200");
    checkCompare("100", "100001");
    checkCompare("100099", "101");
    checkCompare("100099", "100100");
    checkCompare("100099", "DEBUG");
  }

  private void checkCompare(String less, String greater) {
    BuildNumber lessNumber = BuildNumber.create(less);
    BuildNumber greaterNumber = BuildNumber.create(greater);
    assertTrue(lessNumber.compareTo(greaterNumber) < 0);
    assertTrue(greaterNumber.compareTo(lessNumber) > 0);
    assertNotSame(lessNumber, greaterNumber);
  }

  private void checkBuild(String property, int major, int minor, String displayable) {
    BuildNumber buildNumber = BuildNumber.create(property);
    assertEquals(major, buildNumber.getMajor());
    assertEquals(minor, buildNumber.getMinor());
    assertEquals(displayable, buildNumber.toDisplayableString());
  }
}
