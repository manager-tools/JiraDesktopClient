package com.almworks.api.actions;

import com.almworks.api.application.UiItem;
import com.almworks.api.engine.Connection;
import com.almworks.api.engine.ConnectionManager;
import com.almworks.api.engine.Engine;
import com.almworks.integers.LongArray;
import com.almworks.integers.LongList;
import com.almworks.integers.LongSetBuilder;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBOperationCancelledException;
import com.almworks.items.sync.EditCommit;
import com.almworks.items.sync.EditDrain;
import com.almworks.items.sync.ItemVersion;
import com.almworks.items.util.SyncAttributes;
import com.almworks.util.collections.PrimitiveUtils;
import com.almworks.util.exec.Context;
import org.almworks.util.Util;

import java.util.Collection;
import java.util.List;

public class UploadOnSuccess implements EditCommit {
  private final LongArray myItems;
  private final Connection[] myConnections;
  private final Engine myEngine;
  private final DBAttribute<Long> myMasterAttribute;

  public UploadOnSuccess(LongList items, DBAttribute<Long> masterAttribute, Engine engine) {
    myEngine = engine;
    myItems = LongArray.copy(items);
    myMasterAttribute = masterAttribute;
    myConnections = new Connection[items.size()];
  }

  public static UploadOnSuccess create(long item) {
    return create(new LongList.Single(item));
  }

  public static UploadOnSuccess create(LongList items) {
    return new UploadOnSuccess(items, null, Context.require(Engine.ROLE));
  }

  public static EditCommit create(Collection<? extends UiItem> items) {
    return create(PrimitiveUtils.collect(UiItem.GET_ITEM, items));
  }

  public static EditCommit create(UiItem item) {
    if (item == null) return DEAF;
    return create(item.getItem());
  }

  @Override
  public void performCommit(EditDrain drain) throws DBOperationCancelledException {
    if(myMasterAttribute != null) {
      final LongSetBuilder masters = new LongSetBuilder();
      for(final ItemVersion v : drain.readItems(myItems)) {
        final Long master = v.getValue(myMasterAttribute);
        if(master != null && master > 0L) {
          masters.add(master);
        }
      }
      myItems.clear();
      myItems.addAll(masters);
    }
    
    List<ItemVersion> readItems = drain.readItems(myItems);
    ConnectionManager manager = myEngine.getConnectionManager();
    for (int i = 0, readItemsSize = readItems.size(); i < readItemsSize; i++) {
      ItemVersion version = readItems.get(i);
      Long cItem = version.getValue(SyncAttributes.CONNECTION);
      Connection connection;
      if (cItem == null || cItem < 0) connection = null;
      else connection = manager.findByItem(cItem);
      myConnections[i] = connection;
    }
  }

  @Override
  public void onCommitFinished(boolean success) {
    if (!success) return;
    Connection connection;
    do {
      connection = null;
      LongArray toUpload = new LongArray();
      for (int i = 0; i < myItems.size(); i++) {
        long item = myItems.get(i);
        Connection c = myConnections[i];
        if (item <= 0 || c == null) continue;
        if (connection != null && !Util.equals(connection, c)) continue;
        myConnections[i] = null;
        connection = c;
        toUpload.add(item);
      }
      if (connection != null) connection.uploadItems(toUpload);
    } while (connection != null);
  }
}
