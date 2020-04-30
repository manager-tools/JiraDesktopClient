package com.almworks.tags;

import com.almworks.api.application.ExplorerComponent;
import com.almworks.api.application.ItemWrapper;
import com.almworks.api.application.tree.TagNode;
import com.almworks.explorer.tree.FavoritesNode;
import com.almworks.items.api.DBIdentifiedObject;
import com.almworks.util.commons.Condition;
import com.almworks.util.ui.actions.ActionContext;
import com.almworks.util.ui.actions.CantPerformException;
import org.almworks.util.Collections15;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class AddRemoveFavoritesAction extends BaseTagAction {
  public static final Condition<FavoritesNode> IS_FAVORITES = Condition.isInstance(FavoritesNode.class);

  public AddRemoveFavoritesAction(boolean add) {
    super(add, add ? "&Add to Favorites" : "&Remove from Favorites", null);
  }

  protected List<TagNode> getTargetCollections(ActionContext context) throws CantPerformException {
    FavoritesNode node = getFavoritesNode(context);
    return node == null ? Collections15.<TagNode>emptyList() : Collections.<TagNode>singletonList(node);
  }

  @Nullable
  private FavoritesNode getFavoritesNode(ActionContext context) throws CantPerformException {
    ExplorerComponent explorerComponent = context.getSourceObject(ExplorerComponent.ROLE);
    Map<DBIdentifiedObject, TagNode> tags = explorerComponent.getTags();
    return (FavoritesNode) IS_FAVORITES.detect((Collection)tags.values());
  }

  protected void doPerform(ActionContext context) throws CantPerformException {
    List<ItemWrapper> wrappers = CantPerformException.ensureNotEmpty(context.getSourceCollection(ItemWrapper.ITEM_WRAPPER));
    List<TagNode> tags = CantPerformException.ensureNotEmpty(getTargetCollections(context));
    applyChange(wrappers, tags, context);
  }
}
