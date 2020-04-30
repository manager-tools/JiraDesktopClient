package com.almworks.api.explorer;

import com.almworks.util.properties.Role;

/**
 * @author dyoma
 */
public interface WorkflowComponent {
  Role<WorkflowComponent> ROLE = Role.role(WorkflowComponent.class);
}
