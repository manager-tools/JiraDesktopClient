package com.almworks.explorer;

import com.almworks.api.application.*;
import com.almworks.api.engine.Connection;
import com.almworks.api.explorer.TableController;
import com.almworks.api.install.TrackerProperties;
import com.almworks.api.misc.TimeService;
import com.almworks.engine.gui.ItemMessages;
import com.almworks.explorer.loader.ItemUiModelImpl;
import com.almworks.integers.LongList;
import com.almworks.util.Env;
import com.almworks.util.L;
import com.almworks.util.Pair;
import com.almworks.util.Terms;
import com.almworks.util.collections.ChangeListener1;
import com.almworks.util.components.AToolbar;
import com.almworks.util.components.PlaceHolder;
import com.almworks.util.components.SelectionAccessor;
import com.almworks.util.components.ToolbarBuilder;
import com.almworks.util.components.plaf.macosx.Aqua;
import com.almworks.util.components.plaf.patches.Aero;
import com.almworks.util.config.Configuration;
import com.almworks.util.exec.Context;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.i18n.Local;
import com.almworks.util.model.ScalarModel;
import com.almworks.util.model.ScalarModelEvent;
import com.almworks.util.threads.Bottleneck;
import com.almworks.util.threads.Computable;
import com.almworks.util.threads.ThreadAWT;
import com.almworks.util.threads.Threads;
import com.almworks.util.ui.ElementViewer;
import com.almworks.util.ui.UIComponentWrapper;
import com.almworks.util.ui.UIUtil;
import com.almworks.util.ui.actions.*;
import com.almworks.util.ui.actions.dnd.*;
import org.almworks.util.Util;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.DetachComposite;
import org.almworks.util.detach.Lifecycle;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * @author : Dyoma
 */
public class ItemViewer {
  private final Configuration myConfig;
  private final PlaceHolder myViewerToolbarPlace;
  private final PlaceHolder myViewerPlace;

  private final SimpleProvider myDataProvider =
    new SimpleProvider(DBDataRoles.ITEM_ROLE, ItemWrapper.ITEM_WRAPPER, LoadedItem.LOADED_ITEM);

  private final UIComponentWrapper myNoComponent =
    UIComponentWrapper.Simple.messageInScrollPane(L.content(Local.parse("Selected $(" + Terms.key_artifact + ") cannot be viewed.")));
  private final UIComponentWrapper myNothingSelectedViewer =
    UIComponentWrapper.Simple.messageInScrollPane(L.content(Local.parse("No $(" + Terms.key_artifact + ") selected.")));

  private ElementViewer<ItemUiModel> myLastViewer = null;
  private MetaInfo myLastType = null;
  private final MyPanel myWholePanel = new MyPanel();
  private final MyDropHintProvider myDropHintProvider = new MyDropHintProvider();
  private final DndHelper<MyDropHint, MyPanel> myDndHelper;

  private ItemUiModel myModel;
  private final Lifecycle myElementViewLife = new Lifecycle();
  private static final Border EMPTY = new EmptyBorder(0, 0, 0, 0);

  private final Lifecycle myViewerToolbarLife = new Lifecycle();

  private static final int DEFAULT_AUTO_DETAIL_DELAY = 2;
  private boolean myItemUiModelNeeded = true;

  private final PlaceHolder mySmartToolbarPlace = new PlaceHolder();

  public ItemViewer(Configuration config) {
    myConfig = config;
    myViewerToolbarPlace = new PlaceHolder();
    myViewerPlace = new PlaceHolder();
    myDndHelper = new DndHelper<MyDropHint, MyPanel>(myDropHintProvider);
    Aqua.cleanScrollPaneBorder(myNoComponent.getComponent());
    Aero.cleanScrollPaneBorder(myNoComponent.getComponent());
    Aqua.cleanScrollPaneBorder(myNothingSelectedViewer.getComponent());
    Aero.cleanScrollPaneBorder(myNothingSelectedViewer.getComponent());

    createPanel();
    setupUI();
    showItem(null);
  }

  private void setupUI() {
    Color background = UIManager.getColor("EditorPane.background");
    myWholePanel.setBorder(EMPTY);
    myWholePanel.setBackground(background);
  }

