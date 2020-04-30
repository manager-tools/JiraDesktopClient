package com.almworks.jira.provider3.sync.download2.rest;

import com.almworks.items.entities.api.Entity;
import com.almworks.items.entities.api.EntityKey;
import com.almworks.util.LogHelper;
import com.almworks.util.collections.Convertor;
import org.jetbrains.annotations.NotNull;

public class JsonEntityParser<I> {
  private final Entity myType;
  private final EntityKey<I> myIdKey;
  private final EntityKey<String> myNameKey;
  private final EntityParser myParser;
  private final String myJsonIdKey;

  public JsonEntityParser(Entity type, EntityKey<I> idKey, EntityKey<String> nameKey, EntityParser parser, String jsonIdKey) {
    myType = type;
    myIdKey = idKey;
    myNameKey = nameKey;
    myParser = parser;
    myJsonIdKey = jsonIdKey;
  }

  public static <I> JsonEntityParser<I> create(Entity type, EntityKey<I> idKey, EntityKey<String> nameKey, EntityParser parser, String jsonIdKey) {
    return new JsonEntityParser<I>(type, idKey, nameKey, parser, jsonIdKey);
  }

  public EntityKey<I> getIdKey() {
    return myIdKey;
  }

  public EntityKey<String> getNameKey() {
    return myNameKey;
  }

  public EntityParser getParser() {
    return myParser;
  }

  public Entity getType() {
    return myType;
  }

  @NotNull
  public String getJsonIdKey() {
    return myJsonIdKey;
  }

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

  public JsonEntityParser<I> withType(Entity type) {
    LogHelper.assertError(myType == null, myType, type);
    return new JsonEntityParser<I>(type, myIdKey, myNameKey, myParser, myJsonIdKey);
  }

  public JsonEntityParser<I> withParser(EntityParser parser) {
    return new JsonEntityParser<I>(myType, myIdKey, myNameKey, parser, myJsonIdKey);
  }
}
