package com.almworks.items.gui.meta.schema.applicability;

import com.almworks.integers.LongIterator;
import com.almworks.integers.LongList;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.gui.meta.MetaModule;
import com.almworks.items.gui.meta.commons.FeatureRegistry;
import com.almworks.items.sync.util.identity.DBIdentity;
import com.almworks.items.sync.util.identity.DBStaticObject;
import com.almworks.items.sync.util.identity.ScalarSequence;
import com.almworks.items.util.DBNamespace;

public class Applicabilities {
  static final DBNamespace NS = MetaModule.NS.subNs("applicability");
  private static final DBNamespace NS_FEATURES = NS.subNs("features");

  /**
   * Composite applicability, checks all children are applicable<br>
   * Sequence [childCount(int),children(Applicability[])]
   * @see SatisfyAll
   */
  public static final DBIdentity FEATURE_SATISFY_ALL = feature("satisfyAll");

  /**
   * Applicable is primary value is contained in given subset. Primary value is identified by primary attribute and model key.
   * Sequence [primaryAttribute(long),modelKey(long),enumSubset(long[]).
   * @see EnumSubsetApplicability
   */
  public static final DBIdentity FEATURE_ENUM_SUBSET = feature("enumSubset");

  /**
   * Applicable to any primary item
   */
  public static final ScalarSequence SEQUENCE_SATISFY_ANY = new ScalarSequence.Builder().append(FEATURE_SATISFY_ALL).append(0).create();

  public static void registerFeatures(FeatureRegistry registry) {
    registry.register(FEATURE_SATISFY_ALL, SatisfyAll.FEATURE);
    registry.register(FEATURE_ENUM_SUBSET, EnumSubsetApplicability.FEATURE);
  }

  /**
   * @see SatisfyAll
   */
  public static ScalarSequence satisfyAll(ScalarSequence ... applicabilities) {
    ScalarSequence.Builder builder = new ScalarSequence.Builder();
    builder.append(FEATURE_SATISFY_ALL);
    builder.append(applicabilities.length);
    for (ScalarSequence applicability : applicabilities) builder.appendSubsequence(applicability);
    return builder.create();
  }

  /**
   * @see EnumSubsetApplicability
   */
  public static ScalarSequence enumSubset(DBAttribute<Long> attribute, DBStaticObject modelKey, LongList enumSubset) {
    ScalarSequence.Builder builder = new ScalarSequence.Builder();
    builder.append(FEATURE_ENUM_SUBSET);
    builder.append(attribute);
    builder.append(modelKey);
    builder.append(enumSubset.size());
    for (LongIterator it : enumSubset) builder.appendLong(it.value());
    return builder.create();
  }


  private static DBIdentity feature(String id) {
    return DBIdentity.fromDBObject(NS_FEATURES.object(id));
  }
}
