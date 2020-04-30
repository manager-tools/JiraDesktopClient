package com.almworks.extservice;

import com.almworks.api.gui.DialogBuilder;
import com.almworks.api.gui.DialogManager;
import com.almworks.util.advmodel.OrderListModel;
import com.almworks.util.components.Canvas;
import com.almworks.util.components.*;
import com.almworks.util.components.renderer.CellState;
import com.almworks.util.ui.actions.*;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

abstract class EapiClientAction extends SimpleAction {
  protected final OrderListModel<EapiClient> myClientsModel;

  public EapiClientAction(@Nullable String name, @Nullable Icon icon, OrderListModel<EapiClient> pluginConnected) {
    super(name, icon);
    myClientsModel = pluginConnected;
  }

  protected final void customUpdate(UpdateContext context) throws CantPerformException {
    context.updateOnChange(myClientsModel);
    updateUI(context);
    if (myClientsModel.getSize() == 0) {
      context.putPresentationProperty(PresentationKey.ENABLE, EnableState.INVISIBLE);
    } else {
      customUpdate2(context);
    }
  }

  protected void updateUI(UpdateContext context) {
  }

  protected abstract void customUpdate2(UpdateContext context) throws CantPerformException;

  protected EapiClient selectClient(ActionContext context) throws CantPerformException {
    int size = myClientsModel.getSize();
    if (size == 0)
      return null;
    else if (size == 1)
      return myClientsModel.getAt(0);

    // show dialog
    JPanel selectionPanel = new JPanel(new BorderLayout(0, 4));
    final AList<EapiClient> list = new AList<EapiClient>(myClientsModel);
    selectionPanel.add(new JLabel("<html>Multiple tools are connected to this application.<br>" +
      "Please select a tool to send the request to:"), BorderLayout.NORTH);
    selectionPanel.add(new JScrollPane(list), BorderLayout.CENTER);

    list.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    list.getSelectionAccessor().ensureSelectionExists();
    list.setCanvasRenderer(new CanvasRenderer<EapiClient>() {
      public void renderStateOn(CellState state, Canvas canvas, EapiClient item) {
        canvas.appendText(item.getName());
      }
    });

    DialogManager dialogManager = context.getSourceObject(DialogManager.ROLE);
    final DialogBuilder builder = dialogManager.createBuilder("selectEapiClient");
    builder.setTitle("Select External Tool");
    builder.setModal(true);
    builder.setContent(selectionPanel);
    builder.setInitialFocusOwner(list.getSwingComponent());
    builder.setEmptyCancelAction();

    final EapiClient[] selected = {null};
    EnabledAction okAction = new EnabledAction("OK") {
      protected void doPerform(ActionContext context) throws CantPerformException {
        selected[0] = list.getSelectionAccessor().getSelection();
      }
    };

    builder.setOkAction(okAction);
    list.addDoubleClickListener(Lifespan.FOREVER, new CollectionCommandListener<EapiClient>() {
      public void onCollectionCommand(ACollectionComponent<EapiClient> component, int index, EapiClient element) {
        selected[0] = list.getSelectionAccessor().getSelection();
        try {
          builder.closeWindow();
        } catch (CantPerformException e) {
          // ignore
        }
      }
    });


    builder.showWindow();
    return selected[0];
  }

  protected String adjusted(String singleClientName, String multipleClientsName) {
    String name = null;
    int size = myClientsModel.getSize();
    if (size > 0) {
      name = myClientsModel.getAt(0).getShortName();
      if (size > 1) {
        for (int i = 1; i < size; i++) {
          if (!name.equals(myClientsModel.getAt(i).getShortName())) {
            name = null;
            break;
          }
        }
      }
    }
    if (name != null) {
      int k = singleClientName.indexOf('$');
      return k < 0 ? singleClientName : singleClientName.substring(0, k) + name + singleClientName.substring(k + 1);
    } else {
      return multipleClientsName;
    }
  }
}
