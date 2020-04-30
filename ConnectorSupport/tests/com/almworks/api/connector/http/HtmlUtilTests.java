package com.almworks.api.connector.http;

import com.almworks.util.collections.MultiMap;
import com.almworks.util.tests.BaseTestCase;
import com.almworks.util.xml.JDOMUtils;
import org.jdom.Document;
import org.jdom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.StringReader;

public class HtmlUtilTests extends BaseTestCase {
  public void testHtmlMultiple() throws IOException, SAXException {
    Element form = getForm("<form><select multiple name='xxx'><option>x</option><option>y</option></select></form>");
    MultiMap<String, String> map = HtmlUtils.extractDefaultFormParameters(form);
    assertEquals(0, map.size());

    form = getForm("<form><select name='xxx'><option>x</option><option>y</option></select></form>");
    map = HtmlUtils.extractDefaultFormParameters(form);
    assertEquals(1, map.size());
    assertEquals("x", map.getSingle("xxx"));
  }

  private Element getForm(String html) throws SAXException, IOException {
    Document doc = HtmlUtils.buildHtmlDocument(new InputSource(new StringReader(html)));
    Element form = JDOMUtils.searchElement(doc.getRootElement(), "form");
    assertNotNull(form);
    return form;
  }
}
