package com.almworks.jira.provider3.custom.fieldtypes.enums.cascade;

import com.almworks.items.entities.api.Entity;
import com.almworks.jira.provider3.sync.download2.details.fields.ValueSupplement;
import com.almworks.jira.provider3.sync.download2.rest.EntityParser;
import com.almworks.jira.provider3.sync.download2.rest.JsonEntityParser;
import com.almworks.jira.provider3.sync.schema.ServerCustomField;
import com.almworks.restconnector.json.JSONKey;
import com.almworks.util.LogHelper;
import org.jetbrains.annotations.NotNull;
import org.json.simple.JSONObject;

class CascadeJsonParser implements EntityParser {
  private static final EntityParser INSTANCE = new CascadeJsonParser();

  private final JSONKey<Integer> ID = JSONKey.integer("id");
  private final JSONKey<String> NAME = JSONKey.textTrim("value");
  private final JSONKey<JSONObject> CHILD = JSONKey.object("child");

  private CascadeJsonParser() {
  }

  public static JsonEntityParser create(Entity entityType) {
    return new JsonEntityParser.Impl<>(entityType, ServerCustomField.ENUM_ID, ServerCustomField.ENUM_DISPLAY_NAME, INSTANCE, "id");
  }

  @Override
  public boolean fillEntity(Object value, @NotNull Entity entity) {
    if (value == null) return false;
    JSONObject obj = JSONKey.OBJECT.convert(value);
    JSONObject childObj = CHILD.getValue(obj);
    if (childObj != null) {
      if (!loadEntity(entity, childObj)) return false;
      Entity parent = new Entity(entity.getType());
      entity.put(ServerCustomField.ENUM_PARENT, parent);
      entity = parent;
    }
    return loadEntity(entity, obj);
  }

  private boolean loadEntity(Entity target, JSONObject object) {
    if (object == null) return false;
    Integer id = ID.getValue(object);
    String name = NAME.getValue(object);
    if (id == null) {
      LogHelper.error("Missing ID", name);
      return false;
    }
    target.put(ServerCustomField.ENUM_ID, id).putIfNotNull(ServerCustomField.ENUM_DISPLAY_NAME, name);
    return true;
  }

  @Override
  public ValueSupplement<Entity> getSupplement() {
    return null;
  }
}
