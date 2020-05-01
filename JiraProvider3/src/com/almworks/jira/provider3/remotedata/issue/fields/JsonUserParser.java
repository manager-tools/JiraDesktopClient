package com.almworks.jira.provider3.remotedata.issue.fields;

import com.almworks.items.api.DBItemType;
import com.almworks.items.entities.api.Entity;
import com.almworks.items.entities.api.collector.transaction.EntityHolder;
import com.almworks.items.sync.ItemVersion;
import com.almworks.jira.provider3.schema.User;
import com.almworks.jira.provider3.services.upload.UploadJsonUtil;
import com.almworks.jira.provider3.sync.download2.rest.EntityParser;
import com.almworks.jira.provider3.sync.download2.rest.JRUser;
import com.almworks.jira.provider3.sync.download2.rest.JsonEntityParser;
import com.almworks.jira.provider3.sync.download2.rest.LoadedEntity;
import com.almworks.jira.provider3.sync.schema.ServerUser;
import com.almworks.restconnector.json.JSONKey;
import com.almworks.util.LogHelper;
import com.almworks.util.collections.Convertor;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;
import org.json.simple.JSONObject;

/**
 * Supports for different user identities by {@link ServerUser#ACCOUNT_ID}.
 * If an user has accountId value, assumes that user's connection supports accountId and uses it as preferred identity.
 * @author dyoma
 */
public class JsonUserParser implements JsonEntityParser {
  public static final JsonUserParser INSTANCE = new JsonUserParser(ServerUser.PARSER);

  private final EntityParser myParser;

  private JsonUserParser(EntityParser parser) {
    assert parser != null;
    myParser = parser;
  }

  @Override
  public EntityParser getParser() {
    return myParser;
  }

  @Override
  public DBItemType getType() {
    return User.DB_TYPE;
  }

  @Override
  public Convertor<Object, Entity> createConvertor() {
    return new Convertor<Object, Entity>() {
      @Override
      public Entity convert(Object value) {
        return ServerUser.create(value, myParser);
      }
    };
  }

  @Override
  public JsonEntityParser withParser(EntityParser parser) {
    return new JsonUserParser(parser);
  }

  @Override
  public LoadedUser readValue(ItemVersion value) {
    if (value == null || value.getItem() <= 0) return null;
    return new LoadedUser(value.getValue(User.NAME), value.getValue(User.ACCOUNT_ID));
  }

  @Override
  public LoadedUser readValue(EntityHolder value) {
    if (value == null) return null;
    return new LoadedUser(value.getScalarValue(ServerUser.NAME), value.getScalarValue(ServerUser.ACCOUNT_ID));
  }

  private static final Convertor<Object, Entity> CONVERTOR = new Convertor<Object, Entity>() {
    @Override
    public Entity convert(Object value) {
      return ServerUser.create(value, ServerUser.PARSER);
    }
  };
  public static JSONKey<Entity> jsonKey(String key) {
    return new JSONKey<>(key, CONVERTOR);
  }

  public static class LoadedUser implements LoadedEntity {
    private final String myDisplayName;
    private final String myAccountId;

    public LoadedUser(String displayName, String accountId) {
      if (accountId != null) {
        accountId = accountId.trim();
        if (accountId.isEmpty()) accountId = null;
        LogHelper.assertError(accountId == null || accountId.length() > 5, "Too short accountId:", accountId, displayName);
      }
      LogHelper.assertError(accountId != null, "LoadedUser misses both username and accountID", displayName);
      myDisplayName = displayName;
      myAccountId = accountId;
    }

    public String getAccountId() {
      return myAccountId;
    }

    @NotNull
    @Override
    public String getDisplayableText() {
      return myDisplayName != null ? myDisplayName : myAccountId;
    }

    @NotNull
    @Override
    public String getFormValueId() {
      return myAccountId;
    }

    @Override
    public JSONObject toJson() {
      if (myAccountId != null) return UploadJsonUtil.object(JRUser.ACCOUNT_ID.getName(), myAccountId);
      LogHelper.warning("LoadedUser misses any JSON identity:", this);
      return null;
    }

    @Override
    public int hashCode() {
      if (myAccountId != null) return myAccountId.hashCode();
      return super.hashCode(); // This instance is not equal to any other (except itself)
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      LoadedUser other = Util.castNullable(LoadedUser.class, obj);
      if (other == null) return false;
      if (myAccountId != null && other.myAccountId != null) return myAccountId.equals(other.myAccountId);
      return false;
    }

    @Override
    public String toString() {
      return "LoadedUser{" +
        "myDisplayName='" + myDisplayName + '\'' +
        ", myAccountId='" + myAccountId + '\'' +
        '}';
    }

    public boolean sameUser(EntityHolder user) {
      if (user == null) return false;
      String userAccountId = user.getScalarValue(ServerUser.ACCOUNT_ID);
      if (myAccountId != null && userAccountId != null) return myAccountId.equals(userAccountId);
      return true;
    }
  }
}
