package com.almworks.util.i18n;

import com.almworks.util.tests.BaseTestCase;

public class LocalTests extends BaseTestCase {
  private LocalBook myTestBook;
  private TestTextProvider myProvider;

  protected void setUp() throws Exception {
    super.setUp();
    myTestBook = new DefaultLocalBook();
    Local.setBook(myTestBook);
    myProvider = new TestTextProvider();
    myTestBook.installProvider(myProvider);
  }

  protected void tearDown() throws Exception {
    Local.setBook(null);
    myProvider = null;
    myTestBook = null;
    super.tearDown();
  }

  public void testProcessing() {
    myProvider.put("x.y.z", "haba haba");
    myProvider.put("rhyme", "i want to $(x.y.z), i want to $(x.y.z)");
    myProvider.put("test", "a rhyme: $(rhyme)");
    String s = Local.text("test");
    assertEquals("a rhyme: i want to haba haba, i want to haba haba", s);
  }


}
