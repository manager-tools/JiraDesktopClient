package com.almworks.api.application.tree;

import org.jetbrains.annotations.Nullable;

public interface NoteNode extends GenericNode {
  NoteNode setName(String name);

  void setHtmlText(@Nullable String text);

  @Nullable
  String getHtmlText();

  String getName();

  NoteNode setOrder(int order);
}
