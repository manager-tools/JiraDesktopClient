package com.almworks.items.gui.meta.schema.export;

import com.almworks.api.application.ExportValueType;
import com.almworks.api.application.util.ExportContext;
import com.almworks.integers.LongList;
import com.almworks.items.api.DBReader;
import com.almworks.items.gui.meta.GuiFeaturesManager;
import com.almworks.items.gui.meta.LoadedModelKey;
import com.almworks.items.gui.meta.commons.FeatureRegistry;
import com.almworks.items.gui.meta.commons.SerializableFeature;
import com.almworks.items.gui.meta.commons.SerializedObjectAttribute;
import com.almworks.items.sync.util.identity.DBIdentity;
import com.almworks.items.sync.util.identity.DBStaticObject;
import com.almworks.items.sync.util.identity.ScalarSequence;
import com.almworks.items.util.DBNamespace;
import com.almworks.util.LogHelper;
import com.almworks.util.Pair;
import com.almworks.util.collections.ByteArray;
import com.almworks.util.collections.UserDataHolder;
import com.almworks.util.commons.Procedure;
import com.almworks.util.datetime.DateUtil;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.properties.PropertyMap;
import com.almworks.util.threads.Computable;
import org.almworks.util.TypedKey;
import org.almworks.util.Util;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class ModelKeyValueExport<T> implements ExportPolicy {
  private static final DBNamespace NS_FEATURES = Exports.featuresNS("modelKeyValue");

  private static final Object NO_KEY = "noKey";
  private final TypedKey<Object> myKeyKey;
  private final long myModelKey;
  private final ExportValueType myValueType;
  private final ValueExport<T> myExport;

  public ModelKeyValueExport(long modelKey, ExportValueType valueType, ValueExport<T> export) {
    myModelKey = modelKey;
    myValueType = valueType;
    myExport = export;
    myKeyKey = TypedKey.create("modelKey@" + modelKey);
  }

  @Override
  public Pair<String, ExportValueType> export(PropertyMap values, ExportContext context, GuiFeaturesManager features) {
    LoadedModelKey<T> key = getModelKey(context, features);
    if (key == null) return null;
    T value = key.getValue(values);
    String str = Util.NN(myExport.exportValue(context, value), "");
    return Pair.create(str, myValueType);
  }

  @SuppressWarnings({"unchecked"})
  private LoadedModelKey<T> getModelKey(ExportContext context, final GuiFeaturesManager features) {
    TypedKey<Object> keyKey = myKeyKey;
    return getModelKey(context, features, keyKey, myModelKey);
  }

  @SuppressWarnings({"unchecked"})
  public static <T> LoadedModelKey<T> getModelKey(ExportContext context, final GuiFeaturesManager features, TypedKey<Object> keyKey, final DBStaticObject modelKeyItem) {
    UserDataHolder dataHolder = context.getUserData();
    Object key = dataHolder.getUserData(keyKey);
    if (key == NO_KEY) return null;
    if (key != null) return Util.castNullable(LoadedModelKey.class, key);
    LoadedModelKey<T> modelKey = ThreadGate.AWT_IMMEDIATE.compute(new Computable<LoadedModelKey<T>>() {
      @Override
      public LoadedModelKey<T> compute() {
        return (LoadedModelKey<T>) features.getModelKeyCollector().findKey(modelKeyItem);
      }
    });
    dataHolder.putIfAbsent(keyKey, modelKey != null ? modelKey : NO_KEY);
    return modelKey;
  }

  public static <T> LoadedModelKey<T> getModelKey(ExportContext context, final GuiFeaturesManager features, TypedKey<Object> keyKey, final long modelKeyItem) {
    UserDataHolder dataHolder = context.getUserData();
    Object key = dataHolder.getUserData(keyKey);
    if (key == NO_KEY) return null;
    if (key != null) return Util.castNullable(LoadedModelKey.class, key);
    LoadedModelKey<T> modelKey = ThreadGate.AWT_IMMEDIATE.compute(new Computable<LoadedModelKey<T>>() {
      @Override
      public LoadedModelKey<T> compute() {
        return (LoadedModelKey<T>) features.getModelKeyCollector().getKey(modelKeyItem);
      }
    });
    dataHolder.putIfAbsent(keyKey, modelKey != null ? modelKey : NO_KEY);
    return modelKey;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    ModelKeyValueExport other = (ModelKeyValueExport) obj;
    return myModelKey == other.myModelKey && Util.equals(myExport, other.myExport);
  }

  @Override
  public int hashCode() {
    return (int)myModelKey ^ getClass().hashCode();
  }

  @Override
  public String toString() {
    return "MKVE(mk=" + myModelKey + ",export=" + myExport + ")";
  }

  private static final List<ExportValueType> TYPES = Arrays.asList(ExportValueType.NUMBER, ExportValueType.LARGE_STRING, ExportValueType.STRING, ExportValueType.DATE, ExportValueType.STRING_HTML);
  public static final SerializableFeature<ExportPolicy> FEATURE = new SerializableFeature<ExportPolicy>() {
    @Override
    public ExportPolicy restore(DBReader reader, ByteArray.Stream stream, @Nullable Procedure<LongList> invalidate) {
      long modelKey = stream.nextLong();
      byte exportType = stream.nextByte();
      if (modelKey <= 0 || exportType < 0 || exportType >= TYPES.size() || stream.isErrorOccurred()) return null;
      ExportValueType type = TYPES.get(exportType);
      ValueExport export = SerializedObjectAttribute.restore(reader, stream.nextSubstream(), ValueExport.class, invalidate);
      if (export == null || !stream.isSuccessfullyAtEnd()) return null;
      return new ModelKeyValueExport(modelKey, type, export);
    }

    @Override
    public Class<ExportPolicy> getValueClass() {
      return ExportPolicy.class;
    }
  };

  public static ScalarSequence create(DBStaticObject modelKey, ExportValueType type, ScalarSequence valueExport) {
    if (type == null) {
      LogHelper.error("Null type", modelKey);
      type = ExportValueType.STRING;
    }
    return new ScalarSequence.Builder()
      .append(FEATURE_MODEL_KEY_VALUE).append(modelKey).appendByte(TYPES.indexOf(type))
      .appendSubsequence(valueExport)
      .create();
  }

  /**
   * {@link ExportPolicy}<br>
   * Sequence: [modelKey(reference), {@link ExportValueType valueType}(byte), valueExport({@link ModelKeyValueExport.ValueExport})]
   * @see ModelKeyValueExport
   * @see ModelKeyValueExport#create(com.almworks.items.sync.util.identity.DBStaticObject, com.almworks.api.application.ExportValueType, com.almworks.items.sync.util.identity.ScalarSequence) (com.almworks.items.sync.util.identity.ScalarSequence.Builder, com.almworks.items.sync.util.identity.DBStaticObject, com.almworks.api.application.ExportValueType)
   */
  private static final DBIdentity FEATURE_MODEL_KEY_VALUE = feature("modelKeyValue");
  private static final DBIdentity FEATURE_SCALAR_DATE_TIME = feature("dateTime");
  public static final ScalarSequence SEQUENCE_DATE_TIME = ScalarSequence.create(FEATURE_SCALAR_DATE_TIME);
  private static final DBIdentity FEATURE_SCALAR_DAY = feature("day");
  public static final ScalarSequence SEQUENCE_DAY = ScalarSequence.create(FEATURE_SCALAR_DAY);
  private static final DBIdentity FEATURE_SCALAR_SECONDS = feature("seconds");
  public static final ScalarSequence SEQUENCE_SECONDS = ScalarSequence.create(FEATURE_SCALAR_SECONDS);
  private static final DBIdentity FEATURE_SCALAR_DECIMAL = feature("number");
  public static final ScalarSequence SEQUENCE_DECIMAL = ScalarSequence.create(FEATURE_SCALAR_DECIMAL);
  private static final DBIdentity FEATURE_SCALAR_INTEGER = feature("integer");
  public static final ScalarSequence SEQUENCE_INTEGER = ScalarSequence.create(FEATURE_SCALAR_INTEGER);

  private static DBIdentity feature(String id) {
    return DBIdentity.fromDBObject(NS_FEATURES.object(id));
  }

  public static void registerFeatures(FeatureRegistry registry) {
    registry.register(FEATURE_MODEL_KEY_VALUE, FEATURE);
    registry.register(FEATURE_SCALAR_DATE_TIME, SerializableFeature.NoParameters.create(DATE_TIME, ValueExport.class));
    registry.register(FEATURE_SCALAR_DAY, SerializableFeature.NoParameters.create(DAY, ValueExport.class));
    registry.register(FEATURE_SCALAR_SECONDS, SerializableFeature.NoParameters.create(SECONDS, ValueExport.class));
    registry.register(FEATURE_SCALAR_DECIMAL, SerializableFeature.NoParameters.create(DECIMAL, ValueExport.class));
    registry.register(FEATURE_SCALAR_INTEGER, SerializableFeature.NoParameters.create(INTEGER, ValueExport.class));
    RenderValueExport.registerFeatures(registry);
  }

  public interface ValueExport<T> {
    String exportValue(ExportContext context, T value);
  }

  private static final ValueExport<Date> DATE_TIME = new ValueExport<Date>() {
    @Override
    public String exportValue(ExportContext context, Date value) {
      return context.formatDate(value);
    }

    @Override
    public String toString() {
      return "DATE_TIME";
    }
  };
  
  private static final ValueExport<Integer> DAY = new ValueExport<Integer>() {
    @Override
    public String exportValue(ExportContext context, Integer value) {
      if (value == null) return "";
      return context.formatDate(DateUtil.toInstantOnDay(value));
    }

    @Override
    public String toString() {
      return "DAY";
    }
  };

  private static final ValueExport<Integer> SECONDS = new ValueExport<Integer>() {
    @Override
    public String exportValue(ExportContext context, Integer value) {
      if (value == null) return "";
      return context.formatNumber(((double) value) / 3600.0);
    }

    @Override
    public String toString() {
      return "SECONDS";
    }
  };

  private static final ValueExport<Number> DECIMAL = new ValueExport<Number>() {
    @Override
    public String exportValue(ExportContext context, Number value) {
      return context.formatNumber(value);
    }

    @Override
    public String toString() {
      return "DECIMAL";
    }
  };
  
  private static final ValueExport<Integer> INTEGER = new ValueExport<Integer>() {
    @Override
    public String exportValue(ExportContext context, Integer value) {
      if (value == null) return "";
      return String.valueOf(value);
    }

    @Override
    public String toString() {
      return "INTEGER";
    }
  };
}
