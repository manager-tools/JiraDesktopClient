package com.almworks.util.ui.widgets;

import org.almworks.util.TypedKey;

/**
 * The event data for {@link com.almworks.util.ui.widgets.EventContext#FOCUS_TRAVERSE} reason.<br>
 * The host sends this event when is looking for cell to make focused due to user requested focus traverse or no focused
 * cell is currently available.
 */
public abstract class FocusTraverse {
  public static final TypedKey<FocusTraverse> KEY = TypedKey.create("focusTraverse");
  private HostCell myChild;
  private boolean myFocusThis;

  /**
   * Used by host implementation. Calling this method by widget has same effect as ignoring the event.
   */
  public void clear() {
    myChild = null;
    myFocusThis = false;
  }

  /**
   * @return identifier of previously focus owner of candidate (if candidate rejected focus gain).
   * @see #hasPrevChild()
   */
  public abstract int getPrevChild();

  /**
   * Stop lookup and make current cell focus owner
   */
  public void focusMe() {
    myFocusThis = true;
  }

  /**
   * Set child cell as candidate to gain focus. The child is going to be asked to obtain focus or provide other candidate
   * @param cell child to be candidate. If child is inactive or null this call has no effect
   */
  public void moveToChild(HostCell cell) {
    myChild = cell;
  }

  /**
   * Used by host implementation. When event is just delivered to cell this method returns false until widget requested to focus the cell.
   */
  public boolean isFocusThis() {
    return myFocusThis;
  }

  /**
   * Used by host implementation.
   * @return
   */
  protected HostCell getChild() {
    return myChild;
  }

  /**
   * Check if focus it moving to next child (true) or deeper to descendants (false)
   */
  public abstract boolean isTraverse();

  /**
   * Check if host looking for next focusable cell (true) or previous (false)
   * @return
   */
  public abstract boolean isForward();

  /**
   * true means {@link #getPrevChild()} returns cell id but not arbitrary value
   * @return
   */
  public abstract boolean hasPrevChild();

  @Override
  public String toString() {
    String childReason = null;
    if (myChild != null)
      childReason = "to child " + myChild;
    if (myFocusThis)
      return (childReason != null ? childReason + " or " : "") + "focus this";
    return childReason != null ? childReason : "empty";
  }

  /**
   * Utility method for composite widgets. This transferes focus to first focusable child according to lookup direction.
   * This utility requires that cell has children with all id in range [lowChildId, hiChildId] (inclusive)
   * @param context pass EventContext
   */
  public final void defaultTraverse(CellContext context, int lowChildId, int hiChildId) {
    if (!isTraverse())
      focusMe();
    HostCell cell = context.getActiveCell();
    if (cell == null)
      return;
    if (hasPrevChild()) {
      int childId = getPrevChild();
      HostCell nextChild = cell.getNextChild(childId, isForward());
      if (nextChild != null)
        moveToChild(nextChild);
    } else {
      HostCell child = cell.getNextChild(isForward() ? lowChildId -1 : hiChildId + 1, isForward());
      if (child != null)
        moveToChild(child);
    }
  }
}
