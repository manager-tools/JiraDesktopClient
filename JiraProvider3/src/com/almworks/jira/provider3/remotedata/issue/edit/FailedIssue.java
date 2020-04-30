package com.almworks.jira.provider3.remotedata.issue.edit;

import com.almworks.integers.LongArray;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBReader;
import com.almworks.items.dp.DPEqualsIdentified;
import com.almworks.items.dp.DPNotNull;
import com.almworks.items.entities.api.collector.transaction.EntityHolder;
import com.almworks.items.entities.api.collector.transaction.write.EntityWriter;
import com.almworks.items.entities.api.util.EntityUtils;
import com.almworks.items.entities.dbwrite.downloadstage.DownloadStageMark;
import com.almworks.items.sync.ItemVersion;
import com.almworks.items.sync.impl.SyncSchema;
import com.almworks.items.sync.util.BranchSource;
import com.almworks.jira.provider3.schema.Comment;
import com.almworks.jira.provider3.schema.Issue;
import com.almworks.jira.provider3.schema.IssueType;
import com.almworks.jira.provider3.schema.Project;
import com.almworks.jira.provider3.sync.schema.ServerIssue;
import com.almworks.jira.provider3.sync.schema.ServerIssueType;
import com.almworks.jira.provider3.sync.schema.ServerProject;
import com.almworks.util.LogHelper;
import org.almworks.util.Collections15;
import org.almworks.util.Util;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

class FailedIssue {
  private final long myItem;
  private final String mySummary;
  private final String myDescription;
  private final Date myAttempt;
  private final int myParentId;
  private final String myProjectKey;
  private final int myTypeId;

  private FailedIssue(long item, String summary, String description, Date attempt, int parentId, String projectKey, int typeId) {
    myItem = item;
    mySummary = summary;
    myDescription = description;
    myAttempt = attempt;
    myParentId = parentId;
    myProjectKey = projectKey;
    myTypeId = typeId;
  }

  public static void findFailedUploads(EntityWriter writer) {
    List<EntityHolder> unresolved = DownloadStageMark.filterOutDummy(writer.getUnresolved(ServerIssue.TYPE));
    if (unresolved.isEmpty()) return;
    List<FailedIssue> issues = loadIssues(writer.getReader());
    for (EntityHolder issue : unresolved) {
      int index = findIssue(issues, issue);
      if (index < 0) continue;
      FailedIssue failure = issues.remove(index);
      writer.addExternalResolution(issue, failure.getItem());
    }
  }

  private static int findIssue(List<FailedIssue> issues, EntityHolder entity) {
    int index = -1;
    for (int i = 0; i < issues.size(); i++) {
      FailedIssue issue = issues.get(i);
      if (!issue.matches(entity))
        continue;
      if (index < 0) index = i;
      else {
        LogHelper.warning("Several candidates. Issue ignored", issue, EntityUtils.printValue(entity));
        return -1;
      }
    }
    return index;
  }

  private static List<FailedIssue> loadIssues(DBReader reader) {
    LongArray items = reader.query(DPEqualsIdentified.create(DBAttribute.TYPE, Issue.DB_TYPE).and(DPNotNull.create(SyncSchema.UPLOAD_ATTEMPT))).copyItemsSorted();
    ArrayList<FailedIssue> result = Collections15.arrayList();
    for (ItemVersion item : BranchSource.trunk(reader).readItems(items)) {
      if (item.getValue(Issue.KEY) != null) continue;
      if (item.getValue(Issue.ID) != null) continue;
      Date attempt = CreateIssueUnit.loadPrevAttempt(item);
      if (attempt == null) continue;
      long parent = item.getNNValue(Issue.PARENT, 0l);
      String summary = item.getNNValue(Issue.SUMMARY, "").trim();
      String description = Comment.loadHumanText(item, Issue.DESCRIPTION).trim();
      FailedIssue issue = FailedIssue.load(item, parent, summary, description, attempt);
      if (issue != null) result.add(issue);
    }
    return result;
  }


  public long getItem() {
    return myItem;
  }

  @Nullable
  public static FailedIssue load(ItemVersion item, long parentItem, String summary, String description, Date attempt) {
    int parentId;
    ItemVersion prj;
    if (parentItem <= 0) {
      parentId = 0;
      prj = item.readValue(Issue.PROJECT);
    } else {
      ItemVersion parent = item.forItem(parentItem);
      Integer id = parent.getValue(Issue.ID);
      if (id == null) return null;
      parentId = id;
      prj = parent.readValue(Issue.PROJECT);
    }
    ItemVersion type = item.readValue(Issue.ISSUE_TYPE);
    if (prj == null || type == null) return null;
    String prjKey = prj.getNNValue(Project.KEY, "").trim();
    Integer typeId = type.getValue(IssueType.ID);
    if (typeId == null || prjKey.length() == 0) return null;
    return new FailedIssue(item.getItem(), summary, description, attempt, parentId, prjKey, typeId);
  }

  public boolean matches(EntityHolder entity) {
    EntityHolder parent = entity.getReference(ServerIssue.PARENT);
    if (myParentId != 0) {
      if (parent == null) return false;
      if (!Util.equals(parent.getScalarValue(ServerIssue.ID), myParentId)) return false;
    } else if (parent != null) return false;
    Date created = entity.getScalarValue(ServerIssue.CREATED);
    if (created == null || created.before(myAttempt)) return false;
    if (!Util.equals(mySummary, entity.getScalarValue(ServerIssue.SUMMARY))) return false;
    if (!Util.equals(myDescription, entity.getScalarValue(ServerIssue.DESCRIPTION))) return false;
    EntityHolder project = entity.getReference(ServerIssue.PROJECT);
    EntityHolder type = entity.getReference(ServerIssue.ISSUE_TYPE);
    return !(project == null || type == null)
      && Util.equals(myProjectKey, project.getScalarValue(ServerProject.KEY))
      && Util.equals(myTypeId, type.getScalarValue(ServerIssueType.ID));
  }
}
