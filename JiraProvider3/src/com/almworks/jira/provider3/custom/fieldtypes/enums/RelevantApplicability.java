package com.almworks.jira.provider3.custom.fieldtypes.enums;

import com.almworks.api.application.ItemWrapper;
import com.almworks.integers.LongList;
import com.almworks.items.api.DBReader;
import com.almworks.items.gui.edit.EditModelState;
import com.almworks.items.gui.meta.commons.FeatureRegistry;
import com.almworks.items.gui.meta.commons.SerializableFeature;
import com.almworks.items.gui.meta.schema.applicability.Applicabilities;
import com.almworks.items.gui.meta.schema.applicability.Applicability;
import com.almworks.items.sync.ItemVersion;
import com.almworks.items.sync.util.identity.DBIdentity;
import com.almworks.items.sync.util.identity.ScalarSequence;
import com.almworks.jira.provider3.schema.Issue;
import com.almworks.jira.provider3.schema.Jira;
import com.almworks.util.collections.ByteArray;
import com.almworks.util.commons.Procedure;
import org.almworks.util.Util;
import org.jetbrains.annotations.Nullable;

/**
 * This implementation checks if the field is known applicable to the particular issues.<br>
 * It should be used to decide if the editor can be used to EDIT (not CREATE) issue. Do not use this implementation if the editor can be used in CREATE window.
 */
public class RelevantApplicability implements Applicability {
  /**
   * Sequence: {fieldItem:long}
   */
  private static final DBIdentity FEATURE_RELEVANT = Jira.feature("custom.dnd.applicability.relevant");
  public static final SerializableFeature<Applicability> FEATURE = new SerializableFeature<Applicability>() {
    @Override
    public Applicability restore(DBReader reader, ByteArray.Stream stream, @Nullable Procedure<LongList> invalidate) {
      long fieldItem = stream.nextLong();
      if (fieldItem <= 0 || !stream.isSuccessfullyAtEnd()) return null;
      return new RelevantApplicability(fieldItem);
    }

    @Override
    public Class<Applicability> getValueClass() {
      return Applicability.class;
    }
  };
  private final long myField;

  private RelevantApplicability(long field) {
    myField = field;
  }

  @Override
  public boolean isApplicable(ItemWrapper item) {
    return true; // No model key for relevant fields
  }

  @Override
  public boolean isApplicable(EditModelState model) {
    // Assume that editor checks applicability during prepare
    // Not applicable to new issues
    return !model.getEditingItems().isEmpty();
  }

  @Override
  public boolean isApplicable(ItemVersion item) {
    LongList fields = Issue.FIELDS_FOR_EDIT.getValue(item);
    return fields == null || fields.contains(myField);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    RelevantApplicability other = Util.castNullable(RelevantApplicability.class, obj);
    return other != null && myField == other.myField;
  }

  @Override
  public int hashCode() {
    return RelevantApplicability.class.hashCode() ^ Util.hashCode(myField);
  }

  @Override
  public String toString() {
    return "RelevantApplicability(" + myField + ")";
  }

  public static void registerFeature(FeatureRegistry registry) {
    registry.register(FEATURE_RELEVANT, FEATURE);
  }

  public static ScalarSequence sequence(long item, ScalarSequence applicability) {
    ScalarSequence sequence = new ScalarSequence.Builder().append(FEATURE_RELEVANT).appendLong(item).create();
    if (applicability == null || applicability == Applicabilities.SEQUENCE_SATISFY_ANY) return sequence;
    return Applicabilities.satisfyAll(applicability, sequence);
  }
}
