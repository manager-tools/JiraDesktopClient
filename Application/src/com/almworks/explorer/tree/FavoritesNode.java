package com.almworks.explorer.tree;

import com.almworks.items.api.Database;
import com.almworks.tags.TagIcons;
import com.almworks.util.config.Configuration;
import com.almworks.util.ui.actions.ActionContext;

public class FavoritesNode extends TagNodeImpl {
  public FavoritesNode(Database db, Configuration configuration) {
    super(db, new MyPresentation("Favorites", TagIcons.FAVORITES_ICONPATH), configuration);
  }

  public boolean isRenamable() {
    return false;
  }

  public boolean isRemovable() {
    return false;
  }

  @Override
  public boolean isEditable() {
    return false;
  }

  public boolean editNode(ActionContext context) {
    // ignore
    return false;
  }
}
