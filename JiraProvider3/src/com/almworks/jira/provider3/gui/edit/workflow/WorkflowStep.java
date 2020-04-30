package com.almworks.jira.provider3.gui.edit.workflow;

import com.almworks.items.api.DBReader;
import com.almworks.items.sync.DBDrain;
import com.almworks.items.sync.ItemVersionCreator;
import com.almworks.items.sync.util.identity.DBIdentity;
import com.almworks.items.util.AttributeMap;
import com.almworks.items.util.DatabaseUtil;
import com.almworks.jira.provider3.schema.Jira;
import com.almworks.util.LogHelper;
import com.almworks.util.collections.ByteArray;
import org.jetbrains.annotations.Nullable;

public class WorkflowStep {
  public static final DBIdentity STEP_KIND = Jira.historyStep("workflowAction");
  private static final int VERSION_MARK = 1;
  private final long myAction;
  private final AttributeMap myState;
  private final long myComment;
  private final long myStatus;

  private WorkflowStep(long action, AttributeMap state, long comment, long originalStatus) {
    myAction = action;
    myState = state;
    myComment = comment;
    myStatus = originalStatus;
  }

  public long getAction() {
    return myAction;
  }

  public AttributeMap getState() {
    return myState;
  }

  public long getComment() {
    return myComment;
  }

  public long getExpectedStatus() {
    return myStatus;
  }

  public static void addHistory(ItemVersionCreator issue, long action, long addedComment, long initialStatus) {
    AttributeMap state = issue.getAllShadowableMap();
    WorkflowStep step = new WorkflowStep(action, state, addedComment, initialStatus);
    issue.addHistory(STEP_KIND, step.serialize(issue));
  }

  @Nullable
  public static WorkflowStep load(DBReader reader, ByteArray.Stream stream) {
    if (VERSION_MARK != stream.nextInt()) {
      LogHelper.error("Can read step");
      return null;
    }
    long action = stream.nextLong();
    long comment = stream.nextLong();
    long status = stream.nextLong();
    int stateLength = stream.nextInt();
    if (stream.isErrorOccurred() || stateLength <= 0) return null;
    byte[] stateBytes = new byte[stateLength];
    stream.nextBytes(stateBytes);
    if (!stream.isSuccessfullyAtEnd()) return null;
    AttributeMap state = reader.restoreMap(stateBytes);
    return state != null ? new WorkflowStep(action, state, comment, status) : null;
  }

  public byte[] serialize(DBDrain drain) {
    ByteArray array = new ByteArray();
    array.addInt(VERSION_MARK);
    array.addLong(myAction);
    array.addLong(myComment);
    array.addLong(myStatus);
    byte[] stateBytes = DatabaseUtil.serializeAttributeMap(drain.getReader(), myState);
    array.addInt(stateBytes.length);
    array.add(stateBytes);
    return array.toNativeArray();
  }
}
