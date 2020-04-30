package com.almworks.jira.provider3.links.structure;

import com.almworks.api.application.LoadedItem;
import com.almworks.api.engine.DBCommons;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBReader;
import com.almworks.items.api.DP;
import com.almworks.items.dp.DPEqualsIdentified;
import com.almworks.items.gui.meta.LoadedModelKey;
import com.almworks.items.gui.meta.commons.FeatureRegistry;
import com.almworks.items.gui.meta.commons.LoadedItemUtils;
import com.almworks.items.gui.meta.schema.ItemsTreeLayouts;
import com.almworks.items.sync.DBDrain;
import com.almworks.items.sync.ItemVersion;
import com.almworks.items.sync.ItemVersionCreator;
import com.almworks.items.sync.util.identity.DBIdentity;
import com.almworks.items.sync.util.identity.DBStaticObject;
import com.almworks.items.sync.util.identity.ScalarSequence;
import com.almworks.items.util.DBNamespace;
import com.almworks.items.util.SyncAttributes;
import com.almworks.jira.provider3.gui.MetaSchema;
import com.almworks.jira.provider3.links.LoadedLink2;
import com.almworks.jira.provider3.schema.Jira;
import com.almworks.jira.provider3.schema.LinkType;
import com.almworks.util.LogHelper;
import com.almworks.util.bool.BoolExpr;
import com.almworks.util.collections.LongSet;
import org.almworks.util.Collections15;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class IssuesLinkTreeLayout {
  private static final String LAYOUT_ID_PREFIX = "JiraTreeLayout.";

  private static final DBNamespace NS_FEATURE = Jira.NS_FEATURE.subNs("linkTreeLayout");
  public static final DBIdentity FEATURE_ANISOTROPIC_LOADER = feature("anisotopicLoader");
  /**
   * Sequence: [linkType: item]
   */
  public static final DBIdentity FEATURE_ISOTROPIC_LOADER = feature("isotropicLoader");

  private static DBIdentity feature(String name) {
    return DBIdentity.fromDBObject(NS_FEATURE.object(name));
  }

  public static DBStaticObject createLayout(DBIdentity owner, String displayName, ScalarSequence structureData, Integer order) {
    return ItemsTreeLayouts.create(owner, LAYOUT_ID_PREFIX + displayName, displayName, structureData, order);
  }

  public static void registerFeatures(FeatureRegistry registry) {
    registry.register(FEATURE_ANISOTROPIC_LOADER, AnisotropicLinkStructureLoader.INSTANCE);
    registry.register(FEATURE_ISOTROPIC_LOADER, IsotropicLinkTreeLoader.INSTANCE);
  }

  public static void update(DBDrain drain, DBIdentity connection) {
    List<DBStaticObject> newLayouts = loadNewLayouts(drain, connection);
    BoolExpr<DP> connectionLayoutsExpr = DPEqualsIdentified.create(DBAttribute.TYPE, ItemsTreeLayouts.DB_TYPE).and(DBCommons.OWNER.queryEqual(connection));
    DBReader reader = drain.getReader();
    LongSet oldLayoutItems = LongSet.copy(reader.query(connectionLayoutsExpr).copyItemsSorted());
    for (DBStaticObject layout : newLayouts) {
      long item = layout.forceWrite(drain);
      oldLayoutItems.remove(item);
    }
    for (ItemVersionCreator old : drain.changeItems(oldLayoutItems)) old.delete();
  }

  private static List<DBStaticObject> loadNewLayouts(DBDrain drain, DBIdentity connection) {
    List<DBStaticObject> layouts = Collections15.arrayList();
    DBReader reader = drain.getReader();
    BoolExpr<DP> connectionLinkTypesExpr = LinkType.TYPES_EXPR.and(DPEqualsIdentified.create(SyncAttributes.CONNECTION, connection));
    for (ItemVersion linkType : drain.readItems(reader.query(connectionLinkTypesExpr).copyItemsSorted())) {
      String in = linkType.getValue(LinkType.INWARD_DESCRIPTION);
      String out = linkType.getValue(LinkType.OUTWARD_DESCRIPTION);
      if (in == null && out == null) {
        LogHelper.warning("Link type without directions", linkType, linkType.getAllValues());
        continue;
      }
      if (in == null || out == null) continue; // Not fully known, can not discriminate isotropic link
      boolean isotropic = Util.equals(in, out);
      Integer order = linkType.getValue(LinkType.ID);
      long linkTypeItem = linkType.getItem();
      if (isotropic) {
        layouts.add(createLayout(connection, in, IsotropicLinkTreeLoader.createSequence(linkTypeItem), order));
      } else {
        layouts.add(createLayout(connection, in, AnisotropicLinkStructureLoader.createSequence(linkTypeItem, false), order));
        layouts.add(createLayout(connection, out, AnisotropicLinkStructureLoader.createSequence(linkTypeItem, true), order));
      }
    }
    return layouts;
  }

  @NotNull
  static List<LoadedLink2> getLinks(LoadedItem element) {
    LoadedModelKey<?> key = LoadedItemUtils.getModelKey(element, MetaSchema.KEY_LINKS_LIST);
    LoadedModelKey<List<LoadedLink2>> linksKey = key != null ? key.castList(LoadedLink2.class) : null;
    return linksKey != null ? element.getModelKeyValue(linksKey) : Collections.<LoadedLink2>emptyList();
  }
}
