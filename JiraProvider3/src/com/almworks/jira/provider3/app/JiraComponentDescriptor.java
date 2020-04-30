package com.almworks.jira.provider3.app;

import com.almworks.api.container.RootContainer;
import com.almworks.api.gui.MainMenu;
import com.almworks.api.platform.ComponentDescriptor;
import com.almworks.items.gui.edit.helper.EditItemHelper;
import com.almworks.jira.provider3.app.connection.JiraProvider3;
import com.almworks.jira.provider3.app.connection.JiraSchemaInit;
import com.almworks.jira.provider3.custom.impl.CustomFieldsComponent;
import com.almworks.jira.provider3.gui.edit.workflow.WLActionSets;
import com.almworks.jira.provider3.gui.edit.workflow.duplicate.ResolveAsDuplicateSupport;
import com.almworks.jira.provider3.links.structure.ui.CustomizeHierarchyAction;
import com.almworks.jira.provider3.services.upload.JiraUploadComponent;
import com.almworks.jira.provider3.sync.automerge.JiraMergeSetup;
import com.almworks.platform.RegisterActions;
import com.almworks.restconnector.login.AuthenticationRegister;

public class JiraComponentDescriptor implements ComponentDescriptor {
  public void registerActors(RootContainer registrator) {
    registrator.registerActorClass(JiraProvider3.ROLE, JiraProvider3.class);
    registrator.registerActorClass(JiraMergeSetup.ROLE, JiraMergeSetup.class);
    registrator.registerActorClass(JiraFeaturesProvider.ROLE, JiraFeaturesProvider.class);
    registrator.registerActorClass(EditItemHelper.ROLE, EditItemHelper.class);
    registrator.registerActorClass(WLActionSets.ROLE, WLActionSets.class);
    registrator.registerActorClass(ResolveAsDuplicateSupport.ROLE, ResolveAsDuplicateSupport.class);
    registrator.registerActorClass(CustomFieldsComponent.ROLE, CustomFieldsComponent.class);
    registrator.registerActorClass(JiraUploadComponent.ROLE, JiraUploadComponent.class);
    registrator.registerActorClass(JiraSchemaInit.ROLE, JiraSchemaInit.class);
    RegisterActions.registerAction(registrator, MainMenu.Search.CUSTOMIZE_HIERARCHY, CustomizeHierarchyAction.INSTANCE);
    registrator.registerActorClass(AuthenticationRegister.ROLE, AuthenticationRegister.class);
  }
}
