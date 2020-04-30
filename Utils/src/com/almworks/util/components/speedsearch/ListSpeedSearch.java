package com.almworks.util.components.speedsearch;

import com.almworks.util.advmodel.AListModel;
import com.almworks.util.collections.ChangeListener;
import com.almworks.util.components.*;
import com.almworks.util.components.renderer.CellState;
import com.almworks.util.ui.ComponentProperty;
import com.almworks.util.ui.UIUtil;
import com.almworks.util.ui.swing.EventDispatchCache;
import com.almworks.util.ui.swing.SwingTreeUtil;
import org.almworks.util.Log;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class ListSpeedSearch<T> extends KeyAdapter {
  private static final ComponentProperty<ListSpeedSearch<?>> CONTROLLER = ComponentProperty.createProperty("controller");
  private final FlatCollectionComponent<T> myList;
  private final Provider<T> myProvider;
  private boolean myCaseSensitive = false;
  private boolean mySearchSubstring = false;
  private boolean myIgnoreSpace = false;
  private PopupController mySearchPopup;

  private ListSpeedSearch(FlatCollectionComponent<T> aList, Provider<T> provider) {
    myList = aList;
    myProvider = provider;
  }

  public void setCaseSensitive(boolean caseSensitive) {
    myCaseSensitive = caseSensitive;
  }

  public void setIgnoreSpace(boolean ignoreSpace) {
    myIgnoreSpace = ignoreSpace;
  }

  public void setSearchSubstring(boolean searchSubstring) {
    mySearchSubstring = searchSubstring;
  }

  public boolean isSpeedSearchFocused() {
    return mySearchPopup != null && mySearchPopup.isFocused();
  }

  public boolean ownsComponent(Component c) {
    return c != null && mySearchPopup != null && mySearchPopup.ownsComponent(c);
  }

  @Override
  public void keyTyped(KeyEvent e) {
    if (mySearchPopup != null) {
      mySearchPopup.hidePopup(false);
      Log.warn("SSC: no search popup");
    }
    int modifiers = e.getModifiersEx();
    if ((modifiers & (InputEvent.CTRL_DOWN_MASK | InputEvent.ALT_DOWN_MASK | InputEvent.META_DOWN_MASK)) != 0) return;
    if (!myProvider.isApplicableTo(myList) || myList.getCollectionModel().getSize() == 0) return;
    char letter = e.getKeyChar();
    int type = Character.getType(letter);
    if (type == Character.CONTROL) return;
    if (myIgnoreSpace && Character.isSpaceChar(letter)) return;
    openPopup(letter);
  }

  private void openPopup(char letter) {
    PopupController controller = new PopupController();
    controller.myPopup = SpeedSearchPopup.open(myList.getSwingComponent(), controller);
    controller.watchModel(myList);
    controller.myPopup.addLetter(letter);
    mySearchPopup = controller;
  }

  @NotNull
  public static <T> ListSpeedSearch<T> install(BaseAList<T> list) {
    ListSpeedSearch<T> controller = (ListSpeedSearch<T>) CONTROLLER.getClientValue(list);
    if (controller != null) return controller;
    controller = new ListSpeedSearch<T>(list, new ListCanvasSpeedSearchProvider<T>());
    if (list.getCanvasRenderer() == null) {
      assert false; // Current implementation won't work without CanvasRenderer. Install speed-search after CanvasRenderer is provided.
      return controller;
    }
    CONTROLLER.putClientValue(list, controller);
    list.getSwingComponent().addKeyListener(controller);
    list.setSwingSpeedSearchEnabled(false);
    if (list instanceof ACheckboxList) controller.setIgnoreSpace(true);
    return controller;
  }

  public static <T> ListSpeedSearch<T> install(ATable<T> table, int column, CanvasRenderer<T> renderer) {
    ListSpeedSearch<T> controller = (ListSpeedSearch<T>) CONTROLLER.getClientValue(table);
    if (controller != null) return controller;
    controller = new ListSpeedSearch<T>(table, new TableColumnSearchProvider<T>(renderer, column));
    CONTROLLER.putClientValue(table, controller);
    table.getSwingComponent().addKeyListener(controller);
    return controller;
  }

  @Nullable
  public static ListSpeedSearch getInstalled(Component component) {
    if (component instanceof JComponent) {
      ListSpeedSearch<?> controller = CONTROLLER.getClientValue((JComponent) component);
      if (controller != null) return controller;
    }
    FlatCollectionComponent collectionComponent =
      SwingTreeUtil.findAncestorOfType(component, FlatCollectionComponent.class);
    if (collectionComponent == null) return null;
    JComponent jComponent = collectionComponent.toComponent();
    return jComponent != null ? CONTROLLER.getClientValue(jComponent) : null;
  }

  private boolean matches(String text, String search) {
    if (mySearchSubstring)
      return text.indexOf(search) >= 0;
    else {
      if (myIgnoreSpace) text = text.trim();
      return text.startsWith(search);
    }
  }

  public static boolean fixFocusedState(JComponent component, boolean cellHasFocus, int row, int column) {
    if (EventDispatchCache.isFocusOwner(component) || !isFocusOwner(component)) return cellHasFocus;
    for (Container ancestor = component; ancestor != null; ancestor = ancestor.getParent()) {
      if (ancestor instanceof JList) {
        JList list = (JList) ancestor;
        return list.getLeadSelectionIndex() == row;
      } else if (ancestor instanceof JTable) {
        JTable table = (JTable) ancestor;
        return table.getSelectionModel().getLeadSelectionIndex() == row && table.getColumnModel().getSelectionModel().getLeadSelectionIndex() == column;
      } else if (ancestor instanceof JTree) {
        JTree tree = (JTree) ancestor;
        return tree.getLeadSelectionRow() == row;
      }
    }
    return cellHasFocus;
  }

  public static boolean isFocusOwner(Component component) {
    if (EventDispatchCache.isFocusOwner(component)) return true;
    ListSpeedSearch search = getInstalled(component);
    return search != null && search.isSpeedSearchFocused();
  }

  private class PopupController implements SpeedSearchPopup.Controller {
    private SpeedSearchPopup myPopup;

    private PopupController() {
    }

    private void watchModel(FlatCollectionComponent<?> cc) {
      cc.getCollectionModel().addAWTChangeListener(myPopup.getLife(), new ChangeListener() {
        @Override
        public void onChange() {
          hidePopup(true);
        }
      });
      cc.getSelectionAccessor().addAWTChangeListener(myPopup.getLife(), new ChangeListener() {
        @Override
        public void onChange() {
          if (myPopup.isSearching()) return;
          hidePopup(true);
        }
      });
    }

    public void speedSearchClosed() {
      mySearchPopup = null;
    }

    public boolean searchText(String search, int direction) {
      if (direction < -1) direction = -1;
      else if (direction > 1) direction = 1;
      SelectionAccessor<?> accessor = myList.getSelectionAccessor();
      AListModel<? extends T> model = myList.getCollectionModel();
      if (model.getSize() == 0) return false;
      if (!myCaseSensitive) search = Util.lower(search);
      int index = accessor.getSelectedIndex();
      if (index < 0) index = direction >= 0 ? 0 : model.getSize() - 1;
      if (index >= model.getSize()) index = direction >= 0 ? model.getSize() - 1 : 0;
      index += direction;
      int nextIndex = -1;
      if (direction == 0) direction = 1;
      for (int i = 0; i < model.getSize(); i++) {
        int pos = (2*model.getSize() + index + i * direction) % model.getSize();
        T item = model.getAt(pos);
        String text = myProvider.getItemText(myList, item);
        if (!myCaseSensitive) text = text.toLowerCase();
        if (matches(text, search)) {
          nextIndex = pos;
          break;
        }
      }
      if (nextIndex < 0) return false;
      accessor.setSelectedIndex(nextIndex);
      if(myList instanceof ACheckboxList) {
        // kludge: ACheckboxList.scrollSelectionToView() uses
        // checkbox selection, not list selection.
        final ACheckboxList list = (ACheckboxList) myList;
        UIUtil.ensureRectVisiblePartially(list, list.getScrollable().getCellBounds(nextIndex, nextIndex));
      } else {
        myList.scrollSelectionToView();
      }
      return true;
    }

    public boolean maybeStopSearch(SpeedSearchPopup popup, KeyEvent e) {
      if (isStopSearch(e)) {
        popup.hidePopup(true);
        return true;
      }
      return false;
    }

    private boolean isStopSearch(KeyEvent e) {
      int keyCode = e.getKeyCode();
      if (keyCode == KeyEvent.VK_ESCAPE) return true;
      if (keyCode == KeyEvent.VK_ENTER) {
        myList.getSwingComponent().dispatchEvent(e);
        return true;
      }
      if (myIgnoreSpace && keyCode == KeyEvent.VK_SPACE) {
        myList.getSwingComponent().dispatchEvent(e);
        return true;
      }
      return false;
    }

    public boolean isFocused() {
      return myPopup.isFocused();
    }

    public void hidePopup(boolean returnFocus) {
      myPopup.hidePopup(returnFocus);
    }

    public boolean ownsComponent(Component c) {
      return myPopup.ownsComponent(c);
    }
  }

  public interface Provider<T> {
    /**
     * @return true if speedsearch is applicable to the component in current state
     */
    boolean isApplicableTo(FlatCollectionComponent<T> component);

    String getItemText(FlatCollectionComponent<T> component, T item);
  }


  private static class ListCanvasSpeedSearchProvider<T> implements Provider<T> {
    private static final ListCanvasSpeedSearchProvider INSTANCE = new ListCanvasSpeedSearchProvider();
    private static final ThreadLocal<PlainTextCanvas> TEXT_CANVAS = new PlainTextCanvas.ThreadLocalFactory();

    public boolean isApplicableTo(FlatCollectionComponent<T> component) {
      return getCanvasRenderer(component) != null;
    }

    @Nullable
    private CanvasRenderer<? super T> getCanvasRenderer(FlatCollectionComponent<T> component) {
      if (!(component instanceof BaseAList)) return null;
      return ((BaseAList<T>) component).getCanvasRenderer();
    }

    public String getItemText(FlatCollectionComponent<T> component, T item) {
      CanvasRenderer<? super T> renderer = getCanvasRenderer(component);
      if (renderer == null) return "";
      return getItemText(renderer, item);
    }

    private static <T> String getItemText(CanvasRenderer<? super T> renderer, T item) {
      PlainTextCanvas textCanvas = TEXT_CANVAS.get();
      textCanvas.clear();
      renderer.renderStateOn(CellState.LABEL, textCanvas, item);
      return textCanvas.getText();
    }

    public static <T> ListCanvasSpeedSearchProvider<T> getInstance() {
      return INSTANCE;
    }
  }


  private static class TableColumnSearchProvider<T> implements Provider<T> {
    private final CanvasRenderer<T> myRenderer;
    private final int myColumnIndex;

    private TableColumnSearchProvider(CanvasRenderer<T> renderer, int columnIndex) {
      myRenderer = renderer;
      myColumnIndex = columnIndex;
    }

    public boolean isApplicableTo(FlatCollectionComponent<T> component) {
      return component instanceof ATable && ((ATable) component).getColumnModel().getSize() > myColumnIndex;
    }

    public String getItemText(FlatCollectionComponent<T> component, T item) {
      return ListCanvasSpeedSearchProvider.getItemText(myRenderer, item);
    }
  }
}