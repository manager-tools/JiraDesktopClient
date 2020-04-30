package com.almworks.container;

import org.picocontainer.ComponentAdapter;
import org.picocontainer.PicoContainer;
import org.picocontainer.PicoInitializationException;
import org.picocontainer.PicoIntrospectionException;
import org.picocontainer.defaults.DecoratingComponentAdapter;

public class GlobalSynchronizedComponentAdapter extends DecoratingComponentAdapter {
  public GlobalSynchronizedComponentAdapter(ComponentAdapter delegate) {
    super(delegate);
  }

  public Object getComponentInstance(PicoContainer container)
    throws PicoInitializationException, PicoIntrospectionException
  {
    synchronized (GlobalSynchronizedComponentAdapter.class) {
      return super.getComponentInstance(container);
    }
  }
}
