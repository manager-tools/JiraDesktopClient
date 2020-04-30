package com.almworks.tracker.alpha;

import com.almworks.util.tests.BaseTestCase;
import com.almworks.util.tests.CollectionsCompare;

import java.util.List;

public class AlphaImplUtilsTests extends BaseTestCase {
  private CollectionsCompare myCompare;

  protected void setUp() throws Exception {
    super.setUp();
    myCompare = new CollectionsCompare();
  }

  protected void tearDown() throws Exception {
    myCompare = null;
    super.tearDown();
  }

  public void testTokenizeExec() {
    check("", new String[] {});
    check(null, new String[] {});
    check("test", new String[] {"test"});
    check("test something", new String[] {"test", "something"});
    check("test \"something special\"", new String[] {"test", "something special"});
    check("single \"\\\"\" quote", new String[] {"single", "\"", "quote"});
    check("slashed \"\\\\\\\"\" quote", new String[] {"slashed", "\\\"", "quote"});
    check("quote\"inside", new String[] {"quote\"inside"});
    check("   leading      trailing   spaces", new String[] {"leading", "trailing", "spaces"});
    check("C:\\ProgramFiles\\Deskzilla", new String[] {"C:\\ProgramFiles\\Deskzilla"});
  }

  private void check(String execString, String[] tokens) {
    List<String> exec = AlphaImplUtils.tokenizeExec(execString);
    myCompare.order(tokens, exec);
  }
}
