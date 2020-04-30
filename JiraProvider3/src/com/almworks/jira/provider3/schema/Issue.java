package com.almworks.jira.provider3.schema;

import com.almworks.api.engine.util.FixedPrimaryItemStructure;
import com.almworks.integers.LongList;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBItemType;
import com.almworks.items.cache.util.EmptyItemListAttribute;
import com.almworks.items.dp.DPEquals;
import com.almworks.items.sync.ItemVersion;
import com.almworks.items.util.AttributeMap;
import com.almworks.items.util.DBNamespace;
import com.almworks.jira.provider3.services.JiraPatterns;
import com.almworks.jira.provider3.sync.schema.ServerIssue;
import com.almworks.jira.provider3.sync.schema.ServerJira;
import org.almworks.util.Collections15;
import org.almworks.util.StringUtil;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;

public class Issue {
  public static final DBItemType DB_TYPE = ServerJira.toItemType(ServerIssue.TYPE);
  public static final DBAttribute<Integer> ID = ServerJira.toScalarAttribute(ServerIssue.ID);
  public static final DBAttribute<String> KEY = ServerJira.toScalarAttribute(ServerIssue.KEY);
  public static final DBAttribute<Long> PARENT = ServerJira.toLinkAttribute(ServerIssue.PARENT);
  public static final DBAttribute<String> SUMMARY = ServerJira.toScalarAttribute(ServerIssue.SUMMARY);
  public static final DBAttribute<String> DESCRIPTION = ServerJira.toScalarAttribute(ServerIssue.DESCRIPTION);
  public static final DBAttribute<String> ENVIRONMENT = ServerJira.toScalarAttribute(ServerIssue.ENVIRONMENT);

  public static final DBAttribute<Date> CREATED = ServerJira.toScalarAttribute(ServerIssue.CREATED);
  public static final DBAttribute<Date> UPDATED = ServerJira.toScalarAttribute(ServerIssue.UPDATED);
  public static final DBAttribute<Date> RESOLVED = ServerJira.toScalarAttribute(ServerIssue.RESOLVED);
  public static final DBAttribute<Integer> DUE = ServerJira.toScalarAttribute(ServerIssue.DUE);

  public static final DBAttribute<Long> PROJECT = ServerJira.toLinkAttribute(ServerIssue.PROJECT);
  public static final DBAttribute<Long> ISSUE_TYPE = ServerJira.toLinkAttribute(ServerIssue.ISSUE_TYPE);
  public static final DBAttribute<Long> PRIORITY = ServerJira.toLinkAttribute(ServerIssue.PRIORITY);
  public static final DBAttribute<Long> STATUS = ServerJira.toLinkAttribute(ServerIssue.STATUS);
  public static final DBAttribute<Long> RESOLUTION = ServerJira.toLinkAttribute(ServerIssue.RESOLUTION);
  public static final DBAttribute<Long> SECURITY = ServerJira.toLinkAttribute(ServerIssue.SECURITY);
  public static final DBAttribute<Long> REPORTER = ServerJira.toLinkAttribute(ServerIssue.REPORTER);
  public static final DBAttribute<Long> ASSIGNEE = ServerJira.toLinkAttribute(ServerIssue.ASSIGNEE);
  public static final DBAttribute<Set<Long>> COMPONENTS = ServerJira.toLinkSetAttribute(ServerIssue.COMPONENTS);
  public static final DBAttribute<Set<Long>> AFFECT_VERSIONS = ServerJira.toLinkSetAttribute(
    ServerIssue.AFFECTED_VERSIONS);
  public static final DBAttribute<Set<Long>> FIX_VERSIONS = ServerJira.toLinkSetAttribute(ServerIssue.FIX_VERSIONS);
  public static final DBAttribute<Integer> VOTES_COUNT = ServerJira.toScalarAttribute(ServerIssue.VOTES_COUNT);
  // Details
  public static final DBAttribute<Integer> WATCHERS_COUNT = ServerJira.toScalarAttribute(ServerIssue.WATCHERS_COUNT);
  public static final DBAttribute<Boolean> VOTED = ServerJira.toScalarAttribute(ServerIssue.VOTED);
  public static final DBAttribute<Boolean> WATCHING = ServerJira.toScalarAttribute(ServerIssue.WATCHING);
  public static final DBAttribute<Set<Long>> VOTERS = ServerJira.toLinkSetAttribute(ServerIssue.VOTERS);
  public static final DBAttribute<Set<Long>> WATCHERS = ServerJira.toLinkSetAttribute(ServerIssue.WATCHERS);
  public static final DBAttribute<Integer> ORIGINAL_ESTIMATE = ServerJira.toScalarAttribute(ServerIssue.ORIGINAL_ESTIMATE);
  public static final DBAttribute<Integer> REMAIN_ESTIMATE = ServerJira.toScalarAttribute(ServerIssue.REMAIN_ESTIMATE);

