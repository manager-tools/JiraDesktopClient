package com.almworks.util.fx;

import com.almworks.util.LogHelper;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.Hyperlink;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Path;
import javafx.stage.Window;
import org.controlsfx.control.PopOver;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

/**
 * Helps to control popOver associated with one specific node.
 */
public class PopOverHelper {
  public static final PopOverLocation LOCATION_TOP_LEFT = (popOver, hostBounds) -> {
    double arrowOffsetX = popOver.getArrowSize() + popOver.getCornerRadius() + popOver.getArrowIndent();
    popOver.setArrowLocation(PopOver.ArrowLocation.TOP_LEFT);
    return new Point2D(hostBounds.getMinX() + arrowOffsetX, hostBounds.getMaxY());
  };
  public static final PopOverLocation LOCATION_TOP_CENTER = (popOver, hostBounds) -> {
    popOver.setArrowLocation(PopOver.ArrowLocation.TOP_CENTER);
    return new Point2D((hostBounds.getMaxX() + hostBounds.getMinX()) / 2, hostBounds.getMaxY());
  };
  private final Node myOwnerNode;
  private final ChangeListener<Boolean> myWindowFocusListener = (observable, oldValue, newValue) -> {
    if (!newValue) hide();
  };
  private PopOverLocation myPopOverLocation = LOCATION_TOP_LEFT;
  private final ArrayList<String> myStylesheets = new ArrayList<>();
  private Paint myFill;
  private PopOver myPopOver = null;
  private Window myListensWindow = null;
  private boolean myAutoHide = true;

  public PopOverHelper(Node node) {
    myOwnerNode = node;
  }

  public void show(Node content) {
    listenWindow();
    hide();
    PopOver popOver = new PopOver();
    //          popOver.arrowSizeProperty().bind(Bindings.createDoubleBinding(() -> label.getFont().getSize(), label.fontProperty())); // Binding leads to NPE during show
    popOver.setDetachable(false);
    popOver.setHideOnEscape(true);
    popOver.setAutoHide(myAutoHide);
    popOver.setArrowSize(10);
    StackPane root = popOver.getRoot();
    FXUtil.addFontSizeStyle(root);
    popOver.setContentNode(content);
    root.getStylesheets().addAll(myStylesheets);

    Bounds bounds = myOwnerNode.localToScreen(myOwnerNode.getBoundsInLocal());
    Point2D location = myPopOverLocation.setupPopOver(popOver, bounds);
    popOver.show(myOwnerNode, location.getX(), location.getY());
    myPopOver = popOver;
    // Child of type Path is a border and arrow (including background of triangle). These is no way to set array color with CSS.
    // Note: Skin is created when popup is shown. To control array properties - show popup first
    Path path = (Path) ((StackPane) popOver.getSkin().getNode()).getChildren().get(0);
    if (myFill != null) {
      path.setFill(myFill);
      path.strokeProperty().bind(path.fillProperty());
    }
    path.setEffect(new DropShadow(4, 0, 2, Color.color(0, 0, 0, 0.25)));
  }

  /**
   * Sets popOver fill. Also it used as popOver border stroke
   * @param fill paint for fill and stroke of the popOver
   */
  public PopOverHelper setFill(Paint fill) {
    myFill = fill;
    return this;
  }

  /** @see PopOver#autoHide */
  public PopOverHelper setAutoHide(boolean autoHide) {
    myAutoHide = autoHide;
    return this;
  }

  public PopOverHelper setPopOverLocation(PopOverLocation popOverLocation) {
    if (popOverLocation != null) myPopOverLocation = popOverLocation;
    return this;
  }

  public PopOverHelper addStylesheet(String stylesheet) {
    if (stylesheet == null) LogHelper.error("Null stylesheet");
    else myStylesheets.add(stylesheet);
    return this;
  }

  /**
   * Hides popOver if it is visible
   */
  public void hide() {
    if (myPopOver != null) {
      myPopOver.hide();
      myPopOver = null;
    }
  }

  private void listenWindow() {
    Window window = FXUtil.findWindow(myOwnerNode);
    if (window == myListensWindow) return;
    if (myListensWindow != null) myListensWindow.focusedProperty().removeListener(myWindowFocusListener);
    if (window != null) window.focusedProperty().addListener(myWindowFocusListener);
    myListensWindow = window;
  }

  /**
   * This strategy defines how to position popOver relative to the host node
   */
  public interface PopOverLocation {
    /**
     * Sets up the popOver and calculates screen location of the arrow
     * @param popOver popOver to position
     * @param hostBounds host node bounds
     * @return location of popOver's arrow
     */
    @NotNull
    Point2D setupPopOver(PopOver popOver, Bounds hostBounds);
  }

  /**
   * Appends Close link to the bottom right of the popOver content
   */
  public static class WithClose {
    private final PopOverHelper myPopOver;
    private String myCloseText = FXUtil.I18N.getString("popOverHelper.withClose.close.text");
    private String myContentClass;

    public WithClose(PopOverHelper popOver) {
      myPopOver = popOver;
    }

    /**
     * Changes text of the close link
     * @param closeText close link text
     * @return this instance
     */
    public WithClose setCloseText(String closeText) {
      if (closeText != null) myCloseText = closeText;
      return this;
    }

    /**
     * Sets additional style class for the popOver content (if not-null)
     * @param contentClass add style class to popOver content
     * @return this instance
     */
    public WithClose setContentClass(String contentClass) {
      myContentClass = contentClass;
      return this;
    }

    public void show(Node content) {
      Hyperlink closeLink = new Hyperlink(myCloseText);
      closeLink.setStyle("-fx-border-width: 0; -fx-padding: .25em 0 0 0;");
      closeLink.setFocusTraversable(false);
      closeLink.setOnAction(event -> myPopOver.hide());
      VBox box = new VBox(content, new BorderPane(null, null, closeLink, null, null));
      box.setFillWidth(true);
      if (myContentClass != null) box.getStyleClass().add(myContentClass);
      myPopOver.show(box);
    }

    /** Hides the popOver if it is visible */
    public void hide() {
      myPopOver.hide();
    }
  }
}
