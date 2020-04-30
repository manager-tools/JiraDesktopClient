package com.almworks.jira.provider3.comments.gui;

import com.almworks.items.api.DBReader;
import com.almworks.items.gui.edit.EditItemModel;
import com.almworks.items.gui.edit.engineactions.EngineConsts;
import com.almworks.items.gui.edit.merge.MergeValue;
import com.almworks.items.gui.edit.merge.SlaveMergeValue;
import com.almworks.items.gui.meta.GuiFeaturesManager;
import com.almworks.items.sync.ItemVersion;
import com.almworks.items.sync.ItemVersionCreator;
import com.almworks.items.sync.SyncState;
import com.almworks.items.sync.util.BranchSource;
import com.almworks.items.sync.util.SyncUtils;
import com.almworks.jira.provider3.gui.LoadedIssueUtil;
import com.almworks.jira.provider3.gui.edit.editors.VisibilityEditor;
import com.almworks.jira.provider3.schema.Comment;
import com.almworks.jira.provider3.schema.User;
import com.almworks.util.components.Canvas;
import com.almworks.util.datetime.DateUtil;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Date;

public class MergeCommentVersion implements SlaveMergeValue.SlaveVersion {

  private static final MergeCommentVersion DELETED = new MergeCommentVersion("", 0, 0, 0, null);

  private final String myText;
  private final long mySecurity;
  private final long myAuthor;
  private final long myEditor;
  private final Date myUpdated;

  public MergeCommentVersion(@NotNull String text, long security, long author, long editor, Date updated) {
    myText = text;
    mySecurity = security;
    myAuthor = author;
    myEditor = editor;
    myUpdated = updated;
  }

  public static void collectMergers(DBReader reader, EditItemModel model, ArrayList<MergeValue> mergers) {
    long issueItem = MergeValue.getSingleItem(model);
    if (issueItem <= 0) return;
    VisibilityEditor.VARIANTS.prepare(BranchSource.trunk(reader), model);
    EngineConsts.ensureGuiFeatureManager(reader, model);
    ItemVersion issue = SyncUtils.readTrunk(reader, issueItem);
    for (ItemVersion comment : issue.readItems(issue.getSlaves(Comment.ISSUE))) {
      SyncState syncState = comment.getSyncState();
      if (!syncState.isConflict()) continue;
      MergeCommentVersion[] versions = new MergeCommentVersion[3];
      for (int i = 0; i < 3; i++) versions[i] = MergeCommentVersion.load(MergeValue.getItemVersion(reader, comment.getItem(), i));
      mergers.add(SlaveMergeValue.create(getDisplayName(comment), comment.getItem(), "comment", model, versions));
    }
  }

  private static String getDisplayName(ItemVersion comment) {
    Date created = comment.getValue(Comment.CREATED);
    String createdStr = created != null ? DateUtil.toFriendlyDateTime(created) : "<new>";
    String author = User.getDisplayName(comment.readValue(Comment.AUTHOR));
    StringBuilder builder = new StringBuilder("Comment ");
    if (author != null) builder.append(author).append(" - ");
    builder.append(createdStr);
    return builder.toString();
  }

  private static MergeCommentVersion load(ItemVersion version) {
    if (version.isInvisible()) return DELETED;
    String text = version.getNNValue(Comment.TEXT, "").trim();
    long security = Math.max(0, version.getNNValue(Comment.LEVEL, 0l));
    long author = Math.max(0, version.getNNValue(Comment.AUTHOR, 0l));
    long editor = version.getNNValue(Comment.EDITOR, 0l);
    if (editor <= 0) editor = author;
    return new MergeCommentVersion(text, security, author, editor, version.getValue(Comment.UPDATED));
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) return true;
    MergeCommentVersion other = Util.castNullable(MergeCommentVersion.class, obj);
    if (other == null) return false;
    if (isDeleted()) return other.isDeleted();
    return mySecurity == other.mySecurity && Util.equals(myText, other.myText);
  }

  @Override
  public int hashCode() {
    return (int)mySecurity ^ myText.hashCode();
  }

  @Override
  public String toString() {
    return toDisplayableString(null);
  }

  @Override
  public void render(GuiFeaturesManager guiFeaturesManager, Canvas canvas, boolean resolution) {
    canvas.appendText(myText);
  }

  public boolean isDeleted() {
    return myText.length() == 0;
  }

  @Override
  public String toDisplayableString(@Nullable GuiFeaturesManager manager) {
    if (isDeleted()) return "<Deleted Comment>";
    StringBuilder builder = new StringBuilder("Comment ");
    boolean header = false;
    if (myAuthor > 0 && manager != null) {
      String text = LoadedIssueUtil.getUserDisplayName(manager, myAuthor);
      if (text != null) {
        builder.append("by ").append(text).append(" ");
        header = true;
      }
    }
    if (myEditor > 0 && myEditor != myAuthor && manager != null) {
      String text = LoadedIssueUtil.getUserDisplayName(manager, myEditor);
      if (text != null) {
        builder.append("edited by ").append(text).append(" ");
        header = true;
      }
    }
    if (mySecurity > 0 && manager != null) {
      String text = LoadedIssueUtil.getVisibilityText(manager, mySecurity);
      if (text != null) {
        builder.append("visible to ").append(text);
        header = true;
      }
    }
    if (header) builder.append("\n");
    builder.append(myText);
    return builder.toString();
  }

  @Override
  public void commitCopy(ItemVersionCreator issue) {
    commitValues(BaseEditComment.createNewComment(issue));
  }

  @Override
  public void commitResolution(ItemVersionCreator slave) {
    if (isDeleted()) {
      slave.setValue(Comment.TEXT, null);
      slave.setValue(Comment.LEVEL, (Long)null);
      slave.delete();
    } else {
      commitValues(slave);
      slave.setAlive();
    }
  }

  private void commitValues(ItemVersionCreator slave) {
    slave.setValue(Comment.TEXT, myText);
    slave.setValue(Comment.LEVEL, mySecurity > 0 ? mySecurity : null);
  }

  public String getText() {
    return myText;
  }

  public static String getRemoteInfo(SlaveMergeValue<MergeCommentVersion> comment) {
    MergeCommentVersion remote = comment.getVersion(MergeValue.REMOTE);
    if (remote.isDeleted()) return "<Deleted>";
    GuiFeaturesManager manager = EngineConsts.getGuiFeaturesManager(comment.getModel());
    StringBuilder builder = new StringBuilder();
    String editor = LoadedIssueUtil.getUserDisplayName(manager, remote.myEditor);
    if (editor != null) builder.append("by ").append(editor);
    Date updated = remote.myUpdated;
    if (updated != null) builder.append(builder.length() > 0 ? " " : "").append(" (").append(DateUtil.toFriendlyDateTime(updated)).append(")");
    return builder.toString();
  }

  public static MergeCommentVersion copy(@NotNull MergeCommentVersion prev, String text, long visibility) {
    text = Util.NN(text).trim();
    if (text.length() == 0) return DELETED;
    return new MergeCommentVersion(text, visibility, prev.myAuthor, prev.myEditor, prev.myUpdated);
  }

  public long getVisibility() {
    return mySecurity;
  }
}
