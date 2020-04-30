package com.almworks.util.components;

import com.almworks.util.L;
import com.almworks.util.Terms;
import com.almworks.util.advmodel.AListModel;
import com.almworks.util.advmodel.OrderListModel;
import com.almworks.util.commons.Factory;
import com.almworks.util.i18n.Local;
import com.almworks.util.images.Icons;
import com.almworks.util.ui.UIComponentWrapper;
import com.almworks.util.ui.UIUtil;
import com.almworks.util.ui.swing.Shortcuts;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.DetachComposite;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

/**
 * @author : Dyoma
 */
public class AddRemoveList <T> implements UIComponentWrapper {
  private final JPanel myPanel = new JPanel(new BorderLayout(0, 0));
  private final AList<T> myList = new AList<T>();
  private final SelectionAccessor<T> mySelectionAccessor;
  private final DetachComposite myDetach = new DetachComposite();
  private OrderListModel<T> myCollectionModel;
  protected Action myAddAction;
  protected Action myRemoveAction;

  public AddRemoveList(OrderListModel<T> listModel, Factory<T> addFactory) {
    mySelectionAccessor = new ListSelectionAccessor<T>(myList);
    myList.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    myDetach.add(AComponentUtil.selectElementWhenAny(myList));
    JPanel buttons = new JPanel(new GridLayout(1, 2));
    myAddAction = createAddAction(addFactory);
    buttons.add(AToolbarButton.withAction(myAddAction));
    myRemoveAction = createRemoveAction();
    buttons.add(AToolbarButton.withAction(myRemoveAction));
    JPanel buttonsPanel = new JPanel(UIUtil.createBorderLayout());
    buttonsPanel.add(buttons, BorderLayout.WEST);
    myPanel.add(buttonsPanel, BorderLayout.NORTH);
    myPanel.add(new JScrollPane(myList), BorderLayout.CENTER);
    myDetach.add(setModel(listModel));
    initKeymap();
  }

  private void initKeymap() {
    final InputMap inputMap = myPanel.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_SUBTRACT, 0), myRemoveAction);
    inputMap.put(Shortcuts.DELETE, myRemoveAction);
    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, KeyEvent.ALT_MASK), myRemoveAction);

    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ADD, 0), myAddAction);
    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, 0), myAddAction);
    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, KeyEvent.ALT_MASK), myAddAction);

    final ActionMap actionMap = myPanel.getActionMap();
    actionMap.put(myRemoveAction, myRemoveAction);
    actionMap.put(myAddAction, myAddAction);
  }

  public JComponent getComponent() {
    return myPanel;
  }

  public void dispose() {
    myDetach.detach();
  }

  public Detach setModel(OrderListModel<T> collectionModel) {
    myCollectionModel = collectionModel;
    return myList.setCollectionModel(collectionModel);
  }

  public SelectionAccessor<T> getSelectionAccessor() {
    return mySelectionAccessor;
  }

  private Action createRemoveAction() {
    final AbstractAction action = new AbstractAction("", Icons.ACTION_GENERIC_REMOVE) {
      {
        putValue(SHORT_DESCRIPTION,
          L.tooltip(Local.parse("Remove connection and clear database of all " + Terms.ref_artifacts + " that belong to it")));
      }

      public void actionPerformed(ActionEvent e) {
        int index = myList.getSelectedIndex();
        AListModel<T> model = myList.getCollectionModel();
        assert myCollectionModel != null;
        myCollectionModel.removeAt(index);
        index = Math.min(model.getSize() - 1, index);
        if (index < 0)
          mySelectionAccessor.clearSelection();
        else
          mySelectionAccessor.setSelected(myCollectionModel.getAt(index));
      }
    };
    ListSelectionListener listener = new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        action.setEnabled(myList.getSelectedIndex() != -1);
      }
    };
    myList.addListSelectionListener(listener);
    listener.valueChanged(null);
    return action;
  }

  private Action createAddAction(final Factory<T> addFactory) {
    AbstractAction action = new AbstractAction("", Icons.ACTION_GENERIC_ADD) {
      {
        putValue(SHORT_DESCRIPTION, L.tooltip("Create connection"));
        putValue(LONG_DESCRIPTION, L.content("Create and configure new connection"));
      }

      public void actionPerformed(ActionEvent e) {
        T item = addFactory.create();
        if (item == null)
          return;
        int index = myCollectionModel.addElement(item);
        mySelectionAccessor.setSelected(item);
      }
    };
    action.putValue(Action.ACCELERATOR_KEY, Shortcuts.ALT_INSERT);
    return action;
  }

  public Action getAddAction() {
    return myAddAction;
  }

  public Action getRemoveAction() {
    return myRemoveAction;
  }
}