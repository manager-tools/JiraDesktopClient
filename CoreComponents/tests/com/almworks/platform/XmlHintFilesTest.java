package com.almworks.platform;

import com.almworks.util.tests.BaseTestCase;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

public class XmlHintFilesTest extends BaseTestCase {
  public void doTest(String xml, String[] classNames) throws IOException, JDOMException {
    doTestReading(xml, classNames);
    doTestWriting(xml, classNames);
  }

  private void doTestWriting(String xml, String[] classNames) throws IOException, JDOMException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    XmlHintFiles.writeHintStream(baos, Arrays.asList(classNames));
    String xml2 = baos.toString();
    assertEqualXml(xml, xml2);
  }

  private void doTestReading(String xml, String[] classNames) throws IOException, JDOMException {
    Set<String> set1 = XmlHintFiles.readHintStream(new ByteArrayInputStream(xml.getBytes()));
    List<String> set2 = Arrays.asList(classNames);
    assertEqualSets(set1, set2, "parsed class set", "sample class set");
  }


  public void assertEqualSets(Collection c1, Collection c2, String description1, String description2) {
    Iterator it1 = c1.iterator();
    while (it1.hasNext()) {
      Object o1 = it1.next();
      if (!c2.contains(o1))
        fail(description1 + " contains " + o1 + " that is not found in " + description2);
    }
    HashSet temp = new HashSet(c2);
    temp.removeAll(c1);
    if (temp.size() > 0)
      fail(description2 + " contains " + temp.iterator().next() + " that is not found in " + description1);
  }

  private void assertEqualXml(String xml1, String xml2) throws IOException, JDOMException {
    SAXBuilder builder = new SAXBuilder();
    Document document1 = builder.build(new ByteArrayInputStream(xml1.getBytes()));
    Document document2 = builder.build(new ByteArrayInputStream(xml2.getBytes()));
    XMLOutputter outputter = new XMLOutputter(
      Format.getRawFormat().setLineSeparator("").setTextMode(Format.TextMode.TRIM));
    ByteArrayOutputStream baos1 = new ByteArrayOutputStream();
    outputter.output(document1, baos1);
    ByteArrayOutputStream baos2 = new ByteArrayOutputStream();
    outputter.output(document2, baos2);
    String s1 = baos1.toString();
    String s2 = baos2.toString();
    assertTrue(s1.equals(s2));
  }


  public void testBasicFormat() throws IOException, JDOMException {
    doTest("<library>\n" +
      "<component class=\"x.y.z\"/>\n" +
      "<component class=\"haba.haba\"/>\n" +
      "</library>",
      new String[]{"x.y.z", "haba.haba"});
  }
}
