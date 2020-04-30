package com.almworks.explorer.tree;

import com.almworks.api.application.tree.ConnectionLoadingNode;
import com.almworks.api.application.tree.QueryResult;
import com.almworks.api.config.ConfigNames;
import com.almworks.api.engine.CommonConfigurationConstants;
import com.almworks.api.engine.Connection;
import com.almworks.items.api.Database;
import com.almworks.util.components.CanvasRenderable;
import com.almworks.util.components.renderer.CellState;
import com.almworks.util.config.Configuration;
import com.almworks.util.images.Icons;
import com.almworks.util.threads.ThreadSafe;
import com.almworks.util.ui.ColorUtil;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

public class ConnectionLoadingNodeImpl extends GenericNodeImpl implements ConnectionLoadingNode {
  private final Connection myConnection;

  public ConnectionLoadingNodeImpl(Database db, Configuration parentConfig, Connection connection) {
    super(db, new MyPresentation(getName(connection)), parentConfig.getOrCreateSubset(ConfigNames.CONNECTION_LOADING_KEY));
    myConnection = connection;
  }

  private static String getName(Connection connection) {
    return "Loading " + connection.getConfiguration().getSetting(CommonConfigurationConstants.CONNECTION_NAME, "");
  }

  @NotNull
  @ThreadSafe
  public QueryResult getQueryResult() {
    return QueryResult.NO_RESULT;
  }

  public boolean isCopiable() {
    return false;
  }

  public Connection getConnection() {
    return myConnection;
  }

  private static final class MyPresentation implements CanvasRenderable {
    private final String myName;
    private Color myForeground;

    public MyPresentation(String name) {
      myName = name;
    }

    public void renderOn(com.almworks.util.components.Canvas canvas, CellState state) {
      if (!state.isSelected()) {
        if (myForeground == null)
          myForeground = ColorUtil.between(state.getDefaultForeground(), state.getDefaultBackground(), 0.5F);
        canvas.setForeground(myForeground);
      }
      canvas.setIcon(Icons.NODE_CONNECTION_WITH_ALERT);
      canvas.appendText(myName);
    }
  }
}
