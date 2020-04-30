package com.almworks.api.search;

import org.jetbrains.annotations.Nullable;

public interface TextSearchType {
  @Nullable
  TextSearchExecutor parse(String searchString);

  String getDisplayableShortName();

  int getWeight();

  interface Weight {
    int WORD_SEARCH = Integer.MIN_VALUE;

    int ID_SEARCH = 100;
  }
}
