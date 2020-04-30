package com.almworks.util.components;

import com.almworks.util.Pair;
import com.almworks.util.SequenceRunner;
import com.almworks.util.advmodel.AListModel;
import com.almworks.util.collections.Convertor;
import com.almworks.util.collections.FactoryWithParameter;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.DetachComposite;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;

/**
 * Provides tree model that is live representation of a list model. Tree model is read-only.
 */
public class ListModelTreeAdapter <T, N extends TreeModelBridge<T>> {
  private final AListModel<? extends T> mySourceModel;

  private final TreeBuilder<T, N> myTreeBuilder;
  private final SequenceRunner myAddListeners = new SequenceRunner();

  /**
   * @deprecated
   */
  public ListModelTreeAdapter(AListModel<T> sourceModel, final FactoryWithParameter<N, T> nodeFactory,
    final Convertor<T, ? extends Pair<?, ?>> treeFunction, Comparator<? super T> comparator, Runnable runnable) {
    this(sourceModel, new TreeStructure<T, Object, N>() {
      public Object getNodeKey(T element) {
        return treeFunction.convert(element).getFirst();
      }

      public Object getNodeParentKey(T element) {
        return treeFunction.convert(element).getSecond();
      }

      public N createTreeNode(T element) {
        return nodeFactory.create(element);
      }
    }, comparator, runnable);
  }

  public ListModelTreeAdapter(AListModel<? extends T> sourceModel, TreeStructure<T, ?, N> treeStructure,
    @Nullable Comparator<? super T> comparator, Runnable runnable) {
    mySourceModel = sourceModel;
    if (runnable != null)
      addAddListener(runnable);
    myTreeBuilder = new TreeBuilder<T, N>(treeStructure, comparator, treeStructure.createTreeNode(null));
  }

  /**
   * @deprecated
   */
  public static <T, N extends TreeModelBridge<T>> ListModelTreeAdapter<T, N> create(AListModel<T> sourceModel,
    FactoryWithParameter<N, T> nodeFactory, Convertor<T, ? extends Pair<?, ?>> treeFunction,
    Comparator<? super T> comparator) {
    return new ListModelTreeAdapter<T, N>(sourceModel, nodeFactory, treeFunction, comparator, null);
  }

  public static <T, N extends TreeModelBridge<T>> ListModelTreeAdapter<T, N> create(AListModel<? extends T> sourceModel,
    TreeStructure<T, ?, N> treeStructure, Comparator<? super T> comparator) {
    return new ListModelTreeAdapter<T, N>(sourceModel, treeStructure, comparator, null);
  }

  public Detach addAddListener(Runnable runnable) {
    return myAddListeners.addReturningDetach(runnable);
  }

  public Detach attach() {
    DetachComposite detach = new DetachComposite();
    attach(detach);
    return detach;
  }

  public void attach(Lifespan life) {
    myTreeBuilder.clear();
    addElements(mySourceModel.toList());
    life.add(mySourceModel.addListener(new AListModel.Adapter() {
      public void onInsert(int index, int length) {
        if (length > 0) {
          addElements(mySourceModel.subList(index, index + length));
        }
      }

      public void onListRearranged(AListModel.AListEvent event) {
        myTreeBuilder.updateAll(mySourceModel.subList(event.getLowAffectedIndex(), event.getHighAffectedIndex() + 1));
      }

      public void onItemsUpdated(AListModel.UpdateEvent event) {
        myTreeBuilder.updateAll(mySourceModel.subList(event.getLowAffectedIndex(), event.getHighAffectedIndex() + 1));
      }
    }));
    life.add(((AListModel<T>) mySourceModel).addRemovedElementListener(new AListModel.RemovedElementsListener<T>() {
      public void onBeforeElementsRemoved(AListModel.RemoveNotice<T> elements) {
        myTreeBuilder.removeAll(elements.getList());
      }
    }));
    life.add(new Detach() {
      protected void doDetach() {
        myTreeBuilder.clear();
        myTreeBuilder.removeFromTree();
      }
    });
  }

  public N getRootNode() {
    return myTreeBuilder.getRoot();
  }

  @Nullable
  public List<N> findNodes(T element) {
    return myTreeBuilder.findNodes(element);
  }

  private void addElements(List<? extends T> elements) {
    myTreeBuilder.addAll(elements);
    myAddListeners.run();
  }
}
