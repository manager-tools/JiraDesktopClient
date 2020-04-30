package com.almworks.api.explorer.util;

import com.almworks.engine.gui.ErrorFieldController;
import com.almworks.util.tests.BaseTestCase;

/**
 * @author Vasya
 */
@SuppressWarnings({"UseOfSystemOutOrSystemErr"})
public class ErrorLabelControllerTests extends BaseTestCase {
  String s = "There was a conflict when trying to upload bug 189.\n" +
    "You have changed this bug locally, and at the same time somebody changed it on the server. So now you need to merge two versions of the bug. Please re-run synchronization after that.";

  public void testMultilines() {
    System.out.println("Source: " + s);
    for (int j = 5; j < 101; j += 5) {
      String multilineHTMLPresentation = ErrorFieldController.getMultilinePresentation(s, j, true);
      assertNotNull(multilineHTMLPresentation);
      String[] strings = multilineHTMLPresentation.split("\\<br\\>");
      double commonlength = 0;
      for (int i = 0; i < strings.length; i++) {
        String string = strings[i];
        int l = string.length();
        commonlength += l;
        System.out.println("Result" + i + "(" + l + "): [" + string + "]");
        assertTrue(string + "; j=" + j, string.length() <= j || string.indexOf(" ") < 0);
      }
      System.out.println("OK for " + j + " average length is " + Math.round(commonlength / strings.length) + "\n");
    }
  }

}