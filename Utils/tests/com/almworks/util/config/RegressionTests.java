package com.almworks.util.config;

import com.almworks.util.tests.BaseTestCase;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * :todoc:
 *
 * @author sereda
 */
public class RegressionTests extends BaseTestCase {
  public void testCreatingDuplicateSubsetWhenFirstContainsWhitespaceText() throws IOException, JDOMException {
    String sample = "<config><subset>\n\n</subset></config>";
    Document document = new SAXBuilder().build(new ByteArrayInputStream(sample.getBytes()));
    Configuration configuration = Configuration.createSynchonized(new JDOMMedium(document.getRootElement()),
      MediumWatcher.BLIND);
    List<Configuration> subsets = configuration.getAllSubsets("subset");
    assertEquals(1, subsets.size());
    configuration.getOrCreateSubset("subset");
    subsets = configuration.getAllSubsets("subset");
    assertEquals(1, subsets.size());
  }

  /**
   * The error caused setting name to be doubly encoded when calling setSettings(). This resulted in
   * that setting was not removed and a second setting was added.
   */
  public void testDoubleEncodingInSetSettings() {
    Element root = new Element("config");
    JDOMMedium medium = new JDOMMedium(root);
    medium.setSettings("0", Collections.singletonList("1"));
    assertEquals("1", medium.getSettings().get("0"));
    medium.setSettings("0", Collections.singletonList("2"));
    assertEquals("2", medium.getSettings().get("0"));
    medium.setSettings("0", Collections.singletonList("3"));
    assertEquals("3", medium.getSettings().get("0"));
  }
}
