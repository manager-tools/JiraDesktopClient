package com.almworks.items.gui.meta.schema.columns;

import com.almworks.api.application.ItemDownloadStage;
import com.almworks.api.application.LoadedItem;
import com.almworks.api.application.field.AdaptiveComparator;
import com.almworks.items.cache.DataLoader;
import com.almworks.items.gui.meta.commons.FeatureRegistry;
import com.almworks.items.gui.meta.commons.SerializableFeature;
import com.almworks.items.gui.meta.commons.SerializedObjectAttribute;
import com.almworks.items.gui.meta.schema.columns.comparators.ComparatorFeatures;
import com.almworks.items.sync.util.identity.DBIdentity;
import com.almworks.items.sync.util.identity.ScalarSequence;
import com.almworks.util.collections.Containers;
import com.almworks.util.collections.Convertor;
import com.almworks.util.models.TableColumnBuilder;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;

/**
 * Standard features allows to build sequences of decorators:<br>
 * [{@link #FEATURE_NULL_FIRST}, {@link #FEATURE_REVERSE_ORDER}, {@link #FEATURE_CASE_INSENSITIVE}] - orders nullable strings such as nulls comes first, then not null strings
 * in backward case-insensitive order.
 */
public abstract class ColumnComparator {
  public static final DataLoader<ColumnComparator> LOADER = SerializedObjectAttribute.create(ColumnComparator.class,
    Columns.COMPARATOR);

  public abstract <T> boolean setupComparator(TableColumnBuilder<LoadedItem, T> builder, @Nullable Convertor<LoadedItem, T> convertor);

  private static final DBIdentity FEATURE_L2_COMPARATOR = Columns.feature("comparators.l2");
  /**
   * Inverts order provided by subsequent comparator
   */
  public static final DBIdentity FEATURE_REVERSE_ORDER = Columns.feature("comparators.reverseOrder");
  /**
   * Compares any comparables in natural order. This comparator does not fail on nulls but client should take care to avoid null values.
   */
  public static final DBIdentity FEATURE_COMPARABLES = Columns.feature("comparators.comparables");
  /**
   * Compares not-null strings
   */
  public static final DBIdentity FEATURE_CASE_INSENSITIVE = Columns.feature("comparators.caseInsensitive");
  /**
   * Compares strings as numbers, if possible
   */
  public static final DBIdentity FEATURE_NUMBERS_AS_STRINGS = Columns.feature("comparators.numbersAsStrings");
  /**
   * Decorates subsequent comparator: orders nulls before not null values. Next comparator don't ever called with null argument
   */
  public static final DBIdentity FEATURE_NULL_FIRST = Columns.feature("comparators.nulls.first");
  /**
   * Same as {@link #FEATURE_NULL_FIRST} but nulls comes last
   */
  public static final DBIdentity FEATURE_NULL_LAST = Columns.feature("comparators.nulls.last");
  /**
   * Compares sorted lists. Compares their sizes first (reversed), if sizes are equal, compares lexicographically.
   * Requires an element comparator feature as a parameter.
   */
  public static final DBIdentity FEATURE_LIST_SIZE_COMPARATOR = Columns.feature("comparators.listSize");
  /**
   * Compares sorted lists lexicographically. List prefix would be considered greater than list itself.
   * Requires an element comparator feature as a parameter.
   */
  public static final DBIdentity FEATURE_LIST_LEXICAL_COMPARATOR = Columns.feature("comparators.listLexical");
  public static final ScalarSequence SEQUENCE_COMPARATOR_MULTI_ENUM_LEXICAL =
      new ScalarSequence.Builder().append(FEATURE_LIST_LEXICAL_COMPARATOR).append(FEATURE_COMPARABLES).create();

  public static final ScalarSequence SEQUENCE_COMPARATOR_TEXT = new ScalarSequence.Builder().append(FEATURE_NULL_FIRST).append(FEATURE_CASE_INSENSITIVE).create();
  public static final ScalarSequence SEQUENCE_COMPARATOR_DECIMAL_REVERSED = ScalarSequence.create(ColumnComparator.FEATURE_NULL_FIRST, ColumnComparator.FEATURE_REVERSE_ORDER, ColumnComparator.FEATURE_COMPARABLES);
  public static final ScalarSequence SEQUENCE_COMPARATOR_DATE_NULL_EARLIER =
    ScalarSequence.create(ColumnComparator.FEATURE_NULL_LAST, ColumnComparator.FEATURE_REVERSE_ORDER, ColumnComparator.FEATURE_COMPARABLES);
  public static final ScalarSequence SEQUENCE_COMPARATOR_DATE_NULL_LATER =
    ScalarSequence.create(ColumnComparator.FEATURE_NULL_FIRST, ColumnComparator.FEATURE_REVERSE_ORDER, ColumnComparator.FEATURE_COMPARABLES);

  public static ScalarSequence l2Comparator(@Nullable DBIdentity l2Comparator, @Nullable ItemDownloadStage minStage, @Nullable ScalarSequence valueComparator) {
    if (valueComparator == null) valueComparator = ScalarSequence.EMPTY_SEQUENCE;
    ScalarSequence.Builder builder = new ScalarSequence.Builder();
    ScalarSequence l2Sequence = l2Comparator != null ? ScalarSequence.create(l2Comparator) : ScalarSequence.EMPTY_SEQUENCE;
    builder.append(FEATURE_L2_COMPARATOR).appendSubsequence(l2Sequence);
    if (valueComparator != null && !valueComparator.isEmpty()) {
      builder.appendByte(minStage == null ? -1 : minStage.getDbValue());
      builder.appendSubsequence(valueComparator);
    }
    return builder.create();
  }

  public static void registerFeatures(FeatureRegistry registry) {
    registry.register(FEATURE_L2_COMPARATOR, ComparatorFeatures.L2);
    registry.register(FEATURE_REVERSE_ORDER, ComparatorFeatures.REVERSE_ORDER);
    registry.register(FEATURE_COMPARABLES, SerializableFeature.NoParameters.create(Containers.comparablesComparator(), Comparator.class));
    registry.register(FEATURE_CASE_INSENSITIVE, SerializableFeature.NoParameters.create(String.CASE_INSENSITIVE_ORDER, Comparator.class));
    registry.register(FEATURE_NUMBERS_AS_STRINGS, SerializableFeature.NoParameters.create(AdaptiveComparator.instance(), Comparator.class));
    registry.register(FEATURE_NULL_FIRST, ComparatorFeatures.NULL_FIRST);
    registry.register(FEATURE_NULL_LAST, ComparatorFeatures.NULL_LAST);
    registry.register(FEATURE_LIST_SIZE_COMPARATOR, ComparatorFeatures.LIST_SIZE);
    registry.register(FEATURE_LIST_LEXICAL_COMPARATOR, ComparatorFeatures.LIST_LEXICAL);
  }
}
