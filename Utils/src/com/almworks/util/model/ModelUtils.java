package com.almworks.util.model;

import com.almworks.util.Pair;
import com.almworks.util.advmodel.OrderListModel;
import com.almworks.util.collections.ChangeListener;
import com.almworks.util.collections.Convertor;
import com.almworks.util.commons.Condition;
import com.almworks.util.components.SelectionAccessor;
import com.almworks.util.exec.ThreadGate;
import org.almworks.util.Collections15;
import org.almworks.util.Log;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.DetachComposite;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;


public class ModelUtils {
  public static <T> T getImmediately(ScalarModel<T> model) {
    final Object[] placeHolder = {null};
    final boolean[] received = {false};
    model.getEventSource().addStraightListener(Lifespan.FOREVER, new ScalarModel.Adapter<T>() {
      public void onScalarChanged(ScalarModelEvent<T> event) {
        placeHolder[0] = event.getNewValue();
        received[0] = true;
      }
    });
    if (!received[0])
      throw new IllegalStateException("listener was not immediately called");
    return (T) placeHolder[0];
  }

  public static <T> T waitGet(ScalarModel<T> model) throws InterruptedException {
    return waitGet(model, 0);
  }

  public static <T> T waitGet(ScalarModel<T> model, int timeout) throws InterruptedException {
    final Object[] placeHolder = {null};
    final boolean[] received = {false};
    model.getEventSource().addStraightListener(Lifespan.FOREVER, new ScalarModel.Adapter<T>() {
      public void onScalarChanged(ScalarModelEvent<T> event) {
        synchronized (received) {
          placeHolder[0] = event.getNewValue();
          received[0] = true;
          received.notify();
        }
      }
    });
    long finishTime = System.currentTimeMillis() + timeout;
    synchronized (received) {
      while (!received[0]) {
        long waitTime = timeout == 0 ? 0 : finishTime - System.currentTimeMillis();
        if (waitTime <= 0 && timeout != 0)
          throw new IllegalStateException("timeout " + timeout + "ms");
        received.wait(waitTime);
      }
      return (T) placeHolder[0];
    }
  }

  public static <S, T> Detach convert(ThreadGate threadGate, CollectionModel<S> source,
    final CollectionModelSetter<T> target, final Convertor<S, T> convertor)
  {
    DetachComposite life = new DetachComposite();
    source.getEventSource().addListener(life, threadGate, new CollectionModel.Consumer<S>() {
      public void onScalarsAdded(CollectionModelEvent<S> event) {
        Collection<T> collection = target.getWritableCollection();
        for (int i = 0; i < event.size(); i++) {
          collection.add(convertor.convert(event.get(i)));
        }
      }

      public void onScalarsRemoved(CollectionModelEvent<S> event) {
        Collection<T> collection = target.getWritableCollection();
        for (int i = 0; i < event.size(); i++) {
          collection.remove(convertor.convert(event.get(i)));
        }
      }

      public void onContentKnown(CollectionModelEvent<S> event) {
        target.setContentKnown();
      }
    });
    return life;
  }

  public static <D, S extends D> Detach replicate(ScalarModel<S> source, final ScalarModelSetter<D> destination,
    ThreadGate gate)
  {
    assert source != null;
    assert destination != null;
    DetachComposite life = new DetachComposite();
    source.getEventSource().addListener(life, gate, new ScalarModel.Adapter<S>() {
      public void onScalarChanged(ScalarModelEvent<S> event) {
        destination.setValue(event.getNewValue());
      }
    });
    return life;
  }

  public static <T> Detach replicate(final CollectionModel<T> model, final Collection<T> writableCollection,
    ThreadGate gate)
  {
    assert model != null;
    assert writableCollection != null;
    if (model == null || writableCollection == null)
      return Detach.NOTHING;
    DetachComposite life = new DetachComposite();
    model.getEventSource().addListener(life, gate, new CollectionModel.Consumer<T>() {
      public void onScalarsAdded(CollectionModelEvent<T> event) {
        writableCollection.addAll(event.getCollection());
      }

      public void onScalarsRemoved(CollectionModelEvent<T> event) {
        writableCollection.removeAll(event.getCollection());
      }

      public void onContentKnown(CollectionModelEvent<T> event) {
      }
    });
    life.add(new Detach() {
      protected void doDetach() {
        writableCollection.removeAll(model.copyCurrent());
      }
    });
    return life;
  }

  @NotNull
  public static <T> CollectionModel<T> filterStatic(@NotNull Lifespan life, @NotNull CollectionModel<T> collection,
    @NotNull final Condition<T> condition)
  {
    final ArrayListCollectionModel<T> result = ArrayListCollectionModel.create(true, true);
    collection.getEventSource().addStraightListener(life, new CollectionModel.Adapter<T>() {
      public void onScalarsRemoved(CollectionModelEvent<T> event) {
        result.getWritableCollection().removeAll(event.getCollection());
      }

      public void onScalarsAdded(CollectionModelEvent<T> event) {
        Collection<T> writable = result.getWritableCollection();
        for (T element : event.getCollection()) {
          if (condition.isAccepted(element)) {
            writable.add(element);
          }
        }
      }
    });
    return result;
  }

