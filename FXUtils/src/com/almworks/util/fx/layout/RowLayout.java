package com.almworks.util.fx.layout;

import com.almworks.util.LogHelper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import org.almworks.util.ArrayUtil;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Lays out nodes in a row. Controls vertical location and size of it's children.<br>
 * Constraints:
 * <ul>
 *   <li>{@link #GUIDE_LINE Guideline}</li>
 *   <li>{@link #SAME_VERTICAL use same vertical placement as another node}</li>
 * </ul>
 */
public class RowLayout extends Pane {
  /**
   * If child has this constraint it's vertical position is defined by the guideline.<br>
   * If child has no guideline constraint it is positioned at the top of the parent.
   */
  public static final LayoutConstraint<GuideLine> GUIDE_LINE = new LayoutConstraint<>("almworks-rowLayout-guideLine");

  /**
   * If child has this constraint it's vertical size and position is the same as Node specified.<br>
   * The child must have not {@link #GUIDE_LINE} constraint.
   */
  public static final LayoutConstraint<Node> SAME_VERTICAL = new LayoutConstraint<>("almworks-rowLayout-sameVertical");

  /**
   * Controls where baseline guideline is placed in case of the this pane is taller then all baselined children
   * (there are several possible vertical positions for baselined children)<br>
   * TOP - at the topmost possible position<br>
   * BOTTOM - at the lowest possible position<br>
   * CENTER - as close to the geometric height center as possible<br>
   * BASELINE, null - at the middle between of the topmost and lowest positions.
   */
  private final SimpleObjectProperty<VPos> myBaselinePlacement = new SimpleObjectProperty<>(VPos.CENTER);

  @Override
  protected void layoutChildren() {
    SnapToPixel snap = SnapToPixel.fromRegion(this);
    List<Node> nodes = getManagedChildren();
    double[] widths = distributeWidth(nodes, getWidth());
    Baseline[] baselines = new Baseline[nodes.size()];
    double maxHeight = 0;
    for (int i = 0; i < nodes.size(); i++) {
      Node node = nodes.get(i);
      GuideLine guideLine = getGuideLine(node);
      if (guideLine == null) continue;
      switch (guideLine) {
        case BASELINE:
          baselines[i] = Baseline.forNode(node, widths[i], getHeight(), snap);
          break;
        case CENTER:
          maxHeight = Math.max(DesiredDimension.PREF_HEIGHT.get(node, widths[i], snap), maxHeight);
          break;
      }
    }
    double[] xs = copmuteX(nodes, widths);
    placeChildren(nodes, xs, widths, baselines, maxHeight);
    placeSameVertical(nodes, xs, widths);
  }

  @Nullable
  private static GuideLine getGuideLine(Node node) {
    return GUIDE_LINE.get(node);
  }

  private double[] copmuteX(List<Node> nodes, double[] widths) {
    SnapToPixel snap = SnapToPixel.fromRegion(this);
    Insets insets = getInsets();
    double x = snap.size(insets.getTop());
    double[] result = new double[nodes.size()];
    for (int i = 0; i < nodes.size(); i++) {
      result[i] = x;
      x += widths[i];
    }
    return result;
  }

  private void placeSameVertical(List<Node> nodes, double[] xs, double[] widths) {
    for (int i = 0; i < nodes.size(); i++) {
      Node node = nodes.get(i);
      Node copyFrom = SAME_VERTICAL.get(node);
      if (copyFrom == null) continue;
      Bounds layoutBounds = copyFrom.getLayoutBounds();
      node.resizeRelocate(xs[i], copyFrom.getLayoutY() + layoutBounds.getMaxY(), widths[i], layoutBounds.getHeight());
    }
  }

  private void placeChildren(List<Node> nodes, double[] xs, double[] width, Baseline[] baselines, double maxHeight) {
    SnapToPixel snap = SnapToPixel.fromRegion(this);
    Insets insets = getInsets();
    double clientHeight = snap.size(getHeight() - insets.getTop() - insets.getBottom());
    double baseline = snap.size(Baseline.chooseBaseline(baselines, clientHeight, myBaselinePlacement.get()));
    double center = snap.size(clientHeight - maxHeight / 2);
    double top = snap.size(insets.getTop());
    for (int i = 0; i < nodes.size(); i++) {
      Node node = nodes.get(i);
      GuideLine guideLine = getGuideLine(node);
      double y;
      double height;
      double nodeWidth = width[i];
      if (guideLine != null)
        switch (guideLine) {
          case BASELINE:
            y = baseline - baselines[i].myAscent;
            height = baselines[i].myHeight;
            break;
          case CENTER:
            height = DesiredDimension.PREF_HEIGHT.get(node, nodeWidth, snap);
            y = center - height / 2;
            break;
          default:
            LogHelper.error("Unknown guideline", guideLine);
            continue;
      }
      else {
        if (SAME_VERTICAL.get(node) != null) continue;
        y = 0;
        height = DesiredDimension.PREF_HEIGHT.get(node, nodeWidth, snap);
      }
      if (height >  clientHeight) {
        double min = DesiredDimension.MIN_HEIGHT.get(node, nodeWidth, snap);
        height = Math.max(height, min);
      }
      y = snap.size(y);
      height = snap.size(height);
      node.resizeRelocate(xs[i], top + y, nodeWidth, height);
    }
  }

  private double computeSize(DesiredDimension dimension, double across) {
    if (dimension.isWidth()) return computeWidth(dimension, across);
    else return computeHeight(dimension, across);
  }

  private double computeHeight(DesiredDimension dimension, double totalWidth) {
    List<Node> nodes = getManagedChildren();
    double[] width = distributeWidth(nodes, totalWidth);
    SnapToPixel snap = SnapToPixel.fromRegion(this);
    double maxHeight = 0;
    double maxAscent = 0;
    double maxDescent = 0;
    for (int i = 0; i < nodes.size(); i++) {
      Node node = nodes.get(i);
      GuideLine guideLine = getGuideLine(node);
      if (guideLine == null) continue;
      double h = snap.size(node.isResizable() ? dimension.get(node, width[i], snap) : node.getLayoutBounds().getHeight());
      switch (guideLine) {
        case BASELINE:
          double ascent = snap.size(node.getBaselineOffset());
          double descent = Math.max(0, h - ascent);
          maxAscent = Math.max(maxAscent, ascent);
          maxDescent = Math.max(maxDescent, descent);
          break;
        case CENTER:
          maxHeight = Math.max(maxHeight, h);
          break;
        default:
          LogHelper.error("Unknown guideline", guideLine);
          //noinspection UnnecessaryContinue
          continue;
      }
    }
    return Math.max(maxHeight, maxAscent + maxDescent);
  }

  private double[] distributeWidth(List<Node> nodes, double totalWidth) {
    double[] result = new double[nodes.size()];
    SnapToPixel snap = SnapToPixel.fromRegion(this);
    for (int i = 0; i < nodes.size(); i++) {
      Node node = nodes.get(i);
      double w = node.isResizable() ? node.prefWidth(-1) : node.getLayoutBounds().getWidth();
      w = snap.size(w);
      result[i] = w;
    }
    if (totalWidth < 0) return result;
    double sum = ArrayUtil.sum(result);
    double amount = snap.change(totalWidth - sum);
    DesiredDimension.Limit limit = amount > 0 ? DesiredDimension.MAX_WIDTH : DesiredDimension.MIN_WIDTH;
    FXLayoutUtil.distribute(result, amount, snap, index ->  {
      Node node = nodes.get((int) index);
      if (!node.isResizable()) return node.getLayoutBounds().getWidth();
      return limit.get(node, -1, snap);
    });
    return result;
  }

  private double computeWidth(DesiredDimension dimension, double height) {
    SnapToPixel snap = SnapToPixel.fromRegion(this);
    double total = 0;
    for (Node node : getManagedChildren()) {
      double size = dimension.get(node, height, snap);
      total += Math.max(size, 0);
    }
    Insets insets = getInsets();
    total += dimension.sumInsets(insets, snap);
    return total;
  }

  @Override
  protected double computePrefWidth(double height) {
    return computeSize(DesiredDimension.PREF_WIDTH, height);
  }

  @Override
  protected double computePrefHeight(double width) {
    return computeSize(DesiredDimension.PREF_HEIGHT, width);
  }

  @Override
  protected double computeMinWidth(double height) {
    return computeSize(DesiredDimension.MIN_WIDTH, height);
  }

  @Override
  protected double computeMinHeight(double width) {
    return computeSize(DesiredDimension.MIN_HEIGHT, width);
  }

  @Override
  protected double computeMaxWidth(double height) {
    return computeSize(DesiredDimension.MAX_WIDTH, height);
  }

  @Override
  protected double computeMaxHeight(double width) {
    return computeSize(DesiredDimension.MAX_HEIGHT, width);
  }

  private static class Baseline {
    private final double myAscent;
    private final double myHeight;

    public Baseline(double ascent, double height) {
      myAscent = ascent;
      myHeight = height;
    }

    public static Baseline forNode(Node node, double width, double height, SnapToPixel snap) {
      double h = node.prefHeight(width);
      double ascent = snap.size(node.getBaselineOffset());
      if (ascent == BASELINE_OFFSET_SAME_AS_HEIGHT) return null;
      if (h > height) {
        double min = node.minHeight(width);
        h = Math.max(min, height);
      }
      h = snap.size(h);
      return new Baseline(ascent, h);
    }

    public static double chooseBaseline(Baseline[] baselines, double clientHeight, VPos vPos) {
      double maxAscent = 0;
      double maxDescent = 0;
      for (Baseline baseline : baselines) {
        if (baseline == null) continue;
        maxAscent = Math.max(baseline.myAscent, maxAscent);
        maxDescent = Math.max(baseline.getDescent(), maxDescent);
      }
      if (maxAscent + maxDescent >= clientHeight) return maxAscent;
      switch (vPos) {
        case TOP: return maxAscent;
        case BOTTOM: return clientHeight - maxDescent;
        case CENTER:
          double center = clientHeight / 2;
          if (maxAscent > maxDescent) return maxAscent > center ? maxAscent : center;
          else return maxDescent > center ? clientHeight - maxDescent : center;
        case BASELINE:
        default:
          return maxAscent + (clientHeight - maxAscent - maxDescent) / 2;
      }
    }

    private double getDescent() {
      return Math.max(0, myHeight - myAscent);
    }
  }

  /**
   * Defines how to place node vertically.
   * The parent calculates the position of a guideline, and then positions children relative to the specified guideline.
   */
  public enum GuideLine  {
    /**
     * Child's baseline is positioned at "Baseline" guideline.
     */
    BASELINE,
    /**
     * Child's vertical center is positioned at the vertical center of parent's client area (inside insets)
     */
    CENTER
  }
}
