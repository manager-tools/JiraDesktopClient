package com.almworks.items.entities.api;

import com.almworks.util.tests.BaseTestCase;

public class EntityTests extends BaseTestCase {
  public void test() {
    EntityKey<String> key = EntityKey.string("str", null);
    Entity type = Entity.type("type");
    Entity entity = new Entity(type);
    entity.put(key, "abc");
    assertEquals(key.getId(), "str");
    checkEqual(key.getValue(EntityInit.TYPE), EntityInit.T_KEY);
    BaseTestCase.assertEquals(key.getComposition(), EntityKey.Composition.SCALAR);
    BaseTestCase.assertEquals(key.getValueClass(), String.class);
    checkEqual(EntityInit.T_KEY.get(EntityInit.TYPE), EntityInit.META_TYPE);
    checkEqual(EntityInit.META_TYPE.get(EntityInit.TYPE), EntityInit.META_TYPE);
    checkEqual(type.get(EntityInit.TYPE), EntityInit.META_TYPE);
    assertEquals(type.getTypeId(), "type");
    checkEqual(entity.getType(), type);
    assertEquals(entity.get(key), "abc");
  }

  private void checkEqual(Object a, Object b) {
    assertNotNull(a);
    assertNotNull(b);
    BaseTestCase.assertEquals(a, b);
    assertEquals(a.hashCode(), b.hashCode());
  }
}
