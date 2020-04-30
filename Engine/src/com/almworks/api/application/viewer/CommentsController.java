package com.almworks.api.application.viewer;

import com.almworks.api.application.ModelKey;
import com.almworks.api.application.ModelMap;
import com.almworks.api.application.viewer.textdecorator.TextDecoratorRegistry;
import com.almworks.api.gui.MainMenu;
import com.almworks.util.advmodel.*;
import com.almworks.util.collections.ChangeListener;
import com.almworks.util.collections.Containers;
import com.almworks.util.collections.Convertor;
import com.almworks.util.collections.SimpleModifiable;
import com.almworks.util.components.*;
import com.almworks.util.components.plaf.macosx.Aqua;
import com.almworks.util.components.renderer.CellState;
import com.almworks.util.config.Configuration;
import com.almworks.util.images.Icons;
import com.almworks.util.ui.AdjustedSplitPane;
import com.almworks.util.ui.MegaMouseAdapter;
import com.almworks.util.ui.UIUtil;
import com.almworks.util.ui.actions.ActionRegistry;
import com.almworks.util.ui.actions.ConstProvider;
import com.almworks.util.ui.actions.DataRole;
import com.almworks.util.ui.actions.IdActionProxy;
import com.almworks.util.ui.actions.presentation.MenuBuilder;
import com.almworks.util.ui.swing.AwtUtil;
import org.almworks.util.Collections15;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.*;
import java.util.regex.Pattern;

/**
 * @author dyoma
 */
public class CommentsController<T extends Comment> implements UIController<JPanel> {
  public static final DataRole<CommentsController<?>> ROLE = (DataRole)DataRole.createRole(CommentsController.class);
  public static final IdActionProxy ACTION_OLDEST_FIRST = new IdActionProxy(MainMenu.Edit.COMMENTS_SORT_OLDEST_FIRST);
  public static final IdActionProxy ACTION_NEWEST_FIRST = new IdActionProxy(MainMenu.Edit.COMMENTS_SORT_NEWEST_FIRST);
  public static final IdActionProxy ACTION_SHOW_THREAD_TREE = new IdActionProxy(MainMenu.Edit.COMMENTS_SHOW_THREAD_TREE);
  public static final IdActionProxy ACTION_EXPAND_ALL = new IdActionProxy(MainMenu.Edit.COMMENTS_EXPAND_ALL);

  private final MenuBuilder myMenu = new MenuBuilder();
  private final ModelKey<? extends Collection<? extends T>> myKey;
  private final ATable<CommentState<T>> myTable;

  private JComponent myTableComponent;
  private Comparator<CommentState<T>> myComparator;
  @SuppressWarnings({"RawUseOfParameterizedType"})
  private CommentsTree myTreeStructure;
  private final JPanel myPanel;
  private Configuration myConfig;
  private static final String OLDEST_FIRST = "oldestFirst";
  private static final String TREE_DIVIDER = "treeDivider";
  private static final String SETTING_TREE_VISIBLE = "treeVisible";

  private Comparator<? super T> myOriginalComparator;
  private CanvasRenderer<? super TreeModelBridge<T>> myTreeRenderer;
  @SuppressWarnings({"RawUseOfParameterizedType"})
  private ATree myTree;
  private AdjustedSplitPane myTreePane;

  private final SimpleModifiable myModifiable = new SimpleModifiable();

  private Convertor<Collection<? extends T>, Collection<? extends T>> mySelector;

  private AListModel<CommentTextColumnAccessor<T>> myColumns;
  private CommentTextColumnAccessor<T> myCommentColumn;
  private final OrderListModel<CommentState<T>> myCommentsModel = OrderListModel.create();

  private CommentsController(ModelKey<? extends Collection<? extends T>> key, ATable<CommentState<T>> table,
    JPanel panel)
  {
    myKey = key;
    myTable = table;
    myPanel = panel;
    myPanel.setBackground(UIUtil.getEditorBackground());

    CommentMouseListener mouseListener = new CommentMouseListener(table);
    table.getSwingComponent().addMouseListener(mouseListener);
    table.getSwingComponent().addMouseMotionListener(mouseListener);
  }

