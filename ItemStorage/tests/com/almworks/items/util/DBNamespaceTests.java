package com.almworks.items.util;

import com.almworks.util.Pair;
import com.almworks.util.tests.BaseTestCase;

/**
 * @author dyoma
 */
public class DBNamespaceTests extends BaseTestCase {
  public void testReverseFullId() {
    DBNamespace module = DBNamespace.moduleNs("module");
    String moduleTypeId = module.type("TT.typeId", "").getId();
    assertEquals("module:t:TT.typeId", moduleTypeId);
    assertEquals(Pair.create("t", "TT.typeId"), module.reverseFullId(moduleTypeId));

    DBNamespace local = module.subNs("local");
    String localTypeId = local.type("TT.typeId", "").getId();
    assertEquals("module:t:local.TT.typeId", localTypeId);
    assertEquals(Pair.create("t", "TT.typeId"), local.reverseFullId(localTypeId));

    assertEquals(Pair.create("t", "local.TT.typeId"), module.reverseFullId(localTypeId));
    assertNull(local.reverseFullId(moduleTypeId));
  }
}
