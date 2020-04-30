package com.almworks.jira.provider3.custom.fieldtypes.enums;

import com.almworks.items.api.DBItemType;
import com.almworks.items.entities.api.Entity;
import com.almworks.items.entities.api.collector.transaction.EntityHolder;
import com.almworks.items.entities.api.collector.transaction.EntityTransaction;
import com.almworks.items.gui.meta.util.BaseEnumInfo;
import com.almworks.items.gui.meta.util.EnumTypeBuilder;
import com.almworks.items.sync.util.identity.DBStaticObject;
import com.almworks.jira.provider3.schema.CustomField;
import com.almworks.jira.provider3.sync.ServerInfo;
import com.almworks.jira.provider3.sync.download2.meta.CommonEnumOptions;
import com.almworks.jira.provider3.sync.download2.meta.OptionsLoader;
import com.almworks.jira.provider3.sync.schema.ServerCustomField;
import com.almworks.jira.provider3.sync.schema.ServerJira;
import org.almworks.util.TypedKey;
import org.jetbrains.annotations.Nullable;

public interface EnumTypeKind {
  @Nullable
  Entity createType(EntityHolder field);

  void setEnumType(BaseEnumInfo info, DBItemType type);

  Entity createType(String connectionId, String fieldId);

  <T> T getExtension(TypedKey<T> extensionKey);

  class StaticEnumType implements EnumTypeKind {
    private final Entity myType;
    private final DBStaticObject myEnumType;

    StaticEnumType(DBItemType type, DBStaticObject enumType) {
      myType = ServerJira.dbTypeToEntity(type);
      myEnumType = enumType;
    }

    @Override
    public Entity createType(EntityHolder field) {
      return myType;
    }

    @Override
    public Entity createType(String connectionId, String fieldId) {
      return myType;
    }

    @Override
    public void setEnumType(BaseEnumInfo info, DBItemType type) {
      info.setEnumType(myEnumType);
    }

    @Override
    public <T> T getExtension(TypedKey<T> extensionKey) {
      return null;
    }
  }


  class CustomEnumType implements EnumTypeKind {
    private final CommonEnumOptions myEnumOptions;

    public CustomEnumType(CommonEnumOptions enumOptions) {
      myEnumOptions = enumOptions;
    }

    @Override
    @Nullable
    public Entity createType(EntityHolder field) {
      EntityTransaction transaction = field.getTransaction();
      String fieldId = field.getScalarValue(ServerCustomField.ID);
      String connectionId = ServerInfo.getConnectionId(transaction);
      return createType(connectionId, fieldId);
    }

    @Override
    public Entity createType(String connectionId, String fieldId) {
      if (fieldId == null || connectionId == null) return null;
      return ServerCustomField.createEnumType(connectionId, fieldId);
    }

    @Override
    public void setEnumType(BaseEnumInfo info, DBItemType type) {
      EnumTypeBuilder builder = info.buildType();
      builder
        .setUniqueKey(CustomField.ENUM_STRING_ID)
        .renderFirstNotNull(CustomField.ENUM_DISPLAY_NAME, CustomField.ENUM_STRING_ID)
        .setType(type);
      if (myEnumOptions.isOrdered()) builder.orderByNumber(CustomField.ENUM_ORDER, true);
    }

    @Override
    public <T> T getExtension(TypedKey<T> extensionKey) {
      if (OptionsLoader.CREATE_META.equals(extensionKey))
        return extensionKey.cast(myEnumOptions);
      return null;
    }
  }
}
