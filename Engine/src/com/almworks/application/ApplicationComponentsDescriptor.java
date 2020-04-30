package com.almworks.application;

import com.almworks.api.application.NameResolver;
import com.almworks.api.application.viewer.textdecorator.TextDecoratorRegistry;
import com.almworks.api.container.RootContainer;
import com.almworks.api.exec.ExceptionMemory;
import com.almworks.api.platform.ComponentDescriptor;
import com.almworks.engine.gui.ItemMessagesRegistry;
import com.almworks.engine.gui.ItemMessagesRegistryImpl;
import com.almworks.http.errors.SSLProblemHandler;

/**
 * @author dyoma
 */
public class ApplicationComponentsDescriptor implements ComponentDescriptor {
  public void registerActors(RootContainer container) {
    container.registerActorClass(NameResolver.ROLE, NameResolverImpl.class);
    container.registerActorClass(TextDecoratorRegistry.ROLE, TextDecoratorRegistryImpl.class);
    container.registerActorClass(ItemMessagesRegistry.ROLE, ItemMessagesRegistryImpl.class);
    container.registerActorClass(ExceptionMemory.ROLE, ExceptionMemoryImpl.class);
    container.registerActorClass(SSLProblemHandler.ROLE, SSLProblemHandler.class);
  }
}
