package com.almworks.items.entities.api.collector.transaction;

import com.almworks.items.entities.api.Entity;
import com.almworks.items.entities.api.EntityKey;
import com.almworks.items.entities.api.collector.KeyInfo;
import com.almworks.items.entities.api.collector.ValueRow;
import com.almworks.items.entities.api.collector.typetable.EntityTable;
import com.almworks.util.LogHelper;
import org.almworks.util.Collections15;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class EntityBag2 {
  private final EntityTransaction myTransaction;
  private final EntityQuery2 myQuery;
  private final List<EntityHolder> myExclusions = Collections15.arrayList();
  private final ValueRow myChange;
  private boolean myDelete = false;

  public EntityBag2(EntityTransaction transaction, EntityQuery2 query) {
    myTransaction = transaction;
    myQuery = query;
    myChange = new ValueRow(query.getTypeTable().getCollector());
  }

  public EntityTransaction getTransaction() {
    return myTransaction;
  }

  public EntityBag2 exclude(EntityHolder entity) {
    if (entity != null) myExclusions.add(entity);
    return this;
  }

  public <T> void changeValue(EntityKey<T> key, @Nullable T value) {
    LogHelper.assertError(!myDelete, "Already deleted", key, value);
    EntityQuery2.setValue(getTypeTable(), myChange, key, value);
  }

  public void excludeAll(List<EntityHolder> entities) {
    for (EntityHolder entity : entities) exclude(entity);
  }

  public void changeReference(EntityKey<Entity> key, @Nullable EntityHolder value) {
    LogHelper.assertError(!myDelete, "Already deleted", key, value);
    EntityQuery2.setReference(getTypeTable(), myChange, key, value);
  }

  public Entity getTargetType() {
    return getTypeTable().getItemType();
  }

  public EntityTable getTypeTable() {
    return myQuery.getTypeTable();
  }

  public ValueRow getQuery() {
    return myQuery.getQuery();
  }

  public Entity getQueryType() {
    return myQuery.getTypeTable().getItemType();
  }

  public List<EntityHolder> getExclusions() {
    return Collections.unmodifiableList(myExclusions);
  }

  public boolean hasExclusions() {
    return !myExclusions.isEmpty();
  }

  public EntityBag2 delete() {
    myDelete = true;
    LogHelper.assertError(myChange.getColumns().isEmpty(), "Delete not empty change", myChange);
    return this;
  }

  public boolean isDelete() {
    return myDelete;
  }

  public List<KeyInfo> getChangeColumns() {
    return Collections.unmodifiableList(myChange.getColumns());
  }

  public Object getChangeValue(int column) {
    return myChange.getValue(column);
  }

  @NotNull
  public Optional toOptional() {
    return new Optional() {
      @Override
      public void exclude(EntityHolder entity) {
        EntityBag2.this.exclude(entity);
      }
    };
  }

  /**
   * Convinient interface to optionally apply bag operation.
   */
  public interface Optional {
    Optional MOCK = new Optional() {
      @Override
      public void exclude(EntityHolder entity) {}
    };
    void exclude(EntityHolder entity);
  }
}
