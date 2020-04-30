package com.almworks.util.ui;

import com.almworks.util.components.PlaceHolder;
import com.almworks.util.components.SelectionAccessor;
import com.almworks.util.config.Configuration;

/**
 * @author : Dyoma
 */
public class CollectionViewer extends SelectionHandler<Object> {
  private final UIComponentWrapper myNoSelection;
  private final UIComponentWrapper myNoComponent;

  private final PlaceHolder myElementViewerPlace;
  private final Configuration myViewerConfig;

  public CollectionViewer(
      SelectionAccessor<?> selectionAccessor, PlaceHolder elementViewerPlace, Configuration viewerConfig,
      UIComponentWrapper noSelection, UIComponentWrapper noComponent) {
    super(selectionAccessor);
    assert selectionAccessor != null;
    assert elementViewerPlace != null;
    assert noSelection != null;
    assert noComponent != null;
    myViewerConfig = viewerConfig;
    myElementViewerPlace = elementViewerPlace;
    myNoSelection = noSelection;
    myNoComponent = noComponent;
  }

  public CollectionViewer(
      SelectionAccessor<?> selectionAccessor, PlaceHolder elementViewerPlace, Configuration viewerConfig) {
    this(selectionAccessor, elementViewerPlace, viewerConfig,
        UIComponentWrapper.Simple.message("No constraint selected"),
        UIComponentWrapper.Simple.message("No additional options available"));
  }

  protected void onSelectionChanged(Object element) {
    UIComponentWrapper viewer;
    if (element == null) {
      viewer = myNoSelection;
    } else {
      UIPresentable presentable = UIUtil.getImplementor(element, UIPresentable.class);
      UIComponentWrapper uiWrapper = presentable != null ? presentable.createUIWrapper(myViewerConfig) : null;
      viewer = uiWrapper != null ? uiWrapper : myNoComponent;
    }
    myElementViewerPlace.showThenDispose(viewer);
  }
}
