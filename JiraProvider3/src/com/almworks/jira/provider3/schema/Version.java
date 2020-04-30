package com.almworks.jira.provider3.schema;

import com.almworks.api.application.ItemOrder;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBItemType;
import com.almworks.items.api.DBReader;
import com.almworks.items.gui.meta.LoadedItemKey;
import com.almworks.items.gui.meta.commons.FeatureRegistry;
import com.almworks.items.gui.meta.commons.SerializableFeature;
import com.almworks.items.gui.meta.schema.enums.IconLoader;
import com.almworks.items.gui.meta.schema.enums.OrderKind;
import com.almworks.items.gui.meta.util.EnumTypeBuilder;
import com.almworks.items.sync.util.identity.DBIdentity;
import com.almworks.items.sync.util.identity.DBStaticObject;
import com.almworks.items.sync.util.identity.ScalarSequence;
import com.almworks.jira.provider3.sync.schema.ServerJira;
import com.almworks.jira.provider3.sync.schema.ServerVersion;
import com.almworks.util.images.Icons;
import org.almworks.util.Collections15;
import org.almworks.util.Util;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Collection;
import java.util.Date;
import java.util.List;

public class Version {
  public static final String NULL_CONSTRAINT_ID = "_no_version_";
  public static final String NULL_CONSTRAINT_NAME = "No Version";

  public static final DBItemType DB_TYPE = ServerJira.toItemType(ServerVersion.TYPE);
  public static final DBAttribute<Integer> ID = ServerJira.toScalarAttribute(ServerVersion.ID);
  public static final DBAttribute<Long> PROJECT = ServerJira.toLinkAttribute(ServerVersion.PROJECT);
  public static final DBAttribute<String> NAME = ServerJira.toScalarAttribute(ServerVersion.NAME);
  public static final DBAttribute<Integer> SEQUENCE = ServerJira.toScalarAttribute(ServerVersion.SEQUENCE);
  public static final DBAttribute<Boolean> ARCHIVED = ServerJira.toScalarAttribute(ServerVersion.ARCHIVED);
  public static final DBAttribute<Boolean> RELEASED = ServerJira.toScalarAttribute(ServerVersion.RELEASED);
  public static final DBAttribute<Date> RELEASE_DATE = ServerJira.toScalarAttribute(ServerVersion.RELEASE_DATE);


  private static final DBIdentity FEATURE_ICON = Jira.feature("version.icon");
  private static final DBIdentity FEATURE_ORDER = Jira.feature("version.order");
  public static final DBStaticObject ENUM_TYPE = new EnumTypeBuilder()
    .setType(DB_TYPE)
    .setUniqueKey(NAME)
    .narrowByAttribute(Issue.PROJECT, Version.PROJECT)
    .setSearchSubstring(true)
    .renderFirstNotNull(Version.NAME, Version.ID)
    .setOrder(new ScalarSequence.Builder().append(FEATURE_ORDER).create())
    .addAttributeSubloaders(ARCHIVED, RELEASED, RELEASE_DATE)
    .setIconLoader(new ScalarSequence.Builder().append(FEATURE_ICON).create())
    .create();

  public static boolean isArchived(LoadedItemKey version) {
    return Util.NN(version.getValue(ARCHIVED), false);
  }

  public static boolean isArchived(DBReader reader, long item) {
    Boolean archived = ARCHIVED.getValue(item, reader);
    return archived != null && archived;
  }

  public static boolean isReleased(DBReader reader, long item) {
    Boolean released = RELEASED.getValue(item, reader);
    return Boolean.TRUE.equals(released);
  }

  public static boolean isReleasedOrArchived(LoadedItemKey version) {
    Boolean released = version.getValue(RELEASED);
    if (released != null) return released;
    Date releasedDate = version.getValue(RELEASE_DATE);
    return releasedDate != null || isArchived(version);
  }

  public static void registerFeature(FeatureRegistry registry) {
    registry.register(FEATURE_ICON, SerializableFeature.NoParameters.create(VersionIconLoader.INSTANCE, IconLoader.class));
    registry.register(FEATURE_ORDER, SerializableFeature.NoParameters.create(VersionOrder.INSTANCE, OrderKind.class));
  }

  private static class VersionIconLoader implements IconLoader {
    private static final VersionIconLoader INSTANCE = new VersionIconLoader();
    private static final Icon[][] ICONS = new Icon[][]{{Icons.VERSION_UNRELEASED, Icons.VERSION_RELEASED},
            {Icons.VERSION_UNRELEASED_ARCHIVED, Icons.VERSION_RELEASED_ARCHIVED}};

    @Override
    public Icon loadIcon(DBReader reader, long item) {
      boolean archived = isArchived(reader, item);
      boolean released = isReleased(reader, item);
      return ICONS[archived ? 1 : 0][released ? 1 : 0];
    }
  }

  private static class VersionOrder extends OrderKind {
    private static final List<DBAttribute<?>> VERSION_ATTRIBUTES = Collections15.<DBAttribute<?>>unmodifiableListCopy(RELEASED, RELEASE_DATE, ARCHIVED);
    public static final VersionOrder INSTANCE = new VersionOrder();

    @Override
    public ItemOrder create(DBReader reader, long item, String displayName) {
      Number order = SEQUENCE.getValue(item, reader);
      long longOrder = order != null ? -order.longValue() : 0;
      if (isArchived(reader, item)) return ItemOrder.byOrderAndString(longOrder, displayName);
      return ItemOrder.byGroup(displayName, new long[]{0, longOrder});
    }

    @Nullable
    @Override
    public Collection<? extends DBAttribute<?>> getAttributes() {
      return VERSION_ATTRIBUTES;
    }
  }
}
