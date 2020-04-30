package com.almworks.platform.components;

import com.almworks.api.container.RootContainer;
import com.almworks.api.platform.ComponentDescriptor;
import org.picocontainer.Startable;

/**
 * :todoc:
 *
 * @author sereda
 */
public class AbstractTestComponent implements ComponentDescriptor, Startable {
  public void registerActors(RootContainer container) {
  }

  public void start() {
  }

  public void stop() {
  }
}
