package com.almworks.jira.provider3.remotedata.issue.fields;

import com.almworks.items.api.DBItemType;
import com.almworks.items.entities.api.Entity;
import com.almworks.items.entities.api.EntityKey;
import com.almworks.items.entities.api.collector.transaction.EntityHolder;
import com.almworks.items.sync.ItemVersion;
import com.almworks.jira.provider3.sync.download2.details.JsonIssueField;
import com.almworks.jira.provider3.sync.download2.details.fields.DependentCollectionField;
import com.almworks.jira.provider3.sync.download2.details.fields.DependentField;
import com.almworks.jira.provider3.sync.download2.details.fields.ScalarField;
import com.almworks.jira.provider3.sync.download2.details.fields.ValueSupplement;
import com.almworks.jira.provider3.sync.download2.rest.JsonEntityParser;
import com.almworks.jira.provider3.sync.download2.rest.LoadedEntity;
import com.almworks.util.collections.Convertor;
import org.almworks.util.Collections15;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public class EntityType<T> {
  public static final Function<LoadedEntity, JSONObject> GENERIC_JSON = LoadedEntity::toJson;

  private final JsonEntityParser myParser;
  @Nullable
  private final Entity myNullValue;

  private EntityType(JsonEntityParser parser, Entity nullValue) {
    myParser = parser;
    myNullValue = nullValue;
  }

  public static <T> EntityType<T> create(JsonEntityParser parser, @Nullable Entity nullValue) {
    return new EntityType<T>(parser, nullValue);
  }

  public JsonIssueField singleDownload(EntityKey<Entity> key) {
    ValueSupplement<Entity> supplement = myParser.getParser().getSupplement();
    if (supplement != null) return new DependentField(key, myParser.createConvertor(), supplement);
    ScalarField<Entity> field = ScalarField.entity(key, myParser.createConvertor());
    return myNullValue != null ? field.nullValue(myNullValue) : field;
  }

  public JsonIssueField multiDownload(EntityKey<Collection<Entity>> key) {
    ValueSupplement<Entity> supplement = myParser.getParser().getSupplement();
    Convertor<Object,Entity> convertor = myParser.createConvertor();
    return supplement == null ? ScalarField.collection(key, convertor) : new DependentCollectionField(key, convertor, supplement);
  }

  @Nullable
  public LoadedEntity readValue(ItemVersion value) {
    return myParser.readValue(value);
  }

  @Nullable
  public LoadedEntity readValue(EntityHolder value) {
    return myParser.readValue(value);
  }

  public DBItemType getType() {
    return myParser.getType();
  }

  public List<LoadedEntity> readValues(List<ItemVersion> items) {
    return readItems(items, this::readValue);
  }

  @NotNull
  public static <T> List<T> readItems(List<ItemVersion> items, Function<ItemVersion, T> loadItem) {
    if (items == null || items.isEmpty()) return Collections.emptyList();
    ArrayList<T> list = Collections15.arrayList();
    for (ItemVersion item : items) {
      T entity = loadItem.apply(item);
      if (entity != null) list.add(entity);
    }
    return list;
  }
}
