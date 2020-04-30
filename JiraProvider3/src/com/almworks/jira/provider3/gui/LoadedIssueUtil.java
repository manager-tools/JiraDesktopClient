package com.almworks.jira.provider3.gui;

import com.almworks.api.application.ItemWrapper;
import com.almworks.items.gui.meta.EnumTypesCollector;
import com.almworks.items.gui.meta.GuiFeaturesManager;
import com.almworks.items.gui.meta.LoadedItemKey;
import com.almworks.items.gui.meta.commons.LoadedItemUtils;
import com.almworks.items.sync.util.identity.DBStaticObject;
import com.almworks.jira.provider3.schema.Group;
import com.almworks.jira.provider3.schema.ProjectRole;
import com.almworks.jira.provider3.schema.User;
import com.almworks.util.LogHelper;
import org.jetbrains.annotations.Nullable;

public class LoadedIssueUtil {
  public static String getIssueKey(ItemWrapper issue) {
    return LoadedItemUtils.getModelKeyValue(issue, MetaSchema.KEY_KEY, String.class);
  }

  public static String getIssueSummary(ItemWrapper issue) {
    return LoadedItemUtils.getModelKeyValue(issue, MetaSchema.KEY_SUMMARY, String.class);
  }

  public static LoadedItemKey getItemKey(GuiFeaturesManager manager, DBStaticObject enumType, long item) {
    EnumTypesCollector.Loaded type = manager.getEnumTypes().getType(enumType);
    if (type == null) {
      LogHelper.error("Missing enum type", type, item);
      return null;
    }
    return type.getResolvedItem(item);
  }

  public static LoadedItemKey getVisibilityItem(GuiFeaturesManager manager, long visibility) {
    LoadedItemKey loaded = getItemKey(manager, Group.ENUM_TYPE, visibility);
    if (loaded == null) loaded = getItemKey(manager, ProjectRole.ENUM_TYPE, visibility);
    return loaded;
  }

  public static String getVisibilityText(GuiFeaturesManager manager, long visibility) {
    if (visibility <= 0) return null;
    LoadedItemKey group = getItemKey(manager, Group.ENUM_TYPE, visibility);
    if (group != null) return group.getDisplayName();
    LoadedItemKey role = getItemKey(manager, ProjectRole.ENUM_TYPE, visibility);
    return role != null ? role.getDisplayName() + " (role)" : null;
  }

  @Nullable
  public static String getUserDisplayName(GuiFeaturesManager manager, long user) {
    if (user <= 0) return null;
    LoadedItemKey loaded = getItemKey(manager, User.ENUM_TYPE, user);
    return loaded != null ? loaded.getDisplayName() : null;
  }
}
