package com.almworks.explorer;

import com.almworks.api.application.*;
import com.almworks.util.advmodel.AListModel;
import com.almworks.util.collections.ChangeListener;
import com.almworks.util.components.tables.HierarchicalTable;
import com.almworks.util.events.FireEventSupport;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.ui.ElementViewer;
import org.almworks.util.Collections15;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.DetachComposite;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Collection;
import java.util.List;

public class SearchResultImpl implements SearchResult, ChangeListener {
  private final FireEventSupport<SearchListener> myListeners = FireEventSupport.create(SearchListener.class);
  private final DetachComposite myLife = new DetachComposite();
  private final ItemsCollectionController myController;
  private final ItemViewer.Controller myViewer;
  private final HierarchicalTable<LoadedItem> myArtifactsTree;
  private boolean myLoadingCompleted = false;
  private boolean myDisposed = false;

  private SearchResultImpl(ItemsCollectionController controller, ItemViewer.Controller viewer,
    HierarchicalTable<LoadedItem> artifactsTree) {
    myController = controller;
    myViewer = viewer;
    myArtifactsTree = artifactsTree;
  }

  public static SearchResultImpl create(ItemsCollectionController controller, ItemViewer.Controller viewerController,
    HierarchicalTable<LoadedItem> artifactsTree) {
    final SearchResultImpl result = new SearchResultImpl(controller, viewerController, artifactsTree);
    result.myLife.add(new Detach() {
      @Override
      protected void doDetach() throws Exception {
        SearchListener listener;
        synchronized (result.myListeners) {
          if (result.myDisposed) return;
          result.myDisposed = true;
          listener = result.myListeners.getDispatcherSnapshot();
        }
        listener.onSearchClosed(result);
      }
    });
    controller.addChangeListener(result.myLife, ThreadGate.STRAIGHT, result);
    return result;
  }

  public void addListener(Lifespan life, ThreadGate gate, final SearchListener listener) {
    if (life.isEnded()) return;
    final boolean completed;
    final boolean disposed;
    synchronized (myListeners) {
      disposed = myDisposed;
      completed = myLoadingCompleted;
      if (!myDisposed) myListeners.addListener(life, gate, listener);
    }
    if (disposed || completed) gate.execute(new Runnable() {
      public void run() {
        if (completed) listener.onSearchCompleted(SearchResultImpl.this);
        if (disposed) listener.onSearchClosed(SearchResultImpl.this);
      }
    });
  }

  public Detach getDetach() {
    return myLife;
  }

  public boolean isDone() {
    if (myLife.isEnded()) return true;
    return myController.isLoading();
  }

  public Collection<? extends ItemWrapper> getItems() {
    AListModel<? extends LoadedItem> model = myArtifactsTree.getCollectionModel();
    List<ItemWrapper> wrappers = Collections15.arrayList();
    for (int i = 0; i < model.getSize(); i++) wrappers.add(model.getAt(i));
    return wrappers;
  }

  public void showItem(ItemWrapper item) {
    if (!(item instanceof LoadedItem)) return;
    myArtifactsTree.getSelectionAccessor().setSelected((LoadedItem) item);
    myViewer.updateSelectionNow();
  }

  public void onChange() {
    if (myController.isLoading()) return;
    SearchListener listener;
    synchronized (myListeners) {
      if (myDisposed) return;
      if (myLoadingCompleted) return;
      myLoadingCompleted = true;
      listener = myListeners.getDispatcherSnapshot();
    }
    listener.onSearchCompleted(this);
  }

  @Nullable
  public JComponent getViewer() {
    ElementViewer<ItemUiModel> viewer = myViewer.getViewer().getLastViewer();
    return viewer != null ? viewer.getComponent() : null;
  }
}