  public void connectUI(@NotNull Lifespan lifespan, @NotNull final ModelMap model, @NotNull JPanel component) {
    assert component == myPanel : component + " " + myPanel;

    myMenu.addToComponent(lifespan, myTable.getSwingComponent());
    if (myTree != null)
      myMenu.addToComponent(lifespan, myTree.getSwingComponent());


    myTable.setColumnModel(getColumns());
    connectModel(lifespan, model);
    myTable.setCollectionModel(myCommentsModel);
    if (myTreeStructure != null)
      //noinspection RawUseOfParameterizedType
      connectTree(lifespan, myTreeStructure, myTree);
    showTree(isShowTree());
  }

  private AListModel<CommentTextColumnAccessor<T>> getColumns() {
    if (myColumns == null)
      myColumns = FixedListModel.create(myCommentColumn);
    return myColumns;
  }

  public void showTree(boolean show) {
    if (myTreeStructure == null) {
      show = false;
    }
    if (show) {
      if (myTreePane.getRightComponent() != myTableComponent) {
        myTreePane.setRightComponent(myTableComponent);
      }
      if (myPanel.getComponentCount() < 1 || myPanel.getComponent(0) != myTreePane) {
        myPanel.add(myTreePane, BorderLayout.CENTER);
      }
    } else {
      if (myTreePane != null && myTreePane.getParent() != null) {
        myPanel.remove(myTreePane);
      }
      if (myPanel.getComponentCount() < 1 || myPanel.getComponent(0) != myTableComponent) {
        myPanel.add(myTableComponent, BorderLayout.CENTER);
      }
    }
    myPanel.revalidate();
    myPanel.repaint();
    myConfig.setSetting(SETTING_TREE_VISIBLE, show);
    myModifiable.fireChanged();
  }

  private <K, N extends TreeModelBridge<T>> void connectTree(final Lifespan lifespan, CommentsTree<T, K, N> structure, final ATree<N> tree)
  {
    final ConvertingListDecorator<CommentState<T>, T> commentsModel =
      CachingConvertingListDecorator.createCaching(lifespan, myCommentsModel, CommentState.<T>getGetComment());
    myTreeStructure.attachCommentsList(lifespan, commentsModel);
    ListModelTreeAdapter<T, N> adapter = ListModelTreeAdapter.create(commentsModel, structure, myOriginalComparator);
    tree.setRootVisible(false);
    tree.setShowsRootHandles(false);
    tree.setCanvasRenderer(myTreeRenderer);
    adapter.addAddListener(new Runnable() {
      public void run() {
        tree.expandAll();
      }
    });
    adapter.attach(lifespan);
    N newRoot = adapter.getRootNode();

    final boolean[] changing = {false};
    tree.getSelectionAccessor().addAWTChangeListener(lifespan, new ChangeListener() {
      public void onChange() {
        if (!lifespan.isEnded() && !changing[0] && tree.isDisplayable() && myTable.isDisplayable()) {
          N node = tree.getSelectionAccessor().getSelection();
          if (node != null) {
            changing[0] = true;
            try {
              myTable.getSelectionAccessor().setSelectedIndex(commentsModel.indexOf(node.getUserObject()));
            } finally {
              changing[0] = false;
            }
            myTable.scrollSelectionToView();
          }
        }
      }
    });
    myTable.getSelectionAccessor().addAWTChangeListener(lifespan, new ChangeListener() {
      public void onChange() {
        if (!lifespan.isEnded() && !changing[0] && tree.isDisplayable() && myTable.isDisplayable()) {
          CommentState<? extends Comment> state = myTable.getSelectionAccessor().getSelection();
          if (state != null) {
            changing[0] = true;
            try {
              tree.getSelectionAccessor().clearSelection();
            } finally {
              changing[0] = false;
            }
          }
        }
      }
    });

    tree.setRoot(newRoot);
    tree.expandAll();
  }

