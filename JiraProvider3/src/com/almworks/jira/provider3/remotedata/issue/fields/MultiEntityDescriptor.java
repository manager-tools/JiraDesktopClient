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
import com.almworks.jira.provider3.sync.download2.rest.LoadedEntity;
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

public class MultiEntityDescriptor extends IssueFieldDescriptor {
  private final EntityKey<Collection<Entity>> myKey;
  private final EntityType<?> myType;
  private final DBAttribute<Set<Long>> myAttribute;

  public MultiEntityDescriptor(String fieldId, String displayName, EntityKey<Collection<Entity>> key, EntityType<?> type) {
    super(fieldId, displayName);
    myKey = key;
    myType = type;
    myAttribute = ServerJira.toLinkSetAttribute(myKey);
  }

  public static MultiEntityDescriptor create(String fieldId, @Nullable String displayName, EntityKey<Collection<Entity>> key, EntityType<?> type) {
    return new MultiEntityDescriptor(fieldId, displayName, key, type);
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
    List<LoadedEntity> toAdd = myType.readValues(trunk.readItems(addRemove.getFirst()));
    List<LoadedEntity> toRemove = myType.readValues(trunk.readItems(addRemove.getSecond()));
    List<LoadedEntity> newValue = myType.readValues(trunk.readItems(LongArray.create(trunk.getValue(myAttribute))));
    return new MyValue(this, toAdd, toRemove, newValue);
  }

  @Override
  public String toString() {
    return "MultiEntity(" + myKey + ")";
  }

  public DBAttribute<Set<Long>> getAttribute() {
    return myAttribute;
  }

  private JSONObject createToJson(LoadedEntity entity) {
    return entity.toJson();
  }

  private static class MyValue extends EnumDifferenceValue {
    private final MultiEntityDescriptor myDescriptor;

    public MyValue(MultiEntityDescriptor descriptor, List<LoadedEntity> add, List<LoadedEntity> remove, List<LoadedEntity> newValue) {
      super(add, remove, newValue);
      myDescriptor = descriptor;
    }

    @Override
    public IssueFieldDescriptor getDescriptor() {
      return myDescriptor;
    }

    @Override
    protected void addChanges(EditIssueRequest edit, JSONArray target, List<LoadedEntity> change, String operation) {
      if (change.isEmpty()) return;
      String fieldId = myDescriptor.getFieldId();
      if (!edit.hasOperation(fieldId, operation)) LogHelper.error("Operation not supported:", operation, this);
      else
        for (LoadedEntity entity : change) {
          Object jsonObject = myDescriptor.createToJson(entity);
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

    protected LoadedEntity readValue(EntityHolder holder) {
      return myDescriptor.myType.readValue(holder);
    }
  }
}
