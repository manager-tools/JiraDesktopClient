package com.almworks.explorer.workflow;

import com.almworks.api.application.WorkflowComponent2;
import com.almworks.util.advmodel.AListModel;
import com.almworks.util.advmodel.SegmentedListModel;
import com.almworks.util.threads.ThreadAWT;
import com.almworks.util.ui.actions.IdentifiableAction;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.Lifespan;

public class WorkflowComponent2Impl implements WorkflowComponent2 {
  private final SegmentedListModel<IdentifiableAction> myModel = SegmentedListModel.create();

  @ThreadAWT
  public void addWorkflowActions(Lifespan lifespan, final AListModel<? extends IdentifiableAction> model) {
    if (lifespan.isEnded())
      return;
    myModel.addSegment(model);
    lifespan.add(new Detach() {
      protected void doDetach() throws Exception {
        myModel.removeSegment(model);
      }
    });
  }

  @ThreadAWT
  public AListModel<IdentifiableAction> getWorkflowActions() {
    return myModel;
  }
}
