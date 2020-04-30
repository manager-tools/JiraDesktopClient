package com.almworks.config;

import com.almworks.api.container.ActorSelector;
import com.almworks.api.container.ContainerPath;
import com.almworks.api.exec.ApplicationManager;
import com.almworks.api.misc.WorkArea;
import com.almworks.util.DECL;
import com.almworks.util.config.Configuration;
import com.almworks.util.config.JDOMConfigurator;
import org.almworks.util.Log;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

public class ConfigComponent implements ActorSelector<Configuration> {
  private final Configuration myConfiguration;
  private final JDOMConfigurator myConfigurator;

  public ConfigComponent(WorkArea workArea, ApplicationManager applicationManager) {
    applicationManager.addListener(new ApplicationManager.Adapter() {
      public void onBeforeExit() {
        try {
          myConfigurator.save();
        } catch (IOException e) {
          Log.warn("cannot write configuration", e);
        }
      }
    });
    long start = System.currentTimeMillis();
    Log.debug("Config load started");
    myConfigurator = ConfigLoader.loadConfig(workArea);
    myConfiguration = myConfigurator.getConfiguration();
    long duration = System.currentTimeMillis() - start;
    Log.debug("Config load done: " + duration + "ms");
  }

  @NotNull
  public Configuration selectImplementation(ContainerPath selectionKey) {
    ContainerPath parentKey = selectionKey.getParentPath();
    Configuration configuration = parentKey != null ? selectImplementation(parentKey) : myConfiguration;
    String componentId = selectionKey.getName();
    List<Configuration> subsets = configuration.getAllSubsets(componentId);
    if (subsets.size() > 1) {
      String msg = "there are " + subsets.size() + " configurations for " + componentId;
      assert false : msg;
      DECL.robustnessCode();
      Log.warn(msg);
      Iterator<Configuration> iterator = subsets.iterator();
      iterator.next(); // leave first
      while (iterator.hasNext())
        iterator.next().removeMe();
    }
    Configuration subconfig = configuration.getOrCreateSubset(componentId);
    return subconfig;
  }
}
