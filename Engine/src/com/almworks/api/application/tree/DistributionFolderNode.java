package com.almworks.api.application.tree;

import com.almworks.api.application.qb.ConstraintDescriptor;
import com.almworks.util.components.EditableText;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface DistributionFolderNode extends GenericNode {
  void setParameters(@Nullable ConstraintDescriptor descriptor, @NotNull DistributionParameters parameters);

  @Nullable
  ConstraintDescriptor getDescriptor();

  @NotNull
  DistributionParameters getParameters();

  EditableText getPresentation();

  void expandAfterNextUpdate();
}
