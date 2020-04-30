package com.almworks.jira.provider3.links;

import com.almworks.api.application.ItemKey;
import com.almworks.items.gui.meta.GuiFeaturesManager;
import com.almworks.util.ui.actions.DataRole;
import org.almworks.util.TypedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface LoadedLink {
  DataRole<LoadedLink> LINK = DataRole.createRole(LoadedLink.class);

  TypedKey<String> KEY = TypedKey.create("issueKey");
  TypedKey<String> SUMMARY = TypedKey.create("summary");
  TypedKey<ItemKey> STATUS = TypedKey.create("status");
  TypedKey<ItemKey> ISSUE_TYPE = TypedKey.create("issueType");
  TypedKey<ItemKey> PRIORITY = TypedKey.create("priority");

  boolean getOutward();
  
  long getType();
  
  String getDescription(GuiFeaturesManager manager);

  @Nullable
  String getOppositeString(@NotNull TypedKey<String> key);

  @Nullable
  ItemKey getOppositeEnum(GuiFeaturesManager manager, @NotNull TypedKey<ItemKey> key);
}
