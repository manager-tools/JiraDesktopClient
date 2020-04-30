package com.almworks.api.application.tree;

public interface ItemsPreview {
  /**
   * @return true when this object has reached end-of-life and should be discarded. When preview is not valid,
   * request its owner for another preview.
   */
  boolean isValid();

  /**
   * @return false if preview is not available because query cannot be executed
   */
  boolean isAvailable();

  /**
   * @return the number of items in the preview set
   */
  int getItemsCount();

  /**
   * Makes this preview not valid
   */
  void invalidate();

  public class Unavailable implements ItemsPreview {
    private boolean myValid = true;

    public Unavailable() {

    }

    public synchronized boolean isValid() {
      return myValid;
    }

    public boolean isAvailable() {
      return false;
    }

    public int getItemsCount() {
      return 0;
    }

    public synchronized void invalidate() {
      myValid = false;
    }

    public String toString() {
      return "IP.U[" + myValid + "]";
    }
  }
}
