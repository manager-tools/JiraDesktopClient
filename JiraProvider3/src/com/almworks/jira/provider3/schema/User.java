package com.almworks.jira.provider3.schema;

import com.almworks.api.engine.Connection;
import com.almworks.integers.LongArray;
import com.almworks.integers.LongList;
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
import com.almworks.items.sync.*;
import com.almworks.items.sync.util.SyncUtils;
import com.almworks.items.sync.util.identity.DBStaticObject;
import com.almworks.items.util.SyncAttributes;
import com.almworks.items.wrapper.DatabaseUnwrapper;
import com.almworks.jira.provider3.sync.schema.ServerJira;
import com.almworks.jira.provider3.sync.schema.ServerUser;
import com.almworks.util.LogHelper;
import com.almworks.util.Pair;
import org.almworks.util.Collections15;
import org.almworks.util.Util;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class User {
  public static final String NULL_CONSTRAINT_ID = "-1";

  public static final DBAttribute<String> ID = ServerJira.toScalarAttribute(ServerUser.ID);
  public static final DBAttribute<String> NAME = ServerJira.toScalarAttribute(ServerUser.NAME);
  public static final DBItemType DB_TYPE = ServerJira.toItemType(ServerUser.TYPE);

  public static final EnumItemCreator CREATOR = new UserCreator(DB_TYPE, ID);
  public static final DBStaticObject ENUM_TYPE = new EnumTypeBuilder()
    .setType(DB_TYPE)
    .setUniqueKey(ID)
    .setEditorRenderer(ItemRenderers.SEQUENCE_NAME_ID_RENDERER)
    .renderFirstNotNull(NAME, ID)
    .create();

  public static ItemProxy userById(DBIdentifiedObject connection, String userId) {
    return new UserProxy(connection, userId);
  }

  @Nullable
  public static String getDisplayName(ItemVersion user) {
    if (user == null) return null;
    String name = Util.NN(user.getValue(NAME)).trim();
    if (name.length() > 0) return name;
    name = Util.NN(user.getValue(ID)).trim();
    if (name.length() > 0) return name;
    return null;
  }

  /**
   * Username - actual JIRA user id (not login name if actual is other or single sing-on)
   * @see com.almworks.jira.provider3.app.connection.JiraConfigHolder#getJiraUsername()
   */
  @Nullable
  public static String getConnectionUserId(ItemVersion connection) {
    if (connection == null) return null;
    long user = connection.getNNValue(Connection.USER, 0l);
    if (user <= 0) return null;
    String id = connection.forItem(user).getNNValue(ID, "").trim();
    return id.length() > 0 ? id : null;
  }

  public static List<Pair<String, String>> loadIdNames(VersionSource source, LongList items) {
    if (items == null || items.isEmpty()) return Collections.emptyList();
    ArrayList<Pair<String, String>> result = Collections15.arrayList();
    for (ItemVersion user : source.readItems(items)) {
      String id = user.getValue(ID);
      if (id == null) continue;
      String name = user.getValue(NAME);
      if (name == null) name = id;
      result.add(Pair.create(id, name));
    }
    return result;
  }

  private static class UserProxy implements ItemProxy {
    private final DBIdentifiedObject myConnection;
    private final String myId;

    private UserProxy(DBIdentifiedObject connection, String id) {
      myConnection = connection;
      myId = id != null ? Util.lower(id) : null;
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
      user.setValue(ID, myId);
      return user.getItem();
    }

    @Override
    public long findItem(DBReader reader) {
      LongArray items = DatabaseUnwrapper.query(reader,
        DPEqualsIdentified.create(SyncAttributes.CONNECTION, myConnection).and(DPEquals.create(ID, myId))).copyItemsSorted();
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
