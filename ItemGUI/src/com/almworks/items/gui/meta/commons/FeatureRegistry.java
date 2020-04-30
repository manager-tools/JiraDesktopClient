package com.almworks.items.gui.meta.commons;

import com.almworks.items.api.DBReader;
import com.almworks.items.gui.meta.GuiFeaturesManager;
import com.almworks.items.sync.util.identity.DBIdentity;
import com.almworks.util.LogHelper;
import org.almworks.util.Collections15;
import org.almworks.util.Util;

import java.util.Map;

public class FeatureRegistry {
  private final Map<DBIdentity, Object> myFeatures = Collections15.hashMap();

  public FeatureRegistry() {
  }

  private <T> T getFeature(DBIdentity id, Class<T> featureClass) {
    Object feature;
    synchronized (myFeatures) {
      feature = myFeatures.get(id);
    }
    if (feature == null) LogHelper.error("Missing feature", id);
    else {
      T casted = Util.castNullable(featureClass, feature);
      if (casted == null) LogHelper.error("Wrong feature class", id, featureClass, feature);
      return casted;
    }
    return null;
  }

  public void register(DBIdentity identity, Object feature) {
    if (feature == null) return;
    synchronized (myFeatures) {
      Object known = myFeatures.get(identity);
      if (known != null) LogHelper.assertError(known.equals(feature), "Duplicated feature", identity, known, feature);
      else myFeatures.put(identity, feature);
    }
  }

  public static <T> SerializableFeature<T> getSerializableFeature(DBReader reader, long featureItem, Class<T> aClass) {
    FeatureRegistry features = getInstance(reader);
    return features.readSerializableFeature(reader, featureItem, aClass);
  }

  public static FeatureRegistry getInstance(DBReader reader) {
    return GuiFeaturesManager.getInstance(reader).getFeatureRegistry();
  }

  public <T> T getFeature(DBReader reader, long featureItem, Class<T> aClass) {
    DBIdentity id = DBIdentity.load(reader, featureItem);
    if (id == null) {
      LogHelper.error("Unknown feature", featureItem);
      return null;
    }
    return getFeature(id, aClass);
  }

  public <T> SerializableFeature<T> readSerializableFeature(DBReader reader, long featureItem, Class<T> aClass) {
    SerializableFeature<?> feature = getFeature(reader, featureItem, SerializableFeature.class);
    if (feature == null) return null;
    if (!aClass.isAssignableFrom(feature.getValueClass())) {
      LogHelper.error("Wrong value class", feature, feature.getValueClass(), aClass);
      feature = null;
    }
    //noinspection unchecked
    return (SerializableFeature<T>) feature;
  }
}
