package com.almworks.util.components.plaf.macosx;

import com.almworks.util.Env;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

/**
 * The class responsible for patching keyboard selection in
 * tables, lists, and trees on Mac OS. See PLO-371.
 * @author pzvyagin
 */
public class MacKeyboardSelectionPatch {
  private static boolean ourTablePatchInstalled = false;
  private static boolean ourListPatchInstalled = false;
  private static boolean ourTreePatchInstalled = false;

  /**
   * Patch the keyboard actions for {@code JTable}s.
   * It's enough to call this method once on any {@code JTable}
   * instance, all tables would then work the same.
   * Indeed, the method would only do the patching once,
   * so it's safe to call it many times.
   * @param table A table.
   */
  public static void install(JTable table) {
    if(ourTablePatchInstalled) {
      return;
    }

    final InputMap imap2 = table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).getParent();
    imap2.put(KeyStroke.getKeyStroke(KeyEvent.VK_HOME, InputEvent.SHIFT_DOWN_MASK), "selectFirstRowExtendSelection");
    imap2.put(KeyStroke.getKeyStroke(KeyEvent.VK_END, InputEvent.SHIFT_DOWN_MASK), "selectLastRowExtendSelection");

    if(Env.isMac()) {
      final ActionMap amap2 = table.getActionMap().getParent();
      amap2.put("selectPreviousRow", new TableActions(Actions.UP));
      amap2.put("selectNextRow", new TableActions(Actions.DN));
      amap2.put("selectFirstRowExtendSelection", new TableActions(Actions.SHIFT_HOME));
      amap2.put("selectLastRowExtendSelection", new TableActions(Actions.SHIFT_END));

//      Uncomment this and have unanchored selection.
//      amap2.put("selectPreviousRowExtendSelection", new TableActions(Actions.SHIFT_UP));
//      amap2.put("selectNextRowExtendSelection", new TableActions(Actions.SHIFT_DN));
    }

