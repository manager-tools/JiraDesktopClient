package com.almworks.jira.provider3.gui.timetrack;

import com.almworks.api.application.ItemKey;
import com.almworks.api.application.ItemWrapper;
import com.almworks.api.application.UiItem;
import com.almworks.items.gui.meta.GuiFeaturesManager;
import com.almworks.items.gui.meta.LoadedModelKey;
import com.almworks.items.sync.SyncState;
import com.almworks.jira.provider3.app.connection.JiraConnection3;
import com.almworks.jira.provider3.gui.LoadedIssueUtil;
import com.almworks.jira.provider3.gui.MetaSchema;
import com.almworks.jira.provider3.schema.User;
import com.almworks.util.LogHelper;
import com.almworks.util.collections.Containers;
import com.almworks.util.ui.actions.DataRole;
import org.almworks.util.Collections15;
import org.almworks.util.Const;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public class LoadedWorklog implements UiItem {
  public static final DataRole<LoadedWorklog> WORKLOG = DataRole.createRole(LoadedWorklog.class);
  public static final Comparator<LoadedWorklog> BY_STARTED = new Comparator<LoadedWorklog>() {
    public int compare(LoadedWorklog o1, LoadedWorklog o2) {
      return Containers.comparablesComparator().compare(o1.getStarted(), o2.getStarted());
    }
  };

  private final GuiFeaturesManager myManager;
  private final long myItem;
  private final Date myStarted;
  private final SyncState mySyncState;
  private final long myWhoItem;
  private final long mySecurityItem;
  private final int myDurationSeconds;
  private final String myComment;

  LoadedWorklog(GuiFeaturesManager manager, long item, long whoItem, int durationSeconds, Date started, SyncState syncState, long security, String comment) {
    myManager = manager;
    myItem = item;
    myWhoItem = Math.max(0, whoItem);
    myDurationSeconds = Math.max(0, durationSeconds);
    myStarted = started;
    mySyncState = syncState;
    mySecurityItem = Math.max(0, security);
    myComment = comment;
  }

  @Override
  public long getItem() {
    return myItem;
  }

  @NotNull
  public static List<LoadedWorklog> getWorklogs(ItemWrapper issue) {
    LoadedModelKey<List<LoadedWorklog>> key = getWorklogsKey(issue);
    if (key == null) {
      LogHelper.warning("Missing work log key");
      return Collections.emptyList();
    }
    return Util.NN(issue.getModelKeyValue(key), Collections15.<LoadedWorklog>emptyList());
  }

  @Nullable
  public static LoadedModelKey<List<LoadedWorklog>> getWorklogsKey(ItemWrapper issue) {
    JiraConnection3 connection = Util.castNullable(JiraConnection3.class, issue.getConnection());
    if (connection == null) return null;
    return connection.getGuiFeatures().findListModelKey(MetaSchema.KEY_WORKLOG_LIST, LoadedWorklog.class);
  }

  public SyncState getSyncState() {
    return mySyncState;
  }

  public long getWhoItem() {
    return myWhoItem;
  }

  public int getDurationSeconds() {
    return myDurationSeconds;
  }

  @Nullable
  public Date getStarted() {
    return myStarted;
  }

  @Nullable
  public Long getStartedMillis() {
    return myStarted != null ? myStarted.getTime() : null;
  }

  @Nullable
  public Long getEndMillis() {
    if (myStarted == null) return null;
    return myStarted.getTime() + Const.SECOND*myDurationSeconds;
  }

  @NotNull
  public String getWhoText() {
    return Util.NN(LoadedIssueUtil.getUserDisplayName(myManager, myWhoItem));
  }

  public ItemKey getWho() {
    return LoadedIssueUtil.getItemKey(myManager, User.ENUM_TYPE, myWhoItem);
  }

  @NotNull
  public String getSecurityText() {
    return Util.NN(LoadedIssueUtil.getVisibilityText(myManager, mySecurityItem));
  }

  public ItemKey getVisibility() {
    return LoadedIssueUtil.getVisibilityItem(myManager, mySecurityItem);
  }

  @NotNull
  public String getComment() {
    return Util.NN(myComment, "");
  }
}
