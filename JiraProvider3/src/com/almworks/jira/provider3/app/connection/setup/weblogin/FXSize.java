package com.almworks.jira.provider3.app.connection.setup.weblogin;

import javafx.scene.Node;

import java.awt.*;

public interface FXSize {
  FXSize MIN = (node, direction) -> direction == Direction.WIDTH ? node.minWidth(-1) : node.minHeight(-1);
  FXSize PREF = (node, direction) -> direction == Direction.WIDTH ? node.prefWidth(-1) : node.prefHeight(-1);
  FXSize MAX = (node, direction) -> direction == Direction.WIDTH ? node.maxWidth(-1) : node.maxHeight(-1);

  double calc(Node node, Direction direction);
  default int calcInt(Node node, Direction direction) {
    double dim = Math.max(0, calc(node, direction));
    return dim >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int)Math.ceil(dim);
  }
  default double calcHeight(Node node) {
    return calc(node, Direction.HEIGHT);
  }
  default double calcWidth(Node node) {
    return calc(node, Direction.WIDTH);
  }
  default Dimension calcAwtDimension(Node node) {
    return new Dimension(calcInt(node, Direction.WIDTH), calcInt(node, Direction.HEIGHT));
  }
  enum Direction {
    HEIGHT, WIDTH
  }
}
