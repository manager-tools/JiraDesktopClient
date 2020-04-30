package com.almworks.jira.provider3.remotedata.issue.fields;

import com.almworks.integers.LongList;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.entities.api.Entity;
import com.almworks.items.entities.api.EntityKey;
import com.almworks.items.entities.api.collector.transaction.EntityHolder;
import com.almworks.items.sync.ItemVersion;
import com.almworks.jira.provider3.remotedata.issue.EditIssueRequest;
import com.almworks.jira.provider3.services.upload.PostUploadContext;
import com.almworks.jira.provider3.services.upload.UploadProblem;
import com.almworks.jira.provider3.sync.schema.ServerJira;
import com.almworks.restconnector.operations.RestServerInfo;
import com.almworks.util.LogHelper;
import com.almworks.util.Pair;
import com.almworks.util.collections.LongSet;
import org.almworks.util.Collections15;
import org.jetbrains.annotations.NotNull;
import org.json.simple.JSONArray;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public abstract class EnumDifferenceValue<T> extends BaseValue {
  private static final String ADD = "add";
  private static final String REMOVE = "remove";

  private final List<Pair<T, String>> myAdd;
  private final List<Pair<T, String>> myRemove;
  private final List<Pair<T, String>> myNewValue;

  public EnumDifferenceValue(List<Pair<T, String>> add, List<Pair<T, String>> remove, List<Pair<T, String>> newValue) {
    super(true);
    myAdd = add;
    myRemove = remove;
    myNewValue = newValue;
  }

  @NotNull
  @Override
  public String[] getFormValue(RestServerInfo serverInfo) {
    ArrayList<String> result = Collections15.arrayList();
    for (Pair<T, String> value : myNewValue) {
      T id = value.getFirst();
      if (id != null) result.add(id.toString());
    }
    return result.toArray(new String[result.size()]);
  }

  @Override
  public void addChange(EditIssueRequest edit) throws UploadProblem.Thrown {
    String fieldId = getFieldId();
    if (edit.getFieldInfo(fieldId) == null) return; // No such field
    if (!needsUpload(edit.getServerInfo())) return;
    JSONArray changes = new JSONArray();
    addChanges(edit, changes, myAdd, ADD);
    addChanges(edit, changes, myRemove, REMOVE);
    if (changes.isEmpty()) return;
    edit.addEdit(this, fieldId, changes);
  }

  protected abstract String getFieldId();

  protected abstract void addChanges(EditIssueRequest edit, JSONArray target, List<Pair<T, String>> change, String operation);

  @Override
  public boolean isChanged() {
    return !myAdd.isEmpty() || !myRemove.isEmpty();
  }

  @Override
  protected void doFinishUpload(long issueItem, EntityHolder issue, PostUploadContext context) {
    EntityKey<Collection<Entity>> key = getIssueKey();
    EntityHolder[] serverHolders = issue.getReferenceCollection(key);
    List<Pair<T, String>> server = Collections15.arrayList();
    for (EntityHolder holder : serverHolders) {
      Pair<T, String> pair = readValue(holder);
      if (pair != null) server.add(pair);
    }
    boolean uploaded = true;
    for (Pair<T, String> pair : myAdd)
      if (!contains(server, pair)) {
        LogHelper.warning("Failed to add", pair, this);
        uploaded = false;
      }
    for (Pair<T, String> pair : myRemove) {
      if (contains(server, pair)) {
        LogHelper.warning("Failed to remove", pair, this);
        uploaded = false;
      }
    }
    if (!uploaded) {
      LogHelper.warning("Server state", server, this);
      return;
    }
    context.reportUploaded(issueItem, ServerJira.toLinkSetAttribute(key));
  }

  protected abstract Pair<T, String> readValue(EntityHolder holder);

  protected abstract EntityKey<Collection<Entity>> getIssueKey();

  private boolean contains(List<Pair<T, String>> set, Pair<T, String> element) {
    for (Pair<T, String> pair : set) if (EntityType.equalValue(pair, element)) return true;
    return false;
  }

  protected String toString(Object descriptor) {
    return "Upload " + descriptor + "[+" + myAdd + ",-" + myRemove + "]";
  }

  @Override
  public String checkInitialState(EntityHolder issue) {
    return null; // No test because of uploading difference
  }

  /**
   * @return editable sets of items to add and to remove
   */
  public static Pair<LongSet, LongSet> readAddRemove(DBAttribute<Set<Long>> attribute, ItemVersion trunk, ItemVersion base) {
    LongList change = trunk.getLongSet(attribute);
    LongList original = base.getLongSet(attribute);
    LongSet add = LongSet.setDifference(change, original);
    LongSet remove = LongSet.setDifference(original, change);
    return Pair.create(add, remove);
  }
}
