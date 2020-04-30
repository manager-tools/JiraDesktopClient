package com.almworks.extservice;

import com.almworks.util.tests.BaseTestCase;

public class DeskzillaStartCommandTests extends BaseTestCase {
  public void test() {
    String T = "launch -Dcp=C:\\whatever\\whereever \"xxx\\yyy/z\" \"C:\\\\Documents and Settings\\\\user\\\\.Deskzilla\"";
    String V = TrackerStartCommand.adjustCommandForStoringInFileOnWindows(T);
    assertEquals("launch -Dcp=C:\\whatever\\whereever \"xxx\\yyy/z\" \"C:\\Documents and Settings\\user\\.Deskzilla\"", V);
  }
}
