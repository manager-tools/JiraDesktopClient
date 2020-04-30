package com.almworks.util.ui;

import com.almworks.util.advmodel.SelectionInListModel;
import com.almworks.util.advmodel.SelectionListener;
import com.almworks.util.components.AComboBox;
import com.almworks.util.components.PlaceHolder;
import com.almworks.util.config.Configuration;
import com.almworks.util.properties.PropertyMap;
import com.almworks.util.ui.actions.PresentationKey;
import org.almworks.util.Collections15;
import org.almworks.util.Log;
import org.almworks.util.Util;
import org.almworks.util.detach.DetachComposite;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author : Dyoma
 */
public class CompositeComponentWrapper <W extends UIComponentWrapper> implements UIComponentWrapper {
  private static final String CURRENT_VIEWER = "currentViewer";
  private final PlaceHolder myVariantPlace = new PlaceHolder();
  private final JPanel myWholePanel = new JPanel(new BorderLayout(0, 0));
  private final Configuration myConfig;
  private final Map<String, W> myViewerMap = Collections15.hashMap();
  private AComboBox<String> mySelectionCombobox = new AComboBox<String>();
  private SelectionInListModel<String> myViewersSelectionModel;
  private final DetachComposite myUIDetach = new DetachComposite();
  private String myCurrentViewerName = null;
  private final String myDefaultViewerName;

  protected CompositeComponentWrapper(Map<PropertyMap, W> viewers, Configuration config) {
    assert viewers.size() > 0;
    assert config != null;

    myConfig = config;
    myWholePanel.add(myVariantPlace, BorderLayout.CENTER);

    List<String> viewNames = Collections15.arrayList();
    Map.Entry<PropertyMap, W> defaultEntry = viewers.entrySet().iterator().next();
    if (viewers.size() > 1) {
      for (final Map.Entry<PropertyMap, W> entry : viewers.entrySet()) {
        PropertyMap presentation = entry.getKey();
        assert presentation.containsKey(PresentationKey.NAME);
        final String name = presentation.get(PresentationKey.NAME);
        myViewerMap.put(name, entry.getValue());
        viewNames.add(name);
      }
      myDefaultViewerName = viewNames.get(0);
    } else {
      PropertyMap presentation = defaultEntry.getKey();
      myDefaultViewerName = presentation.get(PresentationKey.NAME);
      myViewerMap.put(myDefaultViewerName, defaultEntry.getValue());
      viewNames.add(myDefaultViewerName);
    }

    myViewersSelectionModel = SelectionInListModel.create(viewNames, null);
    mySelectionCombobox.setModel(myViewersSelectionModel);
    if (viewNames.size() < 2)
      mySelectionCombobox.setVisible(false);
  }

  protected void start() {
    myViewersSelectionModel.addSelectionListener(myUIDetach, new SelectionListener.Adapter() {
      public void onSelectionChanged() {
        showViewer(myViewersSelectionModel.getSelectedItem());
      }
    });
    showViewer(myConfig.getSetting(CURRENT_VIEWER, myDefaultViewerName));
  }

  public void dispose() {
    assert !myWholePanel.isDisplayable();
    for (W wrapper : getViewers())
      wrapper.dispose();
    myViewerMap.clear();
    myVariantPlace.dispose();
  }

  protected Collection<W> getViewers() {
    return myViewerMap.values();
  }

  protected void showViewer(String viewerName) {
    if (!Util.equals(viewerName, myCurrentViewerName)) {
      myCurrentViewerName = viewerName;
      myViewersSelectionModel.setSelectedItem(viewerName);
      if (viewerName != null)
        myConfig.setSetting(CURRENT_VIEWER, getCurrentViewerName());
      myVariantPlace.show(getCurrentViewer());
    }
  }

  public AComboBox<String> getSelectionCombobox() {
    return mySelectionCombobox;
  }

  @Nullable
  private String getCurrentViewerName() {
    return myViewersSelectionModel.getSelectedItem();
  }

  @Nullable
  protected final W getCurrentViewer() {
    return getViewer(getCurrentViewerName(), true);
  }

  @Nullable
  protected W getViewer(String viewerName, boolean upgradeSettingIfNotFound) {
    if (myViewerMap.size() == 0) {
      assert false : this;
      return null;
    }
    W viewer = viewerName == null ? null : myViewerMap.get(viewerName);
    if (viewerName != null && viewer == null) {
      Log.debug("cannot find viewer " + viewerName);
      if (upgradeSettingIfNotFound && myDefaultViewerName != null) {
        myConfig.setSetting(CURRENT_VIEWER, myDefaultViewerName);
      }
    }
    if (viewer == null) {
      assert myViewerMap.size() == 1;
      viewer = myViewerMap.values().iterator().next();
    }
    assert viewer != null : this + " " + viewerName;
    return viewer;
  }

  public JComponent getComponent() {
    return myWholePanel;
  }
}
