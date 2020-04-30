package com.almworks.explorer.qbuilder.constraints;

import com.almworks.util.properties.BooleanPropertyKey;
import com.almworks.util.properties.PropertyKey;
import com.almworks.util.properties.PropertyMap;
import com.almworks.util.properties.PropertyModelMap;
import com.almworks.util.tests.BaseTestCase;
import com.almworks.util.ui.swing.DocumentUtil;

import javax.swing.text.Document;

/**
 * @author : Dyoma
 */
public class ConstraintKeyTests extends BaseTestCase {
  private final PropertyKey<Document, String> myParent = PropertyKey.createText("parent");
  private final BooleanPropertyKey myEnable1 = BooleanPropertyKey.createKey("1e", false);
  private final BooleanPropertyKey myEnable2 = BooleanPropertyKey.createKey("2e", false);
  private final PropertyKey.EnablingKey<Document, String> myChild1 = PropertyKey.createEnablingText("1", myParent,
    myEnable1);
  private final PropertyKey.EnablingKey<Document, String> myChild2 = PropertyKey.createEnablingText("2", myParent,
    myEnable2);
  private final PropertyModelMap myMap = new PropertyModelMap(null);

  protected void setUp() throws Exception {
    super.setUp();
  }

  private void installChldren() {
    myMap.installProperty(myChild1);
    myMap.installProperty(myChild2);
  }

  public void testEnabledText() {
    installChldren();
    myParent.setModelValue(myMap, "parent");

    assertEquals("parent", myChild1.getModelValue(myMap));
    assertEquals("parent", myChild2.getModelValue(myMap));

    myEnable1.setModelValue(myMap, Boolean.TRUE);
    myChild1.setModelValue(myMap, "child1");
    assertEquals("child1", myParent.getModelValue(myMap));
    assertEquals("child1", myChild2.getModelValue(myMap));

    Document document = myMap.get(myChild1.getModelKey());
    DocumentUtil.setDocumentText(document, "newText");

    assertEquals("newText", myParent.getModelValue(myMap));
    assertEquals("newText", myChild2.getModelValue(myMap));
  }

  public void testEnabledText2() {
    installChldren();
    myParent.setModelValue(myMap, "abc");
    myEnable1.setModelValue(myMap, Boolean.TRUE);
    Document document = myMap.get(myChild1.getModelKey());

    assertEquals("abc", DocumentUtil.getDocumentText(document));
    DocumentUtil.setDocumentText(document, "1");

    assertEquals("1", myParent.getModelValue(myMap));
    assertEquals("1", myParent.getModelValue(myMap));
    assertEquals("1", myChild1.getModelValue(myMap));

    myMap.ensureInstalled(myChild1);
    assertEquals("1", myParent.getModelValue(myMap));
    assertEquals("1", myChild1.getModelValue(myMap));
  }

  public void testSetupValues() {
    PropertyMap values = new PropertyMap(null);
    myChild1.setInitialValue(values, "init1");

    myMap.installProperty(myParent);
    myParent.tryCopyValue(myMap, values);
    assertEquals("init1", myParent.getModelValue(myMap));
    assertFalse(myMap.containsKey(myChild1));
    assertFalse(myMap.containsKey(myEnable1));

    myMap.installProperty(myChild1);
    myChild1.tryCopyValue(myMap, values);
    assertTrue(myEnable1.getModelValue(myMap).booleanValue());
    assertEquals("init1", myParent.getModelValue(myMap));
    assertEquals("init1", myChild1.getModelValue(myMap));
    assertFalse(myChild1.isChanged(myMap, values).isChanged());

    myEnable1.setModelValue(myMap, Boolean.FALSE);
    assertTrue(myChild1.isChanged(myMap, values).isChanged());

    myEnable1.setModelValue(myMap, Boolean.TRUE);
    assertFalse(myChild1.isChanged(myMap, values).isChanged());
    myChild1.setModelValue(myMap, "newValue");
    assertTrue(myChild1.isChanged(myMap, values).isChanged());
  }

  public void testMissingSetupValues() {
    PropertyMap values = new PropertyMap(null);
    myParent.setInitialValue(values, "initParent");

    myMap.installProperty(myParent);
    myParent.tryCopyValue(myMap, values);
    assertEquals("initParent", myParent.getModelValue(myMap));

    myMap.installProperty(myChild1);
    myChild1.tryCopyValue(myMap, values);
    assertFalse(myEnable1.getModelValue(myMap).booleanValue());
    assertEquals("initParent", myParent.getModelValue(myMap));
    assertEquals("initParent", myChild1.getModelValue(myMap));

    assertFalse(myChild1.isChanged(myMap, values).isChanged());
    myEnable1.setModelValue(myMap, Boolean.TRUE);
    assertTrue(myChild1.isChanged(myMap, values).isChanged());
  }
}
