package com.almworks.tags;

import com.almworks.api.actions.ItemActionUtils;
import com.almworks.api.application.ItemKey;
import com.almworks.api.application.ItemWrapper;
import com.almworks.api.application.ModelKey;
import com.almworks.api.application.tree.TagNode;
import com.almworks.items.gui.meta.GuiFeaturesManager;
import com.almworks.items.sync.SyncManager;
import com.almworks.items.sync.util.AggregatingEditCommit;
import com.almworks.util.ui.actions.ActionContext;
import com.almworks.util.ui.actions.CantPerformException;
import com.almworks.util.ui.actions.SimpleAction;
import com.almworks.util.ui.actions.UpdateContext;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.List;

public abstract class BaseTagAction extends SimpleAction {
  private final boolean myAdd;

  public BaseTagAction(boolean add, @Nullable String name, @Nullable Icon icon) {
    super(name, icon);
    myAdd = add;
  }

  protected boolean isAdd() {
    return myAdd;
  }

  protected void customUpdate(UpdateContext context) throws CantPerformException {
    List<ItemWrapper> wrappers = ItemActionUtils.basicUpdate(context, false);
    wrappers = CantPerformException.ensureNotEmpty(wrappers);
    List<TagNode> tags = getTargetCollections(context);
    boolean applicable = false;
    GuiFeaturesManager features = context.getSourceObject(GuiFeaturesManager.ROLE);
    ModelKey<List<ItemKey>> modelKey = CantPerformException.ensureNotNull(TagsComponentImpl.getModelKey(features));
    for (TagNode tagNode : tags) {
      applicable = isApplicable(tagNode, wrappers, isAdd(), modelKey);
      if (applicable) break;
    }
    context.setEnabled(applicable);
  }

  protected abstract List<TagNode> getTargetCollections(ActionContext context) throws CantPerformException;

  protected static boolean isApplicable(TagNode tagNode, List<ItemWrapper> items, boolean add, ModelKey<List<ItemKey>> modelKey) {
    for (ItemWrapper item : items) {
      List<ItemKey> value = modelKey.getValue(item.getLastDBValues());
      if (value == null) {
        // strange
        continue;
      }
      boolean belongs = belongsTo(tagNode, value);
      if (add && !belongs)
        return true;
      if (!add && belongs)
        return true;
    }
    return false;
  }

  protected void applyChange(final List<ItemWrapper> wrappers, final List<TagNode> nodes, ActionContext context) throws CantPerformException {
    AggregatingEditCommit commit = new AggregatingEditCommit();
    for (TagNode tag : nodes) {
      tag.setOrClearTag(wrappers, isAdd(), commit);
    }
    context.getSourceObject(SyncManager.ROLE).commitEdit(commit);
  }

  public static boolean belongsTo(TagNode tagNode, List<ItemKey> list) {
    long tagItem = tagNode.getTagItem();
    for (ItemKey tag : list) {
      if (tag.getResolvedItem() == tagItem)
        return true;
    }
    return false;
  }
}
