package com.almworks.engine.gui.attachments.content;

import com.almworks.api.config.MiscConfig;
import com.almworks.util.config.Configuration;
import org.almworks.util.detach.Lifespan;

import javax.swing.*;

public interface ContentView {

  void init(Lifespan life, Configuration config, MiscConfig miscConfig);

  void copyToClipboard();

  JComponent getComponent();
}
