package com.almworks.spi.provider.util;

import com.almworks.api.engine.Connection;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.sync.ItemVersion;
import com.almworks.items.sync.ItemVersionCreator;
import com.almworks.util.collections.ByteArray;
import org.almworks.util.Const;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class ServerSyncPoint {
  private static final DBAttribute<byte[]> ATTRIBUTE = Connection.NS.bytes("syncPoint");
  private final long mySyncTime;
  private final int myLatestIssueId;
  private final String myLatestIssueMTime;
  private final long myLastCreatedTime;

  private static final DateFormat FORMAT;

  static {
    FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    FORMAT.setTimeZone(TimeZone.getTimeZone("GMT"));
  }

  public ServerSyncPoint(long syncTime, int latestIssueId, String latestIssueMTime, long lastCreatedTime) {
    mySyncTime = syncTime;
    myLatestIssueId = latestIssueId;
    myLatestIssueMTime = latestIssueMTime;
    myLastCreatedTime = lastCreatedTime;
  }

  @NotNull
  public static ServerSyncPoint loadSyncPoint(ItemVersion connection) {
    if (connection == null) return unsynchronized();
    byte[] syncRaw = connection.getValue(ATTRIBUTE);
    if (syncRaw == null) return unsynchronized();
    ServerSyncPoint point = restore(syncRaw);
    return point != null ? point : unsynchronized();
  }

  public long getSyncTime() {
    return mySyncTime;
  }

  public int getLatestIssueId() {
    return myLatestIssueId;
  }

  public String getLatestIssueMTime() {
    return myLatestIssueMTime;
  }

  public long getLastCreatedTime() {
    return myLastCreatedTime;
  }

  public boolean isValidSuccessorState(ServerSyncPoint successorPoint) {
    if (successorPoint.isUnsynchronized() || isUnsynchronized())
      return true;
    if ((getLatestIssueId() <= 0 || getLatestIssueMTime() == null) && (successorPoint.getLatestIssueId() > 0 && successorPoint.getLatestIssueMTime() != null))
      return true;
    return successorPoint.getSyncTime() >= getSyncTime();
  }

  public boolean isUnsynchronized() {
    return mySyncTime <= Const.DAY;
  }

  public static ServerSyncPoint unsynchronized() {
    return new ServerSyncPoint(0, 0, null, 0);
  }

  public String toString() {
    StringBuilder b = new StringBuilder();
    b.append(mySyncTime);
    if (mySyncTime >= Const.DAY) {
      b.append('[').append(FORMAT.format(new Date(mySyncTime))).append(" GMT]");
    }
    b.append(':').append(myLatestIssueId).append(':');
    if (myLatestIssueMTime != null)
      b.append(myLatestIssueMTime);
    b.append(":[").append(FORMAT.format(new Date(myLastCreatedTime))).append(" GMT]");
    return b.toString();
  }

  public byte[] serialize() {
    ByteArray buffer = new ByteArray();
    buffer.addLong(mySyncTime);
    buffer.addInt(myLatestIssueId);
    buffer.addUTF8(myLatestIssueMTime);
    buffer.addLong(myLastCreatedTime);
    return buffer.toNativeArray();
  }

  @Nullable
  public static ServerSyncPoint restore(byte[] bytes) {
    ByteArray.Stream stream = ByteArray.createStream(bytes);
    long time = stream.nextLong();
    int issueId = stream.nextInt();
    String mTime = stream.nextUTF8();
    long lastCreated = stream.nextLong();
    return stream.isSuccessfullyAtEnd() ? new ServerSyncPoint(time, issueId, mTime, lastCreated) : null;
  }

  public ServerSyncPoint updateLastCreated(Date createDate) {
    if (createDate == null || createDate.getTime() <= myLastCreatedTime) return this;
    return new ServerSyncPoint(mySyncTime, myLatestIssueId, myLatestIssueMTime, createDate.getTime());
  }

  public void setValue(ItemVersionCreator connection) {
    if (connection == null) return;
    connection.setValue(ATTRIBUTE, serialize());
  }
}
