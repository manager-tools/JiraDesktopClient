package com.almworks.util.components;

import com.almworks.util.advmodel.AListModel;
import com.almworks.util.collections.Modifiable;
import com.almworks.util.collections.SimpleModifiable;
import com.almworks.util.ui.InlineLayout;
import com.almworks.util.ui.UIUtil;
import com.almworks.util.ui.actions.AnAction;
import com.almworks.util.ui.actions.DataRole;
import com.almworks.util.ui.actions.dnd.ContextTransfer;
import org.almworks.util.Collections15;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.util.Map;

import static com.almworks.util.ui.swing.Shortcuts.ksPlain;

public class ListWithAddRemoveButtons<T> extends JPanel {
  public static final int ADD_SORTKEY = 100;
  public static final int REM_SORTKEY = 200;

  private final AToolbarButton myAddButton = new AToolbarButton();
  private final AToolbarButton myRemoveButton = new AToolbarButton();
  private final AList<T> mySelectedList = new AList<T>();
  private final AScrollPane mySelectedListScrollpane;
  private final SimpleModifiable myInternalModifiable = new SimpleModifiable();
  private final SimpleModifiable myUserModifiable = new SimpleModifiable();
  private final JPanel myButtonPanel = new JPanel(new InlineLayout(InlineLayout.VERTICAL));
  private final Map<Integer, AToolbarButton> mySortedActions = Collections15.treeMap();

  public ListWithAddRemoveButtons() {
    super(new BorderLayout());
    mySelectedListScrollpane = createScrollPane(mySelectedList);
    addChildComponents();
    setMinimumSize(new Dimension(0, 0));
    setupKeyboardShortcuts();
    handleListSelectionAndFocus();
    setupScrollPane();
  }

  protected AScrollPane createScrollPane(AList<T> list) {
    return new AScrollPane(list);
  }

  private void addChildComponents() {
    add(mySelectedListScrollpane, BorderLayout.CENTER);
    add(myButtonPanel, BorderLayout.EAST);
  }

  private void setupKeyboardShortcuts() {
    final JComponent list = mySelectedList.getSwingComponent();
    list.addKeyListener(
      UIUtil.pressButtonWithKeyStroke(myAddButton,
        ksPlain(KeyEvent.VK_INSERT), ksPlain(KeyEvent.VK_PLUS),
        ksPlain(KeyEvent.VK_EQUALS), ksPlain(KeyEvent.VK_ADD)));
    list.addKeyListener(
      UIUtil.pressButtonWithKeyStroke(myRemoveButton,
        ksPlain(KeyEvent.VK_DELETE), ksPlain(KeyEvent.VK_MINUS),
        ksPlain(KeyEvent.VK_UNDERSCORE), ksPlain(KeyEvent.VK_SUBTRACT)));
  }

  private void handleListSelectionAndFocus() {
    final JList list = mySelectedList.getScrollable();
    list.setFocusable(true);
    list.addFocusListener(
      new SelectionStashingListFocusHandler<T>(mySelectedList.getSelectionAccessor()) {
        @Override
        protected boolean shouldHandle(FocusEvent e) {
          return !isMyComponent(e.getOppositeComponent());
        }
      });
  }

  protected boolean isMyComponent(Component component) {
    if (component == null) {
      return false;
    }
    return component == this || component == myAddButton || component == myRemoveButton ||
      component == mySelectedListScrollpane || component == mySelectedListScrollpane.getViewport() ||
      component == mySelectedList || component == mySelectedList.getSwingComponent();
  }

  private void setupScrollPane() {
    mySelectedListScrollpane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    mySelectedListScrollpane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
  }

  public void setCanvasRenderer(CanvasRenderer<? super T> renderer) {
    mySelectedList.setCanvasRenderer(renderer);
  }

  public void setEnabled(boolean enabled) {
    super.setEnabled(enabled);
    mySelectedListScrollpane.setEnabled(enabled);
    mySelectedList.setEnabled(enabled);
    myInternalModifiable.fireChanged();
  }

  protected Modifiable getInternalModifiable() {
    return myInternalModifiable;
  }

  /**
   * @see com.almworks.util.components.AScrollPane#setAdaptiveVerticalScroll(boolean)
   */
  public void setAdaptiveVerticalScroll(boolean adaptive) {
    mySelectedListScrollpane.setAdaptiveVerticalScroll(adaptive);
  }

  /**
   * @see com.almworks.util.components.AScrollPane#setAdaptiveVerticalScroll(boolean)
   */
  public boolean isAdaptiveVerticalScroll() {
    return mySelectedListScrollpane.isAdaptiveVerticalScroll();
  }

  public void setPrototypeValue(T prototype) {
    mySelectedList.setPrototypeCellValue(prototype);
  }

  public int getVisibleRowCount() {
    return mySelectedList.getVisibleRowCount();
  }

  public void setAddAction(AnAction action) {
    setAction(ADD_SORTKEY, action);
  }

  public void setRemoveAction(AnAction action) {
    setAction(REM_SORTKEY, action);
  }

  public AToolbarButton setAction(int sortKey, AnAction action) {
    final AToolbarButton button = getButton(sortKey);
    button.setAnAction(action);
    mySortedActions.put(sortKey, button);
    recreateActionBar();
    return button;
  }

  private AToolbarButton getButton(int sortKey) {
    if(sortKey == ADD_SORTKEY) {
      return myAddButton;
    } else if(sortKey == REM_SORTKEY) {
      return myRemoveButton;
    } else {
      return new AToolbarButton();
    }
  }

  private void recreateActionBar() {
    myButtonPanel.removeAll();
    for(final AToolbarButton b : mySortedActions.values()) {
      myButtonPanel.add(b);
    }
  }

  protected AToolbarButton getAddButton() {
    return myAddButton;
  }

  public void setVisibleRowCount(int rowCount) {
    mySelectedList.setVisibleRowCount(rowCount);
    invalidate();
  }

  public SelectionAccessor<T> getSelectionAccessor() {
    return mySelectedList.getSelectionAccessor();
  }

  public void setCollectionModel(AListModel<T> m) {
    mySelectedList.setCollectionModel(m);
  }

  public JList getJList() {
    return mySelectedList.getScrollable();
  }

  public void setDataRoles(DataRole roles) {
    mySelectedList.setDataRoles(roles);
  }

  public void setTransfer(ContextTransfer transfer) {
    mySelectedList.setTransfer(transfer);
  }

  protected void fireUserModification() {
    myUserModifiable.fireChanged();
  }

  public Modifiable getUserModifiable() {
    return myUserModifiable;
  }
}
