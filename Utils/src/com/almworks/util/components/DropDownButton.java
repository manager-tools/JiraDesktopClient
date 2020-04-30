package com.almworks.util.components;

import com.almworks.util.advmodel.*;
import com.almworks.util.collections.ChangeListener;
import com.almworks.util.collections.Convertor;
import com.almworks.util.commons.Condition;
import com.almworks.util.components.plaf.macosx.Aqua;
import com.almworks.util.components.renderer.CellState;
import com.almworks.util.components.renderer.Renderers;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.images.Icons;
import com.almworks.util.model.BasicScalarModel;
import com.almworks.util.model.ScalarModel;
import com.almworks.util.ui.AActionComponent;
import com.almworks.util.ui.actions.*;
import com.almworks.util.ui.swing.AwtUtil;
import org.almworks.util.Collections15;
import org.almworks.util.StringUtil;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.Lifecycle;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.Map;

/**
 * @author dyoma
 */
public class DropDownButton extends JComponent implements AActionComponent<JComponent> {
  private static final DataRole<ActionItem> ACTION_ITEM_ROLE = DataRole.createRole(ActionItem.class);

  private final AActionComponent<?> myPrimaryButton;
  private final Map<String, PresentationMapping<?>> myMapping = Collections15.hashMap();
  private final JButton myDropDown = new AToolbarButton();
  private final ListModelHolder<ActionItem> myConvertedActions = ListModelHolder.create();
  private final FilteringListDecorator<ActionItem> myFilteredActions =
    FilteringListDecorator.create(myConvertedActions);

  private final Lifecycle myAttachedActions = new Lifecycle();
  private final Lifecycle mySwingCycle = new Lifecycle(false);

  private final AnAction myUnselected;

  private final BasicScalarModel<AnAction> mySelectedActionModel = BasicScalarModel.createWithValue(null, true);

  private JComponent myLastDropDownComponent = null;
  private Component myFocusReceiver;
  private JComponent myContextComponent;
  private boolean mySetBestSelectionOnAddNotify = false;
  private boolean myAutoHideShow = false;

  private String myLastSelectionActionName;
  private AListModel<? extends AnAction> myActions;

  public DropDownButton(AActionComponent<?> primaryButton, String unselectedName) {
    myPrimaryButton = primaryButton;
    myPrimaryButton.setPresentationMapping(PresentationKey.ACTION_KEY_VISIBLE, PresentationMapping.ALWAYS_VISIBLE);
    myContextComponent = primaryButton.toComponent();
    myDropDown.setIcon(Icons.TABLE_COLUMN_SORTED_DESCENDING);
    myDropDown.setMargin(new Insets(0, 1, 0, 1));
    setLayout(new BorderLayout(0, 0));
    add(myPrimaryButton.toComponent(), BorderLayout.CENTER);
    add(myDropDown, BorderLayout.EAST);
    myDropDown.addActionListener(new MyDropDownListener());

    myUnselected = new SimpleAction(unselectedName) {
      protected void customUpdate(UpdateContext context) throws CantPerformException {
        context.setEnabled(false);
      }

      protected void doPerform(ActionContext context) throws CantPerformException {
      }
    };

    myConvertedActions.addListener(new AListModel.Adapter() {
      public void onInsert(int index, int length) {
        // start update on new items if we are in component tree
        if (mySwingCycle.isCycleStarted()) {
          for (int i = 0; i < length; i++) {
            ActionItem item = myConvertedActions.getAt(index + i);
            item.startUpdate();
          }
        }
      }
    });
    myConvertedActions.addRemovedElementListener(new AListModel.RemovedElementsListener<ActionItem>() {
      public void onBeforeElementsRemoved(AListModel.RemoveNotice<ActionItem> elements) {
        // stop update on removed items if we are in component tree
        if (mySwingCycle.isCycleStarted()) {
          for (ActionItem item : elements.getList()) {
            item.stopUpdate();
          }
        }
      }
    });

    myFilteredActions.setFilter(new Condition<ActionItem>() {
      public boolean isAccepted(ActionItem value) {
        return value.isStarted() && value.isVisible() && value.isAvailable() && value.isEnabled();
      }
    });
    ChangeListener listener = new ChangeListener() {
      public void onChange() {
        updateAutoVisibility();
        tryBestSelectOnAddNotify();
      }
    };
    myFilteredActions.addAWTChangeListener(listener);

    setSelectedAction(null);
  }

