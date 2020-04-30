package com.almworks.api.application.viewer.textdecorator;

import com.almworks.util.LogHelper;
import com.almworks.util.properties.Role;

import java.util.Collection;
import java.util.Collections;

public interface TextDecoratorRegistry {
  Role<TextDecoratorRegistry> ROLE = Role.role(TextDecoratorRegistry.class);

  void addParser(TextDecorationParser parser);

  Collection<? extends TextDecoration> processText(String text);

  /**
   * This class exists to hack around component creation with IntelliJ UI designer. When TextDecoratorRegistry is required before constructor called.
   */
  class Delegating implements TextDecoratorRegistry {
    private TextDecoratorRegistry myDelegate;

    public Delegating() {
      LogHelper.debug("TextDecoratorRegistry.Delegating init", this);
    }

    public void setDelegate(TextDecoratorRegistry delegate) {
      LogHelper.debug("TextDecoratorRegistry.Delegating setDelegate", this, delegate);
      myDelegate = delegate;
    }

    @Override
    public void addParser(TextDecorationParser parser) {
      if (myDelegate != null) myDelegate.addParser(parser);
      else LogHelper.error("No delegate");
    }

    @Override
    public Collection<? extends TextDecoration> processText(String text) {
      if (myDelegate != null) return myDelegate.processText(text);
      else {
        LogHelper.error("No delegate");
        return Collections.emptyList();
      }
    }
  }
}
