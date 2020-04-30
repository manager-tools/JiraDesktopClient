package com.almworks.jira.provider3.remotedata.issue.misc;

import com.almworks.api.connector.ConnectorException;
import com.almworks.api.http.HttpUtils;
import com.almworks.items.entities.api.collector.transaction.EntityHolder;
import com.almworks.items.entities.api.collector.transaction.EntityTransaction;
import com.almworks.items.sync.ItemVersion;
import com.almworks.jira.provider3.remotedata.issue.UploadUnitUtils;
import com.almworks.jira.provider3.remotedata.issue.edit.EditIssue;
import com.almworks.jira.provider3.remotedata.issue.edit.PrepareIssueUpload;
import com.almworks.jira.provider3.remotedata.issue.fields.*;
import com.almworks.jira.provider3.schema.Issue;
import com.almworks.jira.provider3.services.upload.PostUploadContext;
import com.almworks.jira.provider3.services.upload.UploadContext;
import com.almworks.jira.provider3.services.upload.UploadProblem;
import com.almworks.jira.provider3.services.upload.UploadUnit;
import com.almworks.jira.provider3.sync.schema.ServerIssue;
import com.almworks.restconnector.RequestPolicy;
import com.almworks.restconnector.RestResponse;
import com.almworks.restconnector.RestSession;
import com.almworks.util.LogHelper;
import com.almworks.util.Pair;
import com.almworks.util.collections.LongSet;
import com.almworks.util.i18n.text.LocalizedAccessor;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.jetbrains.annotations.NotNull;
import org.json.simple.JSONValue;

import java.util.*;

public class EditWatchersUnit implements UploadUnit {
  private static final LocalizedAccessor.Value M_EDIT_FAILED_SHORT = PrepareIssueUpload.I18N.getFactory("upload.problem.watch.failed.edit.short");
  private static final LocalizedAccessor.MessageIntStr M_EDIT_FAILED_ADD_FULL = PrepareIssueUpload.I18N.messageIntStr("upload.problem.watch.failed.edit.add.full");
  private static final LocalizedAccessor.MessageIntStr M_EDIT_FAILED_REMOVE_FULL = PrepareIssueUpload.I18N.messageIntStr("upload.problem.watch.failed.edit.remove.full");

  private final EditIssue myIssue;
  private final List<JsonUserParser.LoadedUser> myAddWatchers;
  private final List<JsonUserParser.LoadedUser> myRemoveWatchers;
  /**
   * We record true here is no add/remove watcher has failed. Sometimes, Jira ignores add/remove watcher with successful response code.
   * Jira Web UI behaves the same way - it shows user added/removed from the list, but does nothing (page refresh shows that nothing has changed).<br>
   * Here we emulate JIRA Web UI: if all add/remove requests succeeds, we report all changes uploaded even if Jira has ignored them.
   */
  private boolean myDone = false;

  private EditWatchersUnit(EditIssue issue, List<JsonUserParser.LoadedUser> addWatchers, List<JsonUserParser.LoadedUser> removeWatchers) {
    myIssue = issue;
    myAddWatchers = addWatchers;
    myRemoveWatchers = removeWatchers;
  }

  @Override
  public boolean isDone() {
    return myDone;
  }

  @Override
  public boolean isSurelyFailed(UploadContext context) {
    return UploadUnitUtils.isPreEditFailed(context, myIssue);
  }

  @Override
  public Map<UploadUnit, ConnectorException> loadServerState(RestSession session, EntityTransaction transaction, UploadContext context, TypedKey<Boolean> purpose)
    throws ConnectorException
  {
    return myIssue.loadServerState(session, transaction, context, purpose);
  }

  @Override
  public UploadProblem onInitialStateLoaded(EntityTransaction transaction, UploadContext context) {
    return null;
  }

