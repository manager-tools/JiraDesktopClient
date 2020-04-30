package com.almworks.api.application.util;

import com.almworks.api.application.qb.ConstraintDescriptor;
import org.almworks.util.Collections15;

import java.util.List;

/**
 * @author dyoma
 */
public class ConditionDescriptorsCollector {
  private final List<ConstraintDescriptor> myDescriptors = Collections15.arrayList();

  public List<ConstraintDescriptor> getDescriptors() {
    return myDescriptors;
  }
}
