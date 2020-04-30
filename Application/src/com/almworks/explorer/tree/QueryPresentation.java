package com.almworks.explorer.tree;

import com.almworks.api.config.ConfigNames;
import com.almworks.util.L;
import com.almworks.util.Terms;
import com.almworks.util.config.Configuration;
import com.almworks.util.images.Icons;
import com.almworks.util.threads.Threads;

import javax.swing.*;

class QueryPresentation extends CounterPresentation {
  private boolean myCanRunNow = false;
  private boolean mySynced = false;

  public QueryPresentation(String name) {
    this(name, Icons.NODE_QUERY, Icons.NODE_QUERY);
  }

  public QueryPresentation(String name, Icon openIcon, Icon closedIcon) {
    super(name, openIcon, closedIcon);
  }

  public boolean setCanRunNow(boolean value) {
    if (myCanRunNow == value)
      return false;
    myCanRunNow = value;
    setIcon(myCanRunNow ? Icons.NODE_QUERY : Icons.NODE_QUERY.getGrayed());
    return true;
  }

  public boolean setSynced(boolean synced) {
    Threads.assertAWTThread();
    if (mySynced == synced)
      return false;
    mySynced = synced;
    return true;
  }

  protected boolean isSynced() {
    return mySynced;
  }

  public static QueryPresentation create(final Configuration configuration) {
    String name = configuration.getSetting(ConfigNames.NAME_SETTING, null);
    if (name == null) {
      configuration.setSetting(ConfigNames.DEFAULT_NAME_FLAG, true);
      name = L.treeNode("New " + Terms.Query);
    }
    boolean defaultName = configuration.getBooleanSetting(ConfigNames.DEFAULT_NAME_FLAG, false);
    if (name != null && !defaultName)
      return new QueryPresentation(name);
    return new QueryPresentation(name) {
      public void setUserTyped(boolean userTyped) {
        if (userTyped)
          configuration.removeSettings(ConfigNames.DEFAULT_NAME_FLAG);
      }

      public boolean isDefaultContent() {
        return configuration.getBooleanSetting(ConfigNames.DEFAULT_NAME_FLAG, false);
      }
    };
  }
}
