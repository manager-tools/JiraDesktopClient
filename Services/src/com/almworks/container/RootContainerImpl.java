package com.almworks.container;

import com.almworks.api.container.ActorSelector;
import com.almworks.api.container.ContainerPath;
import com.almworks.api.container.MutableComponentContainer;
import com.almworks.api.container.RootContainer;
import com.almworks.util.properties.Role;
import org.almworks.util.TypedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.picocontainer.PicoContainer;
import org.picocontainer.defaults.DefaultPicoContainer;

import java.util.Set;

class RootContainerImpl implements RootContainer, ComponentContainerParent {
  private final DefaultPicoContainer myStartupContainer;
  private final ComponentContainerImpl myMainContainer;

  public RootContainerImpl() {
    myStartupContainer = new DefaultPicoContainer();
    myMainContainer = new ComponentContainerImpl(this, null);
    myStartupContainer.addChildContainer(myMainContainer.getPico());
  }

  public <I> void registerActorClass(Role<I> role, Class<? extends I> actorClass) {
    myMainContainer.registerActorClass(role, actorClass);
  }

  public <I, C extends I> void registerActor(Role<I> role, C actor) {
    myMainContainer.registerActor(role, actor);
  }

  public void registerActor(@NotNull Object actor) {
    myMainContainer.registerActor(actor);
  }

  public <I, C extends I> void reregisterActor(Role<I> role, C actor) {
    throw new UnsupportedOperationException();
  }

  public <I> void registerSelector(Class<I> actorInterface, Class<? extends ActorSelector<I>> selectorClass) {
    myMainContainer.registerSelector(actorInterface, selectorClass);
  }

  public void start() {
    myStartupContainer.start();
  }

  public void startWithDebugging() {
    ComponentContainerImpl.startWithDebugging(myStartupContainer);
  }

  public <T> void registerStartupActor(Role<T> role, T actor) {
    myStartupContainer.registerComponentInstance(role, actor);
  }

  public <T> void registerStartupActorClass(Role<T> role, Class<? extends T> actorClass) {
    myStartupContainer.registerComponentImplementation(role, actorClass);
  }

  public ComponentContainerImpl getMainContainer() {
    return myMainContainer;
  }

  @Nullable
  public <I> I getActor(@NotNull TypedKey<? extends I> role) {
    return myMainContainer.getActor(role);
  }

  @NotNull
  public <I> I requireActor(@NotNull TypedKey<? extends I> role) {
    I r = getActor(role);
    assert r != null : role;
    return r;
  }

  @NotNull
  public <I> I requireActor(@NotNull Class<I> clazz) {
    I r = getActor(clazz);
    assert r != null : clazz;
    return r;
  }

  @NotNull
  public MutableComponentContainer createSubcontainer(@NotNull String name) {
    return myMainContainer.createSubcontainer(name);
  }

  @NotNull
  public <I> I instantiate(@NotNull Class<? extends I> concreteClass, @Nullable String selectionId) {
    return myMainContainer.instantiate(concreteClass, selectionId);
  }

  @NotNull
  public <I> I instantiate(@NotNull Class<? extends I> concreteClass) {
    return myMainContainer.instantiate(concreteClass);
  }

  public void stop() {
    myStartupContainer.stop();
  }

  @NotNull
  public PicoContainer getPico() {
    return myStartupContainer;
  }

  @Nullable
  public Selectors getSelectors() {
    return null;
  }

  @Nullable
  public ContainerPath getPath() {
    return null;
  }

  @Nullable
  public EventRouterImpl getEventRouter() {
    return null;
  }

  public boolean isRegistered(@NotNull TypedKey<?> role) {
    return myMainContainer.isRegistered(role);
  }

  @NotNull
  public Set<Role> getRegisteredRoles() {
    return myMainContainer.getRegisteredRoles();
  }

  @Nullable
  public <I> I getSelectable(@NotNull Class<I> clazz, String selectorName) {
    return myMainContainer.getSelectable(clazz, selectorName);
  }

  @Nullable
  public <I> I getActor(@NotNull Class<I> clazz) {
    return myMainContainer.getActor(clazz);
  }
}
