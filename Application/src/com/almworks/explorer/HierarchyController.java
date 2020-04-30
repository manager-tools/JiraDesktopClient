package com.almworks.explorer;

import com.almworks.api.application.ItemCollectionContext;
import com.almworks.api.application.ItemsTreeLayout;
import com.almworks.api.application.LoadedItem;
import com.almworks.api.application.MetaInfo;
import com.almworks.api.engine.Connection;
import com.almworks.api.explorer.AdditionalHierarchies;
import com.almworks.api.explorer.MetaInfoCollector;
import com.almworks.api.explorer.TableController;
import com.almworks.util.advmodel.*;
import com.almworks.util.collections.ChangeListener;
import com.almworks.util.commons.Condition;
import com.almworks.util.components.AComboBox;
import com.almworks.util.components.ListModelTreeAdapter;
import com.almworks.util.components.SelectionAccessor;
import com.almworks.util.components.TreeModelBridge;
import com.almworks.util.components.renderer.Renderers;
import com.almworks.util.components.tables.HierarchicalTable;
import com.almworks.util.config.Configuration;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.ui.SingleChildLayout;
import com.almworks.util.ui.actions.*;
import org.almworks.util.Collections15;
import org.almworks.util.Util;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.Lifecycle;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static com.almworks.util.ui.SingleChildLayout.CONTAINER;
import static com.almworks.util.ui.SingleChildLayout.PREFERRED;

class HierarchyController implements UpdateRequestable {
  private static final String ARTIFACT_TREE_LAYOUT = "ArtifactTreeLayout";
  private ItemsTreeLayout myTreeLayout = ItemsTreeLayout.NONE;
  private final SimpleProvider myProvider;
  @Nullable
  private volatile Configuration myConfig;
  private final MetaInfoCollector myCollector;
  @Nullable
  private final Connection myConnection;
  private final AtomicReference<String> myLayoutId = new AtomicReference<String>(null);
  private final HierarchicalTable<LoadedItem> myArtifactsTree;
  private final Lifecycle myTreeStructureLife = new Lifecycle();
  private final AComboBox<ItemsTreeLayout> myCombobox = new AComboBox<ItemsTreeLayout>() {
    @Override
    public void addNotify() {
      super.addNotify();
      ComponentUpdateController.connectUpdatable(getDisplayableLifespan(), HierarchyController.this, myCombobox);
      getDisplayableLifespan().add(new Detach() {
        @Override
        protected void doDetach() throws Exception {
          updateConfig();
          myCombobox.setEnabled(false);
          myCombobox.setModel(AComboboxModel.EMPTY_COMBOBOX);
          myUpdateLife.cycle();
        }
      });
    }
  };

  private final JPanel myWholePanel = SingleChildLayout.envelop(myCombobox, CONTAINER, PREFERRED, CONTAINER, CONTAINER, 0F, 0.5F);
  private final Lifecycle myUpdateLife = new Lifecycle();
  @Nullable
  private AListModel<? extends LoadedItem> myModel = null;
  @Nullable
  private AListModelUpdater<? extends LoadedItem> myListModelUpdater = null;

  public HierarchyController(SimpleProvider provider, HierarchicalTable<LoadedItem> artifactsTree, MetaInfoCollector metaInfoCollector, @Nullable Connection connection) {
    myProvider = provider;
    myArtifactsTree = artifactsTree;
    myCollector = metaInfoCollector;
    myConnection = connection;
    myProvider.setSingleData(ItemsTreeLayout.DATA_ROLE, myTreeLayout);
    myCollector.getMetaInfoModel().addChangeListener(Lifespan.FOREVER, new ChangeListener() {
      public void onChange() {
        if (myConfig != null) readLayoutFromConfig();
      }
    });
    myCombobox.setEnabled(false);
    myCombobox.setFocusable(false);
    myCombobox.setCanvasRenderer(Renderers.canvasDefault(ItemsTreeLayout.NO_HIERARCHY));
    myCombobox.setColumns(ItemsTreeLayout.NO_HIERARCHY.length());

    myCombobox.getModel().addSelectionListener(Lifespan.FOREVER, new SelectionListener.Adapter() {
      public void onSelectionChanged() {
        if (!myCombobox.isEnabled()) return;
        DefaultActionContext context = new DefaultActionContext(myCombobox);
        try {
          ItemsTreeLayout item = myCombobox.getModel().getSelectedItem();
          context.getSourceObject(TableController.DATA_ROLE).setTreeLayout(item);
        } catch (CantPerformException e) {
          assert false : e;
        }
      }

      @Override
      public void onItemsUpdated(AListModel.UpdateEvent event) {
        updateConfig();
      }
    });
  }

