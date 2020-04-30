package com.almworks.jira.provider3.gui.timetrack;

import com.almworks.items.gui.meta.GuiFeaturesManager;
import com.almworks.items.gui.meta.commons.FeatureRegistry;
import com.almworks.items.gui.meta.commons.ReferrerLoader;
import com.almworks.items.sync.ItemVersion;
import com.almworks.items.sync.util.identity.ScalarSequence;
import com.almworks.jira.provider3.schema.Jira;
import com.almworks.jira.provider3.schema.Worklog;

import java.util.Date;

public class LoadedWorklogLoader extends ReferrerLoader<LoadedWorklog> {
  private static final LoadedWorklogLoader INSTANCE = new LoadedWorklogLoader();
  private static final ReferrerLoader.Descriptor DESCRIPTOR = ReferrerLoader.Descriptor.create(Jira.NS_FEATURE,
    "worklog.list", INSTANCE);
  public static final ScalarSequence SERIALIZABLE = DESCRIPTOR.getSerializable();

  private LoadedWorklogLoader() {
    super(Worklog.ISSUE, LoadedWorklog.class);
  }

  @Override
  public LoadedWorklog extractValue(ItemVersion slaveVersion, LoadContext context) {
    if (slaveVersion.isInvisible()) return null;
    long author = slaveVersion.getNNValue(Worklog.AUTHOR, 0l);
    int seconds = slaveVersion.getNNValue(Worklog.TIME_SECONDS, 0);
    Date started = slaveVersion.getValue(Worklog.STARTED);
    long security = slaveVersion.getNNValue(Worklog.SECURITY, 0l);
    String comment = slaveVersion.getValue(Worklog.COMMENT);
    return new LoadedWorklog(context.getActor(GuiFeaturesManager.ROLE), slaveVersion.getItem(), author, seconds, started, slaveVersion.getSyncState(), security, comment);
  }

  public static void registerFeature(FeatureRegistry featureRegistry) {
    DESCRIPTOR.registerFeature(featureRegistry);
  }
}
