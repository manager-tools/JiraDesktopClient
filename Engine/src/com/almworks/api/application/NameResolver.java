package com.almworks.api.application;

import com.almworks.api.application.qb.ConstraintDescriptor;
import com.almworks.api.syncreg.ItemHypercube;
import com.almworks.util.advmodel.AListModel;
import com.almworks.util.collections.Modifiable;
import com.almworks.util.properties.Role;
import com.almworks.util.threads.ThreadAWT;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author dyoma
 */
public interface NameResolver {
  Role<NameResolver> ROLE = Role.role(NameResolver.class);

  @ThreadAWT
  @NotNull
  AListModel<ConstraintDescriptor> getConstraintDescriptorModel(Lifespan life, ItemHypercube cube);

  @NotNull
  AListModel<ConstraintDescriptor> getAllDescriptorsModel();

  @Nullable
  ConstraintDescriptor getConditionDescriptor(String id, @NotNull ItemHypercube cube);

  @ThreadAWT
  @NotNull
  Modifiable getModifiable();

  ItemKeyCache getCache();

//  ScalarModel<Boolean> getEnumDescriptorsReady();
}
