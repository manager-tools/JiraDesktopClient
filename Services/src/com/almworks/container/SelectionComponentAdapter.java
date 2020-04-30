package com.almworks.container;

import com.almworks.api.container.ActorSelector;
import com.almworks.api.container.ContainerPath;
import org.picocontainer.Parameter;
import org.picocontainer.defaults.ComponentParameter;
import org.picocontainer.defaults.ConstructorInjectionComponentAdapter;

class SelectionComponentAdapter extends ConstructorInjectionComponentAdapter implements Selectionable {
  private final Selectors mySelectors;
  private final ContainerPath mySelectionKey;

  public SelectionComponentAdapter(ContainerPath selectionKey, Selectors selectors, Object componentKey,
    Class componentImplementation)
  {
    super(componentKey, componentImplementation, null, true);
    if (selectors == null)
      throw new NullPointerException("selectors");
    mySelectionKey = selectionKey;
    mySelectors = selectors;
  }

  protected Parameter[] createDefaultParameters(Class[] parameters) {
    Parameter[] componentParameters = new Parameter[parameters.length];
    for (int i = 0; i < parameters.length; i++) {
      Class<? extends ActorSelector> selectableProvider = mySelectors.getProvider(parameters[i]);
      if (selectableProvider == null)
        componentParameters[i] = ComponentParameter.DEFAULT;
      else
        componentParameters[i] = createSelectionProvider(selectableProvider);
    }
    return componentParameters;
  }

  private Parameter createSelectionProvider(Class<? extends ActorSelector> selectableProvider) {
    return new SelectionParameter(selectableProvider);
  }

  public ContainerPath getSelectionKey() {
    return mySelectionKey;
  }
}
