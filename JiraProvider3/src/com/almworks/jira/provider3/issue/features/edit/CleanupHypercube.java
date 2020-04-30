package com.almworks.jira.provider3.issue.features.edit;

import com.almworks.api.application.ResolvedItem;
import com.almworks.api.application.qb.ConstraintDescriptor;
import com.almworks.api.syncreg.ItemHypercube;
import com.almworks.api.syncreg.ItemHypercubeImpl;
import com.almworks.explorer.qbuilder.filter.BaseEnumConstraintDescriptor;
import com.almworks.integers.LongArray;
import com.almworks.integers.LongList;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.gui.meta.GuiFeaturesManager;
import org.almworks.util.Collections15;
import org.almworks.util.Util;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

class CleanupHypercube {
  private final List<BaseEnumConstraintDescriptor> myAllDescriptors;
  private final Map<DBAttribute<?>, BaseEnumConstraintDescriptor> myDescriptors = Collections15.hashMap();

  public CleanupHypercube(List<BaseEnumConstraintDescriptor> descriptors) {
    myAllDescriptors = descriptors;
  }

  public static CleanupHypercube create(GuiFeaturesManager manager) {
    ArrayList<BaseEnumConstraintDescriptor> descriptors = Collections15.arrayList();
    for (ConstraintDescriptor descriptor : manager.getDescriptors().copyCurrent()) {
      BaseEnumConstraintDescriptor enumDescriptor = Util.castNullable(BaseEnumConstraintDescriptor.class, descriptor);
      if (enumDescriptor != null) descriptors.add(enumDescriptor);
    }
    return new CleanupHypercube(descriptors);
  }

  public ItemHypercube cleanup(ItemHypercube cube) {
    while (true) {
      ItemHypercubeImpl target = new ItemHypercubeImpl();
      boolean changed = false;
      for (DBAttribute<?> attribute : cube.getAxes()) {
        boolean incChanged = filterCube(cube, attribute, target, true);
        boolean excChanged = filterCube(cube, attribute, target, false);
        if (incChanged || excChanged) changed = true;
      }
      if (!changed) return cube;
      cube = target;
    }
  }

  private boolean filterCube(ItemHypercube source, DBAttribute<?> attribute, ItemHypercubeImpl target, boolean sign) {
    BaseEnumConstraintDescriptor descriptor = getDescriptor(attribute);
    if (descriptor == null) return false;
    SortedSet<Long> prev = sign ? source.getIncludedValues(attribute) : source.getExcludedValues(attribute);
    if (prev == null) return false;
    LongList filtered = filter(source, descriptor, prev);
    if (filtered == null) {
      target.addValues(attribute, prev, sign);
      return false;
    }
    target.addValues(attribute, filtered, sign);
    return true;
  }

  @Nullable
  private LongList filter(ItemHypercube cube, BaseEnumConstraintDescriptor descriptor, SortedSet<Long> values) {
    if (values == null) return null;
    LongArray result = new LongArray();
    boolean changed = false;
    for (Long item : values) {
      if (isNotApplicable(cube, descriptor, item)) changed = true;
      else result.add(item);
    }
    if (!changed) return null;
    result.sortUnique();
    return result;
  }
  
  private boolean isNotApplicable(ItemHypercube cube, BaseEnumConstraintDescriptor descriptor, long item) {
    ResolvedItem key = descriptor.findForItem(item);
    if (key == null) return false;
    List<ResolvedItem> resolutions = descriptor.resolveKey(key.getId(), cube);
    for (ResolvedItem resolution : resolutions) if (resolution.getItem() == item) return false;
    return true;
  }

  @Nullable
  private BaseEnumConstraintDescriptor getDescriptor(DBAttribute<?> attribute) {
    if (attribute == null) return null;
    if (myDescriptors.containsKey(attribute)) return myDescriptors.get(attribute);
    List<BaseEnumConstraintDescriptor> descriptors = myAllDescriptors;
    BaseEnumConstraintDescriptor found = null;
    for (BaseEnumConstraintDescriptor descriptor : descriptors) {
      if (attribute.equals(descriptor.getAttribute())) {
        found = descriptor;
        break;
      }
    }
    myDescriptors.put(attribute, found);
    return found;
  }
}
