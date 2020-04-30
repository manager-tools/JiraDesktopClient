package com.almworks.items.entities.api.collector;

import com.almworks.items.entities.api.Entity;
import com.almworks.items.entities.api.EntityKey;
import com.almworks.items.entities.api.util.EntityResolution;

import java.util.Arrays;

public interface Collector2TestConsts {
  EntityKey<String> sID = EntityKey.string("sID", null);
  EntityKey<String> ID1 = EntityKey.string("ID1", null);
  EntityKey<String> ID2 = EntityKey.string("ID2", null);
  EntityKey<Entity> IDe = EntityKey.entity("IDe", null);
  EntityKey<String> VAL1 = EntityKey.string("val1", null);
  EntityKey<String> VAL2 = EntityKey.string("val2", null);
  Entity TYPE_1 = Entity.buildType("type1").put(EntityResolution.KEY, EntityResolution.searchable(false, Arrays.asList(sID), ID1, ID2)).fix();
  Entity TYPE_2 = Entity.buildType("type2").put(EntityResolution.KEY, EntityResolution.singleIdentity(false, IDe)).fix();
  Entity TYPE_3 = Entity.buildType("type3").put(EntityResolution.KEY, EntityResolution.searchable(false, Arrays.asList(sID), ID1)).fix();

  KeyInfo iID1 = KeyInfo.create(ID1);
  KeyInfo iID2 = KeyInfo.create(ID2);
  KeyInfo isID = KeyInfo.create(sID);
  KeyInfo iVAL1 = KeyInfo.create(VAL1);
  KeyInfo iVAL2 = KeyInfo.create(VAL2);

}
