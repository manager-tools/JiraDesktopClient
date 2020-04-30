package com.almworks.util.xml;

import com.almworks.util.tests.BaseTestCase;
import org.jdom.Element;
import org.jdom.JDOMException;

public class JDOMElementIteratorTests extends BaseTestCase {
  public void testSimple() throws JDOMException {
    check("<xml/>");
    check("<xml><whatever>some text<br/>more<br/>text</whatever></xml>", "whatever", "br", "br");
    check("<xml>&lt;<i  ><![CDATA[ cdata <q/>]]></i  >  </xml>", "i");
    check("<xml><i><i><i><i></i><u><u><u></u><b><b></b></b></u></u></i></i></i><i></i></xml>", "i", "i", "i", "i", "u", "u",
      "u", "b", "b", "i");
  }

  private void check(String... s) throws JDOMException {
    JDOMElementIterator ii = createIterator(s[0]);
    for (int i = 1; i < s.length; i++) {
      Element e = ii.next();
      assertNotNull(e);
      assertEquals(s[i], e.getName());
    }
    assertNull(ii.next());
    assertNull(ii.next());
  }

  private static JDOMElementIterator createIterator(String xml) throws JDOMException {
    return new JDOMElementIterator(JDOMUtils.parse(xml).getRootElement());
  }

  public void testSkip() throws JDOMException {
    JDOMElementIterator ii = createIterator("<xml><a><b><c><d></d></c></b><b2/></a></xml>");
    assertEquals("a", ii.next().getName());
    assertEquals("b", ii.next().getName());
    ii.skipLastElementContent();
    assertEquals("b2", ii.next().getName());
    assertNull(ii.next());
  }
}
