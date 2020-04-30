package com.almworks.jira.provider3.sync.download2.rest;

import com.almworks.items.entities.api.Entity;
import com.almworks.items.entities.api.EntityKey;
import com.almworks.jira.provider3.sync.schema.ServerGroup;
import com.almworks.jira.provider3.sync.schema.ServerProjectRole;
import com.almworks.restconnector.json.JSONKey;
import com.almworks.util.LogHelper;
import com.almworks.util.collections.Convertor;
import org.json.simple.JSONObject;

public class JRVisibility {
  public static final JSONKey<String> TYPE = JSONKey.text("type");
  public static final JSONKey<String> VALUE = JSONKey.text("value");
  public static final Convertor<Object, Entity> JSON_CONVERTOR = new Convertor<Object, Entity>() {
    @Override
    public Entity convert(Object json) {
      JSONObject object = JSONKey.ROOT_OBJECT.getValue(json);
      if (object == null) return null;
      String strType = TYPE.getValue(object);
      String value = VALUE.getValue(object);
      Entity type;
      EntityKey<String> idKey;
      if ("role".equals(strType)) {
        type = ServerProjectRole.TYPE;
        idKey = ServerProjectRole.NAME;
      } else if ("group".equals(strType)) {
        type = ServerGroup.TYPE;
        idKey = ServerGroup.ID;
      } else {
        type = null;
        idKey = null;
      }
      if (type == null || value == null || value.isEmpty()) {
        LogHelper.warning("Missing data (JRVisibility)", strType, value);
        return null;
      }
      Entity entity = new Entity(type);
      entity.put(idKey, value);
      return entity;
    }
  };

  public static JSONKey<Entity> jsonKey(String key) {
    return new JSONKey<Entity>(key, JSON_CONVERTOR);
  }
}
