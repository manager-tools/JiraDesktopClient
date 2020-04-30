package com.almworks.jira.provider3.gui.timetrack;

import com.almworks.api.engine.Engine;
import com.almworks.integers.LongList;
import com.almworks.items.api.DBOperationCancelledException;
import com.almworks.items.sync.EditCommit;
import com.almworks.items.sync.EditDrain;
import com.almworks.items.sync.ItemVersionCreator;
import com.almworks.jira.provider3.gui.edit.editors.UploadChangeTask;
import com.almworks.jira.provider3.gui.timetrack.edit.CreateWorklogFeature;
import com.almworks.jira.provider3.schema.Worklog;

import java.util.List;

class UpdateIssueTimes implements EditCommit {
  private final long myIssue;
  private final List<Long> myWhens;
  private final List<Integer> myTimes;
  private final List<String> myTexts;
  private final Integer myNewRemaining;
  private final UploadChangeTask myUpload;
  private final Engine myEngine;

  private UpdateIssueTimes(long issue, List<Long> whens, List<Integer> times, List<String> texts, Integer newRemaining,
    boolean upload, Engine engine) {
    myIssue = issue;
    myWhens = whens;
    myTimes = times;
    myTexts = texts;
    myNewRemaining = newRemaining;
    myEngine = engine;
    myUpload = upload ? new UploadChangeTask() : null;
  }

  @Override
  public void performCommit(EditDrain drain) throws DBOperationCancelledException {
    ItemVersionCreator issue = drain.unsafeChange(myIssue);
    if(!myWhens.isEmpty()) addWorklogsWithRemaining(issue);
    else if (myNewRemaining != null) TimeUtils.commitExplicitRemain(issue, myNewRemaining, false);
    if (myUpload != null) myUpload.addPrimary(issue);
  }

  private void addWorklogsWithRemaining(ItemVersionCreator issue) {
    if (myNewRemaining == null) TimeUtils.commitAutoAdjust(issue, LongList.EMPTY);
    else TimeUtils.commitExplicitRemain(issue, myNewRemaining, true);
    for (int i = 0; i < myWhens.size(); i++) {
      Long when = myWhens.get(i);
      ItemVersionCreator worklog = CreateWorklogFeature.createNewWorklog(issue);
      if (worklog == null) return;
      Worklog.setStarted(worklog, when);
      worklog.setValue(Worklog.TIME_SECONDS, myTimes.get(i));
      worklog.setValue(Worklog.COMMENT, myTexts.get(i));
      if (myNewRemaining == null) worklog.setValue(Worklog.AUTO_ADJUST, true);
    }
  }

  @Override
  public void onCommitFinished(boolean success) {
    if (success && myUpload != null) myUpload.perform(myEngine);
  }

  public static EditCommit create(Engine engine, long item, List<Long> whens, List<Integer> times, List<String> texts,
    Integer newRemaining, boolean upload)
  {
    return new UpdateIssueTimes(item, whens, times, texts, newRemaining, upload, engine);
  }
}
