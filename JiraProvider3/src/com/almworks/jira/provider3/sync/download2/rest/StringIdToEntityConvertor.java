package com.almworks.jira.provider3.sync.download2.rest;

import com.almworks.items.entities.api.Entity;
import com.almworks.items.entities.api.EntityKey;
import com.almworks.jira.provider3.sync.download2.details.fields.ValueSupplement;
import com.almworks.restconnector.json.JSONKey;
import com.almworks.util.LogHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class StringIdToEntityConvertor implements EntityParser {
  private final EntityKey<String> myId;
  @Nullable
  private final EntityKey<?> myClearKey;

  /**
   * @param id identifier of the enum value
   * @param clearKey if not null the parse clears this value (for migration from previous version)
   */
  public StringIdToEntityConvertor(EntityKey<String> id, @Nullable EntityKey<?> clearKey) {
    myId = id;
    myClearKey = clearKey;
  }

  @Override
  public boolean fillEntity(Object value, @NotNull Entity entity) {
    if (value == null) return false;
    String id = JSONKey.TEXT_TRIM.convert(value);
    if (id == null) {
      LogHelper.error("Wrong data", value.getClass());
      return false;
    }
    entity.put(myId, id);
    if (myClearKey != null) entity.put(myClearKey, null);
    entity.fix();
    return true;
  }

    @Override
  public ValueSupplement<Entity> getSupplement() {
    return null;
  }
}
