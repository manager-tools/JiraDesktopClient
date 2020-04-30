package com.almworks.jira.provider3.gui.viewer.links.subtasks;

import com.almworks.items.gui.meta.commons.FeatureRegistry;
import com.almworks.items.gui.meta.commons.ReferrerLoader;
import com.almworks.items.sync.ItemVersion;
import com.almworks.items.sync.util.identity.ScalarSequence;
import com.almworks.jira.provider3.schema.Issue;
import com.almworks.jira.provider3.schema.Jira;

public class SubtasksLoader extends ReferrerLoader<LoadedIssue> {
  private static final SubtasksLoader INSTANCE = new SubtasksLoader();
  private static final ReferrerLoader.Descriptor DESCRIPTOR = ReferrerLoader.Descriptor.create(Jira.NS_FEATURE,
    "subtasks.subtasks", INSTANCE);
  public static final ScalarSequence SERIALIZABLE = DESCRIPTOR.getSerializable();

  protected SubtasksLoader() {
    super(Issue.PARENT, LoadedIssue.class);
  }

  @Override
  public LoadedIssue extractValue(ItemVersion subtask, LoadContext context) {
    return LoadedIssue.load(subtask);
  }

  public static void registerFeature(FeatureRegistry featureRegistry) {
    DESCRIPTOR.registerFeature(featureRegistry);
  }
}
