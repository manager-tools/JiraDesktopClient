package com.almworks.jira.provider3.remotedata.issue.fields;

import com.almworks.items.api.DBAttribute;
import com.almworks.items.entities.api.Entity;
import com.almworks.items.entities.api.EntityKey;
import com.almworks.items.entities.api.collector.transaction.EntityHolder;
import com.almworks.items.sync.ItemVersion;
import com.almworks.jira.provider3.services.upload.UploadJsonUtil;
import com.almworks.jira.provider3.sync.download2.details.JsonIssueField;
import com.almworks.jira.provider3.sync.download2.details.fields.DependentCollectionField;
import com.almworks.jira.provider3.sync.download2.details.fields.DependentField;
import com.almworks.jira.provider3.sync.download2.details.fields.ScalarField;
import com.almworks.jira.provider3.sync.download2.details.fields.ValueSupplement;
import com.almworks.jira.provider3.sync.download2.rest.JsonEntityParser;
import com.almworks.jira.provider3.sync.schema.ServerJira;
import com.almworks.util.LogHelper;
import com.almworks.util.Pair;
import com.almworks.util.collections.Convertor;
import com.almworks.util.commons.Function2;
import org.almworks.util.Collections15;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class EntityType<T> {
  public static final Function2<Pair<?, String>, String, JSONObject> GENERIC_JSON = new Function2<Pair<?, String>, String, JSONObject>() {
    @Override
    public JSONObject invoke(Pair<?, String> pair, String idKey) {
      if (pair == null) return null;
      Object idValue = pair.getFirst();
      if (idValue == null) {
        LogHelper.error("Missing id", pair, this);
        return null;
      }
      return UploadJsonUtil.object(idKey, String.valueOf(idValue));
    }
  };

  private final DBAttribute<T> myIdAttribute;
  private final DBAttribute<String> myNameAttribute;
  private final JsonEntityParser<T> myParser;
  @Nullable
  private final Entity myNullValue;

  private EntityType(JsonEntityParser<T> parser, Entity nullValue) {
    myParser = parser;
    myNullValue = nullValue;
    myIdAttribute = ServerJira.toScalarAttribute(parser.getIdKey());
    myNameAttribute = ServerJira.toScalarAttribute(parser.getNameKey());
  }

  public static <T> EntityType<T> create(JsonEntityParser<T> parser, @Nullable Entity nullValue) {
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
  public Pair<T, String> readValue(ItemVersion value) {
    if (value == null || value.getItem() <= 0) return null;
    return Pair.create(value.getValue(myIdAttribute), value.getValue(myNameAttribute));
  }

  @Nullable
  public Pair<T, String> readValue(EntityHolder value) {
    if (value == null) return null;
    return Pair.create(value.getScalarValue(myParser.getIdKey()), value.getScalarValue(myParser.getNameKey()));
  }

  @NotNull
  public String getJsonIdKey() {
    return myParser.getJsonIdKey();
  }

  public Entity getType() {
    return myParser.getType();
  }

  public List<Pair<T, String>> readValues(List<ItemVersion> items) {
    if (items == null || items.isEmpty()) return Collections.emptyList();
    ArrayList<Pair<T,String>> list = Collections15.arrayList();
    for (ItemVersion item : items) {
      Pair<T, String> pair = readValue(item);
      if (pair != null) list.add(pair);
    }
    return list;
  }

  public static <T> boolean equalValue(Pair<T, String> a, Pair<T, String> b) {
    if (a == b) return true;
    T idA = a != null ? a.getFirst() : null;
    T idB = b != null ? b.getFirst() : null;
    return Util.equals(idA, idB);
  }
}
