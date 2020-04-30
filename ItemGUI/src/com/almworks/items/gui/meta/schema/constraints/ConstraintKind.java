package com.almworks.items.gui.meta.schema.constraints;

import com.almworks.api.application.qb.ConstraintDescriptor;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.cache.DataLoader;
import com.almworks.items.gui.meta.EnumTypesCollector;
import com.almworks.items.gui.meta.commons.SerializedObjectAttribute;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public interface ConstraintKind {
  DataLoader<ConstraintKind> LOADER = SerializedObjectAttribute.create(ConstraintKind.class,
    Descriptors.CONSTRAINT_KIND);

  ConstraintDescriptor createDescriptor(DBAttribute<?> attribute, String displayName, String id, EnumTypesCollector.Loaded type, @Nullable Icon icon);
}
