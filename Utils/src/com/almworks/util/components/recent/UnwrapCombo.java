package com.almworks.util.components.recent;

import com.almworks.util.advmodel.AComboboxModel;
import com.almworks.util.advmodel.ConvertingListDecorator;
import com.almworks.util.advmodel.SelectionListener;
import org.almworks.util.Util;
import org.almworks.util.detach.Lifespan;

public class UnwrapCombo<T> extends ConvertingListDecorator<Object, T> implements AComboboxModel<T> {
  public UnwrapCombo(AComboboxModel<?> domainList) {
    super(domainList, RecentController.UNWRAPPER);
  }

  public T getSelectedItem() {
    return RecentController.<T>unwrap(getDomainModel().getSelectedItem());
  }

  public void setSelectedItem(T item) {
    selectRecent(getDomainModel(), item);
  }

  public void addSelectionListener(Lifespan life, SelectionListener listener) {
    getDomainModel().addSelectionListener(life, listener);
  }

  public void addSelectionChangeListener(Lifespan life, com.almworks.util.collections.ChangeListener listener) {
    getDomainModel().addSelectionChangeListener(life, listener);
  }

  @Override
  protected AComboboxModel<Object> getDomainModel() {
    return (AComboboxModel<Object>) super.getDomainModel();
  }

  public void selectRecent() {
    AComboboxModel<Object> model = getDomainModel();
    selectRecent(model, model.getSelectedItem());
  }

  public static void selectRecent(AComboboxModel<?> model, Object arg) {
    Object selection = arg;
    for (int i = 0; i < model.getSize(); i++) {
      Object wrapper = model.getAt(i);
      if (Util.equals(arg, RecentController.unwrap(wrapper))) {
        selection = wrapper;
        break;
      }
    }
    ((AComboboxModel<Object>) model).setSelectedItem(selection);
  }

  public static <T> T getUnwrapSelected(AComboboxModel<T> model) {
    return RecentController.<T>unwrap(model.getSelectedItem());
  }
}
