package com.almworks.util.ui;

import com.almworks.util.ui.actions.AnAction;

import java.util.List;

/**
 * @author : Dyoma
 */
public interface ActionsOwner {
  List<? extends AnAction> getActions();
}
