package com.almworks.util.advmodel;

import org.almworks.util.detach.Detach;
import org.jetbrains.annotations.Nullable;

/**
 * @author : Dyoma
 */
public class ListModelHolder<T> extends DelegatingAListModel<T> {
//  private final FireEventSupport<Listener> myListeners = FireEventSupport.createSynchronized(Listener.class);
//  private AListModel<? extends T> myList;
  private final SegmentedListModel<T> myModel;

  public ListModelHolder() {
    this(null);
  }

  public ListModelHolder(@Nullable AListModel<? extends T> model) {
    myModel = SegmentedListModel.create(model != null ? model : AListModel.EMPTY);
  }

  protected AListModel<T> getDelegate() {
    return myModel;
  }

  public Detach setModel(@Nullable final AListModel<? extends T> model) {
    myModel.setSegment(0, model == null ? AListModel.EMPTY : model);
    if (model == null) {
      return Detach.NOTHING;
    } else {
      return new Detach() {
        protected void doDetach() {
          myModel.setSegment(0, AListModel.EMPTY);
        }
      };
    }
  }

  public static <T> ListModelHolder<T> create() {
    return new ListModelHolder<T>(null);
  }

  public static <T> ListModelHolder<T> create(@Nullable AListModel<? extends T> model) {
    return new ListModelHolder<T>(model);
  }

  public AListModel<? extends T> getModel() {
    AListModel<? extends T> segment = myModel.getSegment(0);
    return segment == null ? EMPTY : segment;
  }
}
