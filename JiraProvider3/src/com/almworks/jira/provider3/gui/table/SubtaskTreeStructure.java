package com.almworks.jira.provider3.gui.table;

import com.almworks.api.application.LoadedItem;
import com.almworks.explorer.TableTreeStructure;
import com.almworks.items.gui.meta.commons.FeatureRegistry;
import com.almworks.items.gui.meta.commons.LoadedItemUtils;
import com.almworks.items.gui.meta.commons.SerializableFeature;
import com.almworks.items.gui.meta.schema.ItemsTreeLayouts;
import com.almworks.items.sync.DBDrain;
import com.almworks.items.sync.util.identity.DBIdentity;
import com.almworks.items.sync.util.identity.DBStaticObject;
import com.almworks.items.sync.util.identity.ScalarSequence;
import com.almworks.items.util.DBNamespace;
import com.almworks.jira.provider3.gui.MetaSchema;
import com.almworks.jira.provider3.gui.viewer.links.subtasks.LoadedIssue;
import com.almworks.jira.provider3.schema.Jira;

import java.util.Collections;
import java.util.Set;

public class SubtaskTreeStructure extends TableTreeStructure {
  private static final DBNamespace NS_FEATURE = Jira.NS_FEATURE.subNs("subtasks");


  private static final int SUBTASKS_TREE_LAYOUT_ORDER = -100;
  private static final DBIdentity FEATURE_SUBTASKS_STRUCTURE = DBIdentity.fromDBObject(NS_FEATURE.object("structure"));
  private static final DBStaticObject SUBTASKS_TREE_LAYOUT =
    ItemsTreeLayouts.create(Jira.JIRA_PROVIDER_ID, "JiraTreeLayout.Sub-Tasks", "Sub-Tasks", ScalarSequence.create(FEATURE_SUBTASKS_STRUCTURE), SUBTASKS_TREE_LAYOUT_ORDER);

  public static final SubtaskTreeStructure INSTANCE = new SubtaskTreeStructure();

  @Override
  public Set<Long> getNodeParentKeys(LoadedItem element) {
    LoadedIssue parent = LoadedItemUtils.getModelKeyValue(element, MetaSchema.KEY_PARENT, LoadedIssue.class);
    return parent == null ? Collections.<Long>emptySet() : Collections.singleton(parent.getItem());
  }

  public static void registerFeatures(FeatureRegistry registry) {
    registry.register(FEATURE_SUBTASKS_STRUCTURE, SerializableFeature.NoParameters.create(INSTANCE, SubtaskTreeStructure.class));
  }

  public static void materializeObjects(DBDrain drain) {
    drain.materialize(SUBTASKS_TREE_LAYOUT);
  }
}
