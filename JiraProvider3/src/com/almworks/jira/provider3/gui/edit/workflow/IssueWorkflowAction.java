package com.almworks.jira.provider3.gui.edit.workflow;

import com.almworks.api.application.ItemDownloadStage;
import com.almworks.api.application.LoadedItemServices;
import com.almworks.api.application.ModelKey;
import com.almworks.api.application.util.DataAccessor;
import com.almworks.api.application.util.DataIO;
import com.almworks.integers.LongArray;
import com.almworks.integers.LongList;
import com.almworks.items.gui.meta.GuiFeaturesManager;
import com.almworks.items.gui.meta.LoadedModelKey;
import com.almworks.items.gui.meta.commons.FeatureRegistry;
import com.almworks.items.gui.meta.commons.SerializableFeature;
import com.almworks.items.gui.meta.schema.DataHolder;
import com.almworks.items.gui.meta.schema.modelkeys.ModelKeyLoader;
import com.almworks.items.gui.meta.schema.modelkeys.ModelKeys;
import com.almworks.items.sync.HistoryRecord;
import com.almworks.items.sync.ItemVersion;
import com.almworks.items.sync.util.identity.DBIdentity;
import com.almworks.items.sync.util.identity.DBStaticObject;
import com.almworks.items.sync.util.identity.ScalarSequence;
import com.almworks.jira.provider3.schema.Issue;
import com.almworks.jira.provider3.schema.Jira;
import com.almworks.util.properties.PropertyMap;

import java.util.Set;

public class IssueWorkflowAction extends ModelKeyLoader implements DataIO<LongList> {
  private static final DBIdentity FEATURE = Jira.feature("modelKey.issueActions");

  public static DBStaticObject createModelKey() {
    return ModelKeys.create(Jira.JIRA_PROVIDER_ID, "IssueWorkflowActions", ScalarSequence.create(FEATURE), ModelKeys.SEQUENCE_PROMOTION_ALWAYS);
  }

  public static void registerFeatures(FeatureRegistry features) {
    features.register(FEATURE, SerializableFeature.NoParameters.create(new IssueWorkflowAction(), ModelKeyLoader.class));
  }

  private IssueWorkflowAction() {
    super(DataHolder.EMPTY);
  }

  @Override
  public boolean loadKey(LoadedModelKey.Builder<?> b, GuiFeaturesManager guiFeatures) {
    LoadedModelKey.Builder<LongList> builder = b.setDataClass(LongList.class);
    builder.setAccessor(new DataAccessor.SimpleDataAccessor<LongList>(builder.getName()));
    builder.setIO(this);
    return true;
  }

  @Override
  public void extractValue(ItemVersion issue, LoadedItemServices itemServices, PropertyMap values, ModelKey<LongList> modelKey) {
    if (!ItemDownloadStage.getValue(issue).wasFull()) return;
    Set<Long> actions = issue.getValue(Issue.APPLICABLE_WORKFLOW_ACTIONS);
    if (actions == null || actions.isEmpty()) return;
    HistoryRecord[] history = issue.getHistory();
    for (HistoryRecord step : history) if (step.isOfKind(issue.getReader(), WorkflowStep.STEP_KIND)) return;
    LongArray actionItems = LongArray.create(actions);
    actionItems.sortUnique();
    modelKey.setValue(values, actionItems);
  }

  @Override
  public String toString() {
    return "IssueWorkflowAction";
  }
}
