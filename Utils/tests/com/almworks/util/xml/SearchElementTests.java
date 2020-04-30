package com.almworks.util.xml;

import com.almworks.util.tests.BaseTestCase;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;

import java.io.ByteArrayInputStream;

/**
 * :todoc:
 *
 * @author sereda
 */
public class SearchElementTests extends BaseTestCase {
  private static final String xml =
    "<content>" +
    "  <tag1>" +
    "    <tag2></tag2>" +
    "    <tag2 attr1='value1'>" +
    "    </tag2>" +
    "    <TaG2 attR2=''></TaG2>" +
    "    <tag2 atTr2='VAlue2'><something>else</something></tag2>" +
    "  </tag1>" +
    "</content>";

  private Element root;

  protected void setUp() throws Exception {
    SAXBuilder builder = new SAXBuilder();
    Document document = builder.build(new ByteArrayInputStream(xml.getBytes()));
    root = document.getRootElement();
  }

  protected void tearDown() throws Exception {
    root = null;
  }


  public void testSearch() {
    assertTrue(JDOMUtils.searchElement(root, "Tag2") != null);
    assertTrue(JDOMUtils.searchElement(root, "Tagfsagfsag") == null);
    assertTrue(JDOMUtils.searchElement(root, "taG2", "attr1", "dfdfdfd") == null);
    assertTrue(JDOMUtils.searchElement(root, "taG2", "attr2", "VALue2") != null);
  }
}
