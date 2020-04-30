package com.almworks.util.ui;

import com.almworks.util.collections.Convertor;
import com.almworks.util.commons.Factory;
import com.almworks.util.components.PlaceHolder;
import com.almworks.util.config.Configuration;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.model.BasicScalarModel;
import com.almworks.util.model.ModelUtils;
import com.almworks.util.model.ScalarModel;
import com.almworks.util.properties.PropertyMap;
import com.almworks.util.ui.actions.PresentationKey;
import org.almworks.util.Collections15;
import org.almworks.util.Util;
import org.almworks.util.detach.Lifecycle;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

/**
 * @author : Dyoma
 */
public interface ElementViewer<T> extends UIComponentWrapper {
  void showElement(T item);

  /**
   * Provides additional component to be placed on toolbar. Could be null.
   */
  @Nullable
  JComponent getToolbarEastComponent();

  @Nullable
  ScalarModel<? extends JComponent> getToolbarActionsHolder();

  /**
   * Returns a {@code PlaceHolder} for the {@code ArtifactViewer} to put
   * its bug/issue toolbars. The placehoder is returned by
   * {@code CommonIssueViewer#getToolbarPlace()}. It is implementer's
   * responsibility to pull this placeholder through all the layers
   * above the {@code CommonIssueViewer}.
   * @return The placeholder for the bug/issue toolbars;
   * {@code null} if toolbars aren't supported.
   */
  @Nullable
  PlaceHolder getToolbarPlace();

  @Nullable
  PlaceHolder getBottomPlace();

  class Composite<T> extends CompositeComponentWrapper<ElementViewer<T>> implements ElementViewer<T> {
    private T myCurrentItem = null;
    private final Set<String> myInitialized = Collections15.hashSet();
    private final BasicScalarModel<JComponent> myToolbarActionsHolder = BasicScalarModel.createWithValue(null, true);
    private final Lifecycle myToolbarLife = new Lifecycle();

    public Composite(Map<PropertyMap, ElementViewer<T>> viewers, Configuration config) {
      super(viewers, config);
    }

    public void showElement(T item) {
      myInitialized.clear();
      myCurrentItem = item;
      ElementViewer<T> viewer = getCurrentViewer();
      if (viewer != null) {
        viewer.showElement(myCurrentItem);
      }
      reattachToolbar();
    }

    public JComponent getToolbarEastComponent() {
      return getSelectionCombobox();
    }

    public ScalarModel<? extends JComponent> getToolbarActionsHolder() {
      return myToolbarActionsHolder;
    }

    protected void showViewer(String viewerName) {
      if (myCurrentItem != null && !myInitialized.contains(viewerName)) {
        ElementViewer<T> viewer = getViewer(viewerName, true);
        if (viewer != null) {
          viewer.showElement(myCurrentItem);
        }
      }
      super.showViewer(viewerName);
      reattachToolbar();
    }

    private void reattachToolbar() {
      myToolbarLife.cycle();
      ElementViewer<T> viewer = getCurrentViewer();
      if (viewer != null) {
        ScalarModel<? extends JComponent> holder = viewer.getToolbarActionsHolder();
        if (holder != null)
          myToolbarLife.lifespan().add(ModelUtils.replicate(holder, myToolbarActionsHolder, ThreadGate.AWT));
      }
    }

    public PlaceHolder getToolbarPlace() {
      ElementViewer<T> viewer = getCurrentViewer();
      if (viewer != null) {
        return viewer.getToolbarPlace();
      }
      return null;
    }

    public PlaceHolder getBottomPlace() {
      ElementViewer<T> viewer = getCurrentViewer();
      return viewer != null ? viewer.getBottomPlace() : null;
    }
  }


  class CompositeFactory<T> {
    private static final Comparator<PropertyMap> VIEWER_PRESENTATION_COMPARATOR = new Comparator<PropertyMap>() {
      public int compare(PropertyMap map1, PropertyMap map2) {
        String name1 = Util.NN(map1.get(PresentationKey.NAME));
        String name2 = Util.NN(map2.get(PresentationKey.NAME));
        return String.CASE_INSENSITIVE_ORDER.compare(name1, name2);
      }
    };
    private final SortedMap<PropertyMap, Convertor<Configuration, ElementViewer<T>>> myViewers =
      Collections15.<PropertyMap, Convertor<Configuration, ElementViewer<T>>>treeMap(VIEWER_PRESENTATION_COMPARATOR);

    public void addViewer(String name, final Factory<ElementViewer<T>> viewerFactory) {
      addViewer(name, new Convertor<Configuration, ElementViewer<T>>() {
        public ElementViewer<T> convert(Configuration value) {
          return viewerFactory.create();
        }
      });
    } 

    public void addViewer(String name, Convertor<Configuration, ElementViewer<T>> viewerFactory) {
      PropertyMap presentation = PropertyMap.create(PresentationKey.NAME, name);
      myViewers.put(presentation, viewerFactory);             
    }

    public ElementViewer<T> createViewer(Configuration config) {
      SortedMap<PropertyMap, ElementViewer<T>> viewers = Collections15.treeMap(VIEWER_PRESENTATION_COMPARATOR);
      for (Map.Entry<PropertyMap, Convertor<Configuration, ElementViewer<T>>> entry : myViewers.entrySet()) {
        viewers.put(entry.getKey(), entry.getValue().convert(config));
      }
      Composite<T> composite = new Composite<T>(viewers, config);
      composite.start();
      return composite;
    }

    public static <T> CompositeFactory<T> create() {
      return new CompositeFactory<T>();
    }
  }


  class Empty<T> implements ElementViewer<T> {
    private final JPanel myJPanel = new JPanel();

    public void showElement(T item) {
    }

    public JComponent getToolbarEastComponent() {
      return null;
    }

    public ScalarModel<? extends JComponent> getToolbarActionsHolder() {
      return null;
    }

    public JComponent getComponent() {
      return myJPanel;
    }

    public PlaceHolder getToolbarPlace() {
      return null;
    }

    public PlaceHolder getBottomPlace() {
      return null;
    }

    public void dispose() {
    }
  }
}
