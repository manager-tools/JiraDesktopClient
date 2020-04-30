package com.almworks.items.gui.meta;

import com.almworks.items.cache.LoadersSet;
import com.almworks.items.cache.util.ItemSetModel;
import com.almworks.util.LogHelper;
import com.almworks.util.advmodel.AListModel;
import com.almworks.util.advmodel.FilteringConvertingListDecorator;
import com.almworks.util.collections.ChangeListener;
import com.almworks.util.collections.Convertor;
import com.almworks.util.commons.Condition;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.model.ValueModel;
import com.almworks.util.threads.Computable;
import org.almworks.util.Util;
import org.almworks.util.detach.Lifespan;

class ModelScopeFilter<T extends GuiFeature> extends Condition<T> implements ChangeListener {
  private final ValueModel<? extends ScopeFilter> myScopeModel;
  private FilteringConvertingListDecorator<T, ?> myModel;
  private final LoadersSet myLoadersSet;
  private ScopeFilter myLastScope = null;

  private ModelScopeFilter(ValueModel<? extends ScopeFilter> scope, LoadersSet loadersSet) {
    myScopeModel = scope;
    myLoadersSet = loadersSet;
  }

  @Override
  public boolean isAccepted(T value) {
    return value != null && myLastScope != null && value.isLoaded() && myLastScope.isAccepted(myLoadersSet.getSlice(), value.getItem());
  }

  @Override
  public void onChange() {
    ScopeFilter scope = myScopeModel.getValue();
    if (Util.equals(myLastScope, scope)) return;
    myLastScope = scope;
    if (myLastScope == null) myLoadersSet.clear();
    else myLoadersSet.setLoaders(myLastScope.getLoaders());
    if (myModel != null) myModel.setFilter(this);
    else LogHelper.error("Missing model", this, scope);
  }

  public static <D extends GuiFeature, R> AListModel<R> filterAndConvert(final Lifespan life, final ItemSetModel<D> model,
    final ValueModel<? extends ScopeFilter> scope, final Convertor<D, R> convertor)
  {
    return ThreadGate.AWT_IMMEDIATE.compute(new Computable<AListModel<R>>() {
      @Override
      public AListModel<R> compute() {
        LoadersSet loadersSet = model.getSlice().createAttributeSet(life);
        ModelScopeFilter<D> filter = new ModelScopeFilter<D>(scope, loadersSet);
        FilteringConvertingListDecorator<D, R> decorator =
          FilteringConvertingListDecorator.<D, R>create(life, model, filter, convertor);
        filter.myModel = decorator;
        scope.addAWTChangeListener(life, filter);
        filter.onChange();
        return decorator;
      }
    });
  }
}
