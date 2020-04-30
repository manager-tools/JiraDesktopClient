package com.almworks.util.fx.test;

import com.almworks.util.exec.NamedThreadFactory;
import com.sun.glass.ui.CommonDialogs;
import com.sun.javafx.embed.HostInterface;
import com.sun.javafx.geom.Path2D;
import com.sun.javafx.geom.Shape;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.perf.PerformanceTracker;
import com.sun.javafx.runtime.async.AsyncOperation;
import com.sun.javafx.runtime.async.AsyncOperationListener;
import com.sun.javafx.scene.text.HitInfo;
import com.sun.javafx.scene.text.TextLayoutFactory;
import com.sun.javafx.tk.*;
import com.sun.scenario.DelayedRunnable;
import com.sun.scenario.animation.AbstractMasterTimer;
import com.sun.scenario.effect.FilterContext;
import com.sun.scenario.effect.Filterable;
import javafx.geometry.Dimension2D;
import javafx.scene.image.Image;
import javafx.scene.input.Dragboard;
import javafx.scene.input.InputMethodRequests;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.shape.*;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import junit.framework.Assert;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.security.AccessControlContext;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;

public class MockToolkit extends Toolkit {

  public static final ThreadFactory FACTORY = NamedThreadFactory.create("MockFX Toolkit");
  private final MockPerformanceTracker myPerformanceTracker = new MockPerformanceTracker();
  private final Executor myExecutor = Executors.newSingleThreadExecutor(r -> {
    Thread thread = FACTORY.newThread(r);
    thread.setDaemon(true);
    return thread;
  });

