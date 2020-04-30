package com.almworks.items.entities.api.collector.transaction.write;

import com.almworks.items.entities.api.collector.typetable.EntityPlace;
import com.almworks.items.entities.api.collector.typetable.EntityTable;
import org.almworks.util.Collections15;

import java.util.Map;
import java.util.Set;

class FakeCreate {
  private final Map<EntityTable, Set<EntityPlace>> myAllowSearchCreate = Collections15.hashMap();
  
  public boolean isAllowed(EntityPlace place) {
    EntityTable table = place.getTable();
    Set<EntityPlace> allowed = myAllowSearchCreate.get(table);
    if (allowed == null) return false;
    if (allowed.contains(place)) return true;
    for (EntityPlace aPlace : allowed) {
      if (aPlace.getIndex() == place.getIndex()) {
        allowed.add(place);
        return true;
      }
    }
    return false;
  }
  
  public void add(EntityPlace place) {
    EntityTable table = place.getTable();
    Set<EntityPlace> allowed = myAllowSearchCreate.get(table);
    if (allowed == null) {
      allowed = Collections15.hashSet();
      myAllowSearchCreate.put(table, allowed);
    }
    allowed.add(place);
  }
}
