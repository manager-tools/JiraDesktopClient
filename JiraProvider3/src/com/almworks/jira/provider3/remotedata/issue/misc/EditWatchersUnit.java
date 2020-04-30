package com.almworks.jira.provider3.remotedata.issue.misc;

import com.almworks.api.connector.ConnectorException;
import com.almworks.api.http.HttpUtils;
import com.almworks.items.entities.api.collector.transaction.EntityTransaction;
import com.almworks.items.sync.ItemVersion;
import com.almworks.jira.provider3.remotedata.issue.UploadUnitUtils;
import com.almworks.jira.provider3.remotedata.issue.edit.EditIssue;
import com.almworks.jira.provider3.remotedata.issue.edit.PrepareIssueUpload;
import com.almworks.jira.provider3.remotedata.issue.fields.EnumDifferenceValue;
import com.almworks.jira.provider3.schema.Issue;
import com.almworks.jira.provider3.schema.User;
import com.almworks.jira.provider3.services.upload.PostUploadContext;
import com.almworks.jira.provider3.services.upload.UploadContext;
import com.almworks.jira.provider3.services.upload.UploadProblem;
import com.almworks.jira.provider3.services.upload.UploadUnit;
import com.almworks.restconnector.RequestPolicy;
import com.almworks.restconnector.RestResponse;
import com.almworks.restconnector.RestSession;
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
  private final List<Pair<String, String>> myAddWatchers;
  private final List<Pair<String, String>> myRemoveWatchers;
  private boolean myDone = false;
  private boolean myThisDone = false;

  public EditWatchersUnit(EditIssue issue, List<Pair<String, String>> addWatchers, List<Pair<String, String>> removeWatchers) {
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
    String jiraUsername = context.getConnection().getConfigHolder().getJiraUsername();
    List<Pair<String, String>> failedAdd = Collections15.arrayList();
    for (Pair<String, String> idName : myAddWatchers) {
      String userId = idName.getFirst();
      RestResponse response = session.postString("api/2/issue/" + issueId + "/watchers", JSONValue.toJSONString(userId), RequestPolicy.SAFE_TO_RETRY);
      if (!response.isSuccessful()) failedAdd.add(idName);
      else if (userId.equals(jiraUsername)) myThisDone = true;
    }
    List<Pair<String, String>> failedRemove = Collections15.arrayList();
    for (Pair<String, String> idName : myRemoveWatchers) {
      String userId = idName.getFirst();
      RestResponse response = session.restDelete("api/2/issue/" + issueId + "/watchers?username=" + HttpUtils.encode(userId), RequestPolicy.SAFE_TO_RETRY);
      if (!response.isSuccessful()) failedRemove.add(idName);
      else if (userId.equals(jiraUsername)) myThisDone = true;
    }
    if (!failedAdd.isEmpty() || !failedRemove.isEmpty()) {
      StringBuilder full = new StringBuilder();
      if (!failedAdd.isEmpty()) full.append(M_EDIT_FAILED_ADD_FULL.formatMessage(failedAdd.size(), usersList(failedAdd)));
      if (!failedRemove.isEmpty()) {
        if (full.length() > 0) full.append("\n");
        full.append(M_EDIT_FAILED_REMOVE_FULL.formatMessage(failedRemove.size(), usersList(failedRemove)));
      }
      problems.add(UploadProblem.fatal(M_EDIT_FAILED_SHORT.create(), full.toString()));
    }
    myDone = failedAdd.isEmpty() && failedRemove.isEmpty();
    return problems;
  }

  private String usersList(List<Pair<String, String>> idNames) {
    StringBuilder builder = new StringBuilder();
    for (Pair<String, String> idName : idNames) {
      String id = idName.getFirst();
      String name = idName.getSecond();
      if (name == null || name.isEmpty()) name = id;
      if (builder.length() > 0) builder.append("\n");
      if (!name.equals(id)) builder.append(name).append(" (").append(id).append(")");
      else builder.append(id);
    }
    return builder.toString();
  }

  @Override
  public void finishUpload(EntityTransaction transaction, PostUploadContext context) {
    long issue = myIssue.getCreate().getIssueItem();
    if (myThisDone) context.reportUploaded(issue, Issue.WATCHING);
    if (myDone) {
      context.reportUploaded(issue, Issue.WATCHERS_COUNT);
      context.reportUploaded(issue, Issue.WATCHERS);
    }
  }

  @NotNull
  @Override
  public Collection<Pair<Long, String>> getMasterItems() {
    return myIssue.getMasterItems();
  }

  public static Collection<? extends UploadUnit> load(EditIssue editIssue, ItemVersion item) {
    Pair<LongSet,LongSet> addRemove = EnumDifferenceValue.readAddRemove(Issue.WATCHERS, item.switchToTrunk(), item.switchToServer());
    EditWatchersUnit unit = new EditWatchersUnit(editIssue, User.loadIdNames(item, addRemove.getFirst()), User.loadIdNames(item, addRemove.getSecond()));
    return Collections.singleton(unit);
  }
}
