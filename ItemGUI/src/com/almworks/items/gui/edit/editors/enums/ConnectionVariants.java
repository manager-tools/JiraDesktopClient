package com.almworks.items.gui.edit.editors.enums;

import com.almworks.api.application.ItemKey;
import com.almworks.api.engine.Connection;
import com.almworks.items.api.ItemReference;
import com.almworks.items.gui.edit.EditItemModel;
import com.almworks.items.gui.edit.engineactions.EngineConsts;
import com.almworks.items.gui.meta.LoadedItemKey;
import com.almworks.spi.provider.AbstractConnection;
import com.almworks.util.advmodel.AListModel;
import com.almworks.util.collections.UserDataHolder;
import com.almworks.util.config.Configuration;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;

public class ConnectionVariants extends BaseEnumVariantsSource {
  @Nullable
  private final String myConfigId;

  public ConnectionVariants(@Nullable String configId, EnumTypeProvider enumType) {
    super(enumType);
    myConfigId = configId;
  }

  public ConnectionVariants(@Nullable String configId, EnumTypeProvider enumType, @Nullable Comparator<ItemKey> comparator) {
    super(enumType, comparator);
    myConfigId = configId;
  }

  public static ConnectionVariants createStatic(ItemReference enumType, String configId) {
    return createStatic(enumType, configId, null);
  }

  public static ConnectionVariants createStatic(ItemReference enumType, String configId, @Nullable Comparator<ItemKey> comparator) {
    return new ConnectionVariants(configId, new EnumTypeProvider.StaticEnum(enumType, configId), comparator);
  }

  public static ConnectionVariants createDynamic(long enumTypeItem, String configId) {
    return new ConnectionVariants(configId, new EnumTypeProvider.DynamicEnum(enumTypeItem));
  }

  public void sendToAcceptor(Lifespan life, VariantsAcceptor<ItemKey> acceptor, AListModel<LoadedItemKey> variants,
    EditItemModel model, UserDataHolder data)
  {
    sendToAcceptor(acceptor, variants, model, myConfigId);
  }

  public static void sendToAcceptor(VariantsAcceptor<ItemKey> acceptor, AListModel<LoadedItemKey> variants, EditItemModel model, String configId) {
    Connection connection = model.getValue(EngineConsts.VALUE_CONNECTION);
    Configuration config = connection == null || configId == null ? Configuration.EMPTY_CONFIGURATION :
      connection.getConnectionConfig(AbstractConnection.RECENTS, configId);
    acceptor.accept(variants, config);
  }
}
