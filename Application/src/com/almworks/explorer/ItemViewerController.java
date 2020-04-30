package com.almworks.explorer;

import com.almworks.api.application.*;
import com.almworks.api.engine.Connection;
import com.almworks.api.gui.FrameBuilder;
import com.almworks.api.gui.WindowController;
import com.almworks.items.api.DBOperationCancelledException;
import com.almworks.items.api.DBReader;
import com.almworks.items.api.ReadTransaction;
import com.almworks.items.sync.util.SyncUtils;
import com.almworks.util.advmodel.AListModel;
import com.almworks.util.advmodel.AListModelUpdater;
import com.almworks.util.commons.Procedure;
import com.almworks.util.components.SelectionAccessor;
import com.almworks.util.components.SingleSelectionAccessor;
import com.almworks.util.components.plaf.macosx.Aqua;
import com.almworks.util.components.tabs.ContentTab;
import com.almworks.util.config.Configuration;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.threads.Threads;
import com.almworks.util.ui.UIComponentWrapper2Support;
import com.almworks.util.ui.actions.DataRole;
import com.almworks.util.ui.actions.globals.GlobalData;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.DetachComposite;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Map;

/**
 * @author Alex
 */

public class ItemViewerController {
  private static final TypedKey<LoadedItemView<ContentTab>> VIEW_KEY = TypedKey.create("view");

  private Map<Long, LoadedItemView<ContentTab>> myTabMap = Collections15.hashMap(3);
  private Map<Long, LoadedItemView<FrameBuilder>> myFrameMap = Collections15.hashMap(3);
  private ExplorerComponent myExplorer;
  private Configuration myConfig;

  public ItemViewerController(Configuration config, ExplorerComponent explorer) {
    myExplorer = explorer;
    myConfig = config;
  }

  private Detach attachItemController(@NotNull ItemWrapper itemWrapper, ItemViewer viewer) {
    ItemSource source = CollectionBasedItemSource.create(itemWrapper);
    ItemsCollectionController itemsCollector = myExplorer.createLoader(ItemCollectorWidget.DEAF, source);
    Detach detach = attachItemsCollector(itemsCollector, viewer);
    itemsCollector.reload();
    return detach;
  }

  private Detach attachItemsCollector(final ItemsCollectionController controller, final ItemViewer viewer) {
    DetachComposite detach = new DetachComposite();
    detach.add(new Detach() {
      protected void doDetach() {
        controller.dispose();
      }
    });

    final AListModel<? extends LoadedItem> model =
      ((AListModelUpdater<? extends LoadedItem>) controller.getListModelUpdater()).getModel();
    final AListModel.Adapter<LoadedItem> changeModelListener = new AListModel.Adapter<LoadedItem>() {
      boolean empty = true;
      public void onChange() {
        if (model.getSize() == 1) {
          if(empty) {
            viewer.showItem(model.getAt(0));
            empty = false;
          }
        } else {
          viewer.showItem(null);
          empty = true;
        }
      }
    };
    detach.add(((AListModel<LoadedItem>) model).addListener(changeModelListener));
    return detach;
  }


