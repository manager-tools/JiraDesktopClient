package com.almworks.api.application.qb;

import com.almworks.api.container.MutableComponentContainer;
import com.almworks.api.syncreg.ItemHypercube;
import com.almworks.util.properties.Role;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface FilterEditorProvider {
  Role<FilterEditorProvider> ROLE = Role.role("filterEditorProvider");

  FilterEditor createFilterEditor(@NotNull FilterNode filter, @NotNull ItemHypercube hypercube,
    @Nullable MutableComponentContainer registerContextTo);
}
