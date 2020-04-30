package com.almworks.api.application;

/**
 * An interface for {@link ModelKey}s supporting the notion
 * of "default" and "user-modified" state.
 */
public interface UserModifiable {
  /**
   * Returns the "modified by user" state.
   * @param modelMap The model map.
   * @return {@code true} if the value was modified by the user.
   */
  boolean isUserModified(ModelMap modelMap);

  /**
   * Sets the "modified by user" state on or off.
   * @param modelMap The model map.
   * @param modified {@code true} if the value was modified by the user.
   */
  void setUserModified(ModelMap modelMap, boolean modified);
}
