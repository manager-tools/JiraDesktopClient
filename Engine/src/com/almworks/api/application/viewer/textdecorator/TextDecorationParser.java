package com.almworks.api.application.viewer.textdecorator;

import com.almworks.util.ui.actions.AnAction;

import java.util.regex.Matcher;

public interface TextDecorationParser {
  void decorate(Context context);

  interface Context {
    String getText();

    LinkArea addLink(int offset, int length);

    LinkArea addLink(Matcher matcher);
  }

  interface LinkArea {
    void setDefaultAction(AnAction action);

    void addActions(AnAction... actions);

    String getText();
  }
}
