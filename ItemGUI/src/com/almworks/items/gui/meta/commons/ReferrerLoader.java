package com.almworks.items.gui.meta.commons;

import com.almworks.api.application.LoadedItemServices;
import com.almworks.api.application.ModelKey;
import com.almworks.api.application.ModelMap;
import com.almworks.api.application.util.DataAccessor;
import com.almworks.api.application.util.DataIO;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBReader;
import com.almworks.items.gui.meta.GuiFeaturesManager;
import com.almworks.items.gui.meta.LoadedModelKey;
import com.almworks.items.gui.meta.schema.DataHolder;
import com.almworks.items.gui.meta.schema.modelkeys.ModelKeyLoader;
import com.almworks.items.sync.ItemVersion;
import com.almworks.items.sync.util.identity.DBIdentity;
import com.almworks.items.sync.util.identity.ScalarSequence;
import com.almworks.items.util.DBNamespace;
import com.almworks.util.collections.LongSet;
import com.almworks.util.properties.PropertyMap;
import com.almworks.util.properties.Role;
import org.almworks.util.ArrayUtil;
import org.almworks.util.Collections15;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Base class to implement {@link com.almworks.items.gui.meta.schema.modelkeys.ModelKeyLoader} which loads items referring this item via given set of references.<br>
 * Also this class provides facilities to register features, create feature DB identity and scalar sequence object ready to
 * write model model key object ({@link com.almworks.items.gui.meta.schema.modelkeys.ModelKeys#DATA_LOADER}).
 * @param <T>
 */
public abstract class ReferrerLoader<T> {
  private final Class<T> myUIClass;
  private final DBAttribute<Long>[] myReferences;

  protected ReferrerLoader(DBAttribute<Long>[] masterRefs, Class<T> UIClass) {
    myReferences = ArrayUtil.arrayCopy(masterRefs);
    myUIClass = UIClass;
  }

  protected ReferrerLoader(DBAttribute<Long> references, Class<T> UIClass) {
    //noinspection unchecked
    myReferences = new DBAttribute[]{references};
    myUIClass = UIClass;
  }

  @Nullable
  public abstract T extractValue(ItemVersion referrer, LoadContext context);

  /**
   * Called after extraction is done, before loaded list is published into PropertyMap. Loader may change loaded value list.
   * @param issue loaded master item
   * @param values target loaded values container
   * @param elements <b>modifiable</b> list of loaded values.
   */
  protected void afterElementsExtracted(ItemVersion issue, @NotNull PropertyMap values, @NotNull List<T> elements) {}

  @Override
  public String toString() {
    return String.format("ReferredLoaded[UIClass=%s, Attr=%s]", myUIClass, Arrays.asList(myReferences));
  }

  public static class LoadContext {
    private final DBReader myReader;
    private final LoadedItemServices myItemServices;

    public LoadContext(DBReader reader, LoadedItemServices itemServices) {
      myReader = reader;
      myItemServices = itemServices;
    }

    public LoadedItemServices getItemServices() {
      return myItemServices;
    }

    public <T> T getActor(Role<T> role) {
      return myItemServices.getActor(role);
    }
  }

  public static class Descriptor {
    private final DBIdentity myFeature;
    private final ScalarSequence mySerializable;
    private final SerializableFeature<ModelKeyLoader> myFeatureImpl;

    public <T> Descriptor(ReferrerLoader<T> loader, DBIdentity feature) {
      myFeature = feature;
      mySerializable = new ScalarSequence.Builder().append(myFeature).create();
      myFeatureImpl = new SerializableFeature.NoParameters<ModelKeyLoader>(new MyLoader<T>(loader), ModelKeyLoader.class);
    }

    public static <T> Descriptor create(DBNamespace ns, String featureId, ReferrerLoader<T> loader) {
      DBIdentity feature = DBIdentity.fromDBObject(ns.object(featureId));
      return new Descriptor(loader, feature);
    }

    public ScalarSequence getSerializable() {
      return mySerializable;
    }

    public void registerFeature(FeatureRegistry registry) {
      registry.register(myFeature, myFeatureImpl);
    }
  }

  private static class MyLoader<T> extends ModelKeyLoader implements DataIO<List<T>> {
    private final ReferrerLoader<T> myLoader;

    private MyLoader(ReferrerLoader<T> loader) {
      super(DataHolder.EMPTY);
      myLoader = loader;
    }

    @Override
    public boolean loadKey(LoadedModelKey.Builder<?> b, GuiFeaturesManager guiFeatures) {
      LoadedModelKey.Builder<List<T>> builder = b.setListDataClass(myLoader.myUIClass);
      builder.setIO(this);
      builder.setAccessor(new ListDataAccessor<T>(builder.getName()));
      return true;
    }

    @Override
    public void extractValue(ItemVersion issue, LoadedItemServices itemServices, PropertyMap values,
      ModelKey<List<T>> modelKey)
    {
      LoadContext context = new LoadContext(issue.getReader(), itemServices);
      List<T> elements = Collections15.arrayList();
      LongSet slaves = new LongSet();
      for (DBAttribute<Long> ref : myLoader.myReferences) {
        for (ItemVersion slave : issue.readItems(issue.getSlaves(ref))) {
          if (!slaves.addValue(slave.getItem())) continue;
          T element = myLoader.extractValue(slave, context);
          if (element != null) elements.add(element);
        }

      }
      myLoader.afterElementsExtracted(issue, values, elements);
      if (elements.isEmpty()) elements = Collections.emptyList();
      modelKey.setValue(values, elements);
    }

    @Override
    public String toString() {
      return String.format("ReferredLoaded.ModelKeyLoader[%s]", myLoader);
    }
  }

  private static class ListDataAccessor<T> extends DataAccessor.SimpleDataAccessor<List<T>> {
    public ListDataAccessor(String name) {
      super(name);
    }

    @Override
    public List<T> getValue(ModelMap model) {
      return Util.NN(super.getValue(model), Collections15.<T>emptyList());
    }

    @Override
    public List<T> getValue(PropertyMap values) {
      return Util.NN(super.getValue(values), Collections15.<T>emptyList());
    }

    @Override
    protected Object getCanonicalValueForComparison(@Nullable List<T> value) {
      if (value != null && value.isEmpty()) return null;
      return value;
    }
  }
}
