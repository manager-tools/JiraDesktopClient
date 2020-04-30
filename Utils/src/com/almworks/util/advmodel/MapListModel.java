package com.almworks.util.advmodel;

import com.almworks.util.LogHelper;
import com.almworks.util.collections.Convertor;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.threads.ThreadSafe;
import com.almworks.util.threads.Threads;
import org.almworks.util.Collections15;
import org.almworks.util.Log;

import java.util.Map;

/**
 * Wraps the list model and maintain mapping for fast lookup.<br>
 * The original model should be used for modifications only, it should not be available for outer code. Outer code should
 * use the object as list model for read and listen. This allows the mapping to be consistent with model contents. Mapping
 * is updated before any listener is notified that model has changed.<br>
 * This object can be user as AListModel from AWT thread. Element mapping can be accessed from any thread but no consistency
 * can be guarantied in this case since accessing thread cannot be sure about current model state.
 * @param <T> model elements type
 * @param <I> mapping keys type
 */
public class MapListModel<T, I> extends AbstractAListModel<T> {
  private final AListModel<? extends T> myModel;
  private final Convertor<T, I> myConvertor;
  private final Map<I, T> myMapping = Collections15.hashMap();
  private boolean myInitialized = false;

  private MapListModel(AListModel<? extends T> model, Convertor<T, I> convertor) {
    myModel = model;
    myConvertor = convertor;
  }

  @ThreadSafe
  public static <T, I> MapListModel<T, I> create(AListModel<? extends T> model, Convertor<T, I> convertor) {
    if (model == null) {
      LogHelper.error("Missing model");
      model = EMPTY;
    }
    final MapListModel<T, I> map = new MapListModel<T, I>(model, convertor);
    ThreadGate.AWT_OPTIMAL.execute(new Notifier(map));
    return map;
  }

  @Override
  public int getSize() {
    synchronized (myMapping) {
      return myInitialized ? myModel.getSize() : 0;
    }
  }

  @Override
  public T getAt(int index) {
    synchronized (myMapping) {
      if (!myInitialized) Log.error("No such index " + index);
    }
    return myModel.getAt(index);
  }

  @ThreadSafe
  public T get(I key) {
    synchronized (myMapping) {
      return myInitialized ? myMapping.get(key) : null;
    }
  }

  private void init() {
    Threads.assertAWTThread();
    synchronized (myMapping) {
      if (myInitialized) Log.error("Already initialized");
      remap();
    }
  }

  private void remap() {
    synchronized (myMapping) {
      myMapping.clear();
      for (int i = 0; i < myModel.getSize(); i++) {
        T element = myModel.getAt(i);
        if (element != null) {
          I key = myConvertor.convert(element);
          if (key != null) {
            T expunged = myMapping.put(key, element);
            if (expunged != null) Log.warn("LMMC: " + key + " " + element + " " + expunged);
          }
        }
      }
      myInitialized = true;
    }
  }

  @SuppressWarnings( {"unchecked"})
  private static class Notifier implements Runnable, Listener, RemovedElementsListener {
    private final MapListModel<?, ?> myMap;

    public Notifier(MapListModel<?, ?> map) {
      myMap = map;
    }

    @Override
    public void run() {
      myMap.init();
      myMap.myModel.addListener(this);
      myMap.myModel.addRemovedElementListener(this);
    }

    @Override
    public void onInsert(int index, int length) {
      myMap.remap();
      myMap.fireInsert(index, length);
    }

    @Override
    public void onListRearranged(AListEvent event) {
      myMap.remap();
      myMap.fireRearrange(event);
    }

    @Override
    public void onItemsUpdated(UpdateEvent event) {
      myMap.remap();
      myMap.fireUpdate(event);
    }

    @Override
    public void onBeforeElementsRemoved(RemoveNotice elements) {
      myMap.fireBeforeElementsRemoved(elements);
    }

    @Override
    public void onRemove(int index, int length, RemovedEvent removedEvent) {
      myMap.remap();
      myMap.fireRemoved(removedEvent);
    }
  }
}
