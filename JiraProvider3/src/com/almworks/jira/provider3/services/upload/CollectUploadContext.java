package com.almworks.jira.provider3.services.upload;

import com.almworks.integers.LongArray;
import com.almworks.integers.LongIterator;
import com.almworks.integers.LongList;
import com.almworks.items.sync.ItemUploader;
import com.almworks.items.sync.ItemVersion;
import com.almworks.items.sync.VersionSource;
import com.almworks.util.LogHelper;
import com.almworks.util.collections.LongSet;

/**
 * Performs upload preparation. It consists of two stages:<br>
 * 1. Collect items for upload<br>
 * 2. Load data for collected items.<br>
 * <br><b>Collect</b> stage.<br>
 * Some items cannot be uploaded if one or several other items are not uploaded (link requires both ends to be created, subtask requires parent to be submitted). And some items has
 * other as part of own state (slaves are part of master's state) so such items should be uploaded if the master is requested for upload.<br>
 * The main aim of this stage is to collect enough items make initial upload request possible to succeed and upload as much parts of initial upload as possible. Here are two kinds of items:<br>
 * * <i>Mandatory</i> items are items that were explicitly requested for upload. If such an item depends on other item and needs it to be uploaded then the blocking item becomes mandatory too.<br>
 * * <i>Optional</i> items are other items that relates to mandatory and optional items (slaves that form part of the state). But if the optional item is not uploaded it does not
 * block the initial upload request.<br>
 * Related items are optional is most cases, mandatory items are rare case.<br><br>
 * Examples:<br>
 * 1. Slaves (links, comments) are optional items of an issue. If some link fails to upload because of other end is not created yet - the main (issue) item still can be uploaded.<br>
 * 2. Link ends are not related items (nor optional neither mandatory) and should not be added to upload (if link itself is optional). Optional means "safe to fail", so if link doesn't upload (due to an end isn't
 * submitted) the requested upload doesn't fail.<br>
 * 3. Not submitted (new) link end is mandatory items if the link itself is mandatory (explicitly requested for upload). If the link end is already submitted it is not mandatory (even it is changed).<br>
 * 4. New parent of subtask is mandatory if the child is mandatory itself. Parent is not related (even optional) if the it is already submitted.<br>
 * A special case: submitted parent is mandatory (is case child is mandatory) if it is a subtask, but moved to generic issue - this is a complex case but theoretically it is
 * possible to implement it.<br>
 * If the child is optional than the parent is optional too. This means that if the child happens to be optional - it is requested for upload. As it cannot be uploaded without it's
 * parent moved then parent must be uploaded too. But as the child is optional it is safe to fail child upload, so it is safe to keep parent not moved, so parent is optional too.
 */
public class CollectUploadContext {
  private final LongSet myMandatoryPrepare = new LongSet();
  private final LongSet myOptionalPrepare = new LongSet();
  private final UploadContextImpl myConfig;
  private final VersionSource myTrunk;

  public CollectUploadContext(ItemUploader.UploadPrepare prepare, LongList initialRequest, UploadContextImpl config) {
    myConfig = config;
    myMandatoryPrepare.addAll(initialRequest);
    myTrunk = prepare.getTrunk();
  }

  public LongList perform() {
    LongSet processed = new LongSet();
    while (true) {
      if (myMandatoryPrepare.size() + myOptionalPrepare.size() > 1000) {
        LogHelper.error("Too many items", myMandatoryPrepare.size() + myOptionalPrepare.size());
        break;
      }
      LongSet todo = LongSet.copy(myMandatoryPrepare);
      todo.removeAll(processed);
      if (todo.isEmpty()) break;
      for (LongIterator cursor : todo) {
        ItemVersion item = myTrunk.forItem(cursor.value());
        processed.add(item.getItem());
        UploadUnit.Factory factory = myConfig.getFactory(item);
        if (factory == null) {
          LogHelper.error("Don't known how to upload", item);
        } else {
          try {
            factory.collectRelated(item, this);
          } catch (UploadUnit.CantUploadException e) {
            // ignore
          }
        }
      }
    }
    LongArray all = new LongArray();
    all.addAll(myMandatoryPrepare);
    all.addAll(myOptionalPrepare);
    all.sortUnique();
    return all;
  }

  /**
   * Adds item to this upload in mandatory or optional status.<br>
   * @param item item to add
   * @param mandatory assign mandatory status
   */
  public void requestUpload(long item, boolean mandatory) {
    if (myMandatoryPrepare.contains(item)) return;
    if (mandatory) {
      myMandatoryPrepare.add(item);
      myOptionalPrepare.remove(item);
    } else myOptionalPrepare.add(item);
  }

}
