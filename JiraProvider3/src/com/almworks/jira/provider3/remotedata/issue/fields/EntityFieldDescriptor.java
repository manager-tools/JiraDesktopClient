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
import com.almworks.jira.provider3.sync.download2.rest.LoadedEntity;
import com.almworks.jira.provider3.sync.schema.ServerJira;
import com.almworks.restconnector.operations.RestServerInfo;
import com.almworks.util.LogHelper;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Objects;
import java.util.function.Function;

public class EntityFieldDescriptor<T> extends IssueFieldDescriptor {
  private static final int UPLOAD_GENERIC = 0;
  private static final int UPLOAD_NO_CHECK = 1;
  private static final int UPLOAD_SPECIAL = 2;

  private final EntityKey<Entity> myKey;
  private final EntityType<T> myType;
  private final DBAttribute<Long> myAttribute;
  private final Function<LoadedEntity, ?> myToJson;
  private final int myUpload;

  private EntityFieldDescriptor(String fieldId, @Nullable String displayName, EntityKey<Entity> key, EntityType<T> type, int upload,
                                Function<LoadedEntity, ?> toJson) {
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
                                                        Function<LoadedEntity, ?> toJson) {
    return new EntityFieldDescriptor<T>(fieldId, displayName, key, type, UPLOAD_GENERIC, toJson);
  }

  /**
   * The field can be updated via edit, but in some cases it cannot be changed. So upload does check that value has been actually changed on server.<br>
   * This is kind of hack. Really such fields should not even try upload if it is not possible. But this method allows to simplify implementation and get rid of deep JIRA investigation.
   */
  public static <T> EntityFieldDescriptor<T> noConfirm(String fieldId, @Nullable String displayName, EntityKey<Entity> key, EntityType<T> type) {
    return new EntityFieldDescriptor<T>(fieldId, displayName, key, type, UPLOAD_NO_CHECK, EntityType.GENERIC_JSON);
  }

  /**
   * The field cannot be updated via edit - the field is read-only or can be changed via special operation only.
   */
  public static <T> EntityFieldDescriptor<T> special(String fieldId, @Nullable String displayName, EntityKey<Entity> key, EntityType<T> type,
                                                     Function<LoadedEntity, ?> toJson) {
    return new EntityFieldDescriptor<T>(fieldId, displayName, key, type, UPLOAD_SPECIAL, toJson);
  }

  @Nullable
  public MyValue findValue(Collection<? extends IssueFieldValue> values) {
    for (IssueFieldValue value : values) {
      MyValue myValue = Util.castNullable(MyValue.class, value);
      if (myValue != null && myValue.myDescriptor == this) return myValue;
    }
    return null;
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
    LoadedEntity change = readValue(trunk);
    LoadedEntity expected = readValue(base);
    if (Util.equals(change, expected)) expected = change;
    return new MyValue(this, expected, change);
  }

  @Nullable
  private LoadedEntity readValue(ItemVersion issue) {
    ItemVersion value = issue.readValue(myAttribute);
    return myType.readValue(value);
  }

  @Nullable
  private LoadedEntity getValue(EntityHolder issue) {
    if (issue == null) return null;
    EntityHolder value = issue.getReference(myKey);
    return myType.readValue(value);
  }

  private String getConflictMessage(LoadedEntity expected, LoadedEntity actual) {
    return createConflictMessage(getDisplayName(), getDisplayableValue(expected), getDisplayableValue(actual));
  }

  public static String getDisplayableValue(LoadedEntity value) {
    String name = value != null ? value.getDisplayableText() : null;
    return name != null ? name : M_NO_VALUE.create();
  }

  public DBAttribute<Long> getAttribute() {
    return myAttribute;
  }

  @Override
  public String toString() {
    return "Entity("+ myKey + ")";
  }

  private Object createToJson(LoadedEntity entity) {
    return myToJson.apply(entity);
  }

  public static class MyValue extends BaseValue {
    private final EntityFieldDescriptor<?> myDescriptor;
    @Nullable
    private final LoadedEntity myExpected;
    @Nullable
    private final LoadedEntity myChange;

    private MyValue(EntityFieldDescriptor<?> descriptor, @Nullable LoadedEntity expected, @Nullable LoadedEntity change) {
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
      return new String[]{myChange != null ? myChange.getFormValueId() : ""};
    }

    @Override
    public String checkInitialState(EntityHolder issue) {
      LoadedEntity server = myDescriptor.getValue(issue);
      if (Objects.equals(server, myExpected)) return null;
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
    public LoadedEntity getChange() {
      return myChange;
    }

    @Nullable
    public LoadedEntity getExpected() {
      return myExpected;
    }

    @Override
    public boolean isChanged() {
      return !Objects.equals(myExpected, myChange);
    }

    @Override
    protected void doFinishUpload(long issueItem, EntityHolder issue, PostUploadContext context) {
      if (!isDone() && isChanged() && myDescriptor.myUpload != UPLOAD_SPECIAL) {
        LogHelper.debug("No attempt to upload changed", this);
        return;
      }
      LoadedEntity server = myDescriptor.getValue(issue);
      if (myDescriptor.myUpload == UPLOAD_NO_CHECK || Objects.equals(myChange, server)) context.reportUploaded(issueItem, myDescriptor.getAttribute());
      else if (EntityKeyProperties.isShadowable(myDescriptor.getIssueEntityKey()))
        LogHelper.debug("Not uploaded", issueItem, this, server);
    }

    @Override
    public String toString() {
      return "Upload " + myDescriptor + "[" + myExpected + "->" + myChange + "]";
    }
  }
}
