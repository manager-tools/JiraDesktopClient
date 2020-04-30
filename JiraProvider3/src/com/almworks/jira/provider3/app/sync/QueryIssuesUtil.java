package com.almworks.jira.provider3.app.sync;

import com.almworks.api.constraint.Constraint;
import com.almworks.api.constraint.FieldSubstringsConstraint;
import com.almworks.api.constraint.OneFieldConstraint;
import com.almworks.api.engine.SyncTask;
import com.almworks.integers.IntArray;
import com.almworks.integers.IntList;
import com.almworks.integers.LongList;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBFilter;
import com.almworks.jira.provider3.app.connection.JiraConnection3;
import com.almworks.jira.provider3.custom.impl.RemoteMetaConfig;
import com.almworks.jira.provider3.schema.Issue;
import com.almworks.spi.provider.AbstractSyncTask;
import com.almworks.util.LogHelper;
import com.almworks.util.commons.Procedure;
import com.almworks.util.progress.Progress;
import org.almworks.util.Collections15;
import org.almworks.util.Util;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

public class QueryIssuesUtil {
  public static SyncTask synchronizeItemView(JiraConnection3 connection, Constraint constraint, DBFilter view, LongList localResult, String queryName, Procedure<SyncTask> runFinally) {
    List<String> keys = getKeysFromKeysOnlyConstraint(constraint);
    AbstractSyncTask task = keys != null && keys.size() > 0 ?
      new JiraSyncArtifactsByKey(queryName, connection, runFinally, keys) :
      new DownloadJqlQuery.Task(queryName, connection, runFinally, constraint, view, localResult);
    task.startTask();
    return task;
  }

  public static SyncTask synchronizeByKey(JiraConnection3 connection, Collection<String> keys, String queryName, Procedure<IntList> runFinally) {
    JiraIssueSyncTask task = JiraSyncArtifactsByKey.reportIssueIds(queryName, connection, runFinally, keys);
    task.startTask();
    return task;
  }

  @Nullable
  private static List<String> getKeysFromKeysOnlyConstraint(Constraint constraint) {
    if (!(constraint instanceof OneFieldConstraint)) return null;
    DBAttribute<?> attribute = ((OneFieldConstraint) constraint).getAttribute();
    if (!Issue.KEY.equals(attribute)) return null;
    if (constraint instanceof FieldSubstringsConstraint) {
      List<String> substrings = ((FieldSubstringsConstraint) constraint).getSubstrings();
      if (substrings != null && substrings.size() > 0) return substrings;
    }
    return null;
  }

  private static class JiraSyncArtifactsByKey extends JiraIssueSyncTask {
    private final List<String> myKeys;
    @Nullable
    private DownloadIssuesByKey myOperation;

    private JiraSyncArtifactsByKey(String queryName, JiraConnection3 connection, Procedure<SyncTask> runFinally, Collection<String> keys) {
      super(queryName, connection, runFinally);
      myKeys = Collections15.arrayList(keys);
      assert myKeys.size() > 0;
    }

    public static JiraIssueSyncTask reportIssueIds(String queryName, JiraConnection3 connection, final Procedure<IntList> runFinally, Collection<String> keys) {
      return new JiraSyncArtifactsByKey(queryName, connection, new Procedure<SyncTask>() {
        @Override
        public void invoke(SyncTask arg) {
          JiraSyncArtifactsByKey task = Util.castNullable(JiraSyncArtifactsByKey.class, arg);
          LogHelper.assertError(task != null, arg);
          IntArray issueIds;
          DownloadIssuesByKey operation = task != null ? task.myOperation : null;
          if (operation != null) {
            issueIds = new IntArray();
            IntList ids = operation.getActualIds();
            if (ids != null) issueIds.addAll(ids);
            if (issueIds.isEmpty()) issueIds = null;
            else issueIds.sortUnique();
          } else issueIds = null;
          runFinally.invoke(issueIds);
        }
      }, keys);
    }

    public String getTaskName() {
      return myKeys.size() == 1 ? "Loading issue " + myKeys.get(0) : "Loading " + myKeys.size() + " issues";
    }

    @Override
    protected DBConnectorOperation createOperation(Progress progress) {
      JiraConnection3 connection = getConnection();
      RemoteMetaConfig metaConfig = connection.getCustomFields().createIssueConversion();
      myOperation = new DownloadIssuesByKey(myKeys, myCancelFlag, connection.getServerInfo(), progress, metaConfig);
      return myOperation;
    }
  }
}
