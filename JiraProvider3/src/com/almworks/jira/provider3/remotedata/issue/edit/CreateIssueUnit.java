package com.almworks.jira.provider3.remotedata.issue.edit;

import com.almworks.api.connector.ConnectorException;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.entities.api.collector.transaction.EntityHolder;
import com.almworks.items.entities.api.collector.transaction.EntityTransaction;
import com.almworks.items.sync.ItemVersion;
import com.almworks.items.sync.impl.SyncSchema;
import com.almworks.jira.provider3.app.connection.JiraConnection3;
import com.almworks.jira.provider3.remotedata.issue.fields.EntityFieldDescriptor;
import com.almworks.jira.provider3.remotedata.issue.fields.IssueFieldValue;
import com.almworks.jira.provider3.schema.Issue;
import com.almworks.jira.provider3.services.upload.LoadUploadContext;
import com.almworks.jira.provider3.services.upload.UploadContext;
import com.almworks.jira.provider3.services.upload.UploadUnit;
import com.almworks.jira.provider3.sync.download2.details.LoadDetails;
import com.almworks.jira.provider3.sync.download2.process.util.ProgressInfo;
import com.almworks.jira.provider3.sync.download2.rest.LoadedEntity;
import com.almworks.jira.provider3.sync.schema.ServerIssue;
import com.almworks.restconnector.RestSession;
import com.almworks.util.LogHelper;
import com.almworks.util.Pair;
import com.almworks.util.collections.ByteArray;
import com.almworks.util.collections.UserDataHolder;
import com.almworks.util.i18n.text.LocalizedAccessor;
import org.almworks.util.TypedKey;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public abstract class CreateIssueUnit implements UploadUnit {
  private static final LocalizedAccessor.Value M_NEW_ISSUE = PrepareIssueUpload.I18N.getFactory("upload.message.newIssue");
  private static final TypedKey<CreateIssueUnit> CREATE_ISSUE = TypedKey.create("createIssue");

  private final long myItem;
  private EditIssue myEdit;

  CreateIssueUnit(long item) {
    myItem = item;
  }

  @Nullable
  public static Pair<EditIssue, List<UploadUnit>> submitIssue(ItemVersion issue, LoadUploadContext context) throws CantUploadException {
    CreateIssueUnit existing = getExisting(issue, context);
    if (existing != null) {
      LogHelper.error("Already created", existing, issue);
      return null;
    }
    long parentItem = Issue.getParent(issue);
    CreateIssueUnit parent;
    if (parentItem > 0) {
      ItemVersion p = issue.forItem(parentItem);
      if (!p.isAlive()) throw new CantUploadException("Cannot create sub-task of deleted parent");
      parent = getExisting(p, context);
      if (parent == null) return null;
    } else parent = null;
    NotConfirmedSubmits prevSubmits = NotConfirmedSubmits.get(context);
    if (prevSubmits == null) throw new CantUploadException("Submit is impossible");
    Date prevCreate = loadPrevAttempt(issue);
    if (prevCreate == null) context.setUploadAttempt(issue.getItem(), prevSubmits.getSubmitAttemptMark());
    NewIssue newIssue = new NewIssue(issue.getItem(), prevSubmits, prevCreate, parent);
    Pair<EditIssue, List<UploadUnit>> pair = EditIssue.load(issue, newIssue, context);
    prevSubmits.register(newIssue);
    remember(context, newIssue);
    return pair;
  }

  @Nullable
  public static Date loadPrevAttempt(ItemVersion issue) {
    byte[] bytes = issue.getValue(SyncSchema.UPLOAD_ATTEMPT);
    if (bytes == null) return null;
    ByteArray.Stream stream = new ByteArray.Stream(bytes);
    long time = stream.nextLong();
    if (!stream.isSuccessfullyAtEnd()) {
      LogHelper.error("Wrong previous upload attempt", stream, issue.getItem());
      return null;
    }
    return new Date(time);
  }

  public static LoadedEntity.Simple<Integer> findChange(EntityFieldDescriptor<Integer> field, Collection<? extends IssueFieldValue> values) {
    EntityFieldDescriptor.MyValue value = field.findValue(values);
    if (value == null) return null;
    LoadedEntity change = value.getChange();
    if (change == null) return null;
    //noinspection unchecked
    LoadedEntity.Simple<Integer> intIdChange = (LoadedEntity.Simple<Integer>) Util.castNullable(LoadedEntity.Simple.class, change);
    if (intIdChange == null) {
      LogHelper.error("Unexpected change class. Field:", field, "change:", change);
      return null;
    }
    return intIdChange;
  }

  public static Integer findChangeId(EntityFieldDescriptor<Integer> field, Collection<? extends IssueFieldValue> values) {
    LoadedEntity.Simple<Integer> change = findChange(field, values);
    if (change == null) return null;
    Integer changeId = change.getId();
    LogHelper.assertError(changeId != null, "Missing id. Field:", field, "value:", change);
    return changeId;
  }

  @Nullable
  public abstract Integer getIssueId();

  @Nullable
  public abstract String getIssueKey();

  /**
   * Called by upload unit if it changes issue key (example: move issue)
   */
  public abstract void issueKeyUpdated(String key);

  @Override
  public String toString() {
    Integer issueId = getIssueId();
    String key = getIssueKey();
    return "CreateIssue(" + myItem +"):" + (issueId == null ? "notDone" : "ID=" + issueId) + (key != null ? "Key=" + key : "");
  }

  public long getIssueItem() {
    return myItem;
  }

  @Nullable
  protected Map<UploadUnit, ConnectorException> doLoadServerState(RestSession session, EntityTransaction transaction, UploadContext context, TypedKey<Boolean> purpose) throws ConnectorException {
    Integer issueId = getIssueId();
    if (issueId == null) return null;
    UserDataHolder cache = context.getItemCache(myItem);
    if (cache.getUserData(purpose) != null) return null; // Already loaded (or failed)
    cache.putUserData(purpose, false); // Mark start
    JiraConnection3 connection = context.getConnection();
    LogHelper.assertError(BEFORE_UPLOAD == purpose || AFTER_UPLOAD == purpose, "Unknown purpose", purpose);
    if (purpose == AFTER_UPLOAD) LoadDetails.loadAllDetails(transaction, session, ProgressInfo.createDeaf(), issueId, context.getCustomFieldKinds(), connection);
    else LoadDetails.loadDetails(transaction, session, ProgressInfo.createDeaf(), issueId, context.getCustomFieldKinds(), connection.getConnectionID());
    cache.putUserData(purpose, true); // Mark already done
    return null;
  }

  @NotNull
  @Override
  public Collection<Pair<Long, String>> getMasterItems() {
    String displayableName = getDisplayableName();
    return Collections.singleton(Pair.create(myItem, displayableName));
  }

  @Nullable
  private static CreateIssueUnit createSubmitted(ItemVersion issue, LoadUploadContext context) throws CantUploadException {
    Integer id = issue.getValue(Issue.ID);
    String key = issue.getValue(Issue.KEY);
    if (id != null && key != null) return new SubmittedIssue(issue.getItem(), id, key);
    if (id != null || key != null) {
      LogHelper.error("Unexpected issue identity set", id, key);
      throw new CantUploadException("Illegal issue data: " + issue);
    }
    if (!issue.equalValue(DBAttribute.TYPE, Issue.DB_TYPE)) {
      LogHelper.error("Not an issue", issue);
      throw new CantUploadException("Not an issue: " + issue);
    }
    if (!context.isInUpload(issue.getItem())) throw new CantUploadException("Not submitted issue is not in upload: " + issue);
    return null;
  }

  @Nullable
  public EntityHolder findIssue(EntityTransaction transaction) {
    Integer issueId = getIssueId();
    if (issueId == null) return null;
    List<EntityHolder> issues = transaction.getAllEntities(ServerIssue.TYPE);
    for (EntityHolder issue : issues) {
      if (issueId.equals(issue.getScalarValue(ServerIssue.ID))) return issue;
    }
    return null;
  }

  public String getDisplayableName() {
    String key = getIssueKey();
    return key != null ? key : M_NEW_ISSUE.create();
  }

  /**
   * @return create issue unit if the unit is already created for not submitted issue or if the issue need not to be submitted (already submitted)<br>
   * null if the issue is NEW and participate in the upload, but the factory wasn't asked yet to create the unit. Caller should call the method later.
   * @throws UploadUnit.CantUploadException if the create unit won't be ever created for the item (in this upload)
   */
  @Nullable
  public static CreateIssueUnit getExisting(ItemVersion issue, LoadUploadContext context) throws CantUploadException {
    CreateIssueUnit create = context.getItemCache(issue.getItem()).getUserData(CREATE_ISSUE);
    if (create != null) return create;
    create = createSubmitted(issue, context);
    if (create != null) remember(context, create);
    return create;
  }

  static void remember(LoadUploadContext context, CreateIssueUnit newIssue) {
    context.getItemCache(newIssue.getIssueItem()).putUserData(CREATE_ISSUE, newIssue);
  }

  void setEdit(EditIssue edit) {
    LogHelper.assertError(myEdit == null, "Edit overridden", myEdit, edit, this);
    myEdit = edit;
  }

  @Nullable("When uploading no issue changes")
  public EditIssue getEdit() {
    return myEdit;
  }
}
