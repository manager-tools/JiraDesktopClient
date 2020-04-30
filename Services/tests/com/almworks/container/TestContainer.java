package com.almworks.container;

import com.almworks.api.container.ActorSelector;
import com.almworks.api.container.ContainerPath;
import com.almworks.util.config.Configuration;
import com.almworks.util.config.MapMedium;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.picocontainer.PicoContainer;
import org.picocontainer.defaults.DefaultPicoContainer;

public class TestContainer extends ComponentContainerImpl {
  public TestContainer() {
    super(new TestContainerParent(), "test");
    registerSelector(Configuration.class, TestConfiguration.class);
  }

  private static class TestConfiguration implements ActorSelector<Configuration> {
    public Configuration selectImplementation(ContainerPath selectionKey) {
      return MapMedium.createConfig();
    }
  }

  private static class TestContainerParent implements ComponentContainerParent {
    private final PicoContainer myContainer = new DefaultPicoContainer();
    @NotNull
    public PicoContainer getPico() {
      return myContainer;
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
  }
}
