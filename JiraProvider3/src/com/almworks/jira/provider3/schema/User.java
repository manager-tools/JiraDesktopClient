package com.almworks.jira.provider3.schema;

import com.almworks.integers.LongArray;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBIdentifiedObject;
import com.almworks.items.api.DBItemType;
import com.almworks.items.api.DBReader;
import com.almworks.items.dp.DPEquals;
import com.almworks.items.dp.DPEqualsIdentified;
import com.almworks.items.gui.edit.CancelCommitException;
import com.almworks.items.gui.edit.CommitContext;
import com.almworks.items.gui.edit.editors.enums.EnumItemCreator;
import com.almworks.items.gui.meta.schema.renderers.ItemRenderers;
import com.almworks.items.gui.meta.util.EnumTypeBuilder;
import com.almworks.items.sync.DBDrain;
import com.almworks.items.sync.ItemProxy;
import com.almworks.items.sync.ItemVersion;
import com.almworks.items.sync.ItemVersionCreator;
import com.almworks.items.sync.util.SyncUtils;
import com.almworks.items.sync.util.identity.DBStaticObject;
import com.almworks.items.util.SyncAttributes;
import com.almworks.items.wrapper.DatabaseUnwrapper;
import com.almworks.jira.provider3.remotedata.issue.fields.JsonUserParser;
import com.almworks.jira.provider3.sync.schema.ServerJira;
import com.almworks.jira.provider3.sync.schema.ServerUser;
import com.almworks.util.LogHelper;
import org.almworks.util.Util;
import org.jetbrains.annotations.Nullable;

public class User {
  public static final String NULL_CONSTRAINT_ID = "-1";

  public static final DBAttribute<String> ACCOUNT_ID = ServerJira.toScalarAttribute(ServerUser.ACCOUNT_ID);
  public static final DBAttribute<String> NAME = ServerJira.toScalarAttribute(ServerUser.NAME);
  public static final DBItemType DB_TYPE = ServerJira.NS.type(ServerUser.TYPE_ID, ServerUser.TYPE_ID);

  public static final EnumItemCreator CREATOR = new UserCreator(DB_TYPE, ACCOUNT_ID);
  public static final DBStaticObject ENUM_TYPE = new EnumTypeBuilder()
    .setType(DB_TYPE)
    .setUniqueKey(ACCOUNT_ID)
    .setEditorRenderer(ItemRenderers.SEQUENCE_NAME_ID_RENDERER)
    .renderFirstNotNull(NAME, ACCOUNT_ID)
    .create();

  public static ItemProxy userByAccountId(DBIdentifiedObject connection, String accountId) {
    return new UserProxy(connection, accountId, ACCOUNT_ID);
  }

  @Nullable
  public static String getDisplayName(ItemVersion user) {
    JsonUserParser.LoadedUser loadedUser = JsonUserParser.INSTANCE.readValue(user);
    return loadedUser != null ? loadedUser.getDisplayableText() : null;
  }

  private static class UserProxy implements ItemProxy {
    private final DBIdentifiedObject myConnection;
    private final String myId;
    private DBAttribute<String> myIdAttribute;

    private UserProxy(DBIdentifiedObject connection, String id, DBAttribute<String> idAttribute) {
      myConnection = connection;
      myId = id != null ? Util.lower(id) : null;
      myIdAttribute = idAttribute;
    }

    @Override
    public long findOrCreate(DBDrain drain) {
      long item = findItem(drain.getReader());
      if (item > 0) {
        drain.changeItem(item).setAlive();
        return item;
      }
      ItemVersionCreator user = drain.createItem();
      user.setValue(DBAttribute.TYPE, DB_TYPE);
      user.setValue(SyncAttributes.CONNECTION, myConnection);
      user.setValue(myIdAttribute, myId);
      return user.getItem();
    }

    @Override
    public long findItem(DBReader reader) {
      LongArray items = DatabaseUnwrapper.query(reader,
        DPEqualsIdentified.create(SyncAttributes.CONNECTION, myConnection).and(DPEquals.create(myIdAttribute, myId))).copyItemsSorted();
      if (items.isEmpty()) return 0;
      if (items.size() == 1) return items.get(0);
      long candidate = 0;
      for (int i = 0; i < items.size(); i++) {
        ItemVersion item = SyncUtils.readTrunk(reader, items.get(i));
        if (!item.isInvisible()) {
          if (candidate == 0) candidate = item.getItem();
          else LogHelper.error("Duplicated user", candidate, item.getItem());
        }
      }
      if (candidate > 0) return candidate;
      LogHelper.error("Duplicated removed users", items);
      return items.get(0);
    }
  }

  private static class UserCreator extends EnumItemCreator.SimpleCreator {
    public UserCreator(DBItemType type, DBAttribute<String> id) {
      super(type, id);
    }

    @Override
    public long createItem(CommitContext context, String id) throws CancelCommitException {
      if (id == null) {
        LogHelper.error("Missing user id");
        throw new CancelCommitException();
      }
      id = Util.lower(id);
      return super.createItem(context, id);
    }
  }
}
