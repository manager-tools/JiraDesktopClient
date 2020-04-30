package com.almworks.api.application.qb;

import com.almworks.api.application.ItemKeyGroup;
import com.almworks.api.application.ResolvedItem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;

public interface EnumGrouping<G extends ItemKeyGroup> {
  @Nullable
  G getGroup(@Nullable ResolvedItem item);

  G getNullGroup();

  @NotNull
  Comparator<G> getComparator();

  @NotNull
  String getDisplayableName();
}
