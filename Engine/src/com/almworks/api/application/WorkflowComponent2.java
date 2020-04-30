package com.almworks.api.application;

import com.almworks.util.advmodel.AListModel;
import com.almworks.util.properties.Role;
import com.almworks.util.threads.ThreadAWT;
import com.almworks.util.ui.actions.IdentifiableAction;
import org.almworks.util.detach.Lifespan;

public interface WorkflowComponent2 {
  Role<WorkflowComponent2> ROLE = Role.role(WorkflowComponent2.class);

  @ThreadAWT
  void addWorkflowActions(Lifespan lifespan, AListModel<? extends IdentifiableAction> model);

  @ThreadAWT
  AListModel<IdentifiableAction> getWorkflowActions();
}
