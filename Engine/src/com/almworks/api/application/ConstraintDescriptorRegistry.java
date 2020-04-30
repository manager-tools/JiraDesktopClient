package com.almworks.api.application;

import com.almworks.api.application.qb.ConstraintDescriptor;
import com.almworks.api.engine.ItemProvider;

/**
 * @author dyoma
 */
public interface ConstraintDescriptorRegistry {
  void register(ConstraintDescriptor descriptor, ItemProvider provider);

  void deregister(ConstraintDescriptor descriptor, ItemProvider provider);
}
