package com.almworks.api.application.util;

import com.almworks.api.application.ModelKey;
import com.almworks.api.application.ModelMap;
import com.almworks.util.advmodel.OrderListModel;
import com.almworks.util.advmodel.SortedListDecorator;
import com.almworks.util.collections.ChangeListener;
import com.almworks.util.threads.ThreadAWT;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.List;

/**
 * @author dyoma
 */
public class ListModelView<T> implements ChangeListener {
  private final ModelMap myModel;
  private final ModelKey<List<T>> myKey;
  private final OrderListModel<T> myList;

  private ListModelView(ModelKey<List<T>> key, ModelMap model) {
    myKey = key;
    myModel = model;
    List<T> list = getElements();
    myList = list != null && !list.isEmpty() ? OrderListModel.create(list) : OrderListModel.<T>create();
  }

  @ThreadAWT
  public void onChange() {
    List<T> elements = getElements();
    if (elements == null || elements.isEmpty())
      myList.clear();
    else
      myList.replaceElementsSet(elements);
  }

  private List<T> getElements() {
    return myKey.getValue(myModel);
  }

  @NotNull
  public static <T> OrderListModel<T> create(Lifespan life, ModelKey<List<T>> key, ModelMap model) {
    ListModelView<T> view = new ListModelView<T>(key, model);
    model.addAWTChangeListener(life, view);
    return view.myList;
  }

  public static <T> SortedListDecorator<T> createSorted(Lifespan life, ModelKey<List<T>> key, ModelMap model, Comparator<T> comparator) {
    return SortedListDecorator.create(life, ListModelView.<T>create(life, key, model), comparator);
  }
}
