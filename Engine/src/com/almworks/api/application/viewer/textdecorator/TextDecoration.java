package com.almworks.api.application.viewer.textdecorator;

import com.almworks.util.ui.actions.AnAction;
import org.almworks.util.TypedKey;

import java.util.List;

public interface TextDecoration {
  TypedKey<TextDecoration> ATTRIBUTE = TypedKey.create("textDecoration");

  int getOffset();

  int getLength();

  AnAction getDefaultAction();

  List<AnAction> getNotDefaultActions();

  /**
   * @return {@link #getOffset()} + {@link #getLength()}
   */
  int getEndOffset();
}
