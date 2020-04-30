package com.almworks.jira.provider3.remotedata.issue;

import com.almworks.integers.IntArray;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.entities.api.Entity;
import com.almworks.items.entities.api.EntityKey;
import com.almworks.items.entities.api.collector.transaction.EntityHolder;
import com.almworks.items.sync.ItemVersion;
import com.almworks.items.sync.impl.SyncSchema;
import com.almworks.jira.provider3.services.upload.LoadUploadContext;
import com.almworks.jira.provider3.services.upload.UploadContext;
import com.almworks.jira.provider3.services.upload.UploadUnit;
import com.almworks.jira.provider3.sync.schema.ServerIssue;
import com.almworks.jira.provider3.sync.schema.ServerJira;
import com.almworks.util.LogHelper;
import com.almworks.util.collections.UserDataHolder;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class SlaveIds {
  private final IntArray myKnownIds = new IntArray();
  private final Entity mySlaveType;
  private final EntityKey<Integer> mySlaveId;
  private final EntityKey<Entity> myMasterRef;

  private SlaveIds(EntityKey<Entity> masterRef, Entity type, EntityKey<Integer> id) {
    mySlaveType = type;
    mySlaveId = id;
    myMasterRef = masterRef;
  }

  public void searchForSubmitted(EntityHolder issue, UploadContext context, SlaveValues values) {
    String thisUser = context.getConnection().getConfigHolder().getJiraUsername();
    if (thisUser == null || values.getId() != null) {
      LogHelper.error("Need not search", thisUser, values.getId());
      return;
    }
    Integer issueId = issue.getScalarValue(ServerIssue.ID);
    if (issueId == null) return;
    for (EntityHolder slave : issue.getTransaction().getAllEntities(mySlaveType)) {
      if (!Util.equals(slave.getScalarFromReference(myMasterRef, ServerIssue.ID), issueId)) continue;
      Integer id = slave.getScalarValue(mySlaveId);
      if (id == null || myKnownIds.contains(id)) continue;
      if (values.matchesFailure(slave, thisUser)) {
        values.setId(id);
        myKnownIds.add(id);
      }
    }
  }

  private static final TypedKey<Map<Entity, SlaveIds>> LOADED_SLAVE_IDS = TypedKey.create("knownCommentIds");
  @Nullable
  public static SlaveIds markUpload(LoadUploadContext context, ItemVersion slave, EntityKey<Entity> masterRef, Entity type, EntityKey<Integer> idKey)
    throws UploadUnit.CantUploadException {
    byte[] attempt = slave.getValue(SyncSchema.UPLOAD_ATTEMPT);
    if (attempt == null) {
      context.setUploadAttempt(slave.getItem(), new byte[]{0});
      return null;
    }
    return forceLoad(context, slave, masterRef, type, idKey);
  }

  @NotNull
  public static SlaveIds forceLoad(LoadUploadContext context, ItemVersion slave, EntityKey<Entity> masterRef, Entity type, EntityKey<Integer> idKey)
    throws UploadUnit.CantUploadException {
    DBAttribute<Long> masterAttr = ServerJira.toLinkAttribute(masterRef);
    ItemVersion master = slave.readValue(masterAttr);
    if (master == null) {
      LogHelper.error("Missing master", slave, masterAttr);
      throw UploadUnit.CantUploadException.internalError();
    }
    DBAttribute<Integer> attrId = ServerJira.toScalarAttribute(idKey);
    UserDataHolder cache = context.getItemCache(master.getItem());
    Map<Entity, SlaveIds> map = cache.getUserData(LOADED_SLAVE_IDS);
    if (map == null) {
      map = Collections15.hashMap();
      cache.putUserData(LOADED_SLAVE_IDS, map);
    }
    SlaveIds slaveIds = map.get(type);
    if (slaveIds != null) return slaveIds;
    slaveIds = new SlaveIds(masterRef, type, idKey);
    for (ItemVersion item : master.readItems(master.getSlaves(masterAttr))) {
      Integer id = item.getValue(attrId);
      if (id != null) slaveIds.myKnownIds.add(id);
    }
    slaveIds.myKnownIds.sortUnique();
    map.put(type, slaveIds);
    return slaveIds;
  }

  public void addNewId(int id) {
    int index = myKnownIds.binarySearch(id);
    if (index >= 0) LogHelper.error("Id already known", id);
    else myKnownIds.insert(-index - 1, id);
  }
}