  public void setAutoHideShow(boolean auto) {
    myAutoHideShow = auto;
    updateAutoVisibility();
  }

  private void updateAutoVisibility() {
    if (!myAutoHideShow) return;
    boolean show = myFilteredActions.getSize() > 0;
    if (show != isVisible()) {
      setVisible(show);
    }
  }

  public void addNotify() {
    super.addNotify();
    if (mySwingCycle.cycleStart()) {
      for (ActionItem item : myConvertedActions) {
        item.startUpdate();
      }
      attachActions();
      tryBestSelectOnAddNotify();
    }
  }

  private void tryBestSelectOnAddNotify() {
    if (mySetBestSelectionOnAddNotify && isDisplayable()) {
      AnAction action = findBestSelection();
      if (action == null) action = myFilteredActions.getSize() > 0 ?myFilteredActions.getAt(0).getAction() : null;
      if (action != null) {
        setSelectedAction(action);
        mySetBestSelectionOnAddNotify = false;
      }
    }
  }

  public void removeNotify() {
    if (mySwingCycle.cycleEnd()) {
      myAttachedActions.cycle();
      for (ActionItem item : myConvertedActions) {
        item.stopUpdate();
      }
    }
    super.removeNotify();
  }

  public Detach setSelectedAction(@Nullable final AnAction action) {
    assert action == null || myActions.indexOf(action) >= 0;
    mySelectedActionModel.setValue(action);
    Detach result = myPrimaryButton.setAnAction(action == null ? myUnselected : action);
    if (action != null) {
      JComponent c = myPrimaryButton.toComponent();
      if (c instanceof AbstractButton) {
        myLastSelectionActionName = ((AbstractButton) c).getText();
      }
    }
    return result;
  }

  private int getActionIndex(final AnAction action) {
    return myFilteredActions.detectIndex(new Condition<ActionItem>() {
      public boolean isAccepted(ActionItem value) {
        return value.getAction() == action;
      }
    });
  }

  @Nullable
  private AnAction findBestSelection() {
    if (mySwingCycle.isCycleStarted() && myLastSelectionActionName != null) {
      int minAffinity = Integer.MAX_VALUE;
      AnAction selected = null;
      for (ActionItem item : myFilteredActions) {
        int affinity = StringUtil.calculateAffinity(item.getName().trim(), myLastSelectionActionName);
        if (affinity < minAffinity) {
          minAffinity = affinity;
          selected = item.getAction();
        }
      }
      if (selected != null) {
        return selected;
      }
    }
    return myFilteredActions.getSize() == 0 ? null : myFilteredActions.getAt(0).getAction();
  }

  @Nullable
  private AList<ActionItem> createActionsList() {
    if (myFilteredActions.getSize() == 0)
      return null;
    AList<ActionItem> view = new AList<ActionItem>();
    view.setDataRoles(ACTION_ITEM_ROLE);
    view.setCollectionModel(myFilteredActions);

    SelectionAccessor<ActionItem> accessor = view.getSelectionAccessor();
    AnAction selectedAction = mySelectedActionModel.getValue();
    if (selectedAction != null) {
      int index = getActionIndex(selectedAction);
      if (index >= 0) {
        accessor.setSelectedIndex(index);
      }
    }
    accessor.ensureSelectionExists();
    view.setCanvasRenderer(Renderers.defaultCanvasRenderer());
    return view;
  }

  public void setActions(List<? extends AnAction> actions) {
    setActions(FixedListModel.create(actions));
  }

  public void setActions(List<? extends AnAction> actions, AnAction selected) {
    setActions(FixedListModel.create(actions), selected);
  }

