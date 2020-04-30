package com.almworks.util.xml;

import com.almworks.util.tests.AwtTestsGate;
import com.almworks.util.tests.BaseTestCase;

public class HTMLEntityResolverTests extends BaseTestCase {
  private HTMLEntityResolver myResolver;

  public HTMLEntityResolverTests() {
    super(AwtTestsGate.AWT_FOR_TEST);
  }

  protected void setUp() throws Exception {
    super.setUp();
    myResolver = HTMLEntityResolver.getInstance();
  }

  protected void tearDown() throws Exception {
    myResolver = null;
    super.tearDown();
  }

  public void testEntitiesResolved() {
    assertEquals((int)'\u00A0', myResolver.getEntityChar("nbsp"));
  }
}
