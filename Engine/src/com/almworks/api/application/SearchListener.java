package com.almworks.api.application;

public interface SearchListener {
  /**
   * Search is finished - DB, remote searches are completed, the set of items is actual for the moment, however it
   * may change in future
   * @param result
   */
  void onSearchCompleted(SearchResult result);

  /**
   * UI representation is disposed for the search.
   * @param result
   */
  void onSearchClosed(SearchResult result);
}
