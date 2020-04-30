package com.almworks.items.gui.edit.engineactions;

import com.almworks.api.engine.Connection;
import com.almworks.integers.LongArray;
import com.almworks.integers.LongList;
import com.almworks.items.api.DBReader;
import com.almworks.items.gui.edit.EditItemModel;
import com.almworks.items.gui.edit.EditModelState;
import com.almworks.items.gui.meta.GuiFeaturesManager;
import com.almworks.items.sync.VersionSource;
import com.almworks.items.util.SyncAttributes;
import com.almworks.util.LogHelper;
import com.almworks.util.collections.Convertor;
import org.almworks.util.TypedKey;
import org.almworks.util.Util;
import org.jetbrains.annotations.Nullable;

public class EngineConsts {
  public static final TypedKey<Connection> VALUE_CONNECTION = TypedKey.create("connection");
  private static final TypedKey<Long> VALUE_CONNECTION_ITEM = TypedKey.create("connectionItem");

  private static final TypedKey<GuiFeaturesManager> GUI_FEATURES = TypedKey.create("GuiFeatureManager");
  private static final Convertor<EditModelState,LongList> CONNECTION_GETTER = new Convertor<EditModelState, LongList>() {
    @Override
    public LongList convert(EditModelState model) {
      long connection = EngineConsts.getConnectionItem(model);
      return connection > 0 ? LongArray.create(connection) : LongList.EMPTY;
    }
  };

  public static GuiFeaturesManager ensureGuiFeatureManager(VersionSource source, EditModelState model) {
    return ensureGuiFeatureManager(source.getReader(), model);
  }

  public static GuiFeaturesManager ensureGuiFeatureManager(DBReader reader, EditModelState model) {
    GuiFeaturesManager manager = model.getValue(GUI_FEATURES);
    if (manager == null) {
      manager = GuiFeaturesManager.getInstance(reader);
      LogHelper.assertError(manager != null, "Missing GuiFeaturesManagers");
      model.putHint(GUI_FEATURES, manager);
    }
    return manager;
  }

  public static GuiFeaturesManager getGuiFeaturesManager(EditModelState model) {
    if (model == null) return null;
    GuiFeaturesManager manager = model.getValue(GUI_FEATURES);
    LogHelper.assertError(manager != null, "GuiFeaturesManager not installed");
    return manager;
  }

  public static long getConnectionItem(EditModelState model) {
    Long item = model.getValue(VALUE_CONNECTION_ITEM);
    if (item != null && item > 0) return item;
    Connection connection = model.getValue(VALUE_CONNECTION);
    if (connection == null) return 0;
    return connection.getConnectionItem();
  }

  public static void setupConnection(EditItemModel model, Connection connection) {
    if (connection == null) LogHelper.error("No connection");
    else {
      model.putHint(VALUE_CONNECTION_ITEM, connection.getConnectionItem());
      model.putHint(VALUE_CONNECTION, connection);
      model.registerSingleEnum(SyncAttributes.CONNECTION, CONNECTION_GETTER);
    }
  }

  @Nullable
  public static <T extends Connection> T getConnection(Class<T> connectionClass, EditModelState model) {
    Connection connection = model.getValue(VALUE_CONNECTION);
    if (connection == null) {
      LogHelper.error("No connection installed");
      return null;
    }
    T castedConnection = Util.castNullable(connectionClass, connection);
    if (castedConnection == null) {
      LogHelper.error("Wrong connection class. Expected:", connectionClass, "but was:", connection);
      return null;
    }
    return castedConnection;
  }

  public static void setupNestedModel(EditModelState outer, EditItemModel inner) {
    copyKeyValue(outer, inner, VALUE_CONNECTION);
    copyKeyValue(outer, inner, VALUE_CONNECTION_ITEM);
    copyKeyValue(outer, inner, EditItemModel.DEFAULT_VALUES);
    long connection = getConnectionItem(inner);
    if (connection > 0) {
      Long known = inner.getSingleEnumValue(SyncAttributes.CONNECTION);
      if (known == null) inner.registerSingleEnum(SyncAttributes.CONNECTION, CONNECTION_GETTER);
      else LogHelper.assertError(known == connection, "Different connection", known, connection);
    }
  }

  private static <T> T copyKeyValue(EditModelState source, EditModelState target, TypedKey<T> key) {
    T value = source.getValue(key);
    if (value != null) target.putHint(key, value);
    return value;
  }
}
