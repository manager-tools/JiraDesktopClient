package com.almworks.items.gui.meta.schema.applicability;

import com.almworks.api.application.ItemWrapper;
import com.almworks.integers.LongList;
import com.almworks.items.api.DBReader;
import com.almworks.items.gui.edit.EditModelState;
import com.almworks.items.gui.meta.commons.SerializableFeature;
import com.almworks.items.sync.ItemVersion;
import com.almworks.util.collections.ByteArray;
import com.almworks.util.commons.Procedure;
import com.almworks.util.text.TextUtil;
import org.almworks.util.Util;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

public class SatisfyAll implements Applicability {
  public static final Applicability SATISFY_ANY = new SatisfyAll(EMPTY_ARRAY);

  public static final SerializableFeature<Applicability> FEATURE = new SerializableFeature<Applicability>() {
    @Override
    public Applicability restore(DBReader reader, ByteArray.Stream stream, @Nullable Procedure<LongList> invalidate) {
      List<Applicability> children = SequenceDeserializer.restore(reader, stream, Applicability.class, invalidate, true);
      if (children == null) return null;
      if (children.isEmpty()) return SATISFY_ANY;
      return SatisfyAll.create(children);
    }

    @Override
    public Class<Applicability> getValueClass() {
      return Applicability.class;
    }
  };

  private static Applicability create(List<Applicability> children) {
    return new SatisfyAll(children.toArray(new Applicability[children.size()]));
  }

  private final Applicability[] myChildren;

  private SatisfyAll(Applicability[] children) {
    myChildren = children;
  }

  @Override
  public boolean isApplicable(ItemWrapper item) {
    for (Applicability applicability : myChildren) if (!applicability.isApplicable(item)) return false;
    return true;
  }

  @Override
  public boolean isApplicable(EditModelState model) {
    for (Applicability applicability : myChildren) if (!applicability.isApplicable(model)) return false;
    return true;
  }

  @Override
  public boolean isApplicable(ItemVersion item) {
    for (Applicability child : myChildren) if (!child.isApplicable(item)) return false;
    return true;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    SatisfyAll other = Util.castNullable(SatisfyAll.class, obj);
    return other != null && Arrays.equals(myChildren, other.myChildren);
  }

  @Override
  public int hashCode() {
    return SatisfyAll.class.hashCode() ^ Util.hashCode(myChildren);
  }

  @Override
  public String toString() {
    if (myChildren.length == 0) return "SatisfyApplicability(SatisfiesAny)";
    return "SatisfyApplicability(All:" + TextUtil.separateToString(Arrays.asList(myChildren), ",") + ")";
  }
}
