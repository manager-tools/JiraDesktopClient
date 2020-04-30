package com.almworks.jira.provider3.sync.schema;

import com.almworks.items.entities.api.Entity;
import com.almworks.items.entities.api.EntityKey;
import com.almworks.items.entities.api.collector.transaction.EntityHolder;
import com.almworks.items.entities.api.collector.transaction.EntityTransaction;
import com.almworks.items.entities.api.util.EntityResolution;
import com.almworks.jira.provider3.sync.ServerInfo;
import com.almworks.util.LogHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.regex.Pattern;

public class ServerCustomField {
  public static final EntityKey<String> ID = EntityKey.string("customField.id", null);
  public static final EntityKey<String> NAME = EntityKey.string("customField.name", null);
  public static final EntityKey<String> KEY = EntityKey.string("customField.key", null);
  public static final EntityKey<String> DESCRIPTION = EntityKey.string("customField.description", null);
  public static final EntityKey<Entity> ATTRIBUTE = EntityKey.entity("customField.attr", null);
  public static final EntityKey<Entity> ENUM_TYPE = EntityKey.entity("customField.enumType", null);
  public static final EntityKey<Collection<Entity>> ONLY_IN_PROJECTS = Commons.ONLY_IN_PROJECTS;
  public static final EntityKey<Collection<Entity>> ONLY_IN_ISSUE_TYPES = Commons.ONLY_IN_ISSUE_TYPES;
  public static final EntityKey<Entity> PROJECT = EntityKey.entity("customField.project", null);

  public static final Entity TYPE = Commons.singleIdType(true, "types.customField", ID);
  public static final EntityKey<Integer> ENUM_ID = Commons.ENTITY_ID;
  /** We could use {@link #ENUM_ID} if we sticked to JIRA 4.4 and later; in earlier JIRAs, {@link #ENUM_DISPLAY_NAME} is the ID. <br/>
   * However, it is convenient to use a string ID sometimes (code generalizes to users and groups that also have string IDs, and we never actually parse the string.) */
  public static final EntityKey<String> ENUM_STRING_ID = EntityKey.string("customField.enum.idString", null);
  public static final EntityKey<String> ENUM_DISPLAY_NAME = Commons.ENTITY_NAME;
  public static final EntityKey<Integer> ENUM_ORDER = Commons.ENTITY_ORDER;
  public static final EntityKey<Entity> ENUM_PARENT = Commons.ENTITY_PARENT;

  private static final EntityResolution ENUM_RESOLUTION = EntityResolution.singleIdentity(true, ENUM_STRING_ID);
  private static final EntityResolution CASCADE_RESOLUTION = EntityResolution.singleIdentity(true, ENUM_ID);

  public static final Pattern ID_PATTERN = Pattern.compile("customfield_(\\d+)");
  public static final Pattern JQL_ID_PATTERN = Pattern.compile("cf\\[(\\d+)\\]");

  public static Entity createEnumType(String connectionId, String fieldId) {
    return Entity.buildType("type." + connectionId + "." + fieldId)
      .put(EntityResolution.KEY, ENUM_RESOLUTION)
      .fix();
  }

  public static Entity createEnumType(EntityHolder field) {
    EntityTransaction transaction = field.getTransaction();
    String fieldId = field.getScalarValue(ServerCustomField.ID);
    if (fieldId == null) return null;
    return createEnumType(ServerInfo.getConnectionId(transaction), fieldId);
  }

  @Nullable
  public static String createFullId(EntityHolder field, String typePrefix) {
    String id = getFieldId(field, typePrefix);
    if (id == null) return null;
    String connectionId = ServerInfo.getConnectionId(field.getTransaction());
    return createFullId(connectionId, typePrefix, id);
  }

  @NotNull
  public static String createFullId(String connectionId, String typePrefix, String id) {
    return "cf." + connectionId + "." + typePrefix + "." + id;
  }

  private static String getFieldId(EntityHolder field, String typePrefix) {
    String id = field.getScalarValue(ID);
    LogHelper.assertError(id != null, "Missing field ID", field, typePrefix);
    return id;
  }

  @Nullable
  public static EntityHolder getField(EntityTransaction transaction, String id) {
    if (id == null) return null;
    EntityHolder holder = transaction.addEntity(TYPE, ID, id);
    if (holder == null) LogHelper.error("Failed to store custom field", id);
    return holder;
  }

  @Nullable
  public static Entity createCascadeType(EntityHolder field) {
    String fieldId = field.getScalarValue(ID);
    String connectionId = ServerInfo.getConnectionId(field.getTransaction());
    return createCascadeType(fieldId, connectionId);
  }

  public static Entity createCascadeType(String fieldId, String connectionId) {
    if (fieldId == null || connectionId == null) return null;
    return Entity.buildType("type." + connectionId + "." + fieldId)
      .put(EntityResolution.KEY, CASCADE_RESOLUTION)
      .fix();
  }

  public static String jqlId(int intId) {
    return "cf[" + intId + "]";
  }
}