  public static void install() {
    try {
      Field toolkitField = Toolkit.class.getDeclaredField("TOOLKIT");
      toolkitField.setAccessible(true);
      Assert.assertNull(toolkitField.get(null));
      toolkitField.set(null, new MockToolkit());
    } catch (NoSuchFieldException | IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public boolean canStartNestedEventLoop() {
    throw new RuntimeException("Not implemented mock");
  }

  @Override
  public ScreenConfigurationAccessor getScreenConfigurationAccessor() {
    throw new RuntimeException("Not implemented mock");
  }

  @Override
  public PerformanceTracker getPerformanceTracker() {
    return myPerformanceTracker;
  }

  @Override
  public void startup(Runnable runnable) {
    defer(runnable);
  }

  @Override
  public void defer(Runnable runnable) {
    myExecutor.execute(runnable);
  }

  @Override
  public boolean init() {
    throw new RuntimeException("Not implemented mock");
  }

  @Override
  public Object enterNestedEventLoop(Object key) {
    throw new RuntimeException("Not implemented mock");
  }

  @Override
  public void exitNestedEventLoop(Object key, Object rval) {
    throw new RuntimeException("Not implemented mock");
  }

  @Override
  public boolean isNestedLoopRunning() {
    throw new RuntimeException("Not implemented mock");
  }

  @Override
  public TKStage createTKStage(Window peerWindow, boolean securityDialog, StageStyle stageStyle, boolean primary, Modality modality, TKStage owner, boolean rtl, AccessControlContext acc) {
    throw new RuntimeException("Not implemented mock");
  }

  @Override
  public TKStage createTKPopupStage(Window peerWindow, StageStyle popupStyle, TKStage owner, AccessControlContext acc) {
    throw new RuntimeException("Not implemented mock");
  }

  @Override
  public TKStage createTKEmbeddedStage(HostInterface host, AccessControlContext acc) {
    throw new RuntimeException("Not implemented mock");
  }

  @Override
  public AppletWindow createAppletWindow(long parent, String serverName) {
    throw new RuntimeException("Not implemented mock");
  }

  @Override
  public void closeAppletWindow() {
    throw new RuntimeException("Not implemented mock");
  }

  @Override
  public void requestNextPulse() {
    throw new RuntimeException("Not implemented mock");
  }

  @Override
  public Future addRenderJob(RenderJob rj) {
    throw new RuntimeException("Not implemented mock");
  }

  @Override
  public ImageLoader loadImage(String url, int width, int height, boolean preserveRatio, boolean smooth) {
    throw new RuntimeException("Not implemented mock");
  }

  @Override
  public ImageLoader loadImage(InputStream stream, int width, int height, boolean preserveRatio, boolean smooth) {
    throw new RuntimeException("Not implemented mock");
  }

  @Override
  public AsyncOperation loadImageAsync(AsyncOperationListener<? extends ImageLoader> listener, String url, int width, int height, boolean preserveRatio, boolean smooth) {
    throw new RuntimeException("Not implemented mock");
  }

  @Override
  public ImageLoader loadPlatformImage(Object platformImage) {
    throw new RuntimeException("Not implemented mock");
  }

  @Override
  public PlatformImage createPlatformImage(int w, int h) {
    throw new RuntimeException("Not implemented mock");
  }

  @Override
  public Map<Object, Object> getContextMap() {
    throw new RuntimeException("Not implemented mock");
  }

  @Override
  public int getRefreshRate() {
    throw new RuntimeException("Not implemented mock");
  }

  @Override
  public void setAnimationRunnable(DelayedRunnable animationRunnable) {
    throw new RuntimeException("Not implemented mock");
  }

  @Override
  public PerformanceTracker createPerformanceTracker() {
    throw new RuntimeException("Not implemented mock");
  }

  @Override
  public void waitFor(Task t) {
    throw new RuntimeException("Not implemented mock");
  }

  @Override
  protected Object createColorPaint(Color paint) {
    throw new RuntimeException("Not implemented mock");
  }

  @Override
  protected Object createLinearGradientPaint(LinearGradient paint) {
    throw new RuntimeException("Not implemented mock");
  }

  @Override
  protected Object createRadialGradientPaint(RadialGradient paint) {
    throw new RuntimeException("Not implemented mock");
  }

  @Override
  protected Object createImagePatternPaint(ImagePattern paint) {
    throw new RuntimeException("Not implemented mock");
  }

  @Override
  public void accumulateStrokeBounds(Shape shape, float[] bbox, StrokeType type, double strokewidth, StrokeLineCap cap, StrokeLineJoin join, float miterLimit, BaseTransform tx) {
    throw new RuntimeException("Not implemented mock");
  }

  @Override
  public boolean strokeContains(Shape shape, double x, double y, StrokeType type, double strokewidth, StrokeLineCap cap, StrokeLineJoin join, float miterLimit) {
    throw new RuntimeException("Not implemented mock");
  }

  @Override
  public Shape createStrokedShape(Shape shape, StrokeType type, double strokewidth, StrokeLineCap cap, StrokeLineJoin join, float miterLimit, float[] dashArray, float dashOffset) {
    throw new RuntimeException("Not implemented mock");
  }

  @Override
  public int getKeyCodeForChar(String character) {
    throw new RuntimeException("Not implemented mock");
  }

  @Override
  public Dimension2D getBestCursorSize(int preferredWidth, int preferredHeight) {
    throw new RuntimeException("Not implemented mock");
  }

  @Override
  public int getMaximumCursorColors() {
    throw new RuntimeException("Not implemented mock");
  }

  @Override
  public PathElement[] convertShapeToFXPath(Object shape) {
    throw new RuntimeException("Not implemented mock");
  }

  @Override
  public HitInfo convertHitInfoToFX(Object hit) {
    throw new RuntimeException("Not implemented mock");
  }

  @Override
  public Filterable toFilterable(Image img) {
    throw new RuntimeException("Not implemented mock");
  }

  @Override
  public FilterContext getFilterContext(Object config) {
    throw new RuntimeException("Not implemented mock");
  }

  @Override
  public boolean isForwardTraversalKey(KeyEvent e) {
    throw new RuntimeException("Not implemented mock");
  }

  @Override
  public boolean isBackwardTraversalKey(KeyEvent e) {
    throw new RuntimeException("Not implemented mock");
  }

  @Override
  public AbstractMasterTimer getMasterTimer() {
    throw new RuntimeException("Not implemented mock");
  }

  @Override
  public FontLoader getFontLoader() {
    throw new RuntimeException("Not implemented mock");
  }

  @Override
  public TextLayoutFactory getTextLayoutFactory() {
    return new MockTextLayoutFactory();
  }

  @Override
  public Object createSVGPathObject(SVGPath svgpath) {
    throw new RuntimeException("Not implemented mock");
  }

  @Override
  public Path2D createSVGPath2D(SVGPath svgpath) {
    throw new RuntimeException("Not implemented mock");
  }

  @Override
  public boolean imageContains(Object image, float x, float y) {
    throw new RuntimeException("Not implemented mock");
  }

  @Override
  public TKClipboard getSystemClipboard() {
    throw new RuntimeException("Not implemented mock");
  }

  @Override
  public TKSystemMenu getSystemMenu() {
    throw new RuntimeException("Not implemented mock");
  }

  @Override
  public TKClipboard getNamedClipboard(String name) {
    throw new RuntimeException("Not implemented mock");
  }

  @Override
  public ScreenConfigurationAccessor setScreenConfigurationListener(TKScreenConfigurationListener listener) {
    throw new RuntimeException("Not implemented mock");
  }

  @Override
  public Object getPrimaryScreen() {
    throw new RuntimeException("Not implemented mock");
  }

  @Override
  public List<?> getScreens() {
    throw new RuntimeException("Not implemented mock");
  }

  @Override
  public void registerDragGestureListener(TKScene s, Set<TransferMode> tm, TKDragGestureListener l) {
    throw new RuntimeException("Not implemented mock");
  }

  @Override
  public void startDrag(TKScene scene, Set<TransferMode> tm, TKDragSourceListener l, Dragboard dragboard) {
    throw new RuntimeException("Not implemented mock");
  }

  @Override
  public void enableDrop(TKScene s, TKDropTargetListener l) {
    throw new RuntimeException("Not implemented mock");
  }

  @Override
  public void installInputMethodRequests(TKScene scene, InputMethodRequests requests) {
    throw new RuntimeException("Not implemented mock");
  }

  @Override
  public Object renderToImage(ImageRenderingContext context) {
    throw new RuntimeException("Not implemented mock");
  }

  @Override
  public CommonDialogs.FileChooserResult showFileChooser(TKStage ownerWindow, String title, File initialDirectory, String initialFileName, FileChooserType fileChooserType, List<FileChooser.ExtensionFilter> extensionFilters, FileChooser.ExtensionFilter selectedFilter) {
    throw new RuntimeException("Not implemented mock");
  }

  @Override
  public File showDirectoryChooser(TKStage ownerWindow, String title, File initialDirectory) {
    throw new RuntimeException("Not implemented mock");
  }

  @Override
  public long getMultiClickTime() {
    throw new RuntimeException("Not implemented mock");
  }

  @Override
  public int getMultiClickMaxX() {
    throw new RuntimeException("Not implemented mock");
  }

  @Override
  public int getMultiClickMaxY() {
    throw new RuntimeException("Not implemented mock");
  }

}
