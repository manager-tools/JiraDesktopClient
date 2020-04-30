package com.almworks.api.gui;

import com.almworks.util.Enumerable;
import com.almworks.util.ui.UIComponentWrapper;
import org.almworks.util.detach.Detach;

/**
 * Status bar is organized into N + 1 sections.
 * There are N sections that occupy right-aligned area of status bar.
 * There's one "context" section that is left-aligned and takes all remaining space.
 * Each section has its own border. Context section does not contain border.
 * Each section may accommodate several components (they appear within the same border, in a
 * straing line layout).
 *
 */
public interface StatusBar {
  Detach addComponent(Section section, StatusBarComponent component);

  Detach addComponent(Section section, StatusBarComponent component, double weight);

  Detach showContextComponent(UIComponentWrapper component);

  class Section extends Enumerable {
    public static final Section MEMORY_BAR = new Section("MEMORY_BAR");
    public static final Section SYNCHRONIZATION_STATUS = new Section("SYNCHRONIZATION_STATUS");
    public static final Section ARTIFACT_STATS = new Section("ARTIFACT_STATS");
    public static final Section MESSAGES = new Section("MESSAGES");

    private Section(String name) {
      super(name);
    }
  }
}
