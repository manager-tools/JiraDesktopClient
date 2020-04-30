package com.almworks.util.advmodel;

import com.almworks.util.exec.ThreadGate;

import java.util.Collection;

/**
 * @author dyoma
 */
public class OrderListModelGate<T> {
  private final OrderListModel<T> myModel;
  private final ThreadGate myGate;

  public OrderListModelGate(OrderListModel<T> model, ThreadGate gate) {
    myModel = model;
    myGate = gate;
  }

  public static <T> OrderListModelGate<T> create(OrderListModel<T> model, ThreadGate gate) {
    return new OrderListModelGate<T>(model, gate);
  }

  public void addAll(final Collection<? extends T> elements) {
    myGate.execute(new Runnable() {
      public void run() {
        myModel.addAll(elements);
      }
    });
  }

  public void updateElement(final T element) {
    myGate.execute(new Runnable() {
      public void run() {
        myModel.updateElement(element);
      }
    });
  }
}
