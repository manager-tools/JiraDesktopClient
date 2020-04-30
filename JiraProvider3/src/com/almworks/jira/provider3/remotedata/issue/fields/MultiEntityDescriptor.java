package com.almworks.jira.provider3.remotedata.issue.fields;

import com.almworks.integers.LongArray;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.entities.api.Entity;
import com.almworks.items.entities.api.EntityKey;
import com.almworks.items.entities.api.collector.transaction.EntityHolder;
import com.almworks.items.sync.ItemVersion;
import com.almworks.jira.provider3.remotedata.issue.EditIssueRequest;
import com.almworks.jira.provider3.services.upload.UploadJsonUtil;
import com.almworks.jira.provider3.sync.download2.details.JsonIssueField;
import com.almworks.jira.provider3.sync.schema.ServerJira;
import com.almworks.util.LogHelper;
import com.almworks.util.Pair;
import com.almworks.util.collections.LongSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public class MultiEntityDescriptor<T> extends IssueFieldDescriptor {
  private final EntityKey<Collection<Entity>> myKey;
  private final EntityType<T> myType;
  private final DBAttribute<Set<Long>> myAttribute;

  public MultiEntityDescriptor(String fieldId, String displayName, EntityKey<Collection<Entity>> key, EntityType<T> type) {
    super(fieldId, displayName);
    myKey = key;
    myType = type;
    myAttribute = ServerJira.toLinkSetAttribute(myKey);
  }

  public static <T> MultiEntityDescriptor<T> create(String fieldId, @Nullable String displayName, EntityKey<Collection<Entity>> key, EntityType<T> type) {
    return new MultiEntityDescriptor<T>(fieldId, displayName, key, type);
  }

  @NotNull
  @Override
  public EntityKey<Collection<Entity>> getIssueEntityKey() {
    return myKey;
  }

  @Override
  public JsonIssueField createDownloadField() {
    return myType.multiDownload(myKey);
  }

  @Override
  public IssueFieldValue load(ItemVersion trunk, ItemVersion base) {
    Pair<LongSet, LongSet> addRemove = EnumDifferenceValue.readAddRemove(myAttribute, trunk, base);
    List<Pair<T,String>> toAdd = myType.readValues(trunk.readItems(addRemove.getFirst()));
    List<Pair<T,String>> toRemove = myType.readValues(trunk.readItems(addRemove.getSecond()));
    List<Pair<T, String>> newValue = myType.readValues(trunk.readItems(LongArray.create(trunk.getValue(myAttribute))));
    return new MyValue<T>(this, toAdd, toRemove, newValue);
  }

  @Override
  public String toString() {
    return "MultiEntity(" + myKey + ")";
  }

  public DBAttribute<Set<Long>> getAttribute() {
    return myAttribute;
  }

  private JSONObject createToJson(Pair<T, String> pair) {
    return EntityType.GENERIC_JSON.invoke(pair, myType.getJsonIdKey());
  }

  private static class MyValue<T> extends EnumDifferenceValue<T> {
    private final MultiEntityDescriptor<T> myDescriptor;

    public MyValue(MultiEntityDescriptor<T> descriptor, List<Pair<T, String>> add, List<Pair<T, String>> remove, List<Pair<T, String>> newValue) {
      super(add, remove, newValue);
      myDescriptor = descriptor;
    }

    @Override
    public IssueFieldDescriptor getDescriptor() {
      return myDescriptor;
    }

    @Override
    protected void addChanges(EditIssueRequest edit, JSONArray target, List<Pair<T, String>> change, String operation) {
      if (change.isEmpty()) return;
      String fieldId = myDescriptor.getFieldId();
      if (!edit.hasOperation(fieldId, operation)) LogHelper.error("Operation not supported:", operation, this);
      else
        for (Pair<T, String> pair : change) {
          Object jsonObject = myDescriptor.createToJson(pair);
          if (jsonObject != null) //noinspection unchecked
            target.add(UploadJsonUtil.object(operation, jsonObject));
        }
    }

    @Override
    public String toString() {
      return toString(myDescriptor);
    }

    protected String getFieldId() {
      return myDescriptor.getFieldId();
    }

    protected EntityKey<Collection<Entity>> getIssueKey() {
      return myDescriptor.getIssueEntityKey();
    }

    protected Pair<T, String> readValue(EntityHolder holder) {
      return myDescriptor.myType.readValue(holder);
    }
  }
}
