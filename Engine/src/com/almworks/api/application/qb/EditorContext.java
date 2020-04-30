package com.almworks.api.application.qb;

import com.almworks.api.application.NameResolver;
import com.almworks.api.syncreg.ItemHypercube;
import com.almworks.util.Unsorted;
import com.almworks.util.advmodel.AListModel;
import com.almworks.util.properties.Role;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;

/**
 * @author : Dyoma
 */
@Deprecated
public interface EditorContext {
  Role<EditorContext> DATA_ROLE = Role.role("editorContext", EditorContext.class);

  @Unsorted
  AListModel<ConstraintDescriptor> getConditions();

  void dispose();

  @NotNull
  NameResolver getResolver();

  @NotNull
  ItemHypercube getParentHypercube();

  Lifespan getLifespan();
}
