package com.almworks.jira.provider3.schema;

import com.almworks.integers.LongList;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBItemType;
import com.almworks.items.api.DBReader;
import com.almworks.items.dp.DPEquals;
import com.almworks.items.sync.ItemDiff;
import com.almworks.items.sync.ItemVersion;
import com.almworks.items.sync.ItemVersionCreator;
import com.almworks.items.sync.edit.MergeDataImpl;
import com.almworks.items.sync.impl.SyncSchema;
import com.almworks.items.sync.util.ItemDiffImpl;
import com.almworks.items.sync.util.ItemVersionCommonImpl;
import com.almworks.items.sync.util.SyncUtils;
import com.almworks.items.util.AttributeMap;
import com.almworks.items.util.DBNamespace;
import com.almworks.jira.provider3.sync.schema.ServerJira;
import com.almworks.jira.provider3.sync.schema.ServerWorklog;
import org.almworks.util.Util;

import java.util.Date;

public class Worklog {
  private static final DBNamespace NS = ServerJira.NS.subNs("worklog");
  public static final DBItemType DB_TYPE = ServerJira.toItemType(ServerWorklog.TYPE);
  public static final DBAttribute<Long> ISSUE = ServerJira.toLinkAttribute(ServerWorklog.ISSUE);
  public static final DBAttribute<Integer> ID = ServerJira.toScalarAttribute(ServerWorklog.ID);
  public static final DBAttribute<Integer> TIME_SECONDS = ServerJira.toScalarAttribute(ServerWorklog.TIME_SECONDS);
  public static final DBAttribute<String> COMMENT = ServerJira.toScalarAttribute(ServerWorklog.COMMENT);
  public static final DBAttribute<Long> AUTHOR = ServerJira.toLinkAttribute(ServerWorklog.AUTHOR);
  public static final DBAttribute<Date> STARTED = ServerJira.toScalarAttribute(ServerWorklog.START_DATE);
  public static final DBAttribute<Date> CREATED = ServerJira.toScalarAttribute(ServerWorklog.CREATED);
  public static final DBAttribute<Long> EDITOR = ServerJira.toLinkAttribute(ServerWorklog.EDITOR);
  public static final DBAttribute<Date> UPDATED = ServerJira.toScalarAttribute(ServerWorklog.UPDATED);
  public static final DBAttribute<Long> SECURITY = ServerJira.toLinkAttribute(ServerWorklog.SECURITY);
  /**
   * Local only attribute (server value is always null. States that this worklog change affects time calculations
   */
  public static final DBAttribute<Boolean> AUTO_ADJUST = NS.bool("autoAdjustEstimate", "Auto Adjust Estimate", true);

  public static LongList collectWorklogs(DBReader reader, long issue) {
    return reader.query(DPEquals.create(ISSUE, issue)).copyItemsSorted();
  }

  /**
   * Return time spent change by this worklog. Returns 0 if local changes does not affect issue remaining estimate.
   * @param autoAdjustOnly if true do not count not auto-adjust changes
   * @return locally changed spent time. Positive if local spent is larger than server.<br>
   * For new worklog returns +TIME_SECONDS<br>
   * For removed worklog returns -TIME_SECONDS
   */
  public static int getLocalSpentDelta(DBReader reader, long worklog, boolean autoAdjustOnly) {
    if (autoAdjustOnly && !Boolean.TRUE.equals(reader.getValue(worklog, AUTO_ADJUST))) return 0;
    AttributeMap base = reader.getValue(worklog, SyncSchema.BASE);
    if (base == null) return 0;
    int localSeconds = ItemVersionCommonImpl.isInvisible(reader.getValue(worklog, SyncSchema.INVISIBLE)) ? 0 :
      Util.NN(reader.getValue(worklog, TIME_SECONDS), 0);
    int serverSeconds = ItemVersionCommonImpl.isInvisible(base.get(SyncSchema.INVISIBLE)) ? 0 :
      Util.NN(base.get(Worklog.TIME_SECONDS), 0);
    return localSeconds - serverSeconds;
  }

  public static boolean isMergeable(DBReader reader, long item) {
    ItemVersion last = SyncUtils.readLastNotBaseServer(reader, item);
    if (last == null) return true;
    ItemVersion base = SyncUtils.readBaseIfExists(reader, item);
    if (base == null) return true;
    ItemDiffImpl local = ItemDiffImpl.createToTrunk(base, 0);
    ItemDiff server = ItemDiffImpl.createServerDiff(base, last);
    if (local.getChanged().contains(TIME_SECONDS) || local.getChanged().contains(STARTED)) local.addChange(TIME_SECONDS, STARTED);
    MergeDataImpl merge = MergeDataImpl.create(local, server);
    return merge.isConflictResolved();
  }

  public static void setStarted(ItemVersionCreator worklog, long when) {
    when = (when / 1000) * 1000;
    worklog.setValue(STARTED, when > 0 ? new Date(when) : null);
  }
}
