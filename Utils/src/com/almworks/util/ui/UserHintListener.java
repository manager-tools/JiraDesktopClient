package com.almworks.util.ui;

/**
 * This is an interface that may be used in between a component and window manager to provide
 * hints about current action.
 *
 * @author sereda
 */
public interface UserHintListener {
  ComponentProperty<String> TEXT_HINT = ComponentProperty.createProperty("TEXT_HINT");
  ComponentProperty<UserHintListener> USER_HINT_LISTENER = ComponentProperty.createProperty("USER_HINT_LISTENER");

  void onTextHintAvailable(Object source, String hint);

  void onHintsUnavailable(Object source);
}
