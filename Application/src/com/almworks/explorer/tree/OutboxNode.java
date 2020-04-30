package com.almworks.explorer.tree;

import com.almworks.api.application.ItemCollectionContext;
import com.almworks.api.application.ItemSource;
import com.almworks.api.application.tree.ItemsPreview;
import com.almworks.api.application.tree.QueryResult;
import com.almworks.api.application.tree.QueryUtil;
import com.almworks.api.application.util.sources.ItemViewAdapter;
import com.almworks.api.syncreg.ItemHypercube;
import com.almworks.api.syncreg.ItemHypercubeImpl;
import com.almworks.items.api.DBFilter;
import com.almworks.items.api.DBReader;
import com.almworks.items.api.Database;
import com.almworks.util.L;
import com.almworks.util.config.Configuration;
import com.almworks.util.images.Icons;
import com.almworks.util.threads.CanBlock;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.Nullable;

public class OutboxNode extends GenericNodeImpl {
  public static final String NODE_NAME = L.treeNode("Outbox");

  private final String myName;
  private final OutboxState myState;

  private String myConnectionNodeId;

  public OutboxNode(Database db, String connectionNodeId, DBFilter view) {
    super(db, new MyPresentation(), Configuration.EMPTY_CONFIGURATION);
    getPresentation().setNode(this);
    myConnectionNodeId = connectionNodeId;
    assert view != null;
    myName = NODE_NAME;
    myState = new OutboxState(view);
  }

  public String getName() {
    return myName;
  }

  public boolean isCopiable() {
    return false;
  }

  public String getNodeId() {
    return "System." + myName + "@" + myConnectionNodeId;
  }

  public String getPositionId() {
    return null;
  }

  public QueryResult getQueryResult() {
    return myState;
  }

  public MyPresentation getPresentation() {
    return (MyPresentation) super.getPresentation();
  }

  public boolean isNarrowing() {
    return true;
  }

  public boolean isSynchronized() {
    return true;
  }

  @CanBlock
  @Nullable
  protected ItemsPreview calculatePreview(Lifespan lifespan, DBReader reader) {
    DBFilter view = myState.getDbFilter();
    if (view == null)
      return new ItemsPreview.Unavailable();
    if (lifespan.isEnded())
      return null;
    return CountPreview.scanView(view, lifespan, reader);
  }

  private static class MyPresentation extends CounterPresentation {
    public MyPresentation() {
      super(NODE_NAME, Icons.NODE_OUTBOX, Icons.NODE_OUTBOX);
    }

    protected boolean isSynced() {
      return true;
    }
  }


  private class OutboxState extends QueryResult.AlwaysReady {
    private final DBFilter myView;

    public OutboxState(DBFilter view) {
      myView = view;
    }

    @Override
    public DBFilter getDbFilter() {
      return myView;
    }

    @Override
    @Nullable
    public ItemSource getItemSource() {
      DBFilter view = QueryUtil.maybeGetHintedView(OutboxNode.this, this);
      if (view == null) {
        assert false;
        return null;
      }
      return ItemViewAdapter.create(view, OutboxNode.this);
    }

    @Override
    public ItemCollectionContext getCollectionContext() {
      return ItemCollectionContext.createQueryNode(OutboxNode.this, getName(), null);
    }

    @Override
    public long getVersion() {
      return 0;
    }

    @Override
    @Nullable
    public ItemHypercube getHypercube(boolean precise) {
      return new ItemHypercubeImpl();
    }
  }
}
