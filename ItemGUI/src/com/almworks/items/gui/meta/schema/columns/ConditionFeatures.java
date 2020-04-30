package com.almworks.items.gui.meta.schema.columns;

import com.almworks.integers.LongList;
import com.almworks.items.api.DBReader;
import com.almworks.items.gui.meta.MetaModule;
import com.almworks.items.gui.meta.commons.FeatureRegistry;
import com.almworks.items.gui.meta.commons.SerializableFeature;
import com.almworks.items.sync.util.identity.DBIdentity;
import com.almworks.items.sync.util.identity.ScalarSequence;
import com.almworks.items.util.DBNamespace;
import com.almworks.util.LogHelper;
import com.almworks.util.collections.ByteArray;
import com.almworks.util.commons.Condition;
import com.almworks.util.commons.Procedure;
import org.jetbrains.annotations.Nullable;

public class ConditionFeatures {
  private static final DBNamespace NS = MetaModule.NS.subNs("conditions");

  private static final DBIdentity INSTANCEOF = feature("instanceof");

  public static ScalarSequence seqInstanceof(DBIdentity classFeature) {
    return new ScalarSequence.Builder().append(INSTANCEOF).append(classFeature).create();
  }

  public static void registerFeatures(FeatureRegistry registry) {
    registry.register(INSTANCEOF, new InstanceofConditionLoader());
  }

  private static DBIdentity feature(String id) {
    return DBIdentity.fromDBObject(NS.object(id));
  }

  private static class InstanceofConditionLoader implements SerializableFeature<Condition> {
    @Override
    public Condition restore(DBReader reader, ByteArray.Stream stream, @Nullable Procedure<LongList> invalidate) {
      long classItem = stream.nextLong();
      if (!stream.isSuccessfullyAtEnd()) {
        LogHelper.error(classItem);
        return null;
      }
      Class sampleClass = FeatureRegistry.getInstance(reader).getFeature(reader, classItem, Class.class);
      return Condition.isInstance(sampleClass);
    }

    @Override
    public Class<Condition> getValueClass() {
      return Condition.class;
    }
  }

}
