package com.almworks.items.gui.meta.util;

import com.almworks.api.application.ItemWrapper;
import com.almworks.items.gui.meta.GuiFeaturesManager;
import com.almworks.items.gui.meta.LoadedModelKey;
import com.almworks.items.sync.util.identity.DBStaticObject;
import com.almworks.util.collections.Convertor;
import org.almworks.util.Util;

import java.util.List;

public class GetModelKeyValue<T> extends Convertor<ItemWrapper, T> {
  private final DBStaticObject myModelKey;
  private final boolean myCollection;
  private final Class<?> myElementClass;

  private GetModelKeyValue(DBStaticObject modelKey, boolean isCollection, Class<?> elementClass) {
    myModelKey = modelKey;
    myCollection = isCollection;
    myElementClass = elementClass;
  }

  public static <T> Convertor<ItemWrapper, List<T>> list(DBStaticObject modelKey, Class<T> elementClass) {
    return new GetModelKeyValue<List<T>>(modelKey, true, elementClass);
  }

  public static <T> Convertor<ItemWrapper, T> scalar(DBStaticObject modelKey, Class<T> scalarClass) {
    return new GetModelKeyValue<T>(modelKey, false, scalarClass);
  }

  @Override
  public T convert(ItemWrapper item) {
    if (item == null) return null;
    GuiFeaturesManager manager = item.services().getActor(GuiFeaturesManager.ROLE);
    if (manager == null) return null;
    LoadedModelKey<T> modelKey = cast(manager.findModelKey(myModelKey));
    if (modelKey == null) return null;
    return item.getModelKeyValue(modelKey);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) return true;
    GetModelKeyValue<?> other = Util.castNullable(GetModelKeyValue.class, obj);
    return other != null && Util.equals(myModelKey, other.myModelKey) && Util.equals(myElementClass, other.myElementClass) && (myCollection == other.myCollection);
  }

  @Override
  public int hashCode() {
    return Util.hashCode(myModelKey, myElementClass) ^ GetModelKeyValue.class.hashCode();
  }

  @SuppressWarnings( {"unchecked"})
  private LoadedModelKey<T> cast(LoadedModelKey<?> modelKey) {
    if (modelKey == null) return null;
    if (myCollection)
      return (LoadedModelKey<T>)modelKey.castList(myElementClass);
    return (LoadedModelKey<T>) modelKey.castScalar(myElementClass);
  }
}