  public void addItemInNewWindow(LoadedItem itemWrapper, final FrameBuilder frame) {
    final ItemViewer view = createItemViewer();

    Procedure<String> titleSink = new Procedure<String>() {
      public void invoke(String name) {
        if (name != null) {
          final WindowController windowController = frame.getWindowContainer().getActor(WindowController.ROLE);
          if (windowController == null) {
            frame.setTitle(name);
          } else {
            windowController.setTitle(name);
          }
        }
      }
    };
    titleSink.invoke(itemWrapper.getItemUrl());

    final LoadedItemView<FrameBuilder> itemController =
      new LoadedItemView<FrameBuilder>(itemWrapper, frame, view, myFrameMap, titleSink);

    JPanel wholePanel = new JPanel(new BorderLayout());
//    wholePanel.add(toolbar.getComponent(), BorderLayout.NORTH);
    wholePanel.add(itemController.getComponent(), BorderLayout.CENTER);

    wholePanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "esc");
    wholePanel.getActionMap().put("esc", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        WindowController windowController = frame.getWindowContainer().getActor(WindowController.ROLE);
        if (windowController != null)
          windowController.close();
      }
    });

    frame.setContent(wholePanel);
    frame.addGlobalDataRoot();
    frame.setPreferredSize(new Dimension(700, 500));
    frame.detachOnDispose(new Detach() {
      protected void doDetach() throws Exception {
//        toolbar.dispose();
        itemController.dispose();
      }
    });
    frame.showWindow();
  }

  public void addItemInNewTab(final LoadedItem item, final ContentTab tab) {
    final ItemViewer viewer = createItemViewer();
    tab.setUserProperty(LoadedItem.LOADED_ITEM, item);
    tab.setUserProperty(ItemWrapper.ITEM_WRAPPER, item);
    tab.setName("\u2026");
    tab.setTooltip("Loading " + item.getItemUrl());
    Procedure<String> setNameProc = new Procedure<String>() {
      public void invoke(String name) {
        Threads.assertAWTThread();
        if (name != null) {
          if (name.length() > 25) {
            tab.setName(name.substring(0, 22) + "\u2026");
            tab.setTooltip(name);
          } else {
            tab.setName(name);
            tab.setTooltip(null);
          }
        }
      }
    };
    LoadedItemView<ContentTab> loadedItemView =
      new LoadedItemView<ContentTab>(item, tab, viewer, myTabMap, setNameProc);
    tab.setUserProperty(VIEW_KEY, loadedItemView);
    Aqua.setLightNorthBorder(loadedItemView.getComponent());
    tab.setComponent(loadedItemView);
  }

  public static SelectionAccessor<LoadedItem> findSingleSelectionInTab(ContentTab tab) {
    if (tab == null)
      return null;
    LoadedItemView<ContentTab> view = tab.getUserProperty(VIEW_KEY);
    if (view == null)
      return null;
    return view.getSelectionModel();
  }

  private ItemViewer createItemViewer() {
    ItemViewer view = new ItemViewer(myConfig);
    view.setItemUiModelNeeded(false);
    DataRole<?>[] roles = {ItemWrapper.ITEM_WRAPPER, LoadedItem.LOADED_ITEM};
    GlobalData.KEY.addClientValue(view.getComponent(), roles);
    return view;
  }

  public ContentTab getItemTab(ItemWrapper item) {
    final LoadedItemView<ContentTab> view = myTabMap.get(item.getItem());
    return (view != null) ? view.myContainer : null;
  }

  public FrameBuilder getItemFrame(ItemWrapper item) {
    final LoadedItemView<FrameBuilder> descriptor = myFrameMap.get(item.getItem());
    return (descriptor != null) ? descriptor.myContainer : null;
  }

  private class LoadedItemView<C> extends UIComponentWrapper2Support {
    private final DetachComposite myLifespan = new DetachComposite();
    private final LoadedItem myItem;
    private final C myContainer;
    private final ItemViewer myView;
    private final Map<Long, LoadedItemView<C>> myMap;

    private SelectionAccessor<LoadedItem> mySelectionModel;

    private LoadedItemView(LoadedItem item, C container, ItemViewer view,
      Map<Long, LoadedItemView<C>> map, final Procedure setTitle)
    {
      myItem = item;
      myContainer = container;
      myMap = map;
      myView = view;
      myLifespan.add(attachItemController(myItem, myView));
      myMap.put(item.getItem(), this);

      final Connection connection = myItem.getConnection();
      if (connection != null) {
        item.services().getEngine().getDatabase().readForeground(new ReadTransaction<String>() {
          @Override
          public String transaction(DBReader reader) throws DBOperationCancelledException {
            return connection.getExternalIdSummaryString(SyncUtils.readTrunk(reader, myItem.getItem()));
          }
        }).onSuccess(ThreadGate.AWT, setTitle);
      }
    }

    @Deprecated
    public void dispose() {
      super.dispose();
      myView.dispose();
      myMap.remove(myItem.getItem());
    }

    public Detach getDetach() {
      return myLifespan;
    }

    public JComponent getComponent() {
      return myView.getComponent();
    }

    public SelectionAccessor<LoadedItem> getSelectionModel() {
      if (mySelectionModel == null) {
        mySelectionModel = new SingleSelectionAccessor<LoadedItem>(myItem);
      }
      return mySelectionModel;
    }
  }
}