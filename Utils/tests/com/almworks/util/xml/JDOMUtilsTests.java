package com.almworks.util.xml;

import com.almworks.util.collections.Convertor;
import com.almworks.util.tests.BaseTestCase;
import com.almworks.util.tests.CollectionsCompare;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import java.io.IOException;
import java.io.StringReader;

import static com.almworks.util.collections.Functional.convert;
import static com.almworks.util.xml.JDOMUtils.byTag;
import static com.almworks.util.xml.JDOMUtils.byTagAndAttr;
import static org.almworks.util.Collections15.arrayList;

public class JDOMUtilsTests extends BaseTestCase {
  private static final String TREE_HTML = "<root>\n  <a>0\n    <b>1\n      <b>2</b>\n    </b>\n    <b>3</b>\n    <c>4</c>\n  </a>\n  <b>5</b>\n  <c>6</c>\n  <a>7\n    <b>8</b>\n  </a>\n</root>";
  private final CollectionsCompare compare = new CollectionsCompare();
  private static final Convertor<Element,Integer> ELEMENT_TEXT_TO_STRING = new Convertor<Element, Integer>() {
      @Override
      public Integer convert(Element value) {
        return Integer.valueOf(value.getTextTrim());
      }
    };

  public void testGetTextTrimCropsNbsp() throws IOException, JDOMException {
    SAXBuilder builder = new SAXBuilder();
    builder.setExpandEntities(false);
    Element tag = builder.build(new StringReader("<!DOCTYPE tag [ <!ENTITY nbsp \"&#160;\"> ]><tag>\n  &nbsp;   \n   </tag>")).getRootElement();
    assertEquals("", JDOMUtils.getTextTrim(tag));
  }

  public void testPathQuery() throws JDOMException, IOException {
    SAXBuilder builder = new SAXBuilder();
    Element root = builder.build(new StringReader(TREE_HTML)).getRootElement();
    compare.order(arrayList(convert(JDOMUtils.queryPath(root, "a", "b"), ELEMENT_TEXT_TO_STRING)), 1, 3, 8);
  }

  public void testMultiSearchElements() throws JDOMException, IOException {
    SAXBuilder builder = new SAXBuilder();
    Element root = builder.build(new StringReader(TREE_HTML)).getRootElement();
    compare.order(arrayList(convert(JDOMUtils.multiSearchElements(root, byTag("a"), byTag("c")), ELEMENT_TEXT_TO_STRING)), 4);
    root = builder.build(new StringReader(
      // Note: don't split this string, edit it using IDEA's Inject Language feature
      "<root>\n  <l1 attr1=\"irrelevant\">\n    <l2>1</l2>\n    <l2>\n      <l3>2</l3>\n    </l2>\n    <intermediate>\n      <l2>\n        <l3>3</l3>\n      </l2>\n    </intermediate>\n  </l1>\n  <l2 attr=\"relevant\" attr1=\"irrelevant\">\n    <l3>4</l3>    \n  </l2>\n  <l1 attr=\"relevant\" attr1=\"irrelevant\">\n    <l1 attr=\"relevant\">\n      <l3>5</l3>\n      <l2 attr=\"l2-irrelevant\">6</l2>\n      <l2>7</l2>\n      <intermediate>\n        <l2>\n          <intermediate>\n            <l3>8</l3>\n          </intermediate>\n          <l3>9</l3>\n        </l2>\n      </intermediate>\n    </l1>\n    <l2>\n      <l3>10</l3>\n    </l2>\n  </l1>\n  <l2>11</l2>\n  <l2>\n    <l3>12</l3>\n    <l1 attr=\"relevant\">\n      <l2>\n        <l3>13</l3>\n      </l2>\n      <l3>\n        <l1 attr=\"relevant\">\n          <l2>\n            <l3>14</l3>\n          </l2>\n        </l1>\n      </l3>\n    </l1>\n  </l2>\n</root>")).getRootElement();
    compare.order(arrayList(convert(JDOMUtils.multiSearchElements(root, byTagAndAttr("l1", "attr", "relevant"), byTag("l2"), byTag("l3")), ELEMENT_TEXT_TO_STRING)), 8, 9, 10, 13, 14);
  }
}
