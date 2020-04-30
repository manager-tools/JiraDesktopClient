package com.almworks.util.advmodel;

import com.almworks.util.threads.ThreadAWT;
import com.almworks.util.threads.Threads;
import org.almworks.util.Collections15;
import org.almworks.util.detach.Detach;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author dyoma
 */
public class UniqueListModel<T> extends DelegatingAListModel<T> {
  private final OrderListModel<T> myModel = OrderListModel.create();
  private final Map<T, Integer> myInstanceCounter = Collections15.hashMap();
  private final Map<AListModel, Detach> myListenModels = Collections15.hashMap();

  protected AListModel<T> getDelegate() {
    return myModel;
  }

  @ThreadAWT
  public void addElements(@NotNull Collection<? extends T> elements) {
    Threads.assertAWTThread();
    if (elements.isEmpty())
      return;
    List<T> toAdd = Collections15.arrayList();
    for (T element : elements) {
      Integer counter = myInstanceCounter.get(element);
      if (counter == null) {
        toAdd.add(element);
        myInstanceCounter.put(element, 1);
      } else
        myInstanceCounter.put(element, counter + 1);
    }
    myModel.addAll(toAdd);
  }

  @ThreadAWT
  public void removeElements(@NotNull Collection<? extends T> elements) {
    Threads.assertAWTThread();
    if (elements.isEmpty())
      return;
    List<T> toRemove = Collections15.arrayList();
    for (T element : elements) {
      Integer counter = myInstanceCounter.get(element);
      if (counter == null)
        continue;
      if (counter.intValue() == 1) {
        toRemove.add(element);
        myInstanceCounter.remove(element);
      } else
        myInstanceCounter.put(element, counter - 1);
    }
    myModel.removeAll(toRemove);
  }

  @ThreadAWT
  public void listenModel(final AListModel<? extends T> model) {
    Threads.assertAWTThread();
    Adapter listener = new Adapter() {
      public void onInsert(int index, int length) {
        addElements(model.subList(index, index + length));
      }

      public void onRemove(int index, int length, RemovedEvent event) {
        removeElements(event.getAllRemoved());
      }

      public void onItemsUpdated(UpdateEvent event) {
        myModel.updateAll(event.collectUpdated(model));
      }
    };
    Detach detach = model.addListener(listener);
    int size = model.getSize();
    if (size > 0)
      addElements(model.toList());
    myListenModels.put(model, detach);
  }

  public void stopListen(AListModel<? extends T> model) {
    Detach detach = myListenModels.remove(model);
    if (detach == null)
      return;
    detach.detach();
    if (model.getSize() > 0)
      removeElements(model.toList());
  }

  @NotNull
  public static <T> UniqueListModel<T> create() {
    return new UniqueListModel<T>();
  }

  @ThreadAWT
  public void clear() {
    Threads.assertAWTThread();
    for (Detach detach : myListenModels.values())
      detach.detach();
    myInstanceCounter.clear();
    myListenModels.clear();
    myModel.clear();
  }
}
