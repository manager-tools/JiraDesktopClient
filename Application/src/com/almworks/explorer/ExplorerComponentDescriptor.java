package com.almworks.explorer;

import com.almworks.actions.ExplorerActions;
import com.almworks.actions.LocalChangesCounterComponent;
import com.almworks.actions.console.actionsource.ConsoleActionsComponent;
import com.almworks.api.application.DBStatusKey;
import com.almworks.api.application.ExplorerComponent;
import com.almworks.api.application.ItemDownloadStageKey;
import com.almworks.api.application.WorkflowComponent2;
import com.almworks.api.application.qb.FilterEditorProvider;
import com.almworks.api.application.qb.QueryBuilderComponent;
import com.almworks.api.container.RootContainer;
import com.almworks.api.explorer.ApplicationToolbar;
import com.almworks.api.explorer.ItemModelRegistry;
import com.almworks.api.explorer.ItemUrlService;
import com.almworks.api.platform.ComponentDescriptor;
import com.almworks.explorer.loader.ItemModelRegistryImpl;
import com.almworks.explorer.qbuilder.editor.FilterEditorProviderImpl;
import com.almworks.explorer.qbuilder.editor.QueryBuilderComponentImpl;
import com.almworks.explorer.workflow.WorkflowComponent2Impl;
import com.almworks.util.properties.Role;

/**
 * @author : Dyoma
 */
public class ExplorerComponentDescriptor implements ComponentDescriptor {
  public void registerActors(RootContainer registrator) {
    registrator.registerActorClass(ExplorerComponent.ROLE, ExplorerComponentImpl.class);
    registrator.registerActorClass(QueryBuilderComponent.ROLE, QueryBuilderComponentImpl.class);
    registrator.registerActorClass(FilterEditorProvider.ROLE, FilterEditorProviderImpl.class);
    registrator.registerActorClass(Role.role("modalInquiriesDisplayer"), ModalInquiriesDisplayer.class);
    registrator.registerActorClass(ItemModelRegistry.ROLE, ItemModelRegistryImpl.class);
    registrator.registerActorClass(Role.role("explorerActions"), ExplorerActions.class);
//    registrator.registerActorClass(WorkflowComponent.ROLE, WorkflowComponentImpl.class); // todo JC-308
    registrator.registerActorClass(WorkflowComponent2.ROLE, WorkflowComponent2Impl.class);
    registrator.registerActorClass(LocalChangesCounterComponent.ROLE, LocalChangesCounterComponent.class);
    registrator.registerActorClass(ItemUrlService.ROLE, ItemUrlServiceImpl.class);
    registrator.registerActorClass(ApplicationToolbar.ROLE, ApplicationToolbarImpl.class);
    registrator.registerActorClass(ColumnsCollector.ROLE, ColumnsCollector.class);
//    registrator.registerActorClass(AttachmentInfoKey.ROLE, AttachmentInfoKey.class);
    registrator.registerActor(ItemDownloadStageKey.ROLE, ItemDownloadStageKey.INSTANCE);
    registrator.registerActor(DBStatusKey.ROLE, DBStatusKey.KEY);
//    registrator.registerActorClass(RulesManager.ROLE, RulesManagerImpl.class); // todo JC-308
//    registrator.registerActorClass(AutoAssignComponent.ROLE, AutoAssignComponentImpl.class); // todo JC-308
//    registrator.registerActorClass(FaceletRegistry.ROLE, FaceletRegistryImpl.class);
    registrator.registerActorClass(HintScreen.ROLE, HintScreen.class);
    registrator.registerActorClass(ConsoleActionsComponent.ROLE, ConsoleActionsComponent.class);
  }
}