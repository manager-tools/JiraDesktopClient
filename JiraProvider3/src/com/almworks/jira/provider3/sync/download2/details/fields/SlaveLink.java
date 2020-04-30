package com.almworks.jira.provider3.sync.download2.details.fields;

import com.almworks.items.entities.api.Entity;
import com.almworks.items.entities.api.EntityKey;
import com.almworks.items.entities.api.collector.transaction.EntityBag2;
import com.almworks.items.entities.api.collector.transaction.EntityHolder;
import com.almworks.items.entities.api.collector.transaction.EntityTransaction;
import com.almworks.jira.provider3.sync.download2.details.slaves.SlaveLoader;
import com.almworks.jira.provider3.sync.download2.rest.JRLink;
import com.almworks.jira.provider3.sync.schema.ServerLink;
import com.almworks.util.LogHelper;
import com.almworks.util.Pair;
import org.almworks.util.Util;
import org.jetbrains.annotations.Nullable;
import org.json.simple.JSONObject;

import java.util.Collection;
import java.util.Collections;

class SlaveLink implements SlaveLoader<Pair<EntityBag2, EntityBag2>> {
  @Override
  public Collection<? extends Parsed<Pair<EntityBag2, EntityBag2>>> loadValue(Object jsonObject, int order) {
    if (jsonObject == null) return null;
    JSONObject object = Util.castNullable(JSONObject.class, jsonObject);
    if (object == null) {
      LogHelper.error("Expected object", jsonObject.getClass());
      return null;
    }
    final Integer id = JRLink.ID.getValue(object);
    final Entity linkType = JRLink.LINK_TYPE.getValue(object);
    Entity inward = JRLink.INWARD_ISSUE.getValue(object);
    Entity outward = JRLink.OUTWARD_ISSUE.getValue(object);
    final EntityKey<Entity> thisMaster;
    final EntityKey<Entity> oppositeMaster;
    final Entity opposite;
    if (inward != null) {
      thisMaster = ServerLink.TARGET;
      oppositeMaster = ServerLink.SOURCE;
      opposite = inward;
    } else {
      thisMaster = ServerLink.SOURCE;
      oppositeMaster = ServerLink.TARGET;
      opposite = outward;
    }
    if (id == null || linkType == null || opposite == null) {
      LogHelper.error("Missing link data", id, linkType, inward, outward);
      return null;
    }
    return Collections.singleton(new Parsed<Pair<EntityBag2, EntityBag2>>() {
      @Override
      public void addTo(EntityHolder master, @Nullable Pair<EntityBag2, EntityBag2> sourceTarget) {
        EntityTransaction transaction = master.getTransaction();
        EntityTransaction.IdentityBuilder builder = transaction.buildEntity(ServerLink.TYPE);
        if (builder == null) {
          LogHelper.error("Failed to store link");
          return;
        }
        EntityBag2 bag;
        if (sourceTarget != null) {
          if (thisMaster == ServerLink.TARGET) bag = sourceTarget.getSecond();
          else if (thisMaster == ServerLink.SOURCE) bag = sourceTarget.getFirst();
          else {
            LogHelper.error("Unknown master", thisMaster);
            return;
          }
        } else bag = null;
        builder.addReference(thisMaster, master);
        builder.addValue(oppositeMaster, opposite);
        builder.addValue(ServerLink.LINK_TYPE, linkType);
        builder.addValue(ServerLink.ID, id);
        EntityHolder link = builder.create();
        if (bag != null) bag.exclude(link);
      }

      @Override
      public String toString() {
        return "Link#" + id;
      }
    });
  }

  @Override
  public Pair<EntityBag2, EntityBag2> createBags(EntityHolder master) {
    EntityBag2 sourceBag = createLinksBag(ServerLink.SOURCE, master);
    EntityBag2 targetBag = createLinksBag(ServerLink.TARGET, master);
    return Pair.create(sourceBag, targetBag);
  }

  @Override
  public String toString() {
    return "Links";
  }

  private EntityBag2 createLinksBag(EntityKey<Entity> direction, EntityHolder target) {
    EntityBag2 bag = target.getTransaction().addBagRef(ServerLink.TYPE, direction, target);
    bag.delete();
    return bag;
  }
}
