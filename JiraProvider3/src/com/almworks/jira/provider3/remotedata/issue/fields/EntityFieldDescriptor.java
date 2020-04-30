package com.almworks.jira.provider3.remotedata.issue.fields;

import com.almworks.items.api.DBAttribute;
import com.almworks.items.entities.api.Entity;
import com.almworks.items.entities.api.EntityKey;
import com.almworks.items.entities.api.collector.transaction.EntityHolder;
import com.almworks.items.entities.api.util.EntityKeyProperties;
import com.almworks.items.sync.ItemVersion;
import com.almworks.jira.provider3.remotedata.issue.EditIssueRequest;
import com.almworks.jira.provider3.services.upload.PostUploadContext;
import com.almworks.jira.provider3.services.upload.UploadJsonUtil;
import com.almworks.jira.provider3.services.upload.UploadProblem;
import com.almworks.jira.provider3.sync.download2.details.JsonIssueField;
import com.almworks.jira.provider3.sync.schema.ServerJira;
import com.almworks.restconnector.operations.RestServerInfo;
import com.almworks.util.LogHelper;
import com.almworks.util.Pair;
import com.almworks.util.commons.Function2;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public class EntityFieldDescriptor<T> extends IssueFieldDescriptor {
  private static final int UPLOAD_GENERIC = 0;
  private static final int UPLOAD_NO_CHECK = 1;
  private static final int UPLOAD_SPECIAL = 2;

  private final EntityKey<Entity> myKey;
  private final EntityType<T> myType;
  private final DBAttribute<Long> myAttribute;
  private final Function2<? super Pair<? super T, String>, String, ?> myToJson;
  private final int myUpload;

  private EntityFieldDescriptor(String fieldId, @Nullable String displayName, EntityKey<Entity> key, EntityType<T> type, int upload, Function2<Pair<?, String>, String, ?> toJson) {
    super(fieldId, displayName);
    myKey = key;
    myType = type;
    myUpload = upload;
    myAttribute = ServerJira.toLinkAttribute(myKey);
    myToJson = toJson;
  }

  /**
   * Most generic uploadable field. Upload via edit is possible, check upload result before confirm upload
   */
  public static <T> EntityFieldDescriptor<T> generic(String fieldId, @Nullable String displayName, EntityKey<Entity> key, EntityType<T> type) {
    return customJson(fieldId, displayName, key, type, EntityType.GENERIC_JSON);
  }

  public static <T> EntityFieldDescriptor<T> customJson(String fieldId, @Nullable String displayName, EntityKey<Entity> key, EntityType<T> type,
    Function2<Pair<?, String>, String, ?> toJson) {
    return new EntityFieldDescriptor<T>(fieldId, displayName, key, type, UPLOAD_GENERIC, toJson);
  }

  /**
   * The field can be updated via edit, but in some cases it cannot be changed. So upload does check that value has been actually changed on server.<br>
   * This is kind of hack. Really such fields should not even try upload if it is not possible. But this method allows to simplify implementation and get rid of deep JIRA investigation.
   */
  public static <T> EntityFieldDescriptor<T> noConfirm(String fieldId, @Nullable String displayName, EntityKey<Entity> key, EntityType<T> type,
    Function2<Pair<?, String>, String, ?> toJson) {
    return new EntityFieldDescriptor<T>(fieldId, displayName, key, type, UPLOAD_NO_CHECK, toJson);
  }

  /**
   * The field cannot be updated via edit - the field is read-only or can be changed via special operation only.
   */
  public static <T> EntityFieldDescriptor<T> special(String fieldId, @Nullable String displayName, EntityKey<Entity> key, EntityType<T> type, Function2<Pair<?, String>, String, ?> toJson) {
    return new EntityFieldDescriptor<T>(fieldId, displayName, key, type, UPLOAD_SPECIAL, toJson);
  }

  @Nullable
  public MyValue<T> findValue(Collection<? extends IssueFieldValue> values) {
    for (IssueFieldValue value : values) {
      @SuppressWarnings("unchecked") MyValue<T>
      myValue = Util.castNullable(MyValue.class, value);
      if (myValue != null && myValue.myDescriptor == this) return myValue;
    }
    return null;
  }

  @Nullable
  public T findChangeId(Collection<? extends IssueFieldValue> values) {
    MyValue<T> value = findValue(values);
    if (value == null) return null;
    T changeId = value.getChangeId();
    LogHelper.assertError(changeId != null, "Missing id", value);
    return changeId;
  }

  @NotNull
  @Override
  public EntityKey<?> getIssueEntityKey() {
    return myKey;
  }

  @Override
  public JsonIssueField createDownloadField() {
    return myType.singleDownload(myKey);
  }

  @Override
  public IssueFieldValue load(ItemVersion trunk, ItemVersion base) {
    Pair<T, String> change = readValue(trunk);
    Pair<T, String> expected = readValue(base);
    if (Util.equals(change, expected)) expected = change;
    return new MyValue<T>(this, expected, change);
  }

  @Nullable
  private Pair<T, String> readValue(ItemVersion issue) {
    ItemVersion value = issue.readValue(myAttribute);
    return myType.readValue(value);
  }

  @Nullable
  private Pair<T, String> getValue(EntityHolder issue) {
    if (issue == null) return null;
    EntityHolder value = issue.getReference(myKey);
    return myType.readValue(value);
  }

  private String getConflictMessage(Pair<T, String> expected, Pair<T, String> actual) {
    return createConflictMessage(getDisplayName(), getDisplayableValue(expected), getDisplayableValue(actual));
  }

  private String getDisplayableValue(Pair<T, String> value) {
    String name = value != null ? value.getSecond() : null;
    if (name == null) {
      T id = value != null ? value.getFirst() : null;
      name = id != null ? id.toString() : null;
    }
    return name != null ? name : M_NO_VALUE.create();
  }

  public DBAttribute<Long> getAttribute() {
    return myAttribute;
  }

  @Override
  public String toString() {
    return "Entity("+ myKey + ")";
  }

  private Object createToJson(Pair<T, String> change) {
    return myToJson.invoke(change, myType.getJsonIdKey());
  }

  public static class MyValue<T> extends BaseValue {
    private final EntityFieldDescriptor<T> myDescriptor;
    @Nullable
    private final Pair<T, String> myExpected;
    @Nullable
    private final Pair<T, String> myChange;

    private MyValue(EntityFieldDescriptor<T> descriptor, Pair<T, String> expected, Pair<T, String> change) {
      super(descriptor.myUpload == UPLOAD_GENERIC || descriptor.myUpload == UPLOAD_NO_CHECK);
      myDescriptor = descriptor;
      myExpected = expected;
      myChange = change;
    }

    @Override
    public IssueFieldDescriptor getDescriptor() {
      return myDescriptor;
    }

    @NotNull
    @Override
    public String[] getFormValue(RestServerInfo serverInfo) {
      T id = getChangeId();
      return id != null ? new String[]{id.toString()} : new String[]{""};
    }

    @Override
    public String checkInitialState(EntityHolder issue) {
      Pair<T, String> server = myDescriptor.getValue(issue);
      if (EntityType.equalValue(server, myExpected)) return null;
      return myDescriptor.getConflictMessage(myExpected, server);
    }

    @Override
    public void addChange(EditIssueRequest edit) throws UploadProblem.Thrown {
      String fieldId = myDescriptor.getFieldId();
      if (edit.needsUpload(fieldId, SET, needsUpload(edit.getServerInfo()))) {
        Object value = getJsonValue();
        //noinspection unchecked
        edit.addEdit(this, fieldId, UploadJsonUtil.singleObjectElementArray(SET, value));
      }
    }

    public Object getJsonValue() throws UploadProblem.Thrown {
      return myDescriptor.createToJson(myChange);
    }

    @Nullable
    public Pair<T, String> getChange() {
      return myChange;
    }

    @Nullable
    public Pair<T, String> getExpected() {
      return myExpected;
    }

    @Override
    public boolean isChanged() {
      return !EntityType.equalValue(myExpected, myChange);
    }

    @Override
    protected void doFinishUpload(long issueItem, EntityHolder issue, PostUploadContext context) {
      if (!isDone() && isChanged() && myDescriptor.myUpload != UPLOAD_SPECIAL) {
        LogHelper.debug("No attempt to upload changed", this);
        return;
      }
      Pair<T, String> server = myDescriptor.getValue(issue);
      if (myDescriptor.myUpload == UPLOAD_NO_CHECK || EntityType.equalValue(myChange, server)) context.reportUploaded(issueItem, myDescriptor.getAttribute());
      else if (EntityKeyProperties.isShadowable(myDescriptor.getIssueEntityKey()))
        LogHelper.debug("Not uploaded", issueItem, this, server);
    }

    @Nullable
    public T getChangeId() {
      return getId(myChange);
    }

    private T getId(Pair<T, String> value) {
      if (value == null) return null;
      T id = value.getFirst();
      LogHelper.assertError(id != null, "No change id", this);
      return id;
    }

    @Override
    public String toString() {
      return "Upload " + myDescriptor + "[" + myExpected + "->" + myChange + "]";
    }
  }
}
