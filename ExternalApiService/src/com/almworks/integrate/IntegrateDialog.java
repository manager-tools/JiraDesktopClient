package com.almworks.integrate;

import com.almworks.util.advmodel.OrderListModel;
import com.almworks.util.components.AList;
import com.almworks.util.components.Canvas;
import com.almworks.util.components.CanvasRenderer;
import com.almworks.util.components.PlaceHolder;
import com.almworks.util.components.renderer.CellState;
import com.almworks.util.model.BasicScalarModel;
import com.almworks.util.model.ScalarModel;
import org.almworks.util.Collections15;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;

class IntegrateDialog implements CanvasRenderer<IntegrationProcedure> {
  private AList<IntegrationProcedure> myIdeList;
  private JLabel mySelectIdeLabel;
  private JPanel myIdeOptionPanel;
  private JPanel myWholePanel;

  private final PlaceHolder myOptions = new PlaceHolder();
  private final IntegrateWithIDEA myIdeaIntegration = new IntegrateWithIDEA();
  private final JLabel myNoOptions = createNoOptions();
  private final OrderListModel<IntegrationProcedure> myIdeModel = OrderListModel.create();
  private final BasicScalarModel<IntegrationProcedure> mySelectedProcedure = BasicScalarModel.createWithValue(null, true);

  private static JLabel createNoOptions() {
    JLabel label = new JLabel("No IDE selected");
    label.setHorizontalAlignment(SwingConstants.CENTER);
    return label;
  }

  public IntegrateDialog() {
    setupVisual();
    setupIDEs();
    setupOptions();
  }

  private void setupIDEs() {
    java.util.List<IntegrationProcedure> integrations =
      Collections15.arrayList(new IntegrateWithIDEA(), new IntegrateWithVS());
    for (IntegrationProcedure integration : integrations) {
      if (integration.checkAvailability())
        myIdeModel.addElement(integration);
    }
    myIdeList.setCollectionModel(myIdeModel);
    myIdeList.setCanvasRenderer(this);
  }

  public void renderStateOn(CellState state, Canvas canvas, IntegrationProcedure item) {
    canvas.appendText(item.getTitle());  
  }

  private void setupOptions() {
    myIdeOptionPanel.setLayout(new BorderLayout());
    myIdeOptionPanel.add(myOptions, BorderLayout.CENTER);

    myIdeList.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        onSelection();
      }
    });
    onSelection();
  }

  private void onSelection() {
    int selectedIndex = myIdeList.getSelectedIndex();
    if (selectedIndex < 0) {
      myOptions.show(myNoOptions);
      mySelectedProcedure.setValue(null);
    } else {
      IntegrationProcedure ip = myIdeModel.getAt(selectedIndex);
      myOptions.show(ip.getComponent());
      mySelectedProcedure.setValue(ip);
    }
  }

  private void setupVisual() {
    mySelectIdeLabel.setLabelFor(myIdeList);
    mySelectIdeLabel.setBorder(new EmptyBorder(9, 0, 0, 0));
  }

  public JComponent getComponent() {
    return myWholePanel;
  }

  public ScalarModel<IntegrationProcedure> getSelectedIntegrationModel() {
    return mySelectedProcedure;
  }
}
