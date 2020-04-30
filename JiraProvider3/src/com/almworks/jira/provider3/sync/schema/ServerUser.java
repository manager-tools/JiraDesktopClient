package com.almworks.jira.provider3.sync.schema;

import com.almworks.items.entities.api.Entity;
import com.almworks.items.entities.api.EntityKey;
import com.almworks.items.entities.api.collector.transaction.EntityHolder;
import com.almworks.items.entities.api.collector.transaction.EntityTransaction;
import com.almworks.jira.provider3.sync.download2.rest.EntityParser;
import com.almworks.jira.provider3.sync.download2.rest.JRUser;
import com.almworks.util.LogHelper;
import com.almworks.util.commons.Procedure;
import org.almworks.util.Collections15;
import org.almworks.util.Util;
import org.jetbrains.annotations.Nullable;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ServerUser {
  public static final String TYPE_ID = "types.user";
  public static final EntityKey<String> ACCOUNT_ID = EntityKey.string("user.accountId", null);
  public static final EntityKey<String> NAME = EntityKey.string("user.name", null);
  public static final EntityParser PARSER =
    new EntityParser.Builder()
      .map(JRUser.ACCOUNT_ID, ACCOUNT_ID)
      .map(JRUser.NAME, NAME)
      .create(null);
  private static final Entity TYPE_ACCOUNT_ID = Commons.singleIdType(true, TYPE_ID, ACCOUNT_ID);

  @Nullable
  public static EntityHolder fromJson(EntityTransaction transaction, JSONObject object) {
    if (object == null) return null;
    Entity userEntity = create(object, PARSER);
    if (userEntity == null) {
      LogHelper.error("Not a user:", object);
      return null;
    }
    return transaction.addEntity(userEntity);
  }

  public static boolean sameUser(Entity userEntity, EntityHolder userHolder) {
    if (userEntity == null && userHolder == null) return true;
    if (userHolder == null || userEntity == null) return false;
    return sameUser(userHolder.getItemType().getTypeId(), userHolder.getScalarValue(ACCOUNT_ID),
      userEntity.getType().getTypeId(), userEntity.get(ACCOUNT_ID));
  }

  private static boolean sameUser(String type1, String accountId1, String type2, String accountId2) {
    if (!TYPE_ID.equals(type1)) {
      LogHelper.error("Not a user(1):", type1);
      return false;
    }
    if (!TYPE_ID.equals(type2)) {
      LogHelper.error("Not a user(2):", type2);
      return false;
    }
    if (accountId1 != null && accountId2 != null) {
      if (!accountId1.isEmpty() && !accountId2.isEmpty()) {
        return accountId1.equals(accountId2);
      }
    }
    LogHelper.error(String.format("Empty account id. 1: %s, 2: %s", accountId1, accountId2));
    return false;
  }

  public static Entity create(Object userJson, EntityParser userParser) {
    if (userJson == null) return null;
    Entity user = new Entity(TYPE_ACCOUNT_ID);
    userParser.fillEntity(userJson, user);
    if (user.get(ACCOUNT_ID) != null) return user;
    List<EntityKey<?>> valueKeys = user.getValueKeys().stream()
      .filter(k -> k != EntityKey.keyType())
      .collect(Collectors.toList());
    LogHelper.assertError(valueKeys.isEmpty(), "User has no identity, but has some values:", valueKeys);
    return null;
  }

  /**
   * Creates new user Entity identified by username or accountId (prefers accountId).
   * The entity is not {@link Entity#fix() fixed}, caller may add more values
   */
  @Nullable
  public static Entity create(@Nullable String accountId) {
    accountId = Util.NN(accountId).trim();
    Entity entity;
    if (!accountId.isEmpty()) {
      entity = new Entity(TYPE_ACCOUNT_ID).put(ACCOUNT_ID, accountId);
    } else return null;
    return entity;
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
