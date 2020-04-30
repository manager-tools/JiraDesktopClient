package com.almworks.util.components.recent;

import com.almworks.util.advmodel.AComboboxModel;
import com.almworks.util.advmodel.SelectionListener;
import com.almworks.util.components.AComboBox;
import com.almworks.util.ui.UIUtil;
import org.almworks.util.Util;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.Lifespan;

import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

public class AddRecentFromComboBox<T> extends SelectionListener.Adapter implements PopupMenuListener, FocusListener {
  private final RecentController<? super T> myRecents;
  private final JComboBox myComboBox;
  private final AComboboxModel<T> myModel;
  private T myLastSelection;
  private T myRecentOnFocusLost = null;

  private AddRecentFromComboBox(RecentController<T> recents, JComboBox comboBox, AComboboxModel<T> model) {
    myRecents = recents;
    myComboBox = comboBox;
    myModel = model;
    myLastSelection = myModel.getSelectedItem();
  }

  @Override
  public void onSelectionChanged() {
    if (myComboBox.isFocusOwner()) {
      T selected = myModel.getSelectedItem();
      myRecentOnFocusLost = shouldAddRecent(selected, false) ? selected : null;
    } else
      maybeAddRecent(false);
  }

  private void maybeAddRecent(boolean ignorePopupVisibility) {
    T selected = myModel.getSelectedItem();
    if (shouldAddRecent(selected, ignorePopupVisibility)) {
      myRecents.addToRecent(selected);
      myLastSelection = selected;
    }
  }

  private boolean shouldAddRecent(T selected, boolean ignorePopupVisibility) {
    if (Util.equals(selected, myRecents.getInitial())) return false;
    if (!ignorePopupVisibility && myComboBox.isPopupVisible()) return false;
    if (selected == null || Util.equals(selected, myLastSelection)) return false;
    return true;
  }

  public static <T> void install(Lifespan life, RecentController<? super T> recents, AComboBox<T> comboBox) {
    JComboBox combobox = comboBox.getCombobox();
    AComboboxModel<T> model = comboBox.getModel();
    install(life, recents, combobox, model);
  }

  public static <T> AddRecentFromComboBox install(Lifespan life, RecentController<? super T> recents, final JComboBox combobox, AComboboxModel<T> model) {
    final AddRecentFromComboBox listener = new AddRecentFromComboBox(recents, combobox, model);
    model.addSelectionListener(life, listener);
    combobox.addPopupMenuListener(listener);
    UIUtil.addFocusListener(life, combobox, listener);
    life.add(new Detach() {
      @Override
      protected void doDetach() throws Exception {
        combobox.removePopupMenuListener(listener);
      }
    });
    return listener;
  }

  public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
  }

  public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
    onPopupHidden();
  }

  public void popupMenuCanceled(PopupMenuEvent e) {
    onPopupHidden();
  }

  public void focusGained(FocusEvent e) {

  }

  public void focusLost(FocusEvent e) {
    if (myRecentOnFocusLost != null) {
      if (shouldAddRecent(myRecentOnFocusLost, true)) myRecents.addToRecent(myRecentOnFocusLost);
      myRecentOnFocusLost = null;
    }
  }

  private void onPopupHidden() {
    maybeAddRecent(true);
  }
}