  @NotNull
  public static <A, B> ScalarModel<B> convert(@NotNull Lifespan life, @NotNull ScalarModel<A> model,
    @NotNull final Convertor<A, B> convertor)
  {
    final BasicScalarModel<B> result = BasicScalarModel.create(true);
    model.getEventSource().addStraightListener(life, new ScalarModel.Adapter<A>() {
      public void onScalarChanged(ScalarModelEvent<A> event) {
        result.setValue(convertor.convert(event.getNewValue()));
      }
    });
    return result;
  }

  // todo use SetModel
  public static <T> CollectionModel<T> filterDynamicSet(Lifespan life, CollectionModel<T> model,
    final Convertor<Pair<T, Lifespan>, ScalarModel<Boolean>> filter)
  {
    final ArrayListCollectionModel<T> result = ArrayListCollectionModel.create(true, true);
    final Map<T, DetachComposite> detaches = Collections15.synchronizedHashMap();
    final Collection<T> writable = result.getWritableCollection();

    model.getEventSource().addStraightListener(life, new CollectionModel.Adapter<T>() {
      public void onScalarsAdded(CollectionModelEvent<T> event) {
        for (final T element : event.getCollection()) {
          DetachComposite elementLife = new DetachComposite();
          ScalarModel<Boolean> accepted = filter.convert(Pair.<T, Lifespan>create(element, elementLife));
          accepted.getEventSource().addStraightListener(elementLife, new ScalarModel.Adapter<Boolean>() {
            public void onScalarChanged(ScalarModelEvent<Boolean> event) {
              if (event.getNewValue()) {
                result.addIfNotContains(element);
              } else {
                writable.remove(element);
              }
            }
          });
          detaches.put(element, elementLife);
        }
      }

      public void onScalarsRemoved(CollectionModelEvent<T> event) {
        for (T element : event.getCollection()) {
          DetachComposite detach = detaches.remove(element);
          assert detach != null;
          if (detach != null)
            detach.detach();
          writable.remove(element);
        }
      }
    });

    life.add(new Detach() {
      protected void doDetach() {
        // have to use Object[], so not to bother with synchronization
        Object[] todo = detaches.values().toArray();
        detaches.clear();
        for (Object detach : todo) {
          ((Detach) detach).detach();
        }
      }
    });

    // todo may be a scenario where detaches are cleared, while model listener is still active,
    // so detaches will get additional values after the last detach

    return result;
  }


  public static DetachComposite whenTrue(ScalarModel<Boolean> model, ThreadGate gate, final Runnable code) {
    final DetachComposite detach = new DetachComposite(true);
    model.getEventSource().addListener(detach, gate, new ScalarModel.Adapter<Boolean>() {
      public void onScalarChanged(ScalarModelEvent<Boolean> event) {
        Boolean value = event.getNewValue();
        if (value != null && value) {
          try {
            detach.detach();
          } catch (Exception e) {
            Log.error(e);
          }
          code.run();
        }
      }
    });
    return detach;
  }

  public static <T> ScalarModel<T> toScalarModel(Lifespan life, final SelectionAccessor<T> selection, final T nullValue) {
    final BasicScalarModel<T> result = BasicScalarModel.create(true);
    ChangeListener listener = new ChangeListener() {
      public void onChange() {
        result.setValue(selection.hasSelection() ? selection.getSelection() : nullValue);
      }
    };
    selection.addAWTChangeListener(life, listener);
    listener.onChange();
    return result;
  }

  public static <T> void syncModel(OrderListModel<T> model, List<T> newElements, Comparator<? super T> comp) {
    int mi = 0;
    for (T w : newElements) {
      int c = 1;
      while (mi < model.getSize()) {
        T wm = model.getAt(mi);
        c = comp.compare(wm, w);
        if (c == 0 && !w.equals(wm)) {
          // replace
          model.removeAt(mi);
          c = 1;
        }
        if (c >= 0) {
          break;
        }
        model.removeAt(mi);
      }
      if (c != 0) {
        model.insert(mi, w);
      }
      mi++;
    }
    if (mi < model.getSize()) {
      model.removeRange(mi, model.getSize() - 1);
    }
  }

  /** Creates a consumer that calls listener on {@link com.almworks.util.model.ScalarModel.Consumer#onScalarChanged(ScalarModelEvent) value change}*/
  public static <T> ScalarModel.Consumer<T> toConsumer(final ChangeListener l) {
    return new ScalarModel.Adapter<T>() {
      @Override
      public void onScalarChanged(ScalarModelEvent<T> event) {
        l.onChange();
      }

      @Override
      public void onContentKnown(ScalarModelEvent<T> tScalarModelEvent) {
        l.onChange();
      }
    };
  }
}
