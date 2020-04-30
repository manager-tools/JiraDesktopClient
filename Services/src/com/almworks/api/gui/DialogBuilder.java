package com.almworks.api.gui;

import com.almworks.util.ui.UIComponentWrapper;
import com.almworks.util.ui.actions.AnAction;
import com.almworks.util.ui.actions.AnActionListener;
import com.almworks.util.ui.actions.CantPerformException;
import org.almworks.util.detach.Detach;

import javax.swing.*;

/**
 * @author : Dyoma
 */
public interface DialogBuilder extends BasicWindowBuilder<UIComponentWrapper> {
  AnAction setOkAction(AnAction okAction);

  AnAction setOkCancelAction(AnAction okCancelAction);

  void setCancelAction(AnAction cancelAction);

  void setEmptyCancelAction();

  AnAction setEmptyOkAction();

  AnAction setEmptyOkCancelAction();

  void setResizable(boolean resizable);

  void addAction(AnAction action);

  void addCloseAction(String name);

  void setCancelAction(String name);

  DialogBuilder setModal(boolean modal);

  Detach addOkListener(AnActionListener listener);

  Detach addCancelListener(AnActionListener listener);

  void setBottomLineComponent(JComponent component);

  void setBottomBevel(boolean bottomBevel);

  void closeWindow() throws CantPerformException;

  void pressOk();

  void setAlwaysOnTop(boolean alwaysOnTop);

  void setNullOwner(boolean nullOwner);

  void setBorders(boolean borders);

  void setBottomLineShown(boolean shown);
}