  private Comparator<CommentState<T>> getCurrentComparator() {
    return isDirectSorted() ? myComparator : Containers.reverse(myComparator);
  }

  private void connectModel(Lifespan lifespan, final ModelMap model) {
    myCommentsModel.clear();
    ChangeListener listener = new ChangeListener() {
      public void onChange() {
        List<CommentState<T>> states = Collections15.arrayList();
        Collection<? extends T> newComments = myKey.getValue(model);
        Collection<? extends T> shownComments =
          mySelector == null || newComments == null ? newComments : mySelector.convert(newComments);
        if (shownComments != null) {
          for (T comment : shownComments) {
            states.add(new CommentState<T>(comment));
          }
        }
        Collections.sort(states, getCurrentComparator());
        updateModel(states, myCommentsModel);
      }
    };
    model.addAWTChangeListener(lifespan, listener);
    listener.onChange();
  }

  static <T extends Comment> void updateModel(List<CommentState<T>> states, OrderListModel<CommentState<T>> model) {
    if (model.getSize() == 0) {
      for (int i = 0; i < Math.min(10, states.size()); i++)
        states.get(i).setExpanded(true);
      model.setElements(states);
    } else {
      HashSet<T> oldComments = Collections15.hashSet();
      HashSet<T> newComments = Collections15.hashSet();
      for (int i = 0; i < model.getSize(); i++) oldComments.add(model.getAt(i).getComment());
      for (CommentState<T> state : states) newComments.add(state.getComment());
      int ci = 0;
      int mi = 0;
      while (mi < model.getSize() && ci < states.size()) {
        CommentState<T> oldState = model.getAt(mi);
        T oldComment = oldState.getComment();
        CommentState<T> newState = states.get(ci);
        T newComment = newState.getComment();
        if (oldComment.equals(newComment)) {
          oldComments.remove(oldComment);
          newComments.remove(newComment);
          ci++;
          mi++;
        } else {
          boolean removed = !newComments.contains(oldComment);
          boolean inserted = !oldComments.contains(newComment);
          if (inserted == removed) {
            newState.setExpanded(!oldState.isCollapsed());
            inserted = true;
            removed = true;
          } else if (inserted) newState.setExpanded(true);
          if (removed) {
            model.removeAt(mi);
            oldComments.remove(oldComment);
          }
          if (inserted) {
            model.insert(mi, newState);
            mi++;
            ci++;
            newComments.remove(newComment);
          }
        }
      }
      if (mi < model.getSize()) model.removeRange(mi, model.getSize() - 1);
      if (ci < states.size()) {
        List<CommentState<T>> tailStates = states.subList(ci, states.size());
        for (CommentState<T> state : tailStates) state.setExpanded(true);
        model.addAll(tailStates);
      }
    }
  }

  public CommentsController<T> setComparator(Comparator<? super T> comparator) {
    myOriginalComparator = comparator;
    myComparator = CommentState.createOrder(myOriginalComparator);
    return this;
  }

  public CommentsController<T> setRendererProperties(boolean htmlContent, TextDecoratorRegistry decorators, CommentRenderingHelper<T> helper) {
    TextAreaWrapper wrapper =
      htmlContent ? JEditorPaneWrapper.decoratedHtmlViewer(decorators) : JEditorPaneWrapper.decoratedViewer(decorators);
    myCommentColumn = new CommentTextColumnAccessor<T>(myTable, wrapper, htmlContent, helper);
    return this;
  }

  public void setHighlightPattern(Pattern pattern) {
    myTable.setHighlightPattern(pattern);
  }

  public CommentsController<T> addGlobalDataRole(DataRole<?> role) {
    myTable.addGlobalRoles(role);
    return this;
  }

  public CommentsController<T> addMenuAction(String actionId, boolean afterSeparator) {
    if (afterSeparator) myMenu.addSeparator();
    myMenu.addAction(new IdActionProxy(actionId));
    return this;
  }

