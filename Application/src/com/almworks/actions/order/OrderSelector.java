package com.almworks.actions.order;

import com.almworks.api.application.order.Order;
import com.almworks.api.gui.DialogEditorBuilder;
import com.almworks.api.gui.DialogManager;
import com.almworks.util.advmodel.AListModel;
import com.almworks.util.advmodel.SortedListDecorator;
import com.almworks.util.components.*;
import com.almworks.util.components.speedsearch.ListSpeedSearch;
import com.almworks.util.config.Configuration;
import com.almworks.util.properties.ChangeSupport;
import com.almworks.util.properties.PropertyChangeListener;
import com.almworks.util.ui.DialogsUtil;
import com.almworks.util.ui.actions.ActionContext;
import com.almworks.util.ui.actions.AnActionListener;
import com.almworks.util.ui.actions.CantPerformException;
import org.almworks.util.TypedKey;
import org.almworks.util.Util;
import org.almworks.util.detach.DetachComposite;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * @author dyoma
 */
class OrderSelector {
  public static final TypedKey<Order> ORDER = TypedKey.create("order");
  private static final String CURRENT_ORDER = "currentOrder";
  private final Configuration myConfig;
  private final AListModel<? extends Order> myOrders;
  private final ChangeSupport mChangeSupport = new ChangeSupport(this);
  private JComponent myWholePanel;
  private FieldWithMoreButton<JTextField> myField;
  private Order myCurrentOrder;
  private final DetachComposite myLife = new DetachComposite();

  public OrderSelector(Configuration config, AListModel<? extends Order> orders) {
    myConfig = config;
    myOrders = SortedListDecorator.create(myLife, orders, Order.NAME_COMPARATOR);
    Color foreground = myField.getForeground();
    myField.getField().setEditable(false);
    myField.getField().setDisabledTextColor(foreground);
    myField.getField().setFocusable(false);
    myField.setAction(new AnActionListener() {
      public void perform(ActionContext context) throws CantPerformException {
        Order order = selectOrder(context.getSourceObject(DialogManager.ROLE));
        if (order == null || Util.equals(order, myCurrentOrder))
          return;
        if (context.getSourceObject(ReorderWindow.ROLE).hasChanges())
          if (!DialogsUtil.askConfirmation(context.getComponent(),
            "All current changes will be discarded if you select another order field. Proceed?",
            "Confirm Discard Changes"))
            return;
        setCurrentOrder(order);
      }
    });
  }

  public Order getCurrentOrder() {
    return myCurrentOrder;
  }

  public void dispose() {
    myLife.detach();
  }

  public boolean ensureOrderSelected(DialogManager dialogManager) {
    //noinspection SimplifiableIfStatement
    if (chooseOrderFromConfig())
      return true;
    Order order = selectOrder(dialogManager);
    if (order != null) {
      setCurrentOrder(order);
      return true;
    }
    return false;
  }

  public <T> void addChangeListener(Lifespan life, TypedKey<T> property, PropertyChangeListener<T> listener) {
    mChangeSupport.addListener(life, property, listener);
  }

  @Nullable
  private Order selectOrder(DialogManager dialogManager) {
    final DialogEditorBuilder builder = dialogManager.createEditor("orderSelector");
    builder.setTitle("Select Order Field");
    AList<Order> list = AList.create();
    list.setCollectionModel(myOrders);
    list.setCanvasRenderer(Order.RENDERER);
    ListSpeedSearch.install(list);
    list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    SelectionAccessor<Order> selection = list.getSelectionAccessor();
    if (myCurrentOrder != null)
      selection.setSelected(myCurrentOrder);
    else
      selection.ensureSelectionExists();
    list.addDoubleClickListener(Lifespan.FOREVER, new CollectionCommandListener<Order>() {
      public void onCollectionCommand(ACollectionComponent<Order> aCollectionComponent, int index, Order element) {
        builder.pressOk();
      }
    });
    builder.setContent(new JScrollPane(list));
    builder.hideApplyButton();
    builder.setModal(true);
    builder.setInitialFocusOwner(list);
    builder.showWindow();
    DialogEditorBuilder.EditingEvent event = builder.getLastEvent();
    if (event != DialogEditorBuilder.OK_EVENT)
      return null;
    return selection.getSelection();
  }

  private void setCurrentOrder(Order order) {
    if (Util.equals(order, myCurrentOrder))
      return;
    Order prevOrder = myCurrentOrder;
    myCurrentOrder = order;
    myField.getField().setText(order != null ? order.getDisplayName() : "<Not selected>");
    myWholePanel.invalidate();
    myWholePanel.revalidate();
    if (order != null)
      myConfig.setSetting(CURRENT_ORDER, order.getId());
    else
      myConfig.setSetting(CURRENT_ORDER, null);
    mChangeSupport.fireChanged(ORDER, prevOrder, order);
  }

  private boolean chooseOrderFromConfig() {
    String orderId = myConfig.getSetting(CURRENT_ORDER, null);
    if (orderId == null)
      return false;
    for (int i = 0; i < myOrders.getSize(); i++) {
      Order order = myOrders.getAt(i);
      if (Util.equals(order.getId(), orderId)) {
        setCurrentOrder(order);
        return true;
      }
    }
    setCurrentOrder(null);
    return false;
  }

  public JComponent getWholePanel() {
    return myWholePanel;
  }
}
