package com.almworks.explorer.tree;

import com.almworks.api.application.tree.ConnectionNode;
import com.almworks.api.application.tree.GenericNode;
import com.almworks.api.application.tree.TreeNodeFactory;
import com.almworks.api.config.ConfigNames;
import com.almworks.api.engine.Connection;
import com.almworks.util.config.Configuration;
import com.almworks.util.config.ConfigurationUtil;
import com.almworks.util.config.JDOMConfigurator;
import com.almworks.util.config.ReadonlyConfiguration;
import com.almworks.util.exec.ThreadGate;
import org.almworks.util.Log;

import java.util.List;

class CreateDefaultQueries {

  public static void perform(final ConnectionNode connectionNode, final TreeNodeFactory nodeFactory,
    final Runnable whenCreated) {
    final Connection connection = connectionNode.getConnection();
    ThreadGate.LONG(CreateDefaultQueries.class).execute(new Runnable() {
      public void run() {
        String xml = connection.buildDefaultQueriesXML();
        if (xml == null || xml.length() == 0) {
          runWhenCreated(whenCreated);
          return;
        }
        ReadonlyConfiguration configuration = null;
        try {
          configuration = JDOMConfigurator.parse(xml);
        } catch (Exception e) {
          Log.warn("cannot create default queries", e);
        }
        if (configuration != null) {
          final ReadonlyConfiguration finalConfiguration = configuration;
          ThreadGate.AWT.execute(new Runnable() {
            public void run() {
              createDefaultQueries(finalConfiguration, connectionNode, nodeFactory, whenCreated);
            }
          });
        }
      }
    });
  }

  private static void createDefaultQueries(ReadonlyConfiguration config, GenericNode parent,
    TreeNodeFactory nodeFactory, Runnable whenCreated) {

    try {
      createDefaultQueries(config, parent, nodeFactory);
    } finally {
      runWhenCreated(whenCreated);
    }
  }

  private static void runWhenCreated(Runnable whenCreated) {
    try {
      if (whenCreated != null)
        ThreadGate.AWT.execute(whenCreated);
    } catch (Exception e) {
      Log.debug(CreateDefaultQueries.class + ": " + e);
      // ignore
    }
  }

  /**
   * Accepts hierarchial configuration that contains subsets of &lt;presetQuery&gt; tags,
   * which may contain other presetQueries, and must contain one query information according
   * to query format that is used by UserQueryNode.
   * <p/>
   * todo standardize formula language
   */
  static void createDefaultQueries(ReadonlyConfiguration config, GenericNode parent,
    TreeNodeFactory nodeFactory) {

    List<? extends ReadonlyConfiguration> subsets = config.getAllSubsets();
    for (ReadonlyConfiguration distConfig : subsets) {
      String name = distConfig.getName();
      if (name == null)
        continue;

      if (ConfigNames.PRESET_LAZY_DISTRIBUTION_QUERY.equals(name)) {
        GenericNode node = nodeFactory.createLazyDistributionNode(parent, distConfig);
        // lazy distribution has all settings in <prototype> tag and creates children when instantiated
      } else if (ConfigNames.PRESET_DISTRIBUTION_FOLDER.equals(name)) {
        // todo folders
        GenericNode node = nodeFactory.createDistributionFolderNode(parent, stripSubnodes(distConfig));
        createDefaultQueries(distConfig, node, nodeFactory);
      } else if (ConfigNames.PRESET_DISTRIBUTION_QUERY.equals(name)) {
        // todo folders
        GenericNode node = nodeFactory.createDistributionQuery(parent, stripSubnodes(distConfig));
        createDefaultQueries(distConfig, node, nodeFactory);
      } else if (ConfigNames.PRESET_QUERY.equals(name)) {
        GenericNode node = nodeFactory.createUserQuery(parent, stripSubnodes(distConfig));
        createDefaultQueries(distConfig, node, nodeFactory);
      } else if (ConfigNames.PRESET_FOLDER.equals(name)) {
        GenericNode folder = nodeFactory.createGeneralFolder(parent, stripSubnodes(distConfig));
        createDefaultQueries(distConfig, folder, nodeFactory);
      }
    }
  }

  private static Configuration stripSubnodes(ReadonlyConfiguration queryConfig) {
    Configuration flat = ConfigurationUtil.copy(queryConfig);
    strip(flat, ConfigNames.PRESET_QUERY);
    strip(flat, ConfigNames.PRESET_FOLDER);
    strip(flat, ConfigNames.PRESET_DISTRIBUTION_QUERY);
    strip(flat, ConfigNames.PRESET_DISTRIBUTION_FOLDER);
    strip(flat, ConfigNames.PRESET_LAZY_DISTRIBUTION_QUERY);
    return flat;
  }

  private static void strip(Configuration flat, String configName) {
    List<Configuration> subList = flat.getAllSubsets(configName);
    if (subList != null) {
      Configuration[] subs = subList.toArray(new Configuration[subList.size()]);
      for (int j = 0; j < subs.length; j++)
        subs[j].removeMe();
    }
  }
}
