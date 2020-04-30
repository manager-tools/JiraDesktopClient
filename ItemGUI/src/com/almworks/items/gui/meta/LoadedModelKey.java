package com.almworks.items.gui.meta;

import com.almworks.api.application.DataPromotionPolicy;
import com.almworks.api.application.LoadedItemServices;
import com.almworks.api.application.ModelKey;
import com.almworks.api.application.ModelMap;
import com.almworks.api.application.util.DataAccessor;
import com.almworks.api.application.util.DataIO;
import com.almworks.items.sync.ItemVersion;
import com.almworks.util.LogHelper;
import com.almworks.util.properties.PropertyMap;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

public class LoadedModelKey<T> implements ModelKey<T> {
  private final String myName;
  private final DataIO<T> myIo;
  private final DataAccessor<T> myAccessor;
  private final DataPromotionPolicy myPromotionPolicy;
  private final Class<? extends Collection> myCollectionClass;
  private final Class<?> myElementClass;

  public LoadedModelKey(String name, DataIO<T> io, DataAccessor<T> accessor, Class<? extends Collection> collectionClass, Class<?> elementClass, DataPromotionPolicy promotionPolicy) {
    myName = name;
    myIo = io;
    myAccessor = accessor;
    myCollectionClass = collectionClass;
    myElementClass = elementClass;
    myPromotionPolicy = promotionPolicy;
  }

  @NotNull
  @Override
  public String getName() {
    return myName;
  }

  @Override
  public T getValue(ModelMap model) {
    return myAccessor.getValue(model);
  }

  @Override
  public boolean hasValue(ModelMap model) {
    return myAccessor.hasValue(model);
  }

  @Override
  public void setValue(PropertyMap values, T value) {
    myAccessor.setValue(values, value);
  }

  @Override
  public T getValue(PropertyMap values) {
    return myAccessor.getValue(values);
  }

  @Override
  public boolean isEqualValue(ModelMap models, PropertyMap values) {
    return myAccessor.isEqualValue(models, values);
  }

  @Override
  public boolean isEqualValue(PropertyMap values1, PropertyMap values2) {
    return myAccessor.isEqualValue(values1, values2);
  }

  @Override
  public void copyValue(ModelMap to, PropertyMap from) {
    myAccessor.copyValue(to, from, this);
  }

  @Override
  public boolean hasValue(PropertyMap values) {
    return myAccessor.hasValue(values);
  }

  @Override
  public void extractValue(ItemVersion itemVersion, LoadedItemServices itemServices, PropertyMap values) {
    myIo.extractValue(itemVersion, itemServices, values, this);
  }

  @Override
  public void takeSnapshot(PropertyMap to, ModelMap from) {
    myAccessor.takeSnapshot(to, from);
  }


  @Override
  public String toString() {
    return String.format("LoadedModelKey[name=%s, IO=%s]:%s|%s", myName, myIo, myCollectionClass, myElementClass);
  }

  @Override
  public <SM> SM getModel(Lifespan lifespan, ModelMap model, Class<SM> aClass) {
    assert false;
    return null;
  }

  @Override
  public DataPromotionPolicy getDataPromotionPolicy() {
    return myPromotionPolicy;
  }

  @Nullable
  public <T> LoadedModelKey<T> castScalar(Class<T> valueClass) {
    if (myCollectionClass != null) return null;
    //noinspection unchecked
    return valueClass.isAssignableFrom(myElementClass) ? (LoadedModelKey<T>) this : null;
  }

  @Nullable
  public <T> LoadedModelKey<List<T>> castList(Class<T> elementClass) {
    if (myCollectionClass == null || myElementClass == null || elementClass == null) return null;
    if (!List.class.isAssignableFrom(myCollectionClass)) return null;
    //noinspection unchecked
    return elementClass.isAssignableFrom(myElementClass) ? (LoadedModelKey<List<T>>) this : null;
  }

  @Nullable
  public <T> LoadedModelKey<? extends Collection<T>> castCollection(Class<T> elementClass) {
    if (myCollectionClass == null || myElementClass == null || elementClass == null) {
      LogHelper.error("Wrong model key", elementClass, this);
      return null;
    }
    //noinspection unchecked
    if (elementClass.isAssignableFrom(myElementClass)) return (LoadedModelKey<? extends Collection<T>>) this;
    LogHelper.error("No assignable class", elementClass, this);
    return null;
  }

  public static class Builder<T> {
    private String myName;
    private DataIO<T> myIO;
    private DataAccessor<T> myAccessor;
    private Class<? extends Collection> myCollectionClass;
    private Class<?> myElementClass;
    private DataPromotionPolicy myPromotionPolicy = DataPromotionPolicy.STANDARD;

    public static Builder<?> create() {
      return new Builder<Object>();
    }

    public void setName(String name) {
      myName = name;
    }

    public void setPromotionPolicy(DataPromotionPolicy promotionPolicy) {
      myPromotionPolicy = promotionPolicy;
    }

    public <E> Builder<List<E>> setListDataClass(Class<E> element) {
      Class<? extends Collection> collectionClass = List.class;
      if (!priSetDataClass(element, collectionClass))
        return null;
      return (Builder<List<E>>) this;
    }

    private boolean priSetDataClass(Class<?> element, Class<? extends Collection> collectionClass) {
      if ((myCollectionClass != null && myCollectionClass != collectionClass)
        || (myElementClass != null && myElementClass != element)) {
        LogHelper.error("Data class redefinition", myCollectionClass, myElementClass, collectionClass, element);
        return false;
      }
      myCollectionClass = collectionClass;
      myElementClass = element;
      return true;
    }

    public <T> Builder<T> setDataClass(Class<T> valueClass) {
      if (!priSetDataClass(valueClass, null)) return null;
      return (Builder<T>) this;
    }

    public void setIO(DataIO<T> IO) {
      myIO = IO;
    }

    public LoadedModelKey<T> createKey() {
      if (myName == null || myIO == null || myAccessor == null) {
        LogHelper.error("Missing value", myName, myIO, myAccessor);
        return null;
      }
      return new LoadedModelKey<T>(myName, myIO, myAccessor, myCollectionClass, myElementClass, myPromotionPolicy);
    }

    public String getName() {
      return myName;
    }

    public void setAccessor(DataAccessor<T> accessor) {
      myAccessor = accessor;
    }
  }
}
