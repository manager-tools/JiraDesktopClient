package com.almworks.container;

import com.almworks.api.container.ActorSelector;
import com.almworks.api.container.ContainerPath;
import org.picocontainer.*;
import org.picocontainer.defaults.DecoratingComponentAdapter;

/**
 * :todoc:
 *
 * @author sereda
 */
class SelectionParameter implements Parameter {
  private final Class<? extends ActorSelector> mySelectableProvider;

  private boolean reentrancyGuard = false;

  public SelectionParameter(Class<? extends ActorSelector> selectableProvider) {
    mySelectableProvider = selectableProvider;
  }

  public Object resolveInstance(PicoContainer container, ComponentAdapter targetAdapter, Class expectedType)
    throws PicoInitializationException {
    Selectionable selectionable = getSelectionable(targetAdapter);
    if (selectionable == null)
      throw new PicoInitializationException(
        "trying to substitute selectable parameter for class that does not support selection (" + targetAdapter + ")");
    ContainerPath selectionKey = selectionable.getSelectionKey();
    if (selectionKey == null)
      throw new PicoInitializationException("cannot initialize selectable parameter (" + mySelectableProvider +
        ") for a component that does not support selection (" +
        targetAdapter +
        ")");
    ActorSelector implSelector = getImplementationProvider(container);
    if (implSelector == null)
      throw new PicoInitializationException(
        "selectable provider of class " + mySelectableProvider + " is not registered");
    Object result = implSelector.selectImplementation(selectionKey);
    if (result != null && !expectedType.isAssignableFrom(result.getClass()))
      throw new PicoInitializationException(
        "implementation provider returned " + result + " that is not of expected type " + expectedType);
    return result;
  }

  public boolean isResolvable(PicoContainer container, ComponentAdapter targetAdapter, Class expectedType) {
    Selectionable selectionable = getSelectionable(targetAdapter);
    if (selectionable == null)
      return false;
/*
    // This is specifically NOT checked. If we leave this code and we try to depend on selectable implementation
    // from a class, that was not registered with a selection key, we'll access weird exceptions from Pico when he'll
    // try to find implementations of selectable interface.
    Object selectionKey = selectionable.getSelectionKey();
    if (selectionKey == null)
      return false;
*/
    ActorSelector implSelector = getImplementationProvider(container);
    return implSelector != null;
  }

  public void verify(PicoContainer container, ComponentAdapter adapter, Class expectedType)
    throws PicoIntrospectionException {
    if (!isResolvable(container, adapter, expectedType))
      throw new PicoIntrospectionException(expectedType.getName() + " is not resolvable");
  }

  public void accept(PicoVisitor visitor) {
    visitor.visitParameter(this);
  }

  private ActorSelector getImplementationProvider(PicoContainer container) {
    if (reentrancyGuard)
      throw new PicoInitializationException("there's a cyclic dependancy involving ActorSelector");
    reentrancyGuard = true;
    try {
      Object o = container.getComponentInstance(mySelectableProvider);
      if (o == null || !(o instanceof ActorSelector))
        return null;
      return (ActorSelector) o;
    } finally {
      reentrancyGuard = false;
    }
  }

  private Selectionable getSelectionable(ComponentAdapter adapter) {
    if (adapter instanceof Selectionable)
      return (Selectionable) adapter;
    if (adapter instanceof DecoratingComponentAdapter)
      return getSelectionable(((DecoratingComponentAdapter) adapter).getDelegate());
    return null;
  }
}
