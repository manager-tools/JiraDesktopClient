package com.almworks.jira.provider3.sync.download2.rest;

import com.almworks.items.entities.api.Entity;
import com.almworks.items.entities.api.EntityKey;
import com.almworks.items.entities.dbwrite.downloadstage.DownloadStageMark;
import com.almworks.jira.provider3.sync.download2.details.fields.ValueSupplement;
import com.almworks.restconnector.json.JSONKey;
import com.almworks.util.LogHelper;
import com.almworks.util.Trio;
import com.almworks.util.collections.Convertor;
import com.almworks.util.commons.Procedure;
import org.almworks.util.ArrayUtil;
import org.almworks.util.Collections15;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Extracts values from JSON object to entity. Entity is pre-created.
 */
public interface EntityParser {
  /**
   * Extract values and fill provided entity
   * @param object source JSON object
   * @param entity target entity
   * @return true if all required values are added to entity.<br>
   *   false if no value or failed to parse it. In both cases entity should be thrown away, not written to DB.
   */
  boolean fillEntity(Object object, @NotNull Entity entity);

  @Nullable
  ValueSupplement<Entity> getSupplement();

  class Builder {
    private final Map<JSONKey, EntityKey> myMapping = Collections15.hashMap();
    private final Map<JSONKey<JSONObject>, Trio<EntityKey<Entity>, Convertor<Object, Entity>, Boolean>> myEntityMapping = Collections15.hashMap();
    private final Set<JSONKey<JSONObject>> mySetNull = Collections15.hashSet();
    private final List<Procedure<Entity>> myConstSetters = Collections15.arrayList();

    public Builder() {
    }

    public EntityParser create(@Nullable ValueSupplement<Entity> supplement) {
      return priCreate(supplement);
    }

    private Impl priCreate(@Nullable ValueSupplement<Entity> supplement) {
      JSONKey[] scalarJson = new JSONKey[myMapping.size()];
      EntityKey[] scalarEntity = new EntityKey[myMapping.size()];
      ArrayUtil.mapToArrays(myMapping, scalarJson, scalarEntity);
      List<JSONKey<JSONObject>> entityJson = Collections15.arrayList(myEntityMapping.keySet());
      ArrayList<Trio<EntityKey<Entity>, Convertor<Object, Entity>, Boolean>> entityExtractor = Collections15.arrayList(myEntityMapping.values());
      return new Impl(scalarJson, scalarEntity, entityJson, entityExtractor, supplement, myConstSetters);
    }

    public Convertor<Object, Entity> createPartialConvertor(final Entity type) {
      final Impl parser = priCreate(null);
      return new Convertor<Object, Entity>() {
        @Override
        public Entity convert(Object value) {
          Entity entity = new Entity(type);
          return parser.fillNoFix(value, entity) ? entity : null;
        }
      };
    }

    public <T> Builder map(JSONKey<T> jsonKey, EntityKey<T> entityKey) {
      myMapping.put(jsonKey, entityKey);
      return this;
    }

    /**
     * Map JSON key to entity value. If no value is provided (null returned) may set null or ignore (assume no value is provided)
     * @param jsonKey mapped JSON key
     * @param targetKey key to store entity value in target entity
     * @param convertor converts JSON object to entity value
     * @param setNull if true sets null value. If false ignores null value - assume no value
     */
    public Builder mapEntity(JSONKey<JSONObject> jsonKey, EntityKey<Entity> targetKey, Convertor<Object, Entity> convertor, boolean setNull) {
      myEntityMapping.put(jsonKey, Trio.create(targetKey, convertor, setNull));
      if (setNull) mySetNull.add(jsonKey);
      return this;
    }

    /**
     * Defines constant value for a key. It can be used to:<br>
     * 1. provide constants (such as DownloadStage)
     * 2. predefined value - if the defined value is set if not overridden by {@link #map(com.almworks.restconnector.json.JSONKey, com.almworks.items.entities.api.EntityKey) mapping}
     */
    public <T> Builder set(final EntityKey<T> key, @Nullable final T value) {
      return addConst(new Procedure<Entity>() {
        @Override
        public void invoke(Entity arg) {
          arg.put(key, value);
        }
      });
    }

