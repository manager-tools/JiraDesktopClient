package com.almworks.jira.provider3.remotedata.issue.fields;

import com.almworks.items.entities.api.Entity;
import com.almworks.items.entities.api.EntityKey;
import com.almworks.items.entities.api.collector.transaction.EntityHolder;
import com.almworks.jira.provider3.remotedata.issue.EditIssueRequest;
import com.almworks.jira.provider3.services.upload.PostUploadContext;
import com.almworks.jira.provider3.sync.schema.ServerJira;
import com.almworks.restconnector.operations.RestServerInfo;
import com.almworks.util.LogHelper;
import com.almworks.util.Pair;
import org.almworks.util.Collections15;
import org.almworks.util.Const;
import org.jetbrains.annotations.NotNull;
import org.json.simple.JSONArray;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author dyoma
 * @param <T> must implement {@link Object#equals(Object)}
 */
public abstract class DifferenceValue<T> extends BaseValue {
  private static final String ADD = "add";
  private static final String REMOVE = "remove";

  private final List<T> myAdd;
  private final List<T> myRemove;
  private final List<T> myNewValue;

  protected DifferenceValue(List<T> add, List<T> remove, List<T> newValue) {
    super(true);
    myAdd = add;
    myRemove = remove;
    myNewValue = newValue;
  }


  @NotNull
  @Override
  public String[] getFormValue(RestServerInfo serverInfo) {
    ArrayList<String> result = Collections15.arrayList();
    for (T value : myNewValue) {
      String id = extractFormId(value);
      if (!id.isEmpty()) result.add(id);
    }
    return result.toArray(Const.EMPTY_STRINGS);
  }

  protected abstract String extractFormId(@NotNull T value);

  @Override
  public void addChange(EditIssueRequest edit) {
    String fieldId = getFieldId();
    if (edit.getFieldInfo(fieldId) == null) return; // No such field
    if (!needsUpload(edit.getServerInfo())) return;
    JSONArray changes = new JSONArray();
    addChanges(edit, changes, myAdd, ADD);
    addChanges(edit, changes, myRemove, REMOVE);
    if (changes.isEmpty()) return;
    edit.addEdit(this, fieldId, changes);
  }

  protected abstract void addChanges(EditIssueRequest edit, JSONArray target, List<T> change, String operation);

  protected abstract String getFieldId();

  @Override
  public boolean isChanged() {
    return !myAdd.isEmpty() || !myRemove.isEmpty();
  }

  @Override
  protected void doFinishUpload(long issueItem, EntityHolder issue, PostUploadContext context) {
    EntityKey<Collection<Entity>> key = getIssueKey();
    if (isUploadDone(loadCurrentState(issue, key, this::readValue), key, myAdd, myRemove) != null) return;
    context.reportUploaded(issueItem, ServerJira.toLinkSetAttribute(key));
  }

  /**
   * @param <T> MUST implement {@link Object#equals(Object)}
   * @return null if current state has all adds and no removes (everything has been uploaded right)<br>
   *   [notAdded, notRemoved] pair if at least one add is missing or remove present. Both lists are not-null.
   */
  public static <T> Pair<List<T>, List<T>> isUploadDone(List<T> current, EntityKey<Collection<Entity>> key, List<T> expectedAdd, List<T> expectedRemove) {
    List<T> notAdded = new ArrayList<>();
    for (T entity : expectedAdd)
      if (!current.contains(entity)) {
        LogHelper.warning("Failed to add", entity, key);
        notAdded.add(entity);
      }
    List<T> notRemoved = new ArrayList<>();
    for (T entity : expectedRemove) {
      if (current.contains(entity)) {
        LogHelper.warning("Failed to remove", entity, key);
        notRemoved.add(entity);
      }
    }
    if (!notAdded.isEmpty() || !notRemoved.isEmpty()) {
      LogHelper.warning("Server state", current, key);
      return Pair.create(notAdded, notRemoved);
    }
    return null;
  }

  @NotNull
  public static <T> List<T> loadCurrentState(EntityHolder issue, EntityKey<Collection<Entity>> key, Function<EntityHolder, T> readValue) {
    EntityHolder[] serverHolders = issue.getReferenceCollection(key);
    return Arrays.stream(serverHolders)
      .map(readValue).filter(Objects::nonNull)
      .collect(Collectors.toList());
  }

  protected abstract T readValue(EntityHolder holder);

  protected abstract EntityKey<Collection<Entity>> getIssueKey();

  protected String toString(Object descriptor) {
    return "Upload " + descriptor + "[+" + myAdd + ",-" + myRemove + "]";
  }

  @Override
  public String checkInitialState(EntityHolder issue) {
    return null; // No test because of uploading difference
  }
}