    ourTablePatchInstalled = true;
  }

  /**
   * Patch the keyboard actions for {@code JList}s.
   * It's enough to call this method once on any {@code JList}
   * instance, all lists would then work the same.
   * Indeed, the method would only do the patching once,
   * so it's safe to call it many times.
   * @param list A list.
   */
  public static void install(JList list) {
    if(!Env.isMac() || ourListPatchInstalled) {
      return;
    }

    final ActionMap map2 = list.getActionMap().getParent();
    map2.put("selectPreviousRow", new ListActions(Actions.UP));
    map2.put("selectNextRow", new ListActions(Actions.DN));

//    Uncomment this and have unanchored selection.
//    map2.put("selectPreviousRowExtendSelection", new ListActions(Actions.SHIFT_UP));
//    map2.put("selectNextRowExtendSelection", new ListActions(Actions.SHIFT_DN));

    ourListPatchInstalled = true;
  }

  /**
   * Patch the keyboard actions for {@code JTree}s.
   * It's enough to call this method once on any {@code JTree}
   * instance, all trees would then work the same.
   * Indeed, the method would only do the patching once,
   * so it's safe to call it many times.
   * @param tree A tree.
   */
  public static void install(JTree tree) {
    if(!Env.isMac() || ourTreePatchInstalled) {
      return;
    }

    final ActionMap map2 = tree.getActionMap().getParent();
    map2.put("selectPrevious", new TreeActions(Actions.UP));
    map2.put("selectNext", new TreeActions(Actions.DN));

//    Uncomment this and have unanchored selection.
//    map2.put("selectPreviousExtendSelection", new TreeActions(Actions.SHIFT_UP));
//    map2.put("selectNextExtendSelection", new TreeActions(Actions.SHIFT_DN));

    ourTreePatchInstalled = true;
  }

  /**
   * Subclass of Actions for JTables.
   */
  private static class TableActions extends Actions<JTable> {
    public TableActions(int action) {
      super(action);
    }

    protected JTable getComponent(ActionEvent e) {
      final Object source = e.getSource();
      if(source instanceof JTable) {
        return (JTable)source;
      }
      return null;
    }

    protected int getMinSelectionIndex(JTable comp) {
      return comp.getSelectionModel().getMinSelectionIndex();
    }

    protected int getMaxSelectionIndex(JTable comp) {
      return comp.getSelectionModel().getMaxSelectionIndex();
    }

    protected void setSelectionInterval(JTable comp, int from, int to) {
      comp.getSelectionModel().setSelectionInterval(from, to);
    }

    protected int getRowCount(JTable comp) {
      return comp.getRowCount();
    }

    protected void scrollToVisible(JTable comp, int row) {
      final Rectangle visRect = comp.getVisibleRect();
      final Rectangle rowRect = comp.getCellRect(row, 0, true);
      if(visRect != null && rowRect != null) {
        comp.scrollRectToVisible(new Rectangle(visRect.x, rowRect.y, visRect.width, rowRect.height));
      }
    }
  }

  /**
   * Subclass of Actions for JLists.
   */
  private static class ListActions extends Actions<JList> {
    private ListActions(int action) {
      super(action);
    }

    protected JList getComponent(ActionEvent e) {
      final Object source = e.getSource();
      if(source instanceof JList) {
        return (JList)source;
      }
      return null;
    }

    protected int getMinSelectionIndex(JList comp) {
      return comp.getSelectionModel().getMinSelectionIndex();
    }

    protected int getMaxSelectionIndex(JList comp) {
      return comp.getSelectionModel().getMaxSelectionIndex();
    }

    protected void setSelectionInterval(JList comp, int from, int to) {
      comp.getSelectionModel().setSelectionInterval(from, to);
    }

    protected int getRowCount(JList comp) {
      return comp.getModel().getSize();
    }

    protected void scrollToVisible(JList comp, int row) {
      final Rectangle rect = comp.getCellBounds(row, row);
      if(rect != null) {
        comp.scrollRectToVisible(rect);
      }
    }
  }

  /**
   * Subclass of Actions for JTrees.
   */
  private static class TreeActions extends Actions<JTree> {
    private TreeActions(int action) {
      super(action);
    }

    protected JTree getComponent(ActionEvent e) {
      final Object source = e.getSource();
      if(source instanceof JTree) {
        return (JTree)source;
      }
      return null;
    }

    protected int getMinSelectionIndex(JTree comp) {
      return comp.getSelectionModel().getMinSelectionRow();
    }

    protected int getMaxSelectionIndex(JTree comp) {
      return comp.getSelectionModel().getMaxSelectionRow();
    }

    protected void setSelectionInterval(JTree comp, int from, int to) {
      comp.setSelectionInterval(from, to);
    }

    protected int getRowCount(JTree comp) {
      return comp.getRowCount();
    }

    protected void scrollToVisible(JTree comp, int row) {
      comp.scrollRowToVisible(row);
    }
  }

  /**
   * The class that encapsulated all the actions we override.
   * The concrete action is given to the constructor as an int constant.
   * @param <C> Component type.
   */
  private static abstract class Actions<C extends JComponent> extends AbstractAction {
    public static final int UP = 1;
    public static final int DN = 2;
    public static final int SHIFT_UP = 3;
    public static final int SHIFT_DN = 4;
    public static final int SHIFT_HOME = 5;
    public static final int SHIFT_END = 6;

    private final int myAction;

    public Actions(int action) {
      myAction = action;
    }

    protected abstract C getComponent(ActionEvent e);
    protected abstract int getMinSelectionIndex(C comp);
    protected abstract int getMaxSelectionIndex(C comp);
    protected abstract int getRowCount(C comp);
    protected abstract void setSelectionInterval(C comp, int from, int to);
    protected abstract void scrollToVisible(C comp, int row);

    public void actionPerformed(ActionEvent e) {
      final C comp = getComponent(e);
      if(e == null) {
        return;
      }

      final int min = getMinSelectionIndex(comp);
      final int max = getMaxSelectionIndex(comp);
      final int cnt = getRowCount(comp) - 1;

      switch(myAction) {
      case UP:
        if(min < 0 && max < 0) {
          setSelectionInterval(comp, cnt, cnt);
          scrollToVisible(comp, cnt);
        } else if(min > 0) {
          setSelectionInterval(comp, min - 1, min - 1);
          scrollToVisible(comp, min - 1);
        } else if(max == cnt) {
          setSelectionInterval(comp, 0, 0);
          scrollToVisible(comp, 0);
        }
        break;

      case DN:
        if(min < 0 && max < 0) {
          setSelectionInterval(comp, 0, 0);
          scrollToVisible(comp, 0);
        } else if(max < cnt) {
          setSelectionInterval(comp, max + 1, max + 1);
          scrollToVisible(comp, max + 1);
        } else if(min == 0) {
          setSelectionInterval(comp, cnt, cnt);
          scrollToVisible(comp, cnt);
        }
        break;

      case SHIFT_UP:
        if(min > 0) {
          setSelectionInterval(comp, min - 1, max);
          scrollToVisible(comp, min - 1);
        }
        break;

      case SHIFT_DN:
        if(max < cnt) {
          setSelectionInterval(comp, min, max + 1);
          scrollToVisible(comp, max + 1);
        }
        break;

      case SHIFT_HOME:
        if(min >= 0 && max >= 0) {
          setSelectionInterval(comp, 0, max);
          scrollToVisible(comp, 0);
        }
        break;

      case SHIFT_END:
        if(min >= 0 && max >= 0) {
          setSelectionInterval(comp, min, cnt);
          scrollToVisible(comp, cnt);
        }
        break;
      }
    }
  }
}
