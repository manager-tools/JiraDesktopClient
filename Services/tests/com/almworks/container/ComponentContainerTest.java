package com.almworks.container;

import com.almworks.api.container.ActorSelector;
import com.almworks.api.container.ComponentContainer;
import com.almworks.api.container.ContainerPath;
import com.almworks.api.container.MutableComponentContainer;
import com.almworks.util.properties.Role;
import org.picocontainer.Startable;
import org.picocontainer.defaults.AssignabilityRegistrationException;
import org.picocontainer.defaults.UnsatisfiableDependenciesException;

public class ComponentContainerTest extends PicoTestCase {
  private static int ourValue;

  ParentComponent myParent;
  ChildComponent myChild = null;

  @SuppressWarnings({"ConstantConditions"})
  public void testChildContainer() {
    MutableComponentContainer container = getContainer();
    container.registerActor(Role.role("this"), this);
    container.registerSelector(SelectionKeyHolder.class, SelectionKeyHolderSelector.class);
    container.registerActorClass(Role.role("parent"), ParentComponent.class);
    container.start();
    assertNotNull(myParent);
    assertEquals("parent", myParent.myKey.getName());
    assertNull(myParent.myKey.getParentPath());
    assertNull(myChild);

    myParent.createChild();
    assertNotNull(myChild);
    assertSame(myParent, myChild.myParent);
    assertNull(myParent.myContainer.getActor(myChild.getClass()));
    ContainerPath childKey = myChild.myKey;
    assertEquals("child", childKey.getName());
    assertEquals("1", childKey.getParentPath().getName());
    assertNull(childKey.getParentPath().getParentPath());
  }

  public void testMismatchedRegistration() {
    try {
      getContainer().registerActorClass(Role.role("zooable", Zooable.class), (Class) Object.class);
      fail("container allowed to register class that does not implement interface");
    } catch (AssignabilityRegistrationException e) {
      // ok
    }
  }

  public void testMultipleRegistrations() {
    Role<Object> role = Role.role("100");
    getContainer().registerActorClass(role, SimpleZooable.class);
    try {
      getContainer().registerActorClass(role, ConfigurableZooable.class);
      fail("container allowed duplicate registration");
    } catch (Exception e) {
      // ok
    }
  }

  public void testNoRegistration() {
    getContainer().registerActorClass(Role.role("zooable"), ConfigurableZooable.class);
    getContainer().registerActorClass(Role.role("aux"), SimpleRegistrationAux.class);
    try {
      getContainer().start();
      fail("container started without implementation of ZooConfiguration");
    } catch (UnsatisfiableDependenciesException e) {
      // ok
    }
  }


  public void testSelectableRegistration() {
    getContainer().registerActorClass(Role.role("100"), ConfigurableZooable.class);
    getContainer().registerActorClass(Role.role("10"), ConfigurableWeeable.class);
    getContainer().registerSelector(ZooConfiguration.class, ZooConfigurationSelector.class);
    getContainer().registerActorClass(Role.role("aux"), SelectableRegistrationAux.class);
    assertTrue(ourValue == 0);
    getContainer().start();
    assertTrue(ourValue == 60);
    getContainer().stop();
    assertTrue(ourValue == 200);
  }

/*
  // the following tests are deprecated because of platform refactoring


  public void testBothRegistrationAndSelectorRegistration() {
    getContainer().registerActorClass(Role.create("config"), ZooConfigurationImpl.class);
    try {
      getContainer().registerSelector(ZooConfiguration.class, ZooConfigurationSelector.class);
      fail("allowed to register both implementation and implementation provider");
    } catch (Exception e) {
      // ok
    }
  }

  public void testBothRegistrationAndSelectorRegistration2() {
    getContainer().registerSelector(ZooConfiguration.class, ZooConfigurationSelector.class);
    try {
      getContainer().registerActorClass(Role.create("config"), ZooConfigurationImpl.class);
      fail("allowed to register both implementation and implementation provider");
    } catch (Exception e) {
      // ok
    }
  }
*/

  public void testSelectableRegistrationFailure() {
    getContainer().registerActorClass(Role.role("100"), ConfigurableZooable.class);
    getContainer().registerActorClass(Role.role("bad_number"), ConfigurableWeeable.class);
    getContainer().registerSelector(ZooConfiguration.class, ZooConfigurationSelector.class);
    getContainer().registerActorClass(Role.role("aux"), SelectableRegistrationAux.class);
    try {
      getContainer().start();
      fail("container started with bad selection key");
    } catch (NumberFormatException e) {
      // ok
    }
  }

