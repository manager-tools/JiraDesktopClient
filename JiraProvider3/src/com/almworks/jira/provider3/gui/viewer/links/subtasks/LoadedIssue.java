package com.almworks.jira.provider3.gui.viewer.links.subtasks;

import com.almworks.api.application.ItemDownloadStage;
import com.almworks.api.application.ItemKey;
import com.almworks.items.gui.meta.EnumTypesCollector;
import com.almworks.items.gui.meta.GuiFeaturesManager;
import com.almworks.items.sync.ItemVersion;
import com.almworks.jira.provider3.schema.*;
import org.almworks.util.Util;

import java.util.Comparator;

public class LoadedIssue {
  public static final Comparator<LoadedIssue> BY_KEY = new Comparator<LoadedIssue>() {
    @Override
    public int compare(LoadedIssue o1, LoadedIssue o2) {
      if (Util.equals(o1, o2)) return 0;
      String key1 = getKey(o1);
      String key2 = getKey(o2);
      if (Util.equals(key1, key2)) return 0;
      if (key1 == null || key2 == null) return key1 == null ? -1 : 1;
      return IssueKeyComparator.INSTANCE.compare(key1, key2);
    }

    private String getKey(LoadedIssue issue) {
      return issue != null ? issue.getKey() : null;
    }
  };
  private final String myKey;
  private final long myItem;
  private final String mySummary;
  private final ItemDownloadStage myStage;
  private final long myStatus;
  private final long myType;
  private final long myAssignee;

  public LoadedIssue(long item, String key, String summary, ItemDownloadStage stage, Long status, Long type, Long assignee) {
    myItem = item;
    myKey = key;
    mySummary = summary;
    myStage = stage;
    myType = Util.NN(type, 0L);
    myStatus = Util.NN(status, 0L);
    myAssignee = Util.NN(assignee, 0L);
  }

  public static LoadedIssue load(ItemVersion issue) {
    ItemDownloadStage stage = ItemDownloadStage.getValue(issue);
    return new LoadedIssue(issue.getItem(), issue.getValue(Issue.KEY), issue.getValue(Issue.SUMMARY), stage, issue.getValue(Issue.STATUS), issue.getValue(Issue.ISSUE_TYPE), issue.getValue(Issue.ASSIGNEE));
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    LoadedIssue other = Util.castNullable(LoadedIssue.class, obj);
    return other != null && other.myItem == myItem && other.myAssignee == myAssignee && other.myStatus == myStatus && other.myType == myType &&
      Util.equals(other.mySummary, mySummary) && Util.equals(other.myKey, myKey) && Util.equals(other.myStage, myStage);
  }

  @Override
  public int hashCode() {
    return Util.hashCode(myItem);
  }

  public String getKey() {
    return myKey;
  }

  public long getItem() {
    return myItem;
  }

  public String getSummary() {
    return mySummary;
  }

  public boolean isDummy() {
    return myStage == ItemDownloadStage.DUMMY;
  }

  public ItemKey getStatusKey(GuiFeaturesManager features) {
    return EnumTypesCollector.getResolvedItem(features, Status.ENUM_TYPE, myStatus);
  }

  public ItemKey getTypeKey(GuiFeaturesManager features) {
    return EnumTypesCollector.getResolvedItem(features, IssueType.ENUM_TYPE, myType);
  }

  public ItemKey getAssigneeKey(GuiFeaturesManager features) {
    return EnumTypesCollector.getResolvedItem(features, User.ENUM_TYPE, myAssignee);
  }
}
