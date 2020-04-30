package com.almworks.jira.provider3.sync.download2.details.slaves;

import com.almworks.items.entities.api.Entity;
import com.almworks.items.entities.api.EntityKey;
import com.almworks.items.entities.api.collector.transaction.EntityBag2;
import com.almworks.items.entities.api.collector.transaction.EntityHolder;
import com.almworks.items.entities.api.collector.transaction.EntityTransaction;
import com.almworks.jira.provider3.sync.download2.details.JsonIssueField;
import com.almworks.jira.provider3.sync.download2.details.fields.ObjectField;
import com.almworks.util.LogHelper;
import com.almworks.util.collections.Convertor;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;

public class SimpleDependent implements SlaveLoader<EntityBag2> {
  private final Entity myType;
  private final EntityKey<Entity> myMaster;
  private final Convertor<Object, Entity> mySlaveLoader;
  @Nullable
  private final EntityKey<Integer> myOrder;

  /**
   * @param order if not null set order
   */
  public SimpleDependent(Entity type, EntityKey<Entity> master, Convertor<Object, Entity> dependentLoader, @Nullable EntityKey<Integer> order) {
    myType = type;
    myMaster = master;
    mySlaveLoader = dependentLoader;
    myOrder = order;
  }

  public JsonIssueField toField(boolean nullAsEmpty) {
    return DependentBagField.create(this, nullAsEmpty);
  }

  public JsonIssueField toField(boolean nullAsEmpty, String... path) {
    JsonIssueField field = toField(nullAsEmpty);
    for (int i = path.length - 1; i >= 0; i--) {
      String key = path[i];
      field = ObjectField.getField(key, field, nullAsEmpty);
    }
    return field;
  }

  @Override
  public Collection<? extends Parsed<EntityBag2>> loadValue(Object jsonObject, int order) {
    final Entity slave = mySlaveLoader.convert(jsonObject);
    if (slave == null) {
      LogHelper.error("Nothing loaded", myType, myMaster);
      return null;
    }
    return MyParsed.singleton(myType, myMaster, slave, myOrder, order);
  }

  @Override
  public EntityBag2 createBags(EntityHolder master) {
    return master.getTransaction().addBagRef(myType, myMaster, master).delete();
  }

  @Override
  public String toString() {
    return "Slave(" + myMaster + ")";
  }

  public static class MyParsed implements Parsed<EntityBag2> {
    private final Entity myType;
    private final EntityKey<Entity> myMaster;
    private final Entity myDependent;
    @Nullable
    private final EntityKey<Integer> myOrder;
    private final int myIndex;

    public MyParsed(Entity type, EntityKey<Entity> master, Entity dependent, @Nullable EntityKey<Integer> order, int index) {
      myType = type;
      myMaster = master;
      myDependent = dependent;
      myOrder = order;
      myIndex = index;
    }

    public static Collection<? extends Parsed<EntityBag2>> singleton(Entity type, EntityKey<Entity> master, Entity dependent, @Nullable EntityKey<Integer> order, int index) {
      return Collections.singleton(new MyParsed(type, master, dependent, order, index));
    }

    @Override
    public void addTo(EntityHolder master, @Nullable EntityBag2 bag) {
      EntityTransaction.IdentityBuilder builder = master.getTransaction().buildEntity(myType);
      if (builder == null) {
        LogHelper.error("Failed to store", myType);
        return;
      }
      builder.copy(myDependent);
      builder.addReference(myMaster, master);
      if (myOrder != null) builder.addValue(myOrder, myIndex);
      EntityHolder slave = builder.create();
      if (bag != null) bag.exclude(slave);
    }

    @Override
    public String toString() {
      return myDependent != null ? myDependent.toString() : "<null>";
    }
  }
}