  public void setActions(@Nullable AListModel<? extends AnAction> actions) {
    myActions = actions;
    myAttachedActions.cycle();
    attachActions();
    if (isDisplayable()) setSelectedAction(findBestSelection());
    else mySetBestSelectionOnAddNotify = true; 
  }

  public void setActions(@Nullable AListModel<? extends AnAction> actions, @Nullable AnAction selection) {
    myActions = actions;
    myAttachedActions.cycle();
    attachActions();
    setSelectedAction(selection);
    mySetBestSelectionOnAddNotify = false;
  }

  private void attachActions() {
    if (isDisplayable()) {
      if (myActions == null || myActions == AListModel.EMPTY) {
        myConvertedActions.setModel(AListModel.EMPTY);
      } else {
        Lifespan lifespan = myAttachedActions.lifespan();
        Convertor<AnAction, ActionItem> convertor = new Convertor<AnAction, ActionItem>() {
          public ActionItem convert(AnAction action) {
            ActionItem item = new ActionItem(action, myContextComponent);
            item.overridePresentation(myMapping);
            return item;
          }
        };
        FilteringConvertingListDecorator<AnAction, ActionItem> converter =
          FilteringConvertingListDecorator.create(lifespan, myActions, Condition.<AnAction>always(), convertor);
        myConvertedActions.setModel(converter);
      }
    }
  }


  private void actionUpdated(final ActionItem item) {
    ThreadGate.AWT_QUEUED.execute(new Runnable() {
      public void run() {
        FilteringConvertingListDecorator<AnAction, ActionItem> converter = getConverter();
        if (converter != null) {
          int index = converter.indexOf(item);
          if (index >= 0) {
            converter.imageUpdated(index);
          }
        }
      }
    });
  }

  @Nullable
  private FilteringConvertingListDecorator<AnAction, ActionItem> getConverter() {
    AListModel<? extends ActionItem> model = myConvertedActions.getModel();
    if (model instanceof FilteringConvertingListDecorator) {
      return (FilteringConvertingListDecorator<AnAction, ActionItem>) model;
    } else {
      return null;
    }
  }

  public ScalarModel<AnAction> getSelectedActionModel() {
    return mySelectedActionModel;
  }

  public void updateNow() {
    myPrimaryButton.updateNow();
  }

  @Deprecated
  public Detach setAnAction(AnAction action) {
    assert false;
    return setSelectedAction(action);
  }

  @Deprecated
  public void setActionById(String actionId) {
    assert false;
    myPrimaryButton.setActionById(actionId);
  }

  public JComponent toComponent() {
    return this;
  }

  public void setContextComponent(JComponent component) {
    if (component != myContextComponent) {
      myContextComponent = component != null ? component : myPrimaryButton.toComponent();
      myPrimaryButton.setContextComponent(myContextComponent);
      FilteringConvertingListDecorator<AnAction, ActionItem> converter = getConverter();
      if (converter != null) {
        // re-set source to recreate ActionItems
        AListModel<? extends AnAction> source = converter.getSource();
        myAttachedActions.cycle();
        attachActions();
      }
    }
  }

  public void setPresentationMapping(String swingKey, PresentationMapping<?> mapping) {
    if (PresentationKey.ACTION_KEY_VISIBLE.equals(swingKey))
      return;
    myMapping.put(swingKey, mapping);
    myPrimaryButton.setPresentationMapping(swingKey, mapping);
    for (ActionItem item : myConvertedActions) {
      item.setPresentationMapping(swingKey, mapping);
    }
  }

  public void overridePresentation(Map<String, PresentationMapping> mapping) {
    for (Map.Entry<String, PresentationMapping> entry : mapping.entrySet()) {
      setPresentationMapping(entry.getKey(), entry.getValue());
    }
  }

  public void doClick() {
    if (!(myPrimaryButton instanceof AbstractButton)) {
      assert false : myPrimaryButton;
    } else {
      ((AbstractButton) myPrimaryButton).doClick();
    }
  }

  public void setFocusReceiver(Component focusReceiver) {
    myFocusReceiver = focusReceiver;
  }

  public AActionComponent<?> getPrimaryButton() {
    return myPrimaryButton;
  }

  public JButton getDropDownButton() {
    return myDropDown;
  }

