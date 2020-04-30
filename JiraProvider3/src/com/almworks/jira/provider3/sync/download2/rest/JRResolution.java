package com.almworks.jira.provider3.sync.download2.rest;

import com.almworks.items.entities.api.Entity;
import com.almworks.jira.provider3.sync.download2.details.fields.ValueSupplement;
import com.almworks.jira.provider3.sync.schema.ServerResolution;
import com.almworks.restconnector.json.JSONKey;
import com.almworks.restconnector.json.SelfIdExtractor;
import com.almworks.util.collections.Convertor;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;
import org.json.simple.JSONObject;

public class JRResolution {
  private static final Convertor<Object, Integer> ID_EXTRACTOR = new SelfIdExtractor("/rest/api/2/resolution/");

  public static final JSONKey<Integer> ID = new JSONKey<Integer>("self", ID_EXTRACTOR);
  public static final JSONKey<String> NAME = JSONKey.text("name");
  public static final JSONKey<String> ICON = JSONKey.text("iconUrl");
  public static final JSONKey<String> DESCRIPTION = JSONKey.text("description");

  public static final EntityParser PARSER = new EntityParser() {
    private final EntityParser myDefault = new Builder()
      .map(ID, ServerResolution.ID)
      .map(NAME, ServerResolution.NAME)
      .map(ICON, ServerResolution.ICON_URL)
      .map(DESCRIPTION, ServerResolution.DESCRIPTION)
      .create(null);

    @Override
    public boolean fillEntity(Object value, @NotNull Entity entity) {
      if (value == null) {
        entity.copyAllFrom(ServerResolution.UNRESOLVED);
        return true;
      }
      JSONObject object = Util.castNullable(JSONObject.class, value);
      if (object != null) {
        Integer id = ID.getValue(object);
        if (id != null && id == -1) {
          entity.copyAllFrom(ServerResolution.UNRESOLVED);
          return true;
        }
      }
      return myDefault.fillEntity(value, entity);
    }

    @Override
    public ValueSupplement<Entity> getSupplement() {
      return myDefault.getSupplement();
    }
  };
}
