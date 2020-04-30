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
import com.almworks.jira.provider3.sync.download2.details.fields.ScalarField;
import com.almworks.jira.provider3.sync.download2.rest.EntityParser;
import com.almworks.jira.provider3.sync.download2.rest.StringIdToEntityConvertor;
import com.almworks.jira.provider3.sync.schema.ServerCustomField;
import com.almworks.jira.provider3.sync.schema.ServerJira;
import com.almworks.util.LogHelper;
import com.almworks.util.Pair;
import com.almworks.util.collections.LongSet;
import org.almworks.util.Collections15;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.simple.JSONArray;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public class LabelsFieldsDescriptor extends IssueFieldDescriptor {
  private static final EntityKey<String> ID_KEY = ServerCustomField.ENUM_STRING_ID;
  private static final DBAttribute<String> ID_ATTRIBUTE = ServerJira.toScalarAttribute(ID_KEY);

  private final EntityKey<Collection<Entity>> myKey;
  private final Entity myType;
  private final DBAttribute<Set<Long>> myAttribute;

  public LabelsFieldsDescriptor(String fieldId, @Nullable String fieldName, EntityKey<Collection<Entity>> key, Entity type) {
    super(fieldId, fieldName);
    myKey = key;
    myType = type;
    myAttribute = ServerJira.toLinkSetAttribute(key);
  }

  @NotNull
  @Override
  public EntityKey<Collection<Entity>> getIssueEntityKey() {
    return myKey;
  }

  @Override
  public JsonIssueField createDownloadField() {
    return ScalarField.collection(myKey, new EntityParser.AsConvertor(myType, new StringIdToEntityConvertor(ID_KEY, null)));
  }

  @Override
  public IssueFieldValue load(ItemVersion trunk, ItemVersion base) {
    Pair<LongSet, LongSet> addRemove = EnumDifferenceValue.readAddRemove(myAttribute, trunk, base);
    List<String> toAdd = readValues(trunk.readItems(addRemove.getFirst()));
    List<String> toRemove = readValues(trunk.readItems(addRemove.getSecond()));
    List<String> newValue = readValues(trunk.readItems(LongArray.create(trunk.getValue(myAttribute))));
    return new MyValue(this, toAdd, toRemove, newValue);
  }

  private List<String> readValues(List<ItemVersion> items) {
    List<String> pairs = Collections15.arrayList(items.size());
    for (ItemVersion item : items) {
      String id = item.getValue(ID_ATTRIBUTE);
      if (id != null) pairs.add(id);
    }
    return pairs;
  }

  private static class MyValue extends DifferenceValue<String> {
    private final LabelsFieldsDescriptor myDescriptor;

    private MyValue(LabelsFieldsDescriptor descriptor, List<String> add, List<String> remove, List<String> newValue) {
      super(add, remove, newValue);
      myDescriptor = descriptor;
    }

    @Override
    public IssueFieldDescriptor getDescriptor() {
      return myDescriptor;
    }

    @Override
    protected String extractFormId(@NotNull String value) {
      return value;
    }

    @Override
    protected void addChanges(EditIssueRequest edit, JSONArray target, List<String> change, String operation) {
      if (change.isEmpty()) return;
      String fieldId = myDescriptor.getFieldId();
      if (!edit.hasOperation(fieldId, operation)) LogHelper.error("Operation not supported:", operation, this);
      else
        for (String label : change) {
          //noinspection unchecked
          target.add(UploadJsonUtil.object(operation, label));
        }
    }

    @Override
    protected String getFieldId() {
      return myDescriptor.getFieldId();
    }

    @Override
    protected String readValue(EntityHolder holder) {
      return holder.getScalarValue(ID_KEY);
    }

    @Override
    protected EntityKey<Collection<Entity>> getIssueKey() {
      return myDescriptor.getIssueEntityKey();
    }

    @Override
    public String toString() {
      return toString(myDescriptor);
    }
  }
}
