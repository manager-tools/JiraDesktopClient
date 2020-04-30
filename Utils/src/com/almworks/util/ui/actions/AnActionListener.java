package com.almworks.util.ui.actions;

/**
 * @author : Dyoma
 */
public interface AnActionListener {
  void perform(ActionContext context) throws CantPerformException;

  AnActionListener DEAF = new AnActionListener() {
    public void perform(ActionContext context) throws CantPerformException {
    }
  };
}