  private void createPanel() {
    myWholePanel.add(myViewerPlace, BorderLayout.CENTER);
    myWholePanel.setName("itemViewer");
    DataProvider.DATA_PROVIDER.putClientValue(myWholePanel, myDataProvider);
  }

  public JComponent getComponent() {
    return myWholePanel;
  }

  @ThreadAWT
  public void showItem(@Nullable LoadedItem item) {
    Threads.assertAWTThread();
    myElementViewLife.cycle();
    if (item != null) {
      Lifespan lifespan = myElementViewLife.lifespan();
      lifespan.add(setupModel(item));
      lifespan.add(showElement());
      setupDelayedFullDownload(lifespan, item);
    } else {
      setViewer(myNothingSelectedViewer);
    }
  }

  private void setupDelayedFullDownload(Lifespan lifespan, final LoadedItem item) {
    ItemDownloadStage stage = ItemDownloadStageKey.retrieveValue(item.getValues());
    if (stage != ItemDownloadStage.FULL && stage != ItemDownloadStage.NEW) {
      int delay = getAutoDownloadDelay();
      final Connection connection = item.getConnection();
      if (delay > 0 && connection != null) {
        Context.require(TimeService.ROLE).awtInvokeIn(lifespan, delay * 1000, new Computable<Object>() {
          @Override
          public Object compute() {
            connection.downloadItemDetails(new LongList.Single(item.getItem()));
            return null;
          }
        });
      }
    }
  }

  private int getAutoDownloadDelay() {
    return Env.getInteger(TrackerProperties.AUTO_DETAIL_DELAY, DEFAULT_AUTO_DETAIL_DELAY);
  }

  private Detach showElement() {
    MetaInfo metaInfo = myModel.getMetaInfo();
    if (myLastType != metaInfo) {
      disposeLastViewer();
      myLastType = metaInfo;
      myLastViewer = metaInfo.createViewer(myConfig);
      // todo do something with toolbar component
      ToolbarBuilder builder = metaInfo.getToolbarBuilder(true);
      if (builder == null) {
        mySmartToolbarPlace.show((JComponent) null);
      } else {
        mySmartToolbarPlace.show(createSmartToolbar(builder));
      }
    }

    setViewer(Util.NN(myLastViewer, myNoComponent));

    myViewerToolbarLife.cycle();
    if (myLastViewer != null) {
      myLastViewer.showElement(myModel);
      ScalarModel<JComponent> holder = (ScalarModel<JComponent>) myLastViewer.getToolbarActionsHolder();
      if (holder != null) {
        holder.getEventSource().addAWTListener(myViewerToolbarLife.lifespan(), new ScalarModel.Adapter<JComponent>() {
          public void onScalarChanged(ScalarModelEvent<JComponent> event) {
            myViewerToolbarPlace.show(event.getNewValue());
          }
        });
      }
    }
    
    return new Detach() {
      protected void doDetach() {
        myViewerToolbarLife.cycle();
        myViewerToolbarPlace.clear();
      }
    };
  }

  private static JComponent createSmartToolbar(ToolbarBuilder builder) {
    final ItemUrlServiceImpl urlService = Context.get(ItemUrlServiceImpl.class);
    if(urlService == null) {
      return builder.createFlowToolbar();
    }

    final SmartToolbar st = new SmartToolbar();
    builder.addAllToToolbar(st);
    st.addExcess();
    final Pair<JComponent, AnAction> pair = urlService.createUrlFieldAndAction();
    st.addField(pair.getFirst());
    st.addAction(pair.getSecond()).overridePresentation(PresentationMapping.VISIBLE_NONAME);
    return st;
  }

  private void disposeLastViewer() {
    if (myLastViewer != null)
      myLastViewer.dispose();
    myLastViewer = null;
  }

  private void setViewer(UIComponentWrapper viewer) {
    if (viewer != myViewerPlace.getShown()) {
      if(viewer instanceof ElementViewer) {
        ElementViewer elementViewer = (ElementViewer) viewer;
        PlaceHolder toolbar = elementViewer.getToolbarPlace();
        if(toolbar != null) toolbar.show((JComponent)mySmartToolbarPlace);
        PlaceHolder bottom = elementViewer.getBottomPlace();
        if (bottom != null) bottom.show(ItemMessages.createMessagesForViewer());
      }
    }
    myViewerPlace.show(viewer);
  }