  private static final DBNamespace NS = ServerJira.NS.subNs("issue");
  /**
   * Local base estimate value to calculate relative estimate changes (by local worklogs)
   */
  public static final DBAttribute<Integer> LOCAL_REMAIN_ESTIMATE = NS.integer("localRemainEstimate", "Remaining Estimate", true);
  /**
   * Flag marks that the issue has local worklogs. Any editor that changes worklogs has to set the flag to hold issue BASE
   * and keep local TIME_SPENT value. With out this flag the issue may become synchronized in spite of it has changed worklogs.
   */
  public static final DBAttribute<Boolean> LOCAL_WORKLOGS = NS.bool("localWorklogs", "Local Work Log Change", true);
  public static final DBAttribute<Integer> TIME_SPENT = ServerJira.toScalarAttribute(ServerIssue.TIME_SPENT);

  public static final DBAttribute<Set<Long>> APPLICABLE_WORKFLOW_ACTIONS = ServerJira.toLinkSetAttribute(ServerIssue.APPLICABLE_WORKFLOW_ACTIONS);
  
  private static final DBAttribute<List<Long>> _FIELDS_FOR_EDIT = ServerJira.toLinkListAttribute(ServerIssue.FIELDS_FOR_EDIT);
  public static final EmptyItemListAttribute FIELDS_FOR_EDIT = new EmptyItemListAttribute(_FIELDS_FOR_EDIT);

  public static final FixedPrimaryItemStructure STRUCTURE =
    new FixedPrimaryItemStructure(DB_TYPE, Comment.ISSUE, Attachment.ISSUE, Link.SOURCE.getAttribute(), Link.TARGET.getAttribute(), Worklog.ISSUE);

  public static final DBAttribute<AttributeMap> ISSUE_DEFAULTS = NS.attributeMap("newIssueDefaults", "New Issue Defaults");

  public static String extractBadKeys(String opposites) {
    List<String> keys = extractIssueKeys(opposites, false);
    if (keys == null || keys.isEmpty()) {
      return null;
    }
    return StringUtil.implode(keys, ", ");
  }

  @NotNull
  public static List<String> extractIssueKeys(@Nullable String text) {
    return Util.NN(extractIssueKeys(text, true), Collections.EMPTY_LIST);
  }

  private static List<String> extractIssueKeys(@Nullable String text, boolean good) {
    if (text == null) return null;
    text = text.trim();
    //noinspection ConstantConditions
    if (text.isEmpty()) return null;
    // todo: Util.upper() here is asking for trouble
    String[] words = Util.upper(text).split("[,;\\s]+");
    List<String> result = Collections15.arrayList();
    for (String word : words) {
      if (JiraPatterns.canBeAnIssueKey(word) == good) {
        result.add(word);
      }
    }
    return result.isEmpty() ? null : result;
  }

  @NotNull
  public static LongList getSubtasks(ItemVersion issue) {
    return issue.getReader().query(DPEquals.create(PARENT, issue.getItem())).copyItemsSorted();
  }

  public static long getParent(ItemVersion issue) {
    Long parent = issue.getValue(PARENT);
    return parent != null && parent > 0 ? parent : 0;
  }
}
