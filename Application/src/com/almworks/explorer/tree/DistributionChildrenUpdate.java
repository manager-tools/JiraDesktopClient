package com.almworks.explorer.tree;

import com.almworks.api.application.qb.ConstraintDescriptor;
import com.almworks.api.application.tree.GenericNode;
import com.almworks.api.application.tree.ItemsPreview;
import com.almworks.api.application.tree.ItemsPreviewManager;
import com.almworks.api.application.tree.RootNode;
import com.almworks.api.syncreg.ItemHypercube;
import com.almworks.explorer.qbuilder.filter.BaseEnumConstraintDescriptor;
import com.almworks.items.api.DBFilter;
import com.almworks.items.api.DBReader;
import com.almworks.util.collections.StackIterator;
import com.almworks.util.commons.Procedure2;
import com.almworks.util.exec.ThreadGate;
import org.almworks.util.Collections15;
import org.almworks.util.Log;
import org.almworks.util.Util;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

class DistributionChildrenUpdate implements Runnable {
  private final DistributionFolderNodeImpl myNode;
  private boolean myDisabled = false;
  private final AtomicBoolean myUpdateRequested = new AtomicBoolean(false);
  private boolean myCancelRequested = false;

  DistributionChildrenUpdate(DistributionFolderNodeImpl node) {
    myNode = node;
  }

  public void maybeUpdate() {
    if (myDisabled) return;
    requestUpdate();
  }

  private void requestUpdate() {
    if (!myUpdateRequested.compareAndSet(false, true)) return;
    ThreadGate.AWT_QUEUED.execute(this);
  }

  @Override
  public void run() {
    myUpdateRequested.set(false);
    boolean cancel = myCancelRequested;
    myCancelRequested = false;
    RootNode root = myNode.getRoot();
    if (root == null) return;
    ItemsPreviewManager manager = root.getItemsPreviewManager();
    if (cancel) manager.cancel(this);
    else if (manager.isJobEnqueued(this)) return;
    manager.schedule(this, getCalcChildrenJob(), false);
  }

  public void forceUpdate() {
    if (myDisabled) return;
    myCancelRequested = true;
    requestUpdate();
  }

  public void setDisabled(boolean disabled) {
    myDisabled = disabled;
  }

  @Nullable
  public Procedure2<Lifespan, DBReader> getCalcChildrenJob() {
    ConstraintDescriptor descr = myNode.getDescriptor();
    if (descr == null) return null;
    BaseEnumConstraintDescriptor descriptor = descr.cast(BaseEnumConstraintDescriptor.class);
    if (descriptor == null) return null;
    final ItemHypercube cube = myNode.getHypercube(false);
    final List<DistributionQueryNodeImpl> childQueries = Collections15.arrayList();
    StackIterator<GenericNode> it = StackIterator.create(myNode.getChildrenIterator());
    boolean wholeSync = myNode.isSynchronized();
    while (it.hasNext()) {
      GenericNode child = it.next();
      DistributionGroupNodeImpl group = Util.castNullable(DistributionGroupNodeImpl.class, child);
      if (group != null) {
        it.push(group.getChildrenIterator());
        continue;
      }
      DistributionQueryNodeImpl query = Util.castNullable(DistributionQueryNodeImpl.class, child);
      if (query != null) {
        ItemsPreview preview = query.getPreview(false);
        if (preview != null && preview.isValid()) continue;
        if (wholeSync || query.isSynchronized()) childQueries.add(query);
        else {
          if (preview == null || !preview.isValid() || preview.isAvailable()) query.setPreview(new ItemsPreview.Unavailable());
        }
      } else if (!(child instanceof DistributionFolderNodeImpl.ExpandingProgressNode))
        Log.error("Unexpected node " + child);
    }
    if (childQueries.isEmpty()) return null;
    final BaseEnumConstraintDescriptor finalDescriptor = descriptor;
    final DBFilter filter = myNode.getQueryResult().getDbFilter();
    return filter == null ? null : new Procedure2<Lifespan, DBReader>() {
      @Override
      public void invoke(Lifespan lifespan, DBReader reader) {
        new DistributionPreviewComputation(finalDescriptor, filter, childQueries).perform(lifespan, reader, cube);
      }
    };
  }
}