    public Builder downloadStage(final DownloadStageMark stage) {
      return addConst(new Procedure<Entity>() {
        @Override
        public void invoke(Entity arg) {
          stage.setTo(arg);
        }
      });
    }

    public Builder addConst(Procedure<Entity> setter) {
      myConstSetters.add(setter);
      return this;
    }
  }

  class AsConvertor extends Convertor<Object, Entity> {
    private final Entity myType;
    private final EntityParser myParser;

    public AsConvertor(Entity type, EntityParser parser) {
      myType = type;
      myParser = parser;
    }

    @Override
    public Entity convert(Object value) {
      Entity entity = new Entity(myType);
      if (!myParser.fillEntity(value, entity)) return null;
      if (myParser.getSupplement() == null) entity.fix();
      return entity;
    }
  }

  class Impl implements EntityParser {
    private final JSONKey[] myScalarJson;
    private final EntityKey[] myScalarEntity;
    private final List<JSONKey<JSONObject>> myEntityJson;
    private final List<Trio<EntityKey<Entity>, Convertor<Object, Entity>, Boolean>> myEntityExtractors;
    @Nullable private final ValueSupplement<Entity> mySupplement;
    private final List<Procedure<Entity>> myConstSetters;

    public Impl(JSONKey[] scalarJson, EntityKey[] scalarEntity, List<JSONKey<JSONObject>> entityJson, List<Trio<EntityKey<Entity>, Convertor<Object, Entity>, Boolean>> entityExtractors,
      @Nullable ValueSupplement<Entity> supplement, List<Procedure<Entity>> constSetters) {
      myScalarJson = scalarJson;
      myScalarEntity = scalarEntity;
      myEntityJson = entityJson;
      myEntityExtractors = entityExtractors;
      mySupplement = supplement;
      myConstSetters = Collections15.unmodifiableListCopy(constSetters);
    }

    @Override
    @Nullable
    public ValueSupplement<Entity> getSupplement() {
      return mySupplement;
    }

    @Override
    public boolean fillEntity(Object value, @NotNull Entity entity) {
      if (!fillNoFix(value, entity)) return false;
      if (mySupplement == null) entity.fix();
      return true;
    }

    private boolean fillNoFix(Object value, Entity entity) {
      JSONObject object = JSONKey.ROOT_OBJECT.getValue(value);
      if (object == null) return false;
      for (Procedure<Entity> setter : myConstSetters) setter.invoke(entity);
      for (int j = 0; j < myScalarJson.length; j++) {
        JSONKey jsonKey = myScalarJson[j];
        //noinspection unchecked
        copyScalar(jsonKey, object, myScalarEntity[j], entity);
      }
      for (int i = 0; i < myEntityJson.size(); i++) {
        JSONKey<JSONObject> jsonKey = myEntityJson.get(i);
        Trio<EntityKey<Entity>, Convertor<Object, Entity>, Boolean> extractor = myEntityExtractors.get(i);
        extractEntity(jsonKey, object, extractor, entity);
      }
      return true;
    }

    private static void extractEntity(JSONKey<JSONObject> jsonKey, JSONObject object, Trio<EntityKey<Entity>, Convertor<Object, Entity>, Boolean> extractor, Entity target) {
      extractEntity(jsonKey, object, target, extractor.getFirst(), extractor.getSecond(), extractor.getThird());
    }

    public static void extractEntity(JSONKey<JSONObject> jsonKey, JSONObject object, Entity target, EntityKey<Entity> entityKey, Convertor<Object, Entity> convertor,
      boolean setNull)
    {
      JSONObject subObject = jsonKey.getValue(object);
      if (subObject == null) {
        if (setNull) target.put(entityKey, null);
      } else {
        Entity valueEntity = convertor.convert(subObject);
        if (valueEntity != null) target.put(entityKey, valueEntity);
        else LogHelper.warning("Failed to load", jsonKey, entityKey);
      }
    }

    private static <T> void copyScalar(JSONKey<T> jsonKey, JSONObject object, EntityKey<T> entityKey, Entity entity) {
      T value = jsonKey.getValue(object);
      if (value == null && !jsonKey.hasValue(object)) return;
      entity.put(entityKey, value);
    }
  }
}