  // todo remove
  public void setItemUiModelNeeded(boolean needed) {
    myItemUiModelNeeded = needed;
  }

  private Detach setupModel(final LoadedItem item) {
    myDataProvider.setSingleData(DBDataRoles.ITEM_ROLE, item.getItem());
    myDataProvider.setSingleData(LoadedItem.LOADED_ITEM, item);
    ItemUiModelImpl model = ItemUiModelImpl.create(item);

    final DetachComposite detach = new DetachComposite();
    detach.add(ItemUiModelImpl.listenItem(model, item));
    myModel = model;

//    assert myItemUiModelNeeded : "debug this";
    if (myItemUiModelNeeded) {
      final ChangeListener1<LoadedItem> listener = new ChangeListener1<LoadedItem>() {
        public void onChange(LoadedItem object) {
          myDataProvider.setSingleData(ItemWrapper.ITEM_WRAPPER, myModel, true);
        }
      };
      item.addAWTListener(listener);
      listener.onChange(null);
      detach.add(new Detach() {
        @Override
        protected void doDetach() throws Exception {
          item.removeAWTListener(listener);
        }
      });
    } else {
      myDataProvider.setSingleData(ItemWrapper.ITEM_WRAPPER, item);
    }
    return new Detach() {
      protected void doDetach() {
        detach.detach();
        myDataProvider.removeAllData();
        myModel = null;
      }
    };
  }

  public void dispose() {
    showItem(null);
    disposeLastViewer();
  }

  public ElementViewer<ItemUiModel> getLastViewer() {
    return myLastViewer;
  }

  public static Controller create(Configuration config, SelectionAccessor<? extends LoadedItem> selectedArtifact)
  {
    ItemViewer viewer = new ItemViewer(config.getOrCreateSubset("artifactViewers"));
    Controller controller = new Controller(selectedArtifact, viewer);

    selectedArtifact.addListener(controller);
    if (selectedArtifact.hasSelection())
      controller.onSelectionChanged(selectedArtifact.getSelection());
    ConstProvider.addRoleValue(viewer.getComponent(), TableController.ARTIFACT_VIEW_FOCUS_MARK, new Object());

    return controller;
  }

  private class MyDropHint extends DropHint {
  }


  private class MyPanel extends JPanel implements DndComponentAdapter<MyDropHint>, DndTarget {
    private final Lifecycle mySwingLife = new Lifecycle(false);
    private boolean myDnd;

    public MyPanel() {
      super(UIUtil.createBorderLayout());
      TransferHandlerBridge.install(this, new ItemsContextTransfer());
    }

    public void addNotify() {
      super.addNotify();
      mySwingLife.cycleStart();
      DndManager dnd = DndManager.instance();
      if (dnd != null) {
        dnd.registerTarget(mySwingLife.lifespan(), this);
      }
    }

    public void removeNotify() {
      mySwingLife.cycleEnd();
      super.removeNotify();
    }

    public void dragNotify(DndEvent event) {
      myDndHelper.dragNotify(event, getTransfer(), this);
    }

    @Nullable
    public ContextTransfer getTransfer() {
      TransferHandler handler = getTransferHandler();
      if (!(handler instanceof TransferHandlerBridge))
        return null;
      return ((TransferHandlerBridge) handler).getTransfer();
    }

    public JComponent getTargetComponent() {
      return this;
    }

    public void setDndActive(boolean dndActive, boolean dndEnabled) {
      myDnd = dndActive && dndEnabled;
      invalidateBorder();
    }

    private void invalidateBorder() {
      if (isShowing()) {
        Dimension size = getSize();
        repaint(0, 0, 2, size.height);
        repaint(size.width - 2, 0, 2, size.height);
        repaint(0, 0, size.width, 2);
        repaint(0, size.height - 2, size.width, 2);
      }
    }

    protected void paintChildren(Graphics g) {
      super.paintChildren(g);
      if (myDnd) {
//        Graphics gg = g.create();
//        try {
        Graphics gg = g;
        DndUtil.paintDropBorder((Graphics2D) gg, this);
//        } finally {
//          gg.dispose();
//        }
      }
    }

    public boolean isDndWorking() {
      return myDnd;
    }

    public void setDropHint(MyDropHint hint) {
      // ignore
    }
  }


