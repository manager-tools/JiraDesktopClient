package com.almworks.jira.provider3.gui.timetrack.edit;

import com.almworks.items.api.DBReader;
import com.almworks.items.gui.edit.EditItemModel;
import com.almworks.items.gui.edit.engineactions.EngineConsts;
import com.almworks.items.gui.edit.merge.MergeValue;
import com.almworks.items.gui.edit.merge.SlaveMergeValue;
import com.almworks.items.gui.meta.GuiFeaturesManager;
import com.almworks.items.sync.ItemVersion;
import com.almworks.items.sync.ItemVersionCreator;
import com.almworks.items.sync.SyncState;
import com.almworks.items.sync.util.SyncUtils;
import com.almworks.jira.provider3.gui.LoadedIssueUtil;
import com.almworks.jira.provider3.schema.Issue;
import com.almworks.jira.provider3.schema.User;
import com.almworks.jira.provider3.schema.Worklog;
import com.almworks.util.components.Canvas;
import com.almworks.util.components.CanvasSection;
import com.almworks.util.components.PlainTextCanvas;
import com.almworks.util.datetime.DateUtil;
import com.almworks.util.datetime.TimeIntervalUtil;
import org.almworks.util.Util;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.ArrayList;
import java.util.Date;

public class MergeWorklogVersion implements SlaveMergeValue.SlaveVersion {
  private static final MergeWorklogVersion DELETED = new MergeWorklogVersion(null, 0, null, 0);
  private final Date myStarted;
  private final int mySeconds;
  private final String myComment;
  private final long myVisibility;

  MergeWorklogVersion(Date started, int seconds, String comment, long visibility) {
    myStarted = started;
    mySeconds = seconds;
    myComment = comment;
    myVisibility = visibility;
  }

  public static void collectMergers(DBReader reader, EditItemModel model, ArrayList<MergeValue> mergers) {
    EngineConsts.ensureGuiFeatureManager(reader, model);
    long issueItem = MergeValue.getSingleItem(model);
    if (issueItem <= 0) return;
    ItemVersion issue = SyncUtils.readTrunk(reader, issueItem);
    for (ItemVersion worklog : issue.readItems(issue.getSlaves(Worklog.ISSUE))) {
      SyncState syncState = worklog.getSyncState();
      if (!syncState.isConflict()) continue;
      MergeWorklogVersion[] versions = new MergeWorklogVersion[3];
      for (int i = 0; i < 3; i++) versions[i] = MergeWorklogVersion.load(MergeValue.getItemVersion(reader, worklog.getItem(), i));
      mergers.add(SlaveMergeValue.create(getDisplayName(worklog), worklog.getItem(), "worklog", model, versions));
    }
  }

  private static String getDisplayName(ItemVersion worklog) {
    ItemVersion author = worklog.readValue(Worklog.AUTHOR);
    String prefix = author != null ? User.getDisplayName(author) : null;
    String displayName = prefix != null ? prefix + " logged work" : "Logged work";
    Date started = worklog.getValue(Worklog.STARTED);
    if (started != null) displayName = displayName + DateUtil.toFriendlyDateTime(started);
    return displayName;
  }

  public boolean isDeleted() {
    return myComment == null;
  }

  public static MergeWorklogVersion load(ItemVersion worklog) {
    if (worklog.isInvisible()) return DELETED;
    Date started = worklog.getValue(Worklog.STARTED);
    int seconds = Math.max(0, worklog.getNNValue(Worklog.TIME_SECONDS, 0));
    String comment = worklog.getNNValue(Worklog.COMMENT, "").trim();
    long visibility = Math.max(0, worklog.getNNValue(Worklog.SECURITY, 0l));
    return new MergeWorklogVersion(started, seconds, comment, visibility);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) return true;
    MergeWorklogVersion other = Util.castNullable(MergeWorklogVersion.class, obj);
    if (other == null) return false;
    if (isDeleted()) return other.isDeleted();
    return other != null && mySeconds == other.mySeconds && Util.equals(myStarted, other.myStarted) &&
      Util.equals(myComment, other.myComment) && myVisibility == other.myVisibility;
  }

  @Override
  public int hashCode() {
    return Util.hashCode(myStarted, myComment) ^ mySeconds ^ ((int)myVisibility);
  }

  public void render(@Nullable GuiFeaturesManager manager, Canvas canvas, boolean resolution) {
    if (myStarted != null) {
      canvas.appendText(DateUtil.toFriendlyDateTime(myStarted));
      canvas.appendText(" ");
    }
    canvas.appendText(TimeIntervalUtil.toTextDuration(mySeconds));
    if (myComment.length() > 0) {
      canvas.appendText(" ");
      canvas.appendText(myComment);
    }
    if (myVisibility > 0 && manager != null) {
      String visibility = LoadedIssueUtil.getVisibilityText(manager, myVisibility);
      if (visibility != null) {
        canvas.appendText(" ");
        CanvasSection section = canvas.newSection();
        section.setForeground(Color.RED);
        section.appendText(visibility);
      }
    }
  }

  public String toDisplayableString(@Nullable GuiFeaturesManager manager) {
    PlainTextCanvas canvas = new PlainTextCanvas();
    render(manager, canvas, false);
    return canvas.getText();
  }

  @Override
  public String toString() {
    return toDisplayableString(null);
  }

  @Override
  public void commitCopy(ItemVersionCreator issue) {
    commitValues(CreateWorklogFeature.createNewWorklog(issue));
  }

  @Override
  public void commitResolution(ItemVersionCreator slave) {
    slave.setAlive();
    commitValues(slave);
  }

  private void commitValues(ItemVersionCreator worklog) {
    Date started = getStarted();
    Worklog.setStarted(worklog, started != null ? started.getTime() : 0);
    worklog.setValue(Worklog.TIME_SECONDS, getSeconds());
    worklog.setValue(Worklog.SECURITY, getVisibility());
    String comment = getComment();
    if (comment.length() > 0) worklog.setValue(Worklog.COMMENT, comment);
    ItemVersion issue = worklog.readValue(Worklog.ISSUE);
    if (issue != null && issue.getValue(Issue.LOCAL_REMAIN_ESTIMATE) != null) worklog.setValue(Worklog.AUTO_ADJUST, true);
  }

  public Date getStarted() {
    return myStarted;
  }

  public int getSeconds() {
    return mySeconds;
  }

  public String getComment() {
    return myComment;
  }

  public long getVisibility() {
    return myVisibility;
  }
}
