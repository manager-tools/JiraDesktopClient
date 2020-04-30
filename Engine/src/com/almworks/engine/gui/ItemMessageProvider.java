package com.almworks.engine.gui;

import com.almworks.api.application.ModelMap;
import org.almworks.util.detach.Lifespan;

public interface ItemMessageProvider {
  void attachMessages(Lifespan life, ModelMap model, ItemMessages itemMessages);
}
