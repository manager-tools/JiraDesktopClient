package com.almworks.container;

import com.almworks.api.container.*;
import com.almworks.util.properties.Role;
import org.almworks.util.Collections15;
import org.almworks.util.Log;
import org.almworks.util.TypedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.picocontainer.ComponentAdapter;
import org.picocontainer.MutablePicoContainer;
import org.picocontainer.PicoContainer;
import org.picocontainer.Startable;
import org.picocontainer.defaults.*;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Set;


class ComponentContainerImpl implements MutableComponentContainer, ComponentContainerParent {
  final Role<ComponentContainerImpl> CONTAINER_IMPL = Role.role(ComponentContainerImpl.class);
  private final Selectors mySelectors;
  private final ContainerPath myPath;
  private final EventRouterImpl myEventRouter;
  private final MutablePicoContainer myPico;

  ComponentContainerImpl(@NotNull final ComponentContainerParent parent, @Nullable String name) {
    myPico = new DefaultPicoContainer(parent.getPico());

    ContainerPath parentPath = parent.getPath();
    assert name != null || parentPath == null;
    myPath = name == null ? null : new ContainerPath(parentPath, name);

    mySelectors = new Selectors(parent.getSelectors());

    ComponentContainerDelegator actor = new ComponentContainerDelegator(this);
    registerActor(ComponentContainer.ROLE, actor);
    myPico.registerComponentInstance(ComponentContainer.class, actor);

    myEventRouter = new EventRouterImpl(parent.getEventRouter());
    registerActor(EventRouter.ROLE, myEventRouter);
  }

  @NotNull
  public PicoContainer getPico() {
    return myPico;
  }

  public <I, C extends I> void registerActor(Role<I> role, C actor) {
    doRegister(role, actor, false);
  }

  public void registerActor(@NotNull Object actor) {
    doRegister(Role.role(Object.class), actor, false);
  }

  private <I, C extends I> void doRegister(Role<I> role, C actor, boolean unregister) {
    assert role != null;
    assert actor != null;
    checkCompatibility(role, actor.getClass());
    if (unregister) {
      myPico.unregisterComponent(role);
    }
    myPico.registerComponentInstance(role, actor);
/*
    Class<I> intf = role.getRoleInterface();
    if (intf != null) {
      if (unregister) {
        myPico.unregisterComponent(intf);
      }
      myPico.registerComponentInstance(intf, actor);
    }
*/
  }

  public <I, C extends I> void reregisterActor(Role<I> role, C actor) {
    doRegister(role, actor, true);
  }

  public <I> void registerActorClass(Role<I> role, Class<? extends I> actorClass) {
    assert role != null;
    assert actorClass != null;
    checkCompatibility(role, actorClass);
    registerAdapter(role, actorClass, role.getName());
  }

  public <I> void registerSelector(Class<I> actorInterface, Class<? extends ActorSelector<I>> selectorClass) {
    registerAdapter(selectorClass, selectorClass, selectorClass.getName());
    mySelectors.addProvider(actorInterface, selectorClass);
  }

  public void start() {
    myPico.start();
  }

  public void startWithDebugging() {
    startWithDebugging(myPico);
  }

  static void startWithDebugging(MutablePicoContainer pico) {
    try {
      Method startMethod = Startable.class.getMethod("start");
      new LifecycleVisitor(startMethod, Startable.class, true) {
        public void visitComponentAdapter(ComponentAdapter componentAdapter) {
          ComponentAdapter c = componentAdapter;
          if (!(c instanceof DecoratingComponentAdapter)) {
            Class clazz = c.getComponentImplementation();
            Log.debug("starting: " + clazz.getName());
          }
          super.visitComponentAdapter(componentAdapter);
        }
      }.traverse(pico);
    } catch (NoSuchMethodException e) {
      Log.error(e);
    }
  }

  public void stop() {
    try {
      myPico.stop();
    } catch (IllegalStateException e) {
      Log.debug("state mismatch when stopping container: " + e);
    }
  }

  public boolean isRegistered(@NotNull TypedKey<?> role) {
    return myPico.getComponentInstance(role) != null;
  }

  @NotNull
  public Set<Role> getRegisteredRoles() {
    Set<Role> result = null;
    for (PicoContainer pico = myPico; pico != null; pico = pico.getParent()) {
      Collection<ComponentAdapter> adapters = pico.getComponentAdapters();
      for (ComponentAdapter adapter : adapters) {
        Object key = adapter.getComponentKey();
        if (key instanceof Role) {
          if (result == null)
            result = Collections15.hashSet();
          result.add((Role) key);
        }
      }
    }
    return result != null ? result : Collections15.<Role>emptySet();
  }

  @NotNull
  public <I> I instantiate(@NotNull Class<? extends I> concreteClass) {
    SelectionComponentAdapter adapter =
      new SelectionComponentAdapter(myPath, mySelectors, concreteClass, concreteClass);
    return (I) adapter.getComponentInstance(myPico);
  }

  @NotNull
  public <I> I instantiate(Class<? extends I> concreteClass, String selectionId) {
    return (I) createComponentAdapter(selectionId, concreteClass, concreteClass).getComponentInstance(myPico);
  }

  public <I> I getActor(TypedKey<? extends I> role) {
    return (I) myPico.getComponentInstance(role);
  }

  @Nullable
  public <I> I getActor(@NotNull Class<I> clazz) {
    return (I) myPico.getComponentInstanceOfType(clazz);
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

  @Nullable
  public <I> I getSelectable(@NotNull Class<I> clazz, String selectorName) {
    Class<? extends ActorSelector> provider = mySelectors.getProvider(clazz);
    if (provider == null)
      return null;
    ActorSelector selector = (ActorSelector) myPico.getComponentInstance(provider);
    if (selector == null)
      return null;
    Object object = selector.selectImplementation(new ContainerPath(myPath, selectorName));
    return (I) object;
  }

  <I> void registerInstance(Class<? super I> keyClass, I instance) {
    myPico.registerComponentInstance(keyClass, instance);
  }

  private void checkCompatibility(Role role, Class actorClass) {
    Class roleInterface = role.getValueClass();
    if (roleInterface == null) {
      // not a strict role - no check
      return;
    }
    if (!roleInterface.isAssignableFrom(actorClass))
      throw new AssignabilityRegistrationException(roleInterface, actorClass);
  }

  private <I> void registerAdapter(Object componentKey, Class<? extends I> implClass, String selectionId) {
    SelectionComponentAdapter delegate = createComponentAdapter(selectionId, componentKey, implClass);
    ComponentAdapter adapter = new GlobalSynchronizedComponentAdapter(new CachingComponentAdapter(delegate));
    myPico.registerComponent(adapter);
  }

  private SelectionComponentAdapter createComponentAdapter(String selectionId, Object componentKey, Class implClass) {
    ContainerPath key = selectionId != null ? new ContainerPath(myPath, selectionId) : null;
    return new SelectionComponentAdapter(key, mySelectors, componentKey, implClass);
  }

  @NotNull
  public MutableComponentContainer createSubcontainer(@NotNull String name) {
    return new ComponentContainerImpl(this, name);
  }

  @Nullable
  public Selectors getSelectors() {
    return mySelectors;
  }

  @Nullable
  public ContainerPath getPath() {
    return myPath;
  }

  @Nullable
  public EventRouterImpl getEventRouter() {
    return myEventRouter;
  }
}
