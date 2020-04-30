package com.almworks.jira.provider3.schema;

import com.almworks.api.application.ItemKey;
import com.almworks.api.application.ItemOrder;
import com.almworks.integers.LongList;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBItemType;
import com.almworks.items.api.DBReader;
import com.almworks.items.gui.meta.LoadedItemKey;
import com.almworks.items.gui.meta.commons.FeatureRegistry;
import com.almworks.items.gui.meta.commons.SerializableFeature;
import com.almworks.items.gui.meta.schema.enums.OrderKind;
import com.almworks.items.gui.meta.util.EnumTypeBuilder;
import com.almworks.items.sync.ItemVersion;
import com.almworks.items.sync.util.identity.DBIdentity;
import com.almworks.items.sync.util.identity.DBStaticObject;
import com.almworks.items.sync.util.identity.ScalarSequence;
import com.almworks.jira.provider3.sync.schema.ServerIssueType;
import com.almworks.jira.provider3.sync.schema.ServerJira;
import com.almworks.util.LogHelper;
import com.almworks.util.collections.ByteArray;
import com.almworks.util.commons.Procedure;
import org.almworks.util.Util;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

public class IssueType {
  public static final DBItemType DB_TYPE = ServerJira.toItemType(ServerIssueType.TYPE);
  public static final DBAttribute<Integer> ID = ServerJira.toScalarAttribute(ServerIssueType.ID);
  public static final DBAttribute<String> NAME = ServerJira.toScalarAttribute(ServerIssueType.NAME);
  public static final DBAttribute<Set<Long>> ONLY_IN_PROJECTS = ServerJira.toLinkSetAttribute(ServerIssueType.ONLY_IN_PROJECTS);
  public static final DBAttribute<String> DESCRIPTION = ServerJira.toScalarAttribute(ServerIssueType.DESCRIPTION);
  public static final DBAttribute<Boolean> SUBTASK = ServerJira.toScalarAttribute(ServerIssueType.SUBTASK);

  private static final DBIdentity FEATURE_ORDER = Jira.feature("issueType.orderKind");


  public static final DBStaticObject ENUM_TYPE = new EnumTypeBuilder()
    .setType(DB_TYPE)
    .setUniqueKey(ID)
    .renderFirstNotNull(NAME, ID)
    .setOrder(new ScalarSequence.Builder().append(FEATURE_ORDER).create())
    .narrowByAttribute(Issue.PROJECT, ONLY_IN_PROJECTS)
    .addAttributeSubloaders(SUBTASK)
    .create();

  public static boolean isSubtask(ItemVersion type, boolean subtask) {
    if (type == null) return false;
    Boolean value = type.getValue(IssueType.SUBTASK);
    LogHelper.assertWarning(value != null, "Missing sub-task", type); // May happen if issue types meta is not loaded yet for the type
    return value != null && value == subtask;
  }

  public static boolean isSubtask(ItemKey t, boolean subtask) {
    LoadedItemKey type = Util.castNullable(LoadedItemKey.class, t);
    if (type == null) return false;
    Boolean value = type.getValue(SUBTASK);
    LogHelper.assertWarning(value != null, "Missing sub-task", type); // May happen if issue types meta is not loaded yet for the type
    return value != null && value == subtask;
  }

  @Nullable
  public static Boolean getSubtask(DBReader reader, Long type) {
    if (type == null || type <= 0) return null;
    return reader.getValue(type, SUBTASK);
  }

  public static void registerFeature(FeatureRegistry registry) {
    final OrderKind orderKind = new OrderKind() {
      @Override
      public ItemOrder create(DBReader reader, long item, String displayName) {
        String name = reader.getValue(item, NAME);
        if (name != null) {
          Boolean subtask = reader.getValue(item, SUBTASK);
          if (Boolean.TRUE.equals(subtask)) name = (char)(0xFFFF) + name;
        } else name = displayName;
        return ItemOrder.byStringNoAdjust(name);
      }

      @Override
      public Collection<? extends DBAttribute<?>> getAttributes() {
        return Collections.singleton(SUBTASK);
      }
    };
    SerializableFeature<OrderKind> feature = new SerializableFeature<OrderKind>() {
      @Override
      public OrderKind restore(DBReader reader, ByteArray.Stream stream, @Nullable Procedure<LongList> invalidate) {
        return orderKind;
      }

      @Override
      public Class<OrderKind> getValueClass() {
        return OrderKind.class;
      }
    };
    registry.register(FEATURE_ORDER, feature);
  }

  @Nullable
  public static Long resolveById(ItemVersion connection, int typeId) {
    return Jira.resolveEnum(connection, DB_TYPE, ID, typeId);
  }
}
