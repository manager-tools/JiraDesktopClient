package com.almworks.jira.provider3.remotedata.issue;

import com.almworks.api.connector.ConnectorException;
import com.almworks.items.entities.api.collector.transaction.EntityHolder;
import com.almworks.items.entities.api.collector.transaction.EntityTransaction;
import com.almworks.items.sync.ItemVersion;
import com.almworks.jira.provider3.remotedata.issue.edit.CreateIssueUnit;
import com.almworks.jira.provider3.remotedata.issue.edit.EditIssue;
import com.almworks.jira.provider3.schema.User;
import com.almworks.jira.provider3.services.upload.PostUploadContext;
import com.almworks.jira.provider3.services.upload.UploadContext;
import com.almworks.jira.provider3.services.upload.UploadProblem;
import com.almworks.jira.provider3.services.upload.UploadUnit;
import com.almworks.restconnector.RestSession;
import com.almworks.util.LogHelper;
import com.almworks.util.Pair;
import org.almworks.util.TypedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;

public abstract class AddEditSlaveUnit<V extends SlaveValues> implements UploadUnit {
  private final long myItem;
  private final CreateIssueUnit myIssue;
  @Nullable("When slave is new")
  private final V myBase;
  private final V myChange;
  private Boolean myDone;
  @Nullable("When no previous failure")
  protected final SlaveIds myKnownSlaves;

  protected AddEditSlaveUnit(long item, CreateIssueUnit issue, @Nullable V base, V change, SlaveIds knownSlaves) {
    myItem = item;
    myIssue = issue;
    myBase = base;
    myChange = change;
    myKnownSlaves = knownSlaves;
  }

  protected abstract UploadProblem checkForConflict(@Nullable EntityHolder issue, @NotNull V base);

  protected abstract Collection<? extends UploadProblem> doPerform(RestSession session, int issueId, @Nullable EditIssue edit, @Nullable V base, V change) throws ConnectorException;

  protected abstract void doFinishUpload(PostUploadContext context, EntityHolder issue, long item, V change, boolean newSlave);

  @Override
  public boolean isDone() {
    return Boolean.TRUE.equals(myDone) || myDone == null && myBase == null && isSubmitted(); // successfully uploaded or previous failure found
  }

  @Override
  public boolean isSurelyFailed(UploadContext context) {
    if (myDone != null) return !myDone;
    return isIssueFailed(context);
  }

  protected boolean isIssueFailed(UploadContext context) {
    return isEditFailed(context, myIssue);
  }

  /**
   * @return true if issue submit or edit has failed (includes conflict)
   */
  public static boolean isEditFailed(UploadContext context, CreateIssueUnit issue) {
    EditIssue edit = issue.getEdit();
    return context.isFailed(edit != null ? edit : issue);  //
  }

  @Override
  public Map<UploadUnit, ConnectorException> loadServerState(RestSession session, EntityTransaction transaction, UploadContext context, TypedKey<Boolean> purpose)
    throws ConnectorException {
    return myIssue.loadServerState(session, transaction, context, purpose);
  }

  @Override
  public UploadProblem onInitialStateLoaded(EntityTransaction transaction, UploadContext context) {
    if (myBase == null) {
      if (myKnownSlaves == null) return null;
      EntityHolder issue = myIssue.findIssue(transaction);
      if (issue != null) myKnownSlaves.searchForSubmitted(issue, context, myChange);
    } else {
      EntityHolder issue = myIssue.findIssue(transaction);
      return checkForConflict(issue, myBase);
    }
    return null;
  }

  @Override
  public Collection<? extends UploadProblem> perform(RestSession session, UploadContext context) throws ConnectorException, UploadProblem.Thrown {
    if (isIssueFailed(context)) return UploadProblem.stop(myIssue).toCollection();
    UploadProblem afterEdit = UploadUnitUtils.checkEditNotDoneYet(myIssue, context);
    if (afterEdit != null) return afterEdit.toCollection();
    Integer issueId = myIssue.getIssueId();
    EditIssue edit = myIssue.getEdit();
    if (issueId == null) {
      LogHelper.error("Must not happen after edit");
      return UploadProblem.internalError().toCollection();
    }
    Collection<? extends UploadProblem> problems = doPerform(session, issueId, edit, myBase, myChange);
    if (problems == null || problems.isEmpty()) myDone = true;
    return problems;
  }

  @Override
  public void finishUpload(EntityTransaction transaction, PostUploadContext context) {
    if (!isDone()) return;
    EntityHolder issue = myIssue.findIssue(transaction);
    if (issue == null) return;
    doFinishUpload(context, issue, myItem, myChange, myBase == null);
  }

  @NotNull
  @Override
  public Collection<Pair<Long, String>> getMasterItems() {
    return myIssue.getMasterItems();
  }

  public static String loadAuthor(ItemVersion author) {
    String authorName = null;
    if (author != null) {
      authorName = author.getValue(User.NAME);
      if (authorName == null) authorName = author.getValue(User.ID);
    }
    if (authorName == null) authorName = "Unknown Author";
    return authorName;
  }

  protected UploadProblem conflict(String shortDescription, @Nullable String longDescription) {
    return UploadProblem.conflict(myItem, shortDescription, longDescription);
  }

  private boolean isSubmitted() {
    return myChange.getId() != null;
  }
}
