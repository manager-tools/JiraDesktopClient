package com.almworks.util.config;

import com.almworks.util.BadFormatException;
import com.almworks.util.tests.BaseTestCase;
import com.almworks.util.tests.CollectionsCompare;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author : Dyoma
 */
public class JDOMConfigTests extends BaseTestCase {
  private final CollectionsCompare CHECK = new CollectionsCompare();
  private static final List<String> A_VALUES = Arrays.asList(new String[]{"v"});

  public void testReadonly() throws ReadonlyConfiguration.NoSettingException, IOException, BadFormatException {
    ReadonlyConfiguration configuration = JDOMConfigurator.parse("<r><s1/><s2><v>x</v></s2><s3 empty=\"\"></s3></r>");
    assertNotNull(configuration.getSubset("s1"));
    assertNotNull(configuration.getSubset("s2"));
    assertTrue(configuration.isSet("s3"));
    CHECK.singleElement("s3", configuration.getAllSettingNames());
    CHECK.unordered(configuration.getAllSubsetNames(), new String[]{"s1", "s2"});
    ReadonlyConfiguration c2 = configuration.getSubset("s2");
    assertEquals("x", c2.getMandatorySetting("v"));
  }

  public void testDigitsInSubsetNames() {
    Element root = new Element("root");
    Medium medium = new JDOMMedium(root);
    Medium subset = medium.createSubset("123");
    assertNotNull(root.getChild(XMLWriter.PREFIX_CHAR + "123"));
    assertEquals("123", subset.getName());
    subset.setSettings("a", A_VALUES);
    ReadonlyMedium subset1 = (ReadonlyMedium) medium.getSubsets().get("123");
    assertNotNull(subset1);
    assertEquals(A_VALUES.get(0), subset1.getSettings().get("a"));
  }

  public void testEncodeColon() {
    Element root = new Element("root");
    JDOMMedium medium = new JDOMMedium(root);
    medium.setSettings("x:y", Collections.singletonList("value"));
    List<Element> children = root.getChildren();
    assertEquals(1, children.size());
    assertEquals("x_3ay", children.get(0).getName());
    CHECK.singleElement("x:y", medium.getSettings().getAllNames());
  }

  public void testEncodeDecode() {
    String prefix = String.valueOf(XMLWriter.PREFIX_CHAR);
    JDOMMedium medium = new JDOMMedium(new Element("root"));
    medium.setSettings(" ", A_VALUES);
    medium.setSettings(prefix, A_VALUES);
    medium.setSettings("a ", A_VALUES);
    CHECK.unordered(medium.getSettings().getAllNames(), new String[]{" ", prefix, "a "});
  }

  public void testForebiddenUpperSymbolInName() {
    checkEncodeDecode("_\u2011:", "__5f_L2011_3a");
    checkEncodeDecode("_<:", "__5f_3c_3a");
    checkEncodeDecode("_a", "__5fa");
    checkEncodeDecode("a_b", "a__b");
    checkEncodeDecode("a_:", "a___3a");
  }

  private void checkEncodeDecode(String tagName, String encoded) {
    assertEquals(tagName, XMLWriter.decode(encoded));
    assertEquals(encoded, XMLWriter.encode(tagName));
  }

  public void testSettingVsSubsets() throws IOException, BadFormatException, ReadonlyConfiguration.NoSettingException,
    JDOMException {
    Element root = new Element("root");
    JDOMMedium medium = new JDOMMedium(root);
    CHECK.empty(medium.getSubsets().getAllNames());
    medium.createSubset("subset");
    CHECK.singleElement("subset", medium.getSubsets().getAllNames());
    CHECK.empty(medium.getSettings().getAllNames());
    medium.setSettings("a", A_VALUES);
    CHECK.singleElement("subset", medium.getSubsets().getAllNames());
    CHECK.singleElement("a", medium.getSettings().getAllNames());
    medium.setSettings("b", Arrays.asList(new String[]{""}));
    assertTrue(medium.getSettings().isSet("b"));
    Document document = new Document(root);
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    JDOMConfigurator.storeDocument(document, stream);
    ReadonlyMedium loaded = JDOMReadonlyMedium.createReadonly(new ByteArrayInputStream(stream.toByteArray()));
    assertTrue(loaded.getSettings().isSet("b"));
    assertEquals("", loaded.getSettings().get("b"));
  }

  public void testWriteConfiguration() throws ReadonlyConfiguration.NoSettingException, IOException, BadFormatException {
    Configuration config = MapMedium.createConfig(null, "Config");
    config.setSetting("a", "1");
    config.createSubset("X").setSetting("aa", "2");
    Configuration subset = config.createSubset("X");
    subset.setSettings("bb", new String[]{"3", "4"});
    String xml = JDOMConfigurator.writeConfiguration(config);

    ReadonlyConfiguration copy = JDOMConfigurator.parse(xml);
    assertEquals("Config", copy.getName());
    assertEquals("1", copy.getMandatorySetting("a"));
    List<? extends ReadonlyConfiguration> Xs = copy.getAllSubsets("X");
    assertEquals("2", Xs.get(0).getMandatorySetting("aa"));
    CHECK.order(new String[]{"3", "4"}, Xs.get(1).getAllSettings("bb"));
  }
}
