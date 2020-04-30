package com.almworks.jira.provider3.gui;

import com.almworks.api.application.*;
import com.almworks.api.application.qb.ConstraintDescriptor;
import com.almworks.api.application.qb.EnumGrouping;
import com.almworks.explorer.qbuilder.filter.BaseEnumConstraintDescriptor;
import com.almworks.explorer.qbuilder.filter.EnumConstraintKind;
import com.almworks.explorer.qbuilder.filter.EnumSubtreeKind;
import com.almworks.integers.LongList;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBReader;
import com.almworks.items.gui.meta.EnumTypesCollector;
import com.almworks.items.gui.meta.LoadedItemKey;
import com.almworks.items.gui.meta.commons.FeatureRegistry;
import com.almworks.items.gui.meta.commons.SerializableFeature;
import com.almworks.items.gui.meta.schema.constraints.ConstraintKind;
import com.almworks.items.sync.util.identity.ScalarSequence;
import com.almworks.jira.provider3.schema.CustomField;
import com.almworks.util.LogHelper;
import com.almworks.util.collections.ByteArray;
import com.almworks.util.collections.Containers;
import com.almworks.util.collections.Convertor;
import com.almworks.util.commons.Procedure;
import com.almworks.util.properties.TypedKeyWithEquality;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public class CascadeSchema {
  public static final TypedKeyWithEquality<LoadedItemKey> PARENT_KEY =
    TypedKeyWithEquality.create((Object)(CustomField.ENUM_PARENT.getId() + "/parent"));

  public static final TypedKeyWithEquality<Set<Long>> SUBTREE_KEY =
    TypedKeyWithEquality.create((Object)(CustomField.ENUM_PARENT.getId() + "/subtree"));

  public static final List<EnumGrouping> CASCADE_GROUPING = Collections.<EnumGrouping>singletonList(new CascadeGrouping());

  private static final SerializableFeature<ConstraintKind> CASCADE_NULLABLE = new SerializableFeature<ConstraintKind>() {
    @Override
    public ConstraintKind restore(DBReader reader, ByteArray.Stream stream, @Nullable Procedure<LongList> invalidate) {
      String id = stream.nextUTF8();
      String displayName = stream.nextUTF8();
      if (!stream.isSuccessfullyAtEnd() || id == null || displayName == null) {
        LogHelper.error("Failed to read", id, displayName, stream);
        return null;
      }
      return new CascadeConstraintKind(new ItemKeyStub(id, displayName, ItemOrder.byOrder(-1)));
    }

    @Override
    public Class<ConstraintKind> getValueClass() {
      return ConstraintKind.class;
    }
  };

  public static ScalarSequence createNullableKind(String id, String displayName) {
    return new ScalarSequence.Builder().append(MetaSchema.FEATURE_CASCADE_CONSTRAINT).append(id).append(displayName).create();
  }

  public static void registerFeatures(FeatureRegistry registry) {
    registry.register(MetaSchema.FEATURE_CASCADE_CONSTRAINT, CASCADE_NULLABLE);
  }

  private static class CascadeConstraintKind implements ConstraintKind {
    private final ItemKey myNotSet;
    public CascadeConstraintKind(ItemKey notSet) {
      myNotSet = notSet;
    }
    @Override
    public ConstraintDescriptor createDescriptor(DBAttribute<?> attribute, String displayName, String id, EnumTypesCollector.Loaded type, @Nullable Icon icon) {
      LogHelper.assertError(attribute.getScalarClass() == Long.class, "Attribute of type Long expected:", attribute);
      LogHelper.assertError(attribute.getComposition() == DBAttribute.ScalarComposition.SCALAR, "Scalar attribute expected:", attribute);
      LogHelper.assertError(type != null, "Missing enum type:", attribute);
      if (type == null) return null;

      final EnumConstraintKind kind = new EnumSubtreeKind(CustomField.ENUM_PARENT, SUBTREE_KEY, type.getModifiable());
      final List<EnumGrouping> grouping = CASCADE_GROUPING;
      final Convertor<ItemKey, String> filterConvertor = null;

      BaseEnumConstraintDescriptor descriptor = type.createEnumDescriptor(attribute, displayName, myNotSet, kind, grouping, filterConvertor, id);
      if (icon != null) descriptor.setIcon(icon);
      return descriptor;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) return true;
      CascadeConstraintKind other = Util.castNullable(CascadeConstraintKind.class, obj);
      return other != null && Util.equals(myNotSet, other.myNotSet);
    }

    @Override
    public int hashCode() {
      return CascadeConstraintKind.class.hashCode() ^ myNotSet.hashCode();
    }
  }

  private static class CascadeGrouping implements EnumGrouping<CascadeGroup> {
    @Override
    public CascadeGroup getGroup(ResolvedItem item) {
      if(item instanceof LoadedItemKey) {
        final LoadedItemKey loaded = (LoadedItemKey)item;
        return new CascadeGroup(Util.NN(loaded.getValue(PARENT_KEY), loaded));
      }
      return null;
    }

    @Override
    public CascadeGroup getNullGroup() {
      return CascadeGroup.NULL_GROUP;
    }

    @NotNull
    @Override
    public Comparator<CascadeGroup> getComparator() {
      return Containers.comparablesComparator();
    }

    @NotNull
    @Override
    public String getDisplayableName() {
      return "Cascade";
    }
  }

  private static class CascadeGroup extends ItemKeyGroup implements Comparable<CascadeGroup> {
    public static final CascadeGroup NULL_GROUP = new CascadeGroup("- No group -", ItemOrder.byOrder(-1));
    private final ItemOrder myOrder;

    private CascadeGroup (String displayName, ItemOrder order) {
      super(displayName);
      myOrder = order;
    }

    public CascadeGroup(LoadedItemKey parent) {
      this(parent.getDisplayName(), parent.getOrder());
    }

    @Override
    public int compareTo(CascadeGroup o) {
      return myOrder.compareTo(o.myOrder);
    }
  }
}
