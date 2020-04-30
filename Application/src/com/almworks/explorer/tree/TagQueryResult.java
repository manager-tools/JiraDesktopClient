package com.almworks.explorer.tree;

import com.almworks.api.application.ItemCollectionContext;
import com.almworks.api.application.ItemSource;
import com.almworks.api.application.tree.GenericNode;
import com.almworks.api.application.tree.QueryUtil;
import com.almworks.api.application.tree.RootNode;
import com.almworks.api.application.util.sources.ItemViewAdapter;
import com.almworks.api.constraint.Constraint;
import com.almworks.api.syncreg.ItemHypercube;
import com.almworks.api.syncreg.ItemHypercubeImpl;
import com.almworks.integers.LongArray;
import com.almworks.items.api.*;
import com.almworks.items.dp.DPIntersectsIdentified;
import com.almworks.tags.ImportTagsOnFirstRun;
import com.almworks.tags.ImportTagsOnFirstRunUi;
import com.almworks.tags.TagsComponentImpl;
import com.almworks.tags.TagsFeatures;
import com.almworks.util.bool.BoolExpr;
import com.almworks.util.commons.Lazy;
import com.almworks.util.commons.Procedure;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.threads.BottleneckJobs;
import com.almworks.util.threads.ThreadAWT;
import org.almworks.util.Log;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;

public class TagQueryResult extends AbstractQueryResult {
  private static final String FAVORITES_TAG_ITEM_ID = TagsFeatures.NS.obj(TreeNodeFactoryImpl.FAVORITES_TAG_NODE_ID);

  private final TagNodeImpl myNode;
  private final TagItem myTag;
  private final BoolExpr<DP> myFilter;
  private final Lazy<DBFilter> myView = new MyLazyView();
  /**
   *  myInitialized ? (myTagItem > 0L) : (myTagItem == 0L);
   *  Confined to AWT thread.
   */
  private long myTagItem;
  private boolean myInitialized = false;

  public TagQueryResult(TagNodeImpl node) {
    myNode = node;
    myTag = new TagItem(getTagId(myNode), myNode.getName());
    myFilter = DPIntersectsIdentified.create(TagsComponentImpl.TAGS_ATTRIBUTE, Collections.singleton(myTag));
  }

  private static final BottleneckJobs<TagQueryResult> NAME_UPDATER =
    new BottleneckJobs<TagQueryResult>(500, ThreadGate.AWT) {
      protected void execute(TagQueryResult job) {
        job.doUpdateTagName(null);
      }
    };


  @Nullable
  public ItemSource getItemSource() {
    DBFilter view = QueryUtil.maybeGetHintedView(myNode, this);
    if (view == null)
      return null;
    return ItemViewAdapter.create(view, myNode);
  }

  public ItemCollectionContext getCollectionContext() {
    return ItemCollectionContext.createQueryNode(myNode, myNode.getPresentation().getText(), null);
  }

  public long getVersion() {
    return 0;
  }

  @Nullable
  public DBFilter getDbFilter() {
    return myView.get();
  }

  @Nullable
  public Constraint getValidConstraint() {
    return Constraint.NO_CONSTRAINT;
  }

  public boolean isRunnable() {
    return myInitialized;
  }

  @Nullable
  public ItemHypercube getHypercube(boolean precise) {
    ItemHypercubeImpl cube = new ItemHypercubeImpl();
    if (myTagItem > 0) cube.addAxisIncluded(TagsComponentImpl.TAGS_ATTRIBUTE, LongArray.create(myTagItem));
    return cube;
  }

  @ThreadAWT
  public void initialize() {
    doUpdateTagName(new Procedure<Long>() {
      @Override
      public void invoke(Long tagItem) {
        myTagItem = tagItem;
        myInitialized = true;
        fireChanged();
      }
    });
  }

  @NotNull
  public TagItem getTagObj() {
    return myTag;
  }

  /**
   * @return 0 if <tt>!</tt>{@link #isInitialized()}, tag item otherwise
   */
  @ThreadAWT
  public long getTagItem() {
    return myTagItem;
  }

  @ThreadAWT
  public boolean isInitialized() {
    return myInitialized;
  }

  @ThreadAWT
  public void delete() {
    if (!isInitialized())
      return;
    DBFilter filter = myView.get();
    final BoolExpr<DP> taggedItems = filter.getExpr();
    // Not using ItemSync here: tags are not uploadable
    filter.getDatabase().writeBackground(new WriteTransaction<Object>() {
      public Object transaction(DBWriter writer) {
        long tagItem = writer.findMaterialized(myTag);
        if (tagItem > 0L) {
          TagsComponentImpl.deleteTag(writer, tagItem, taggedItems);
        } else assert false: tagItem;
        return null;
      }
    });
  }

  public void updateTag() {
    NAME_UPDATER.addJob(this);
  }

  @ThreadAWT
  private void doUpdateTagName(Procedure<Long> awtInitializer) {
    final boolean initializing = awtInitializer != null;
    assert !(initializing && isInitialized());

    final String name = myNode.getName();
    final String iconPath = myNode.getIconPath();
    // direct access to writer here is justified because tag item cannot be accessed concurrently
    DBResult<Long> res = myNode.getDatabase().writeForeground(new WriteTransaction<Long>() {
      public Long transaction(DBWriter writer) {
        boolean isCreatingFavoritesTag = isCreatingFavoritesTag(writer);

        long tagItem = writer.materialize(myTag);
        DBAttribute.NAME.setValue(writer, tagItem, name);
        TagsComponentImpl.ICON_PATH.setValue(writer, tagItem, iconPath);

        if (isCreatingFavoritesTag) {
          importTagsOnFirstRun();
        }
        return tagItem;
      }
    });
    if (initializing) {
      res.onSuccess(ThreadGate.AWT, awtInitializer);
    }
  }

  private boolean isCreatingFavoritesTag(DBWriter writer) {
    return myTag.getId().equals(FAVORITES_TAG_ITEM_ID) && writer.findMaterialized(myTag) == 0L;
  }

  private void importTagsOnFirstRun() {
    RootNode rootNode = myNode.getRoot();
    if (rootNode == null) {
      Log.error("Not starting tag migration: no rootNode");
    } else {
      ImportTagsOnFirstRun importer = rootNode.getContainer().getActor(ImportTagsOnFirstRun.ROLE);
      if (importer == null) {
        Log.error("Not starting tag migration: importer not found");
      } else {
        ImportTagsOnFirstRunUi ui = ImportTagsOnFirstRunUi.setupUi(rootNode.getContainer());
        importer.run(ui, ui.getProgress());
      }
    }
  }

  static String getTagId(GenericNode node) {
    return TagsFeatures.NS.obj(node.getNodeId());
  }

  public static class TagItem extends DBIdentifiedObject {
    public TagItem(String id, String name) {
      super(id);
      initialize(DBAttribute.TYPE, TagsComponentImpl.TYPE_TAG);
      initialize(DBAttribute.NAME, name);
    }
  }

  private class MyLazyView extends Lazy<DBFilter> {
    @NotNull
    protected DBFilter instantiate() {
      DBFilter view = filterParent(myFilter);
      return view == null ? new DBFilter(myNode.getDatabase(), myFilter) : view;
    }

    @Nullable
    private DBFilter filterParent(@NotNull BoolExpr<DP> filter) {
      GenericNode parent = myNode.getParent();
      DBFilter view = QueryUtil.maybeGetHintedView(parent);
      return view == null ? null : view.filter(filter);
    }
  }
}