  private void updateConfig() {
    AComboboxModel<ItemsTreeLayout> model = myCombobox.getModel();
    ItemsTreeLayout selected = model.getSelectedItem();
    if (myConfig != null && selected != null && model.getSize() > 0 && myCombobox.isEnabled())
      setLayoutId(myConfig, selected.getId());
  }

  public void setConfig(ItemCollectionContext contextInfo, Configuration common) {
    setConfig(chooseConfig(contextInfo, common));
  }

  private Configuration chooseConfig(ItemCollectionContext contextInfo, Configuration common) {
    if (contextInfo == null) return common;
    Configuration special = contextInfo.getContextConfig();
    if (special == null) return common;
    if (!special.isSet(ARTIFACT_TREE_LAYOUT))
      setLayoutId(special, getLayoutId(common));
    return special;
  }

  private static String getLayoutId(Configuration common) {
    return common.getSetting(ARTIFACT_TREE_LAYOUT, ItemsTreeLayout.NONE.getId());
  }

  private void setConfig(Configuration config) {
    if (config == null) {
      myConfig = null;
      return;
    }
    myLayoutId.set(getLayoutId(config));
    myConfig = config;
    readLayoutFromConfig();
  }

  private void readLayoutFromConfig() {
    String layoutId = myLayoutId.get();
    if (layoutId == null) return;
    if (layoutId.equals(myTreeLayout.getId())) {
      myLayoutId.compareAndSet(layoutId, null);
      return;
    }
    for (ItemsTreeLayout layout : getAllLayouts()) {
      if (layoutId.equals(layout.getId())) {
        setTreeLayout(layout);
        return;
      }
    }
  }

  public void setTreeLayout(@Nullable ItemsTreeLayout layout) {
    layout = layout != null ? layout : ItemsTreeLayout.NONE;
    String initialId = myLayoutId.get();
    @SuppressWarnings("ConstantConditions")
    String newLayoutId = layout.getId();
    if (initialId != null && initialId.equals(newLayoutId)) myLayoutId.compareAndSet(initialId, null);
    boolean storeLayout = myLayoutId.get() == null;
    Configuration config = myConfig;
    if (config != null && storeLayout && myCombobox.isEnabled())
      setLayoutId(config, newLayoutId);
    if (Util.equals(myTreeLayout, layout)) return;
    myTreeLayout = layout;
    if (myModel != null) updateTreeRoot();
    myProvider.setSingleData(ItemsTreeLayout.DATA_ROLE, layout);
  }

  public void setTreeLayoutById(String layoutId) {
    if (layoutId == null) return;
    ItemsTreeLayout selected = null;
    for (ItemsTreeLayout layout : myCombobox.getModel()) {
      if (layoutId.equals(layout.getId())) {
        selected = layout;
        break;
      }
    }
    if (selected != null) setTreeLayout(selected);
  }

  private void setLayoutId(Configuration config, String newLayoutId) {
    config.setSetting(ARTIFACT_TREE_LAYOUT, newLayoutId);
  }

  private void updateTreeRoot() {
    myArtifactsTree.setShowRootHanders(!Util.equals(myTreeLayout, ItemsTreeLayout.NONE));
    SelectionAccessor<LoadedItem> accessor = myArtifactsTree.getSelectionAccessor();
    List<LoadedItem> selection = accessor.getSelectedItems();
    assert myModel != null;

    // Create new tree and set root to table before detaching old to speed up detach.
    ListModelTreeAdapter<LoadedItem, TreeModelBridge<LoadedItem>> adapter =
      ListModelTreeAdapter.create(myModel, myTreeLayout.getTreeStructure(), null);
    myArtifactsTree.clearRoot();
    myTreeStructureLife.cycle();
    adapter.attach(myTreeStructureLife.lifespan());
    myArtifactsTree.setRoot(adapter.getRootNode());
    myArtifactsTree.expandAll();
    accessor.setSelected(selection);
    myArtifactsTree.scrollSelectionToView();
  }

  public void setListModelUpdater(Lifespan life, AListModelUpdater<? extends LoadedItem> updater,
    AListModel<? extends LoadedItem> model) {
    myModel = model;
    myListModelUpdater = updater;
    updateTreeRoot();
    life.add(myTreeStructureLife.getDisposeDetach());
  }

