package com.almworks.sumtable;

import com.almworks.util.advmodel.AListModel;
import com.almworks.util.ui.actions.*;

import javax.swing.*;
import java.util.List;

class MoveCounterAction extends SimpleAction {
  private final CounterConfiguration myCounterConfiguration;
  private final int myIncrement;

  public MoveCounterAction(CounterConfiguration counterConfiguration, int increment, String name, Icon icon) {
    super(name, icon);
    myCounterConfiguration = counterConfiguration;
    myIncrement = increment;
    setDefaultPresentation(PresentationKey.ENABLE, EnableState.DISABLED);
    watchRole(CountersConfigurationPanel.COUNTER_ROLE);
  }

  protected void customUpdate(UpdateContext context) throws CantPerformException {
    List<STFilter> counters = context.getSourceCollection(CountersConfigurationPanel.COUNTER_ROLE);
    boolean enabled = counters.size() > 0;
    AListModel<STFilter> model = myCounterConfiguration.getCounterModel();
    for (STFilter counter : counters) {
      int k = model.indexOf(counter);
      k += myIncrement;
      if (k < 0 || k >= model.getSize()) {
        enabled = false;
        break;
      }
    }
    context.setEnabled(enabled);
  }

  protected void doPerform(ActionContext context) throws CantPerformException {
    List<STFilter> counters = context.getSourceCollection(CountersConfigurationPanel.COUNTER_ROLE);
    myCounterConfiguration.move(counters, myIncrement);
  }
}