  public JComponent getLastDropDownComponent() {
    return myLastDropDownComponent;
  }

  public void setRolloverEnabled(boolean rollover) {
    myDropDown.setRolloverEnabled(rollover);
    JComponent c = myPrimaryButton.toComponent();
    if (c instanceof AbstractButton)
      ((AbstractButton) c).setRolloverEnabled(rollover);
  }


  private class ActionItem extends ActionBridge implements CanvasRenderable {
    public ActionItem(AnAction action, JComponent contextComponent) {
      super(action, contextComponent);
    }

    protected void onUpdated() {
      actionUpdated(ActionItem.this);
    }

    public void renderOn(Canvas canvas, CellState state) {
      if (!isStarted()) {
        assert false : this;
        return;
      }
      if (!isEnabled()) {
        canvas.setForeground(Color.GRAY);
      }
      AnAction action = getAction();
      if (action instanceof CanvasRenderable) {
        ((CanvasRenderable) action).renderOn(canvas, state);
      } else {
        canvas.setIcon((Icon) getPresentation().getValue(Action.SMALL_ICON));
        canvas.appendText(getName());
      }
    }

    private String getName() {
      return "  " + (String) getPresentation().getValue(Action.NAME) + "  ";
    }

    @Nullable
    public String getTooltip() {
      if (!isStarted())
        return null;
      return (String) getPresentation().getValue(Action.SHORT_DESCRIPTION);
    }
  }


  private class MyDropDownListener extends DropDownListener.ForComponent {
    public MyDropDownListener() {
      super(DropDownButton.this);
    }

    protected void onDropDownHidden() {
      myLastDropDownComponent = null;
      if (myFocusReceiver != null)
        myFocusReceiver.requestFocus();
    }

    protected JComponent createPopupComponent() {
      final AList<ActionItem> view = createActionsList();
      if (view == null)
        return null;
      final MyListListener listener = new MyListListener(view);
      JComponent swingComponent = view.getSwingComponent();
      swingComponent.addKeyListener(listener);
      swingComponent.addMouseListener(listener);
      swingComponent.addMouseMotionListener(listener);
      JScrollPane scrollPane = new JScrollPane(view);
      if(Aqua.isAqua()) {
        scrollPane.setBorder(new EmptyBorder(3, 0, 3, 0));
      }
      Dimension preferredSize = view.getPreferredSize();
      AwtUtil.addInsets(preferredSize, scrollPane.getInsets());
      scrollPane.setPreferredSize(preferredSize);
      myLastDropDownComponent = scrollPane;
      return scrollPane;
    }

    protected void onDropDownShown() {
      ((JScrollPane) myLastDropDownComponent).getViewport().getView().requestFocusInWindow();
    }

    private class MyListListener extends MouseAdapter implements KeyListener, MouseMotionListener, Runnable {
      private final AList<ActionItem> myView;

      public MyListListener(AList<ActionItem> view) {
        myView = view;
      }

      public void act() {
        ActionItem selection = myView.getSelectionAccessor().getSelection();
        AnAction action = selection != null ? selection.getAction() : null;
        if (action != null) {
          setSelectedAction(action);
          hideDropDown();
          selection.updateNow();
          if (selection.isEnabled()) selection.performAction();
        }
      }

      /**
       * when finished selection action
       */
      public void run() {
        decrementDropDownReasons();
      }

      public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ENTER)
          act();
      }

      public void mouseReleased(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1)
          act();
      }

      public void keyReleased(KeyEvent e) {
      }

      public void keyTyped(KeyEvent e) {
      }

      public void mouseMoved(MouseEvent e) {
        int row = myView.getElementIndexAt(e.getX(), e.getY());
        if (row >= 0) {
          SelectionAccessor<ActionItem> accessor = myView.getSelectionAccessor();
          accessor.setSelectedIndex(row);
          ActionItem item = accessor.getSelection();
          if (item != null) {
            myView.getSwingComponent().setToolTipText(item.getTooltip());
          }
        }
      }

      public void mouseDragged(MouseEvent e) {
      }
    }
  }
}
