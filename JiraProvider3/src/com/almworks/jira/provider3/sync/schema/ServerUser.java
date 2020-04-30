package com.almworks.jira.provider3.sync.schema;

import com.almworks.items.entities.api.Entity;
import com.almworks.items.entities.api.EntityKey;
import com.almworks.items.entities.api.collector.transaction.EntityHolder;
import com.almworks.items.entities.api.collector.transaction.EntityTransaction;
import com.almworks.jira.provider3.sync.download2.rest.JRUser;
import com.almworks.util.commons.Procedure;
import org.almworks.util.Collections15;
import org.almworks.util.Util;
import org.jetbrains.annotations.Nullable;
import org.json.simple.JSONObject;

import java.util.ArrayList;

public class ServerUser {
  public static final EntityKey<String> ID = EntityKey.string("user.id", null);
  public static final EntityKey<String> NAME = EntityKey.string("user.name", null);
  public static final Entity TYPE = Commons.singleIdType(true, "types.user", ID);

  public static EntityHolder create(EntityTransaction transaction, String userId) {
    userId = Util.NN(userId).trim();
    if (userId.length() == 0) return null;
    userId = Util.lower(userId);
    return transaction.addEntity(TYPE, ID, userId);
  }

  @Nullable
  public static EntityHolder fromJson(EntityTransaction transaction, JSONObject object) {
    if (object == null) return null;
    EntityHolder user = ServerUser.create(transaction, JRUser.ID.getValue(object));
    if (user != null) user.setNNValue(ServerUser.NAME, JRUser.NAME.getValue(object));
    return user;
  }

  public static class CollectFromJson implements Procedure<JSONObject> {
    private final EntityTransaction myTransaction;
    private final ArrayList<EntityHolder> myUsers = Collections15.arrayList();

    public CollectFromJson(EntityTransaction transaction) {
      myTransaction = transaction;
    }

    public ArrayList<EntityHolder> getUsers() {
      return myUsers;
    }

    @Override
    public void invoke(JSONObject userObj) {
      EntityHolder user = fromJson(myTransaction, userObj);
      if (user != null) myUsers.add(user);
    }

    public int getCount() {
      return myUsers.size();
    }
  }
}
