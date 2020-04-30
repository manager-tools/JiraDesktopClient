package com.almworks.jira.provider3.app.sync;

import com.almworks.spi.provider.util.ServerSyncPoint;
import com.almworks.util.Env;
import org.almworks.util.Const;
import org.jetbrains.annotations.Nullable;

import java.util.Date;
import java.util.Set;

class ConnectionSyncInfo {
  private static final long RELOAD_PERIOD_FULL = Env.getInteger("jira.reload.stepback", (int) Const.HOUR * 24);
  private static final long RELOAD_PERIOD_QUICK = Env.getInteger("jira.reload.stepback.quick", (int) Const.MINUTE * 3);

  private final ServerSyncPoint mySyncPoint;
  @Nullable
  private final Set<Integer> myProjectFilter;
  private final boolean myUseQuickCheck;

  public ConnectionSyncInfo(ServerSyncPoint syncPoint, Set<Integer> projectFilter, boolean useQuickCheck) {
    mySyncPoint = syncPoint;
    myProjectFilter = projectFilter;
    myUseQuickCheck = useQuickCheck;
  }

  @Nullable
  public Set<Integer> getProjectFilterIds() {
    return myProjectFilter;
  }

  public ServerSyncPoint getSyncPoint() {
    return mySyncPoint;
  }

  @Override
  public String toString() {
    return "SyncPoint:" + mySyncPoint + " Projects:" + myProjectFilter + (myUseQuickCheck ? "Quick allowed" : "");
  }

  @Nullable
  public Date getSyncDate() {
    if (mySyncPoint.isUnsynchronized()) return null;
    long stepback = myUseQuickCheck ? RELOAD_PERIOD_QUICK : RELOAD_PERIOD_FULL;
    return new Date(mySyncPoint.getSyncTime() - stepback);
  }
}
