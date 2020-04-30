package com.almworks.jira.provider3.remotedata.issue;

import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBReader;
import com.almworks.items.sync.DBDrain;
import com.almworks.items.sync.ItemVersion;
import com.almworks.items.sync.ItemVersionCreator;
import com.almworks.items.sync.util.identity.DBIdentity;
import com.almworks.items.util.AttributeMap;
import com.almworks.items.util.DatabaseUtil;
import com.almworks.jira.provider3.schema.Issue;
import com.almworks.jira.provider3.schema.IssueType;
import com.almworks.jira.provider3.schema.Jira;
import com.almworks.util.LogHelper;
import com.almworks.util.collections.ByteArray;
import org.almworks.util.Util;
import org.jetbrains.annotations.Nullable;

public class MoveIssueStep {
  public static final DBIdentity STEP_KIND = Jira.historyStep("moveIssue");

  private static final int VERSION_MARK = 1;
  private final AttributeMap myValues;
  private final long myPreviousParent;
  private final long myNewParent;

  private MoveIssueStep(AttributeMap values, long previousParent, long newParent) {
    myValues = values;
    myPreviousParent = previousParent;
    myNewParent = newParent;
  }

  public static void addHistory(ItemVersionCreator issue, long previousParent, long newParent) {
    AttributeMap state = issue.getAllValues();
    MoveIssueStep step = new MoveIssueStep(state, previousParent, newParent);
    issue.addHistory(STEP_KIND, step.serialize(issue));
  }

  public int getExpectedTypeId(DBReader reader) {
    return getReferredValue(reader, Issue.ISSUE_TYPE, IssueType.ID, -1);
  }

  private <T> T getReferredValue(DBReader reader, DBAttribute<Long> issueAttr, DBAttribute<T> refAttr, T nullValue) {
    Long project = myValues.get(issueAttr);
    if (project == null || project <= 0) {
      LogHelper.error("Missing project");
      return nullValue;
    }
    return Util.NN(reader.getValue(project, refAttr), nullValue);
  }

  @Nullable
  public static MoveIssueStep load(DBReader reader, ByteArray.Stream stream) {
    if (VERSION_MARK != stream.nextInt()) {
      LogHelper.error("Can read step");
      return null;
    }
    long prevParent = stream.nextLong();
    long newParent = stream.nextLong();
    int stateLength = stream.nextInt();
    if (stream.isErrorOccurred() || stateLength <= 0) return null;
    byte[] stateBytes = new byte[stateLength];
    stream.nextBytes(stateBytes);
    if (!stream.isSuccessfullyAtEnd()) return null;
    AttributeMap state = reader.restoreMap(stateBytes);
    return state != null ? new MoveIssueStep(state, prevParent, newParent) : null;
  }


  public byte[] serialize(DBDrain drain) {
    ByteArray array = new ByteArray();
    array.addInt(VERSION_MARK);
    array.addLong(myPreviousParent);
    array.addLong(myNewParent);
    byte[] bytes = DatabaseUtil.serializeAttributeMap(drain.getReader(), myValues);
    array.addInt(bytes.length);
    array.add(bytes);
    return array.toNativeArray();
  }

  public AttributeMap getState() {
    return myValues;
  }

  public long getPreviousParent() {
    return myPreviousParent;
  }

  public long getNewParent() {
    return myNewParent;
  }

  public boolean sameTarget(ItemVersion version) {
    long parent = Util.NN(version.getValue(Issue.PARENT), 0l);
    if (myNewParent != parent) return false;
    Long type = version.getValue(Issue.ISSUE_TYPE);
    if (type == null || type <= 0 || !Util.equals(type, myValues.get(Issue.ISSUE_TYPE))) return false;
    if (parent > 0) return true;
    Long project = version.getValue(Issue.PROJECT);
    return project != null && project > 0 && Util.equals(project, myValues.get(Issue.PROJECT));
  }
}
