package com.almworks.jira.provider3.sync.download2.meta;

import com.almworks.items.entities.api.Entity;
import com.almworks.items.entities.api.EntityKey;
import com.almworks.items.entities.api.collector.transaction.EntityBag2;
import com.almworks.items.entities.api.collector.transaction.EntityHolder;
import com.almworks.items.entities.api.collector.transaction.EntityTransaction;
import com.almworks.restconnector.json.JSONKey;
import com.almworks.util.LogHelper;
import com.almworks.util.Pair;
import org.jetbrains.annotations.Nullable;
import org.json.simple.JSONObject;

import java.util.Iterator;

class StoreIterator {
  private final EntityTransaction myTransaction;
  private final Iterator<JSONObject> mySourceIt;
  protected final Entity myType;
  protected final EntityKey<Integer> myIdKey;
  private final JSONKey<Integer> myIdAccessor;
  private int myLastId = -1;
  private int myIndex = 0;
  @Nullable
  private EntityBag2 myBag = null;

  public StoreIterator(EntityTransaction transaction, Entity type, EntityKey<Integer> idKey, JSONKey<Integer> idAccessor, Iterable<JSONObject> sourceIt) {
    myType = type;
    myIdKey = idKey;
    myIdAccessor = idAccessor;
    myTransaction = transaction;
    mySourceIt = sourceIt.iterator();
  }

  @Nullable
  public final Pair<JSONObject, EntityHolder> next() {
    while (mySourceIt.hasNext()) {
      myIndex++;
      myLastId = -1;
      JSONObject source = mySourceIt.next();
      Integer id = myIdAccessor.getValue(source);
      if (id == null) {
        LogHelper.error("Missing id", myType, source);
        continue;
      }
      myLastId = id;
      EntityHolder holder = createEntity(id, source);
      if (holder == null) {
        LogHelper.error("Failed to store project", myType, id, source);
        continue;
      }
      if (myBag != null) myBag.exclude(holder);
      return Pair.create(source, holder);
    }
    return null;
  }

  protected EntityHolder createEntity(int id, JSONObject source) {
    return myTransaction.addEntity(myType, myIdKey, id);
  }

  public final int getLastId() {
    LogHelper.assertError(myLastId >= 0, myType);
    return myLastId;
  }

  protected EntityBag2 createBag() {
    return myTransaction.addBag(myType);
  }

  public final int getIndex() {
    LogHelper.assertError(myLastId >= 0, myType);
    return myIndex - 1;
  }

  /**
   * Automatically creates {@link com.almworks.items.entities.api.collector.transaction.EntityBag2#delete() delete} bag for all entities in scope.
   */
  public final void managerDeleteBag() {
    LogHelper.assertError(myIndex == 0, "Already started", myType);
    if (myBag != null) {
      LogHelper.error("Already has bag", myType);
      return;
    }
    myBag = createBag();
    myBag.delete();
  }
}
