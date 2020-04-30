package com.almworks.jira.provider3.services.upload;

import com.almworks.integers.LongIterator;
import com.almworks.integers.LongList;
import com.almworks.items.sync.ItemUploader;
import com.almworks.items.sync.ItemVersion;
import com.almworks.items.sync.VersionSource;
import com.almworks.jira.provider3.app.connection.JiraConnection3;
import com.almworks.jira.provider3.custom.FieldKind;
import com.almworks.util.LogHelper;
import com.almworks.util.collections.UserDataHolder;
import org.almworks.util.Collections15;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public class LoadUploadContext implements UploadContext {
  private final ItemUploader.UploadPrepare myPrepare;
  private final UploadContextImpl myContext;
  private final Map<Long, Collection<? extends UploadUnit>> myUnits = Collections15.hashMap();

  public LoadUploadContext(ItemUploader.UploadPrepare prepare, UploadContextImpl context) {
    myPrepare = prepare;
    myContext = context;
  }

  public Collection<? extends UploadUnit> perform(LongList uploadItems) {
    int loadedCount;
    do {
      loadedCount = myUnits.size();
      for (LongIterator cursor : uploadItems) {
        long item = cursor.value();
        if (myUnits.containsKey(item)) continue;
        ItemVersion trunk = myPrepare.addToUpload(item);
        if (trunk == null) {
          myUnits.put(item, Collections.<UploadUnit>emptyList());
          continue;
        }
        UploadUnit.Factory factory = myContext.getFactory(trunk);
        if (factory == null) {
          LogHelper.error("Missing factory", trunk);
          continue;
        }
        Collection<? extends UploadUnit> units;
        try {
          units = factory.prepare(trunk, this);
        } catch (UploadUnit.CantUploadException e) {
          LogHelper.warning("Removing from upload", item, e);
          myPrepare.removeFromUpload(item);
          myUnits.put(item, Collections.<UploadUnit>emptyList());
          continue;
        }
        if (units != null) myUnits.put(item, units);
      }
    } while (myUnits.size() > loadedCount);
    if (myUnits.size() != uploadItems.size()) logNotLoaded(uploadItems);
    ArrayList<UploadUnit> result = Collections15.arrayList();
    for (Collection<? extends UploadUnit> units : myUnits.values()) result.addAll(units);
    return result;
  }

  private void logNotLoaded(LongList uploadItems) {
    StringBuilder builder = new StringBuilder();
    for (ItemVersion item : myPrepare.getTrunk().readItems(uploadItems)) {
      if (myUnits.containsKey(item.getItem())) continue;
      builder.append(item).append("\n");
    }
    LogHelper.warning("Some prepared items are not loaded for upload", builder.toString());
  }

  /**
   * @return true if the item surely participates in the upload
   */
  public boolean isInUpload(long item) {
    return myPrepare.isPrepared(item);
  }

  public VersionSource getTrunk() {
    return myPrepare.getTrunk();
  }

  /**
   * @see com.almworks.items.sync.ItemUploader.UploadPrepare#setUploadAttempt(long, byte[])
   */
  public void setUploadAttempt(long item, byte[] attempt) {
    myPrepare.setUploadAttempt(item, attempt);
  }

  @NotNull
  public UserDataHolder getItemCache(long item) {
    return myContext.getItemCache(item);
  }

  @NotNull
  public UserDataHolder getUserData() {
    return myContext.getUserData();
  }

  @NotNull
  @Override
  public JiraConnection3 getConnection() {
    return myContext.getConnection();
  }

  @Override
  public boolean isFailed(UploadUnit unit) {
    return myContext.isFailed(unit);
  }

  @Override
  public Map<String, FieldKind> getCustomFieldKinds() {
    return myContext.getCustomFieldKinds();
  }

  @Override
  public void addMessage(UploadUnit unit, UploadProblem message) {
    myContext.addMessage(unit, message);
  }
}
