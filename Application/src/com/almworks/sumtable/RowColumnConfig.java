package com.almworks.sumtable;

import com.almworks.api.application.tree.QueryResult;
import com.almworks.util.advmodel.AComboboxModel;
import com.almworks.util.components.AComboBox;
import com.almworks.util.components.ToolbarBuilder;
import org.almworks.util.detach.Lifespan;

public class RowColumnConfig {
  private final SummaryTableConfiguration myTableConfiguration;
  private final AxisConfigurationModelHolder myColumnsModel;
  private final AxisConfigurationModelHolder myRowsModel;

  public RowColumnConfig(SummaryTableConfiguration tableConfiguration) {
    myTableConfiguration = tableConfiguration;
    myColumnsModel = new AxisConfigurationModelHolder(myTableConfiguration.getColumnsConfiguration());
    myRowsModel = new AxisConfigurationModelHolder(myTableConfiguration.getRowsConfiguration());
  }

  public void buildToolbar(ToolbarBuilder builder) {
    builder.addComponent(createCombo(myRowsModel.getModel(), "Rows"));
    builder.addAction(
      new TransposeAction(myTableConfiguration.getRowsConfiguration(), myTableConfiguration.getColumnsConfiguration()));
    builder.addComponent(createCombo(myColumnsModel.getModel(), "Columns"));
  }

  private AComboBox<AxisDefinition> createCombo(AComboboxModel<AxisDefinition> model, String tooltip) {
    AComboBox<AxisDefinition> comboBox = new AComboBox<AxisDefinition>();
    comboBox.setModel(model);
    comboBox.setCanvasRenderer(AxisDefinition.RENDERER);
//    comboBox.setToolTipText(tooltip);
    return comboBox;
  }

  public void attach(Lifespan life, QueryResult result) {
    myColumnsModel.attach(life, result);
    myRowsModel.attach(life, result);
  }
}
