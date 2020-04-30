package com.almworks.sumtable;

import com.almworks.util.images.Icons;
import com.almworks.util.ui.actions.*;

public class ConfigureSummaryTableAction extends SimpleAction {
  private final SummaryTableMainPanel myPanel;

  public ConfigureSummaryTableAction(SummaryTableMainPanel panel) {
    super("", Icons.ACTION_CONFIGURE);
    myPanel = panel;
    setDefaultPresentation(PresentationKey.SHORT_DESCRIPTION, "Configure tabular distribution");
  }

  protected void customUpdate(UpdateContext context) throws CantPerformException {
    context.updateOnChange(myPanel.getShowingConfigModifiable());
    context.setEnabled(!myPanel.isShowingConfiguration());
  }

  protected void doPerform(ActionContext context) throws CantPerformException {
    myPanel.showConfiguration();
  }
}
