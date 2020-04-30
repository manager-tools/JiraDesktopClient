package com.almworks.jira.provider3.sync.download2.rest;

import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBItemType;
import com.almworks.items.entities.api.Entity;
import com.almworks.items.entities.api.EntityKey;
import com.almworks.items.entities.api.collector.transaction.EntityHolder;
import com.almworks.items.sync.ItemVersion;
import com.almworks.jira.provider3.sync.schema.ServerJira;
import com.almworks.util.LogHelper;
import com.almworks.util.collections.Convertor;

public interface JsonEntityParser {

  static <I> Impl<I> create(Entity type, EntityKey<I> idKey, EntityKey<String> nameKey, EntityParser parser, String jsonIdKey) {
    return new Impl<I>(type, idKey, nameKey, parser, jsonIdKey);
  }

  EntityParser getParser();

  DBItemType getType();

  Convertor<Object, Entity> createConvertor();

  JsonEntityParser withParser(EntityParser parser);

  LoadedEntity readValue(ItemVersion value);

  LoadedEntity readValue(EntityHolder value);

  class Impl<I> implements JsonEntityParser {
    private final Entity myType;
    private final EntityKey<I> myIdKey;
    private final EntityKey<String> myNameKey;
    private final EntityParser myParser;
    private final String myJsonIdKey;
    private final DBAttribute<I> myIdAttribute;
    private final DBAttribute<String> myNameAttribute;

    public Impl(Entity type, EntityKey<I> idKey, EntityKey<String> nameKey, EntityParser parser, String jsonIdKey) {
      myType = type;
      myIdKey = idKey;
      myNameKey = nameKey;
      myParser = parser;
      myJsonIdKey = jsonIdKey;
      myIdAttribute = ServerJira.toScalarAttribute(myIdKey);
      myNameAttribute = ServerJira.toScalarAttribute(myNameKey);
    }

    @Override
    public EntityParser getParser() {
      return myParser;
    }

    @Override
    public DBItemType getType() {
      return ServerJira.toItemType(myType);
    }

    @Override
    public Convertor<Object, Entity> createConvertor() {
      return new Convertor<Object, Entity>() {
        @Override
        public Entity convert(Object value) {
          Entity entity = new Entity(myType);
          if (!myParser.fillEntity(value, entity)) return null;
          return entity;
        }
      };
    }

    public Impl<I> withType(Entity type) {
      LogHelper.assertError(myType == null, myType, type);
      return new Impl<I>(type, myIdKey, myNameKey, myParser, myJsonIdKey);
    }

    @Override
    public Impl<I> withParser(EntityParser parser) {
      return new Impl<I>(myType, myIdKey, myNameKey, parser, myJsonIdKey);
    }

    @Override
    public LoadedEntity readValue(ItemVersion value) {
      if (value == null || value.getItem() <= 0) return null;
      return new LoadedEntity.Simple<>(myJsonIdKey, value.getValue(myIdAttribute), value.getValue(myNameAttribute));
    }

    @Override
    public LoadedEntity readValue(EntityHolder value) {
      if (value == null) return null;
      return new LoadedEntity.Simple<>(myJsonIdKey, value.getScalarValue(myIdKey), value.getScalarValue(myNameKey));
    }
  }
}
