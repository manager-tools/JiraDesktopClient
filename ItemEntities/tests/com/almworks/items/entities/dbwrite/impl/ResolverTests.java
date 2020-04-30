package com.almworks.items.entities.dbwrite.impl;

import com.almworks.items.entities.api.Entity;
import com.almworks.items.entities.api.EntityKey;
import com.almworks.items.entities.api.util.EntityResolution;
import com.almworks.util.tests.BaseTestCase;
import org.almworks.util.Collections15;

import java.util.Arrays;
import java.util.Map;

public class ResolverTests extends BaseTestCase {
  public void testCollectReplacements() {
    EntityKey<String> id1 = EntityKey.string("id1", null);
    EntityKey<String> id2 = EntityKey.string("id2", null);
    EntityResolution resolution = EntityResolution.singleAttributeIdentities(true, id1, id2);
    Entity type = typeWithResolution("type", resolution);
    Entity e1 = new Entity(type).put(id1, "1").fix();
    Entity e2 = new Entity(type).put(id2, "a").fix();
    Entity e3 = new Entity(type).put(id1, "1").put(id2, "a").fix();
    UniteEntities union = new UniteEntities();
    Resolver.collectReplacements(Collections15.arrayList(e1, e2, e3), resolution, union);
    Map<Entity, Entity> result = union.getUnited();
    assertEquals(2, result.size());
    Entity replacement = getReplacement(result, e3);
    assertSame(replacement, getReplacement(result, e1));
    assertSame(replacement, getReplacement(result, e2));
  }

  public void testReplaceEntityReferences() {
    EntityKey<String> strId = EntityKey.string("strId", null);
    Entity type1 = typeWithResolution("type1", EntityResolution.singleAttributeIdentities(true, strId));
    EntityKey<Entity> eId = EntityKey.entity("eId", null);
    Entity type2 = typeWithResolution("type2", EntityResolution.singleAttributeIdentities(true, eId));
    EntityKey<String> strVal1 = EntityKey.string("strVal1", null);
    EntityKey<String> strVal2 = EntityKey.string("strVal2", null);

    Entity e11 = new Entity(type1).put(strId, "a").put(strVal1, "1").fix();
    Entity e12 = new Entity(type1).put(strId, "a").put(strVal2, "2").fix();
    Entity e21 = new Entity(type2).put(eId, e11).fix();
    Entity e22 = new Entity(type2).put(eId, e12).fix();

    Resolver resolver = Resolver.create(Arrays.asList(e21, e22, e11, e12));
    Entity e1 = resolver.getGeneric(e11);
    assertSame(e1, resolver.getGeneric(e12));
    Entity e2 = resolver.getGeneric(e21);
    assertSame(e2, resolver.getGeneric(e22));
  }

  public void testBugReplaceWithOther() {
    EntityKey<String> id = EntityKey.string("id", null);
    EntityKey<String> val1 = EntityKey.string("val1", null);
    EntityKey<String> val2 = EntityKey.string("val2", null);
    Entity type = typeWithResolution("type", EntityResolution.singleAttributeIdentities(true, id));

    UniteEntities builder = new UniteEntities();
    Entity e1 = new Entity(type).put(id, "1");
    Entity e2 = new Entity(type).put(id, "1").put(val1, "v1");
    Entity e3 = new Entity(type).put(id, "1").put(val1, "v1").put(val2, "v2");

    Entity u1 = builder.unite(e1, e2);
    assertSame(u1, e2);
    Entity u2 = builder.unite(u1, e3);
    assertSame(u2, e3);
    Map<Entity, Entity> result = builder.getUnited();
    assertSame(u2, result.get(e1));
    assertSame(u2, result.get(e2));
    assertTrue(u2 == e3 || u2 == result.get(e3));
  }

  private Entity getReplacement(Map<Entity, Entity> result, Entity entity) {
    Entity replacement = result.get(entity);
    return replacement != null ? replacement : entity;
  }

  private Entity typeWithResolution(String typeId, EntityResolution resolution) {
    return Entity.buildType(typeId).put(EntityResolution.KEY, resolution).fix();
  }
}
