package com.almworks.items.entities.dbwrite.downloadstage;

import com.almworks.api.application.ItemDownloadStage;
import com.almworks.items.entities.api.Entity;
import com.almworks.items.entities.api.EntityKey;
import com.almworks.items.entities.api.EntityValueMerge;
import com.almworks.items.entities.api.collector.transaction.EntityHolder;
import com.almworks.items.entities.api.collector.transaction.EntityTransaction;
import com.almworks.items.entities.api.collector.transaction.write.EntityWriter;
import com.almworks.items.sync.DBDrain;
import com.almworks.items.sync.ItemVersionCreator;
import com.almworks.items.util.SyncAttributes;
import com.almworks.util.LogHelper;
import com.almworks.util.commons.Condition;
import com.almworks.util.commons.Procedure;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.almworks.util.Util;

import java.util.List;
import java.util.Set;

/**
 * Controls item download stage
 */
public class DownloadStageMark {
  private static final EntityKey<DownloadStageMark> KEY;

  static {
    Entity key = EntityKey.buildKey().put(EntityValueMerge.KEY, new EntityValueMerge() {
      @SuppressWarnings( {"unchecked"})
      @Override
      public <T> T mergeValues(T value1, T value2) {
        if (value1 == value2) return value1;
        if (value1 == null || value2 == null) return value1 != null ? value1 : value2;
        DownloadStageMark mark1 = Util.castNullable(DownloadStageMark.class, value1);
        DownloadStageMark mark2 = Util.castNullable(DownloadStageMark.class, value2);
        if (mark1 == null || mark2 == null) {
          LogHelper.error("Wrong values", value1, value2);
          return (T) (mark1 != null ? mark1 : mark2);
        }
        return (T) mark1.chooseMax(mark2);
      }
    });
    KEY = EntityKey.hint("sys.store.downloadStage.mark", DownloadStageMark.class, key);
  }

  public static final DownloadStageMark DUMMY = new DownloadStageMark(ItemDownloadStage.DUMMY, 0);
  public static final DownloadStageMark QUICK = new DownloadStageMark(ItemDownloadStage.QUICK, 1);
  public static final DownloadStageMark FULL = new DownloadStageMark(ItemDownloadStage.FULL, 2);

  public static final Condition<EntityHolder> NOT_DUMMY = new Condition<EntityHolder>() {
    @Override
    public boolean isAccepted(EntityHolder value) {
      if (value == null) return false;
      DownloadStageMark stage = value.getScalarValue(KEY);
      if (stage == null) {
        LogHelper.error("Missing download stage", value);
        return false;
      }
      return stage != DUMMY;
    }
  };

  private final ItemDownloadStage myStage;
  private final int myOrder;

  private DownloadStageMark(ItemDownloadStage stage, int order) {
    myStage = stage;
    myOrder = order;
  }

  public DownloadStageMark chooseMax(DownloadStageMark other) {
    if (other == null) return this;
    return myOrder >= other.myOrder ? this : other;
  }

  public void setTo(ItemVersionCreator creator) {
    myStage.setTo(creator);
  }

  public void setTo(EntityHolder entity) {
    ensureInstalled(entity.getTransaction(), entity.getItemType());
    entity.setValue(KEY, this);
  }

  private static final TypedKey<Set<Entity>> TYPES = TypedKey.create("writeDownloadStage");
  public static final Procedure<EntityWriter> WRITE_PROCEDURE = new Procedure<EntityWriter>() {
    @Override
    public void invoke(EntityWriter writer) {
      Set<Entity> types = writer.getTransaction().getUserData().getUserData(TYPES);
      DBDrain drain = writer.getDrain();
      for (Entity type : types) {
        for (EntityHolder entity : writer.getAllEntities(type)) {
          DownloadStageMark stage = entity.getScalarValue(DownloadStageMark.KEY);
          if (stage == null) LogHelper.error("Missing download stage", entity);
          else {
            long item = writer.getItem(entity);
            if (item <= 0) LogHelper.error("Unresolved issue", entity);
            else {
              ItemVersionCreator issueItem = drain.changeItem(item);
              Integer prev = issueItem.getValue(SyncAttributes.ITEM_DOWNLOAD_STAGE);
              stage.update(issueItem, prev);
            }
          }
        }

      }
    }
  };
  public static void ensureInstalled(EntityTransaction transaction, Entity type) {
    Set<Entity> types = transaction.getUserData().getUserData(TYPES);
    if (types == null) {
      types = Collections15.hashSet();
      if (transaction.getUserData().putIfAbsent(TYPES, types)) transaction.addPostWriteProcedure(WRITE_PROCEDURE);
    }
    types.add(type);
  }

  public void setTo(Entity entity) {
    entity.put(KEY, this);
  }

  public static List<EntityHolder> getNotDummy(EntityTransaction transaction, Entity type) {
    return filterOutDummy(transaction.getAllEntities(type));
  }

  public static List<EntityHolder> filterOutDummy(List<EntityHolder> entities) {
    return NOT_DUMMY.filterList(entities);
  }

  @Override
  public String toString() {
    return "DownloadStage[" + myStage + "]";
  }

  public void update(ItemVersionCreator creator, Integer prevDBStage) {
    if (prevDBStage == null || prevDBStage.equals(ItemDownloadStage.DUMMY.getDbValue())) {
      setTo(creator);
      return;
    }
    ItemDownloadStage prevStage = ItemDownloadStage.fromDbValue(prevDBStage);
    if (this == DUMMY) {
      if (prevStage == ItemDownloadStage.NEW) ItemDownloadStage.STALE.setTo(creator);
      return;
    }
    switch (prevStage) {
    case NEW:
    case STALE:
    case FULL: (this == FULL ? ItemDownloadStage.FULL : ItemDownloadStage.STALE).setTo(creator); break;
    case QUICK: if (this == FULL) setTo(creator); break;
    case DUMMY:
    default: LogHelper.error("Wrong prev stage", prevStage, creator);
    }
  }
}
