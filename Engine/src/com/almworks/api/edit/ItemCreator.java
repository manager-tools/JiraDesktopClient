package com.almworks.api.edit;

import com.almworks.api.application.ItemUiModel;
import com.almworks.util.components.AToolbar;
import com.almworks.util.properties.PropertyMap;
import com.almworks.util.threads.CanBlock;
import com.almworks.util.threads.ThreadAWT;
import com.almworks.util.ui.ElementViewer;
import com.almworks.util.ui.actions.DataRole;
import org.almworks.util.TypedKey;

/**
 * @author dyoma
 */
public interface ItemCreator {
  DataRole<ItemCreator> ROLE = DataRole.createRole(ItemCreator.class);
  TypedKey<Boolean> NEW_ITEM = TypedKey.create("newItem");

  ElementViewer<ItemUiModel> getEditor();

  @CanBlock
  void prepareModel(ItemUiModel model);

  @ThreadAWT
  void valueCommitted(PropertyMap propertyMap);

  void setupToolbar(AToolbar toolbar);
}