  public CommentsController<T> setTreeStructure(CommentsTree<T, ?, ?> treeStructure,
    CanvasRenderer<? super TreeModelBridge<T>> renderer)
  {
    myTreeStructure = treeStructure;
    myTreeRenderer = renderer;
    myTree = new ATree();

//    JScrollPane scrollPane = new ScrollPaneBorder(myTree);
//    scrollPane.setViewportBorder(UIUtil.EMPTY_BORDER);
    myTreePane = UIUtil.createSplitPane(myTree, null, true, myConfig, TREE_DIVIDER, 100);
    myTreePane.setValidateRoot(false);
    myTreePane.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY, new PropertyChangeListener() {
      public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getOldValue().equals(evt.getNewValue()))
          return;
        myTreePane.revalidate();
        myTreePane.repaint();
      }
    });
    myTreePane.setResizeWeight(0);

    if(Aqua.isAqua()) {
      Aqua.makeLeopardStyleSplitPane(myTreePane);
      myTreePane.setOpaque(false);
      myTreePane.setBorder(new EmptyBorder(0, 5, 0, 0));
    }

    myModifiable.fireChanged();
    return this;
  }

  public CommentsController<T> setConfiguration(Configuration config) {
    myConfig = config;
    return this;
  }

  public CommentsController<T> setTableBorder(boolean wrap) {
    if (wrap) {
      ScrollPaneBorder border = new ScrollPaneBorder(myTable);
      border.setValidateRoot(false);
      myTableComponent = border;
    } else {
      myTableComponent = myTable;
    }
    return this;
  }

  public static <T extends Comment> CommentsController<T> setup(JPanel panel,
    ModelKey<? extends Collection<? extends T>> key)
  {
    panel.removeAll();
    panel.setLayout(new BorderLayout());
    panel.setBorder(AwtUtil.EMPTY_BORDER);

    final ATable<CommentState<T>> table = new ATable<CommentState<T>>();
    table.setDefaultFocusTraversalKeys(true);
    panel.add(table, BorderLayout.CENTER);

    table.getSwingComponent().putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
    table.setRowHeightByRenderer(true);
    table.setIntercellSpacing(new Dimension(0, 0));
    table.setShowGrid(false, false);

    table.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    SelectionDataProvider.replaceProvider(table, new SelectionDataProvider(table.getSelectionAccessor(), table) {
      @Nullable
      protected <T> List<T> provideData(DataRole<? extends T> dataRole, List<?> items) {
        List<T> original = super.provideData(dataRole, items);
        if (original != null)
          return original;
        List<Comment> result = Collections15.arrayList();
        for (Object item : items) {
          CommentState<?> state = (CommentState<?>) item;
          result.add(state.getComment());
        }
        return super.provideData(dataRole, result);
      }
    });

    CommentsController<T> controller = new CommentsController<T>(key, table, panel);
    ConstProvider.addRoleValue(panel, ROLE, controller);
    CONTROLLER.putClientValue(panel, controller);

    return controller;
  }

  public static void setWrapperText(TextAreaWrapper wrapper, CommentState<?> state) {
    Object parsed = state.getParsedText();
    if (parsed != null)
      wrapper.setCachedTextData(parsed, state.getText());
    else
      state.setParsed(wrapper.setText(state.getText()));
  }

  public SimpleModifiable getModifiable() {
    return myModifiable;
  }

  public AListModel<? extends CommentState<T>> getCommentsModel() {
    return myTable.getCollectionModel();
  }

  public void expandAll() {
    OrderListModel<CommentState<T>> model = myCommentsModel;
    for (int i = 0; i < model.getSize(); i++) {
      CommentState<?> state = model.getAt(i);
      state.setExpanded(true);
    }
    model.updateAll();
  }

  public List<T> getComments() {
    return (List<T>) (List)CommentState.GET_COMMENT.collectList(myCommentsModel.toList());
  }

  public boolean isShowTree() {
    return canShowTree() && myConfig.getBooleanSetting(SETTING_TREE_VISIBLE, false);
  }

  public boolean canShowTree() {
    return myTreeStructure != null;
  }

  public boolean isDirectSorted() {
    return myConfig.getBooleanSetting(OLDEST_FIRST, false);
  }

  public void sort(boolean direct) {
    if (isDirectSorted() == direct) return;
    myConfig.setSetting(OLDEST_FIRST, direct);
    expandAll();
    myCommentsModel.sort(getCurrentComparator());
    myModifiable.fireChanged();
  }

  public static void registerActions(ActionRegistry registry) {
    registry.registerAction(ACTION_OLDEST_FIRST.getId(), new SortAction("Oldest First", Icons.ARROW_DOWN, true));
    registry.registerAction(ACTION_NEWEST_FIRST.getId(), new SortAction("Newest First", Icons.ARROW_UP, false));
    registry.registerAction(ACTION_SHOW_THREAD_TREE.getId(), new ShowCommentsTreeAction());
    registry.registerAction(ACTION_EXPAND_ALL.getId(), new ExpandCommentsAction());
  }

  public void setCommentsSelector(Convertor<Collection<? extends T>, Collection<? extends T>> selector) {
    mySelector = selector;
  }

  public void scrollToVisible(int commentIndex) {
    if (commentIndex < 0 || commentIndex >= myCommentsModel.getSize()) return;
    myTable.getSelectionAccessor().setSelectedIndex(commentIndex);
    myTable.scrollSelectionToView();
  }

  public void requestFocus() {
    myTable.requestFocusInWindow();
  }

  public interface CommentsTree<E, K, N extends TreeModelBridge<? extends E>> extends TreeStructure<E, K, N> {
    void attachCommentsList(Lifespan lifespan, AListModel<E> comments);
  }


  public static <T extends Comment> boolean processCommentMouseEvent(MouseEvent e, ATable<CommentState<T>> table, CommentStateRenderer<T> renderer) {
    int index = table.getElementIndexAt(e.getX(), e.getY());
    boolean processed = false;
    if (index >= 0 && index < table.getCollectionModel().getSize()) {
      Rectangle rect = table.getCellRect(index, 0, false);
      CellState state = table.getCellState(index, 0);
      CommentState<T> comment = table.getCollectionModel().getAt(index);
      e.translatePoint(-rect.x, -rect.y);
      try {
        processed = renderer.processMouse(state, comment, e, rect);
      } finally {
        e.translatePoint(rect.x, rect.y);
      }
      if (!processed)
        e.getComponent().setCursor(null);
    }
    return processed;
  }

  private class CommentMouseListener extends MegaMouseAdapter {
    private final ATable<CommentState<T>> myTable;

    public CommentMouseListener(ATable<CommentState<T>> table) {
      myTable = table;
    }

    @Override
    public void mouseEntered(MouseEvent e) {
      processCommentMouseEvent(e, myTable, myCommentColumn.getRenderer());
    }

    @Override
    public void mouseExited(MouseEvent e) {
      processCommentMouseEvent(e, myTable, myCommentColumn.getRenderer());
    }

    @Override
    public void mouseMoved(MouseEvent e) {
      processCommentMouseEvent(e, myTable, myCommentColumn.getRenderer());
    }

    public void mouseClicked(MouseEvent e) {
      if (e.isConsumed())
        return;
      int index = myTable.getElementIndexAt(e.getX(), e.getY());
      processCommentMouseEvent(e, myTable, myCommentColumn.getRenderer());
      if (e.isConsumed())
        return;
      if (index >= 0 && myTable.getSelectionAccessor().isSelectedAt(index)) {
        CommentState<T> state = myTable.getCollectionModel().getAt(index);
        if (state.isCollapsed()) {
          state.expand(myTable);
          e.consume();
        }
      }
    }
  }
}