  private class MyDropHintProvider implements DropHintProvider<MyDropHint, MyPanel> {
    public boolean prepareDropHint(MyPanel component, Point p, DragContext context, ContextTransfer transfer) {
      if (context != null) {
        LoadedItem item = myDataProvider.getSingleValue(LoadedItem.LOADED_ITEM);
        context.putValue(BaseItemContextTransfer.TARGET, item);
      }
      return false;
    }

    public MyDropHint createDropHint(MyPanel component, DragContext context) {
      return null;
    }

    public void cleanContext(DragContext context) {
      if (context != null) {
        context.putValue(BaseItemContextTransfer.TARGET, null);
      }
    }
  }


  public static class Controller implements SelectionAccessor.Listener<LoadedItem> {
    private Bottleneck myProcess;
    private final SelectionAccessor<? extends LoadedItem> mySelectedArtifact;
    private final ItemViewer myViewer;
    private LoadedItem myCurrentSelection = null;

    public Controller(SelectionAccessor<? extends LoadedItem> selectedArtifact, ItemViewer viewer) {
      mySelectedArtifact = selectedArtifact;
      myViewer = viewer;
      myProcess = new Bottleneck(100, ThreadGate.AWT_QUEUED, new Runnable() {
        public void run() {
          updateSelectionNow();
        }
      });
    }

    public void onSelectionChanged(LoadedItem newSelection) {
      myProcess.request();
    }

    public void updateSelectionNow() {
      LoadedItem newSelection = mySelectedArtifact.getSelection();
      if (myCurrentSelection == newSelection)
        return;
      myCurrentSelection = newSelection;
      if (myViewer != null)
        myViewer.showItem(myCurrentSelection);
    }

    public ItemViewer getViewer() {
      return myViewer;
    }

    public void dispose() {
      myViewer.dispose();
    }
  }

  private static class SmartToolbar extends AToolbar {
    private JComponent myExcess;
    private JComponent myField;

    SmartToolbar() {
      super(null, new FlowLayout(FlowLayout.LEADING, 0, 0), JToolBar.HORIZONTAL);
      setOpaque(false);
    }

    public void addExcess() {
      myExcess = new JLabel();
      add(myExcess);
    }

    public void addField(final JComponent field) {
      UIUtil.addOuterBorder(field, new EmptyBorder(0, 2, 0, 2));
      final JComponent fb = new JComponent() {{
        setLayout(new BorderLayout());
        add(field, BorderLayout.CENTER);
      }};
      myField = fb;
      add(fb);
    }

    @Override
    public void doLayout() {
      final int excess = getWidth() - getBorderWidth() - getButtonsWidth();
      final int fmax = getFieldWidth();

      if(excess <= 0) {
        setWidth(myField, 0);
        setWidth(myExcess, 0);
      } else if(excess <= fmax) {
        setWidth(myField, excess);
        setWidth(myExcess, 0);
      } else {
        setWidth(myField, fmax);
        setWidth(myExcess, excess - fmax);
      }

      super.doLayout();
    }

    @Override
    public Dimension getPreferredSize() {
      final Dimension ps = super.getPreferredSize();
      final int wob = getButtonsWidth();

      // hack based on current state
      final int w = getWidth() - getBorderWidth();
      if(w >= 32 && w < wob) {
        final int count = (wob + w - 1) / w;
        ps.height *= count;
        ps.height += count - 1;
      }

      return ps;
    }

    private int getBorderWidth() {
      final Border border = getBorder();
      if(border == null) {
        return 0;
      }
      final Insets ins = border.getBorderInsets(this);
      return ins.left + ins.right;
    }

    private int getButtonsWidth() {
      int width = 0;
      final int n = getComponentCount();
      for(int i = 0; i < n; i++) {
        final Component c = getComponent(i);
        if(c != myExcess && c != myField && c.isVisible()) {
          width += c.getPreferredSize().width;
        }
      }
      return width;
    }

    private int getFieldWidth() {
      return myField.getComponent(0).getPreferredSize().width;
    }

    private void setWidth(JComponent c, int w) {
      c.setVisible(w > 0);
      if(w > 0) {
        c.setPreferredSize(new Dimension(w, c.getPreferredSize().height));
        c.setSize(w, c.getHeight());
      }
    }
  }
}