package com.almworks.jira.provider3.remotedata.issue.edit;

import com.almworks.api.connector.ConnectorException;
import com.almworks.api.constraint.CompositeConstraint;
import com.almworks.items.entities.api.collector.transaction.EntityTransaction;
import com.almworks.items.sync.ItemVersion;
import com.almworks.jira.provider3.schema.User;
import com.almworks.jira.provider3.services.upload.LoadUploadContext;
import com.almworks.jira.provider3.services.upload.UploadContext;
import com.almworks.jira.provider3.services.upload.UploadUnit;
import com.almworks.jira.provider3.sync.ServerFields;
import com.almworks.jira.provider3.sync.download2.details.RestQueryPager;
import com.almworks.jira.provider3.sync.download2.rest.JRIssue;
import com.almworks.jira.provider3.sync.download2.rest.JRIssueType;
import com.almworks.jira.provider3.sync.download2.rest.JRProject;
import com.almworks.restconnector.RestSession;
import com.almworks.restconnector.jql.JQLCompareConstraint;
import com.almworks.restconnector.jql.JqlQuery;
import com.almworks.restconnector.json.sax.JSONCollector;
import com.almworks.spi.provider.util.ServerSyncPoint;
import com.almworks.util.LogHelper;
import com.almworks.util.collections.ByteArray;
import com.almworks.util.collections.UserDataHolder;
import com.almworks.util.commons.Procedure;
import org.almworks.util.Collections15;
import org.almworks.util.Const;
import org.almworks.util.TypedKey;
import org.jetbrains.annotations.Nullable;
import org.json.simple.JSONObject;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class NotConfirmedSubmits {
  public static final TypedKey<NotConfirmedSubmits> INSTANCE_KEY = TypedKey.create("notConfirmedIssues");
  private final Date myLastCreation;
  private final List<NewIssue> mySubmits = Collections15.arrayList();
  private boolean myPrevSubmitsChecked = false;
  private final String myThisUser;

  private NotConfirmedSubmits(Date lastCreation, String thisUser) {
    myLastCreation = lastCreation;
    myThisUser = thisUser;
  }

  public Map<UploadUnit, ConnectorException> checkFailedSubmits(RestSession session, EntityTransaction transaction, UploadContext context) throws ConnectorException {
    if (myPrevSubmitsChecked) return null; // Already done
    myPrevSubmitsChecked = true;
    Date earliestFailure = null;
    final List<NewIssue> failed = Collections15.arrayList();
    for (NewIssue submit : mySubmits) {
      Date failure = submit.getPrevFailure();
      if (failure == null) continue;
      failed.add(submit);
      if (earliestFailure == null || failure.before(earliestFailure)) earliestFailure = failure;
    }
    if (earliestFailure == null) return null; // No issue submit has failed earlier
    LogHelper.debug("Detected", failed.size(), "previously failed submits");
    FindMatch found = FindMatch.perform(session, myThisUser, earliestFailure, failed);
    return found.loadFound(session, transaction, context);
  }

  @Nullable
  public static NotConfirmedSubmits get(LoadUploadContext context) {
    UserDataHolder userData = context.getUserData();
    NotConfirmedSubmits submits = userData.getUserData(INSTANCE_KEY);
    if (submits == null) {
      submits = create(context);
      userData.putUserData(INSTANCE_KEY, submits);
    }
    return submits;
  }

  @Nullable
  private static NotConfirmedSubmits create(LoadUploadContext context) {
    ItemVersion connection = context.getTrunk().forItem(context.getConnection().getConnectionItem());
    String thisUser = User.getConnectionUserId(connection);
    if (thisUser == null) {
      LogHelper.error("No userName", connection);
      return null;
    }
    ServerSyncPoint syncPoint = ServerSyncPoint.loadSyncPoint(connection);
    long time = syncPoint.getLastCreatedTime();
    if (time < Const.DAY) {
      LogHelper.warning("Missing last created time - step back by one day");
      time = System.currentTimeMillis() - Const.DAY;
    }
    Date lastCreation = new Date(time);
    return new NotConfirmedSubmits(lastCreation, thisUser);
  }

  public byte[] getSubmitAttemptMark() {
    byte[] bytes = new byte[8];
    ByteArray.setLong(myLastCreation.getTime(), bytes, 0);
    return bytes;
  }

  public void register(NewIssue submit) {
    mySubmits.add(submit);
  }

  private static class FindMatch implements Procedure<JSONObject> {
    private final List<NewIssue> myFailed;
    private final List<NewIssue> myFound = Collections15.arrayList();

    private FindMatch(List<NewIssue> failed) {
      myFailed = Collections15.arrayList(failed);
    }

    public static FindMatch perform(RestSession session, String thisUser, Date earliestFailure, List<NewIssue> failed) throws ConnectorException {
      RestQueryPager pager = new RestQueryPager(new JqlQuery(CompositeConstraint.Simple.and(
              JQLCompareConstraint.equal("reporter", thisUser),
              new JQLCompareConstraint.Single("created", String.valueOf(earliestFailure.getTime()), ">=", null))));
      pager.setFields(ServerFields.CREATED, ServerFields.SUMMARY, ServerFields.ISSUE_TYPE, ServerFields.PROJECT, ServerFields.PARENT);
      FindMatch find = new FindMatch(failed);
      pager.loadAll(session, JSONCollector.objectConsumer(find), null, null, null);
      return find;
    }

    public Map<UploadUnit, ConnectorException> loadFound(RestSession session, EntityTransaction transaction, UploadContext context) {
      HashMap<UploadUnit, ConnectorException> problems = Collections15.hashMap();
      for (NewIssue newIssue : myFound)
        try {
          newIssue.loadServerState(session, transaction, context, UploadUnit.BEFORE_UPLOAD);
        } catch (ConnectorException e) {
          //noinspection ThrowableResultOfMethodCallIgnored
          problems.put(newIssue, e);
        }
      return problems;
    }

    @Override
    public void invoke(JSONObject issue) {
      int detectedIndex = -1;
      for (int i = 0; i < myFailed.size(); i++) {
        NewIssue submit = myFailed.get(i);
        Date created = JRIssue.CREATED.getValue(issue);
        String summary = JRIssue.SUMMARY.getValue(issue);
        JSONObject issueType = JRIssue.ISSUE_TYPE.getValue(issue);
        JSONObject project = JRIssue.PROJECT.getValue(issue);
        JSONObject parent = JRIssue.PARENT.getValue(issue);
        if (created == null ||summary == null || issueType == null || project == null) {
          LogHelper.error("Missing data", created, summary, issueType, project);
          continue;
        }
        Integer typeId = JRIssueType.ID.getValue(issueType);
        Integer projectId = JRProject.ID.getValue(project);
        if (typeId == null || projectId == null) {
          LogHelper.error("Missing ID", typeId, issueType, projectId, project);
          continue;
        }
        Integer parentId;
        if (parent != null) {
          parentId = JRIssue.ID.getValue(parent);
          if (parentId == null) {
            LogHelper.error("Missing parent id", parent);
            continue;
          }
        } else parentId = null;
        if (!submit.matches(created, summary, projectId, typeId, parentId)) continue;
        if (detectedIndex < 0) detectedIndex = i;
        else {
          LogHelper.error("Two submits matches one issue", myFailed.get(detectedIndex), submit, issue.toJSONString());
          detectedIndex = -1;
          break;
        }
      }
      if (detectedIndex < 0) return;
      NewIssue submit = myFailed.remove(detectedIndex);
      submit.submittedFound(issue);
      myFound.add(submit);
      LogHelper.debug("Failed submit found", submit);
    }
  }
}
