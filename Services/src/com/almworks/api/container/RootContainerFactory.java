package com.almworks.api.container;

import com.almworks.util.Env;
import com.almworks.util.commons.Factory;
import org.jetbrains.annotations.NotNull;

/**
 * This class provides means for api.container users to create root container.
 * Root container implementation is hidden, and this class must not depend on the implementation.
 * That's why reflection is used to search for a given class.
 * todo To be even less dependent, DEFAULT_CONTAINER may be moved to another place
 */
public abstract class RootContainerFactory implements Factory<RootContainer> {
  private static final String CONTAINER_KEY = "com.almworks.container";
  private static final String DEFAULT_CONTAINER = "com.almworks.container.DefaultRootContainerFactory";

  @NotNull
  public static RootContainerFactory findFactory() throws MissingContainerFactoryException {
    String containerImpl = Env.getString(CONTAINER_KEY);
    if (containerImpl == null) {
      // throw new MissingContainerFactoryException("container factory is not registered");
      containerImpl = DEFAULT_CONTAINER;
    }
    try {
      Class<?> clazz = Class.forName(containerImpl);
      Object factory = clazz.newInstance();
      if (!RootContainerFactory.class.isInstance(factory))
        throw new MissingContainerFactoryException("container factory is of a bad class");
      return (RootContainerFactory) factory;
    } catch (ClassNotFoundException e) {
      throw new MissingContainerFactoryException(e);
    } catch (IllegalAccessException e) {
      throw new MissingContainerFactoryException(e);
    } catch (InstantiationException e) {
      throw new MissingContainerFactoryException(e);
    }
  }
}