  public void testSimpleRegistration() {
    getContainer().registerActorClass(Role.role("zooable"), SimpleZooable.class);
    getContainer().registerActorClass(Role.role("aux"), SimpleRegistrationAux.class);
    assertTrue(ourValue == 0);
    getContainer().start();
    assertTrue(ourValue == 100);
    getContainer().stop();
    assertTrue(ourValue == 300);
  }

  protected void picoSetUp() throws Exception {
    ourValue = 0;
  }

  public static class SimpleRegistrationAux implements Startable {
    private final Zooable myZooable;

    public SimpleRegistrationAux(Zooable zooable) {
      myZooable = zooable;
    }

    public void start() {
      myZooable.zoo(1);
    }

    public void stop() {
      myZooable.zoo(2);
    }
  }

  public static class SelectableRegistrationAux implements Startable {
    private final Weeable myWeeable;
    private final Zooable myZooable;

    public SelectableRegistrationAux(Zooable zooable, Weeable weeable) {
      myZooable = zooable;
      myWeeable = weeable;
    }

    public void start() {
      myZooable.zoo(1);
      myWeeable.wee(4);
    }

    public void stop() {
      myZooable.zoo(2);
      myWeeable.wee(6);
    }
  }

  class ChildComponent implements Startable {
    ContainerPath myKey;
    ParentComponent myParent;
    ComponentContainer myProvider;

    public ChildComponent(ComponentContainer provider, ParentComponent parent, SelectionKeyHolder keyHolder) {
      myProvider = provider;
      myParent = parent;
      myKey = keyHolder.getKey();
    }

    public void start() {
      myChild = this;
    }

    public void stop() {
    }
  }

  class ParentComponent implements Startable {
    ContainerPath myKey;
    ComponentContainer myContainer;

    public ParentComponent(ComponentContainer container, SelectionKeyHolder keyHolder) {
      myContainer = container;
      myParent = this;
      myKey = keyHolder.getKey();
    }

    public void start() {
    }

    public void stop() {
    }

    public void createChild() {
      MutableComponentContainer childContainer = myContainer.createSubcontainer("1");
      childContainer.registerActorClass(Role.role("child"), ChildComponent.class);
      childContainer.start();
    }
  }


  public static interface Zooable {
    void zoo(int z);
  }

  public static interface Weeable {
    void wee(int z);
  }

  public static class SimpleZooable implements Zooable {
    public void zoo(int z) {
      ourValue += z * 100;
    }
  }

  public static class ConfigurableZooable implements Zooable {
    private final int myMultiplier;

    public ConfigurableZooable(ZooConfiguration configuration) {
      myMultiplier = configuration.getMultiplier();
    }

    public void zoo(int z) {
      ourValue += z * myMultiplier;
    }
  }

  public static class ConfigurableWeeable implements Weeable {
    private final int myMultiplier;

    public ConfigurableWeeable(ZooConfiguration configuration) {
      myMultiplier = configuration.getMultiplier();
    }

    public void wee(int z) {
      ourValue -= z * myMultiplier;
    }
  }


  public static interface ZooConfiguration {
    int getMultiplier();
  }

  public static class ZooConfigurationSelector implements ActorSelector<ZooConfiguration> {
    public ZooConfigurationSelector() {
    }

    public ZooConfiguration selectImplementation(ContainerPath selectionKey) {
      String multiplier = selectionKey.getName();
      return new ZooConfigurationImpl(Integer.parseInt(multiplier));
    }
  }

  public static class ZooConfigurationImpl implements ZooConfiguration {
    private final int myMultiplier;

    public ZooConfigurationImpl(int multiplier) {
      myMultiplier = multiplier;
    }

    public int getMultiplier() {
      return myMultiplier;
    }
  }

  public static class SelectionKeyHolder {
    private final ContainerPath myKey;

    public SelectionKeyHolder(ContainerPath key) {
      myKey = key;
    }

    public ContainerPath getKey() {
      return myKey;
    }
  }

  public static class SelectionKeyHolderSelector implements ActorSelector<SelectionKeyHolder> {
    public SelectionKeyHolder selectImplementation(ContainerPath selectionKey) {
      return new SelectionKeyHolder(selectionKey);
    }
  }
}
