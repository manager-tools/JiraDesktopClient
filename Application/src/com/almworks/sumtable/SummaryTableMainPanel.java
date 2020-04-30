package com.almworks.sumtable;

import com.almworks.api.application.tree.GenericNode;
import com.almworks.api.application.tree.QueryResult;
import com.almworks.api.gui.DialogBuilder;
import com.almworks.api.gui.DialogManager;
import com.almworks.util.L;
import com.almworks.util.collections.Modifiable;
import com.almworks.util.components.plaf.macosx.Aqua;
import com.almworks.util.config.Configuration;
import com.almworks.util.exec.Context;
import com.almworks.util.model.ValueModel;
import com.almworks.util.ui.actions.DataProvider;
import com.almworks.util.ui.actions.PresentationKey;
import com.almworks.util.ui.actions.SimpleAction;
import com.almworks.util.ui.swing.Shortcuts;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.Lifespan;

import javax.swing.*;
import java.awt.*;

public class SummaryTableMainPanel {
  private final JPanel myWholePanel = new JPanel(new BorderLayout());
  private final SummaryTable myTable;
  private final SummaryTableConfiguration myTableConfiguration;
  private final CountersConfigurationPanel myCountersConfiguration;

  private final ValueModel<Boolean> myShowingConfigModel = ValueModel.create();

  public SummaryTableMainPanel(
    Configuration uiConfig, SummaryTableConfiguration tableConfig, boolean windowed,
    SummaryTableQueryExecutor executor)
  {
    myTableConfiguration = tableConfig;
    final AxisConfiguration horizontal = myTableConfiguration.getColumnsConfiguration();
    final AxisConfiguration vertical = myTableConfiguration.getRowsConfiguration();
    myCountersConfiguration = new CountersConfigurationPanel(
      uiConfig.getOrCreateSubset("CounterConfigPanel"), myTableConfiguration.getCounterConfiguration());
    myTable = new SummaryTable(myTableConfiguration, windowed, executor);
    myWholePanel.add(myTable.getComponent(), BorderLayout.CENTER);
    if(Aqua.isAqua()) {
      myWholePanel.setBorder(Aqua.MAC_BORDER_NORTH);
    }
    myShowingConfigModel.setValue(false);
  }

  public void attach(Lifespan lifespan, GenericNode node) {
    QueryResult result = node.getQueryResult();
    myTableConfiguration.attach(lifespan, result);
    myTable.attach(lifespan, node, result);
    myCountersConfiguration.attach(lifespan, result);
  }

  public JComponent getComponent() {
    return myWholePanel;
  }

  public void showConfiguration() {
    final DialogBuilder builder = Context.require(DialogManager.ROLE).createBuilder("CountersConfiguration");
    builder.setTitle("Counters Configuration");
    builder.setModal(true);
    builder.setContent(myCountersConfiguration.getComponent());
    SimpleAction cancelAction = SimpleAction.createDoNothing(L.actionName("Close Window"));
    cancelAction.setDefaultPresentation(PresentationKey.SHORTCUT, Shortcuts.CTRL_ENTER);
    builder.setCancelAction(cancelAction);
    builder.setPreferredSize(new Dimension(450, 278));
    builder.setBottomBevel(false);

    myShowingConfigModel.setValue(true);
    builder.showWindow(new Detach() {
      protected void doDetach() throws Exception {
        myShowingConfigModel.setValue(false);
      }
    });
  }

  public Modifiable getShowingConfigModifiable() {
    return myShowingConfigModel;
  }

  public boolean isShowingConfiguration() {
    return Boolean.TRUE.equals(myShowingConfigModel.getValue());
  }

  public void installSummaryDataProvider(JComponent component) {
    DataProvider.DATA_PROVIDER.putClientValue(component, myTable.getDataProvider());
  }
}