  public void loadingDone(Runnable finish) {
    AListModelUpdater<?> updater = myListModelUpdater;
    if (updater == null) {
      finish.run();
    } else {
      updater.runWhenNoPendingUpdates(ThreadGate.AWT, finish);
    }
  }

  public void listenAdditionalHierarchies(Lifespan life) {
    if (myConnection == null) return;
    AdditionalHierarchies hierarchies = myConnection.getActor(AdditionalHierarchies.ROLE);
    if (hierarchies == null) return;
    hierarchies.getAdditionalLayouts().addChangeListener(life, new ChangeListener() {
      @Override
      public void onChange() {
        AComboboxModel<ItemsTreeLayout> model = myCombobox.getModel();
        ItemsTreeLayout prevLayout = model.getSelectedItem();
        if (prevLayout == null)
          return;
        String id = prevLayout.getId();
        ItemsTreeLayout updatedLayout = null;
        for (ItemsTreeLayout treeLayout : model) {
          if (id.equals(treeLayout.getId())) {
            updatedLayout = treeLayout;
            break;
          }
        }
        if (updatedLayout == null) model.setSelectedItem(ItemsTreeLayout.NONE);
        else if (updatedLayout != prevLayout) model.setSelectedItem(updatedLayout);
      }
    });
  }

  private List<ItemsTreeLayout> getAllLayouts() {
    HashSet<String> ids = Collections15.hashSet();
    ArrayList<ItemsTreeLayout> result = Collections15.arrayList();
    Condition<ItemsTreeLayout> filter = myConnection != null ? new ItemsTreeLayout.OwnerFilter(myConnection) : Condition.<ItemsTreeLayout>always();
    for (MetaInfo metaInfo : myCollector.getAllMetaInfos()) {
      AListModel<ItemsTreeLayout> layouts = metaInfo.getTreeLayouts();
      for (ItemsTreeLayout layout : layouts) {
        if (filter.isAccepted(layout)) {
          if (ids.add(layout.getId())) result.add(layout);
        }
      }
    }
    if (myConnection != null) {
      AdditionalHierarchies additional = myConnection.getActor(AdditionalHierarchies.ROLE);
      if (additional != null) for (ItemsTreeLayout layout : additional.getAdditionalLayouts()) result.add(layout);
    }
    return result;
  }

  public void update(UpdateRequest request) {
    myUpdateLife.cycle();
    request.watchRole(TableController.DATA_ROLE);
    request.watchRole(ItemsTreeLayout.DATA_ROLE);
    request.watchRole(ItemCollectionContext.ROLE);
    TableController controller = request.getSourceObjectOrNull(TableController.DATA_ROLE);
    myCombobox.setEnabled(controller != null);
    if (controller == null) {
      myCombobox.setModel(AComboboxModel.EMPTY_COMBOBOX);
      return;
    }
    request.updateOnChange(controller.getMetaInfoCollector().getMetaInfoModel());
    ItemsTreeLayout item = myCombobox.getModel().getSelectedItem();

    MetaInfoCollector metaInfos = controller.getMetaInfoCollector();
    AListModel<ItemsTreeLayout> allLayoutsModel =
      SegmentedListModel.flatten(myUpdateLife.lifespan(), metaInfos.getMetaInfoModel(), MetaInfo.TREE_LAYOUTS);
    AListModel<ItemsTreeLayout> layoutsModel = myConnection == null ? allLayoutsModel :
      FilteringListDecorator.create(myUpdateLife.lifespan(), allLayoutsModel, new ItemsTreeLayout.OwnerFilter(myConnection));
    SegmentedListModel<ItemsTreeLayout> allLayouts = SegmentedListModel.create(myUpdateLife.lifespan());
    allLayouts.addSegment(FixedListModel.create(ItemsTreeLayout.NONE));
    allLayouts.addSegment(layoutsModel);
    if (myConnection != null) {
      AdditionalHierarchies additional = myConnection.getActor(AdditionalHierarchies.ROLE);
      if (additional != null) allLayouts.addSegment(additional.getAdditionalLayouts());
    }
    SelectionInListModel<ItemsTreeLayout> model = SelectionInListModel.createForever(allLayouts, null);
    model.setSelectedItem(model.indexOf(item) > -1 ? item : null);
    myCombobox.setModel(model);
    ItemsTreeLayout layout = request.getSourceObjectOrNull(ItemsTreeLayout.DATA_ROLE);
    model.setSelectedItem(layout != null ? layout : ItemsTreeLayout.NONE);
  }

  public JComponent getComponent() {
    return myWholePanel;
  }
}
