package com.almworks.jira.provider3.gui.viewer.links.subtasks;

import com.almworks.api.application.LoadedItemServices;
import com.almworks.api.application.ModelKey;
import com.almworks.api.application.util.DataAccessor;
import com.almworks.api.application.util.DataIO;
import com.almworks.items.gui.meta.GuiFeaturesManager;
import com.almworks.items.gui.meta.LoadedModelKey;
import com.almworks.items.gui.meta.commons.FeatureRegistry;
import com.almworks.items.gui.meta.commons.SerializableFeature;
import com.almworks.items.gui.meta.schema.DataHolder;
import com.almworks.items.gui.meta.schema.modelkeys.ModelKeyLoader;
import com.almworks.items.sync.ItemVersion;
import com.almworks.items.sync.util.identity.DBIdentity;
import com.almworks.items.sync.util.identity.ScalarSequence;
import com.almworks.jira.provider3.schema.Issue;
import com.almworks.jira.provider3.schema.Jira;
import com.almworks.util.properties.PropertyMap;

public class ParentLoader extends ModelKeyLoader implements DataIO<LoadedIssue> {
  private static final ModelKeyLoader INSTANCE = new ParentLoader();
  private static final DBIdentity FEATURE_ID = DBIdentity.fromDBObject(Jira.NS_FEATURE.object("subtasks.parent"));
  private static final SerializableFeature<ModelKeyLoader> FEATURE_IMPL = new SerializableFeature.NoParameters<ModelKeyLoader>(INSTANCE, ModelKeyLoader.class);
  public static final ScalarSequence SERIALIZABLE = new ScalarSequence.Builder().append(FEATURE_ID).create();

  protected ParentLoader() {
    super(DataHolder.EMPTY);
  }

  @Override
  public boolean loadKey(LoadedModelKey.Builder<?> b, GuiFeaturesManager guiFeatures) {
    LoadedModelKey.Builder<LoadedIssue> builder = b.setDataClass(LoadedIssue.class);
    builder.setIO(this);
    builder.setAccessor(new DataAccessor.SimpleDataAccessor<LoadedIssue>(builder.getName()));
    return true;
  }

  public static void registerFeature(FeatureRegistry registry) {
    registry.register(FEATURE_ID, FEATURE_IMPL);
  }

  @Override
  public void extractValue(ItemVersion issue, LoadedItemServices itemServices, PropertyMap values, ModelKey<LoadedIssue> key)
  {
    Long parent = issue.getValue(Issue.PARENT);
    if (parent == null || parent <= 0) return;
    LoadedIssue loadedParent = LoadedIssue.load(issue.forItem(parent));
    key.setValue(values, loadedParent);
  }

  @Override
  public String toString() {
    return "ParentLoader";
  }
}