  @Override
  public Collection<? extends UploadProblem> perform(RestSession session, UploadContext context) throws ConnectorException, UploadProblem.Thrown {
    Integer issueId = myIssue.getCreate().getIssueId();
    if (issueId == null) return UploadProblem.notNow("Issue not submitted").toCollection();
    ArrayList<UploadProblem> problems = Collections15.arrayList();
    JsonUserParser.LoadedUser thisUser = context.getConnection().getConfigHolder().getConnectionLoadedUser();
    if (thisUser == null) {
      LogHelper.error("Missing connection user");
      return Collections.singleton(UploadProblem.internalError());
    }
    List<JsonUserParser.LoadedUser> failedAdd = Collections15.arrayList();
    for (JsonUserParser.LoadedUser user : myAddWatchers) {
      String userId = user.getAccountId();
      RestResponse response = session.postString("api/2/issue/" + issueId + "/watchers", JSONValue.toJSONString(userId), RequestPolicy.SAFE_TO_RETRY);
      if (!response.isSuccessful()) failedAdd.add(user);
    }
    List<JsonUserParser.LoadedUser> failedRemove = Collections15.arrayList();
    for (JsonUserParser.LoadedUser user : myRemoveWatchers) {
      String userId = user.getAccountId();
      String paramName;
      if (userId != null) paramName = "accountId";
      else {
        LogHelper.error("Missing any identity", user);
        failedRemove.add(user);
        continue;
      }
      String url = String.format("api/2/issue/%s/watchers?%s=%s", issueId, paramName, HttpUtils.encode(userId));
      RestResponse response = session.restDelete(url, RequestPolicy.SAFE_TO_RETRY);
      if (!response.isSuccessful()) failedRemove.add(user);
    }
    myDone = failedAdd.isEmpty() && failedRemove.isEmpty();
    return problems;
  }

  private String usersList(List<JsonUserParser.LoadedUser> users) {
    StringBuilder builder = new StringBuilder();
    for (JsonUserParser.LoadedUser user : users) {
      String displayableText = user.getDisplayableText();
      if (builder.length() > 0) builder.append("\n");
      builder.append(displayableText);
    }
    return builder.toString();
  }

  @Override
  public void finishUpload(EntityTransaction transaction, PostUploadContext context) {
    long issueItem = myIssue.getCreate().getIssueItem();
    Integer issueId = myIssue.getCreate().getIssueId();
    if (issueId == null) return;
    EntityHolder issue = ServerIssue.findIssue(transaction, issueId);
    if (issue == null) {
      LogHelper.error("Missing issue");
      return;
    }
    List<JsonUserParser.LoadedUser> currentWatchers = DifferenceValue.loadCurrentState(issue, ServerIssue.WATCHERS, IssueFields.P_USER::readValue);
    Pair<List<JsonUserParser.LoadedUser>, List<JsonUserParser.LoadedUser>> notUploaded = DifferenceValue.isUploadDone(currentWatchers, ServerIssue.WATCHERS, myAddWatchers, myRemoveWatchers);
    if (notUploaded == null || myDone) { // Everything uploaded or nothing has failed (see myDone doc)
      context.reportUploaded(issueItem, Issue.WATCHERS_COUNT);
      context.reportUploaded(issueItem, Issue.WATCHERS);
      context.reportUploaded(issueItem, Issue.WATCHING);
    } else { // If edit watchers failed due, we still may successfully uploaded self-watching state
      List<JsonUserParser.LoadedUser> notAdded = notUploaded.getFirst();
      List<JsonUserParser.LoadedUser> notRemoved = notUploaded.getSecond();
      JsonUserParser.LoadedUser thisUser = context.getConnection().getConfigHolder().getConnectionLoadedUser();
      if (!notAdded.contains(thisUser) && !notRemoved.contains(thisUser))
        context.reportUploaded(issueItem, Issue.WATCHING); // Self-watching uploaded successfully (or has not been requested)
      // Report all failures including ignored (see myDone doc)
      StringBuilder full = new StringBuilder();
      if (!notAdded.isEmpty()) full.append(M_EDIT_FAILED_ADD_FULL.formatMessage(notAdded.size(), usersList(notAdded)));
      if (!notRemoved.isEmpty()) {
        if (full.length() > 0) full.append("\n");
        full.append(M_EDIT_FAILED_REMOVE_FULL.formatMessage(notRemoved.size(), usersList(notRemoved)));
      }
      context.addMessage(this, UploadProblem.fatal(M_EDIT_FAILED_SHORT.create(), full.toString()));
    }
  }

  @NotNull
  @Override
  public Collection<Pair<Long, String>> getMasterItems() {
    return myIssue.getMasterItems();
  }

  public static Collection<? extends UploadUnit> load(EditIssue editIssue, ItemVersion item) {
    ItemVersion trunk = item.switchToTrunk();
    Pair<LongSet,LongSet> addRemove = EnumDifferenceValue.readAddRemove(Issue.WATCHERS, trunk, item.switchToServer());
    List<JsonUserParser.LoadedUser> toAdd = EntityType.readItems(trunk.readItems(addRemove.getFirst()), IssueFields.P_USER::readValue);
    List<JsonUserParser.LoadedUser> toRemove = EntityType.readItems(trunk.readItems(addRemove.getSecond()), IssueFields.P_USER::readValue);

    EditWatchersUnit unit = new EditWatchersUnit(editIssue, toAdd, toRemove);
    return Collections.singleton(unit);
  }
}
