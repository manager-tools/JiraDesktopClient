package com.almworks.jira.provider3.services.upload.queue;

import com.almworks.integers.LongArray;
import com.almworks.integers.LongIterator;
import com.almworks.integers.LongList;
import com.almworks.items.api.*;
import com.almworks.items.sync.SyncManager;
import com.almworks.items.sync.SyncState;
import com.almworks.items.sync.util.SyncUtils;
import com.almworks.items.util.AttributeMap;
import com.almworks.items.util.SlaveUtils;
import com.almworks.items.util.SyncAttributes;
import com.almworks.util.LogHelper;
import com.almworks.util.collections.Containers;
import com.almworks.util.collections.LongSet;
import com.almworks.util.commons.Function2;
import com.almworks.util.commons.Procedure;
import com.almworks.util.exec.ThreadGate;
import org.almworks.util.Util;
import org.almworks.util.detach.Lifespan;

import java.util.*;

import static com.almworks.util.collections.Functional.foldl;
import static org.almworks.util.Collections15.*;

class RetryConflicts implements SyncManager.Listener {
  private final UploadQueue myQueue;
  private final LongSet myWaitMerge = new LongSet();
  private final LongSet myBeingRetried = new LongSet();

  RetryConflicts(UploadQueue queue) {
    myQueue = queue;
  }

  public void startListen(Lifespan life) {
    myQueue.getConnection().getActor(SyncManager.ROLE).addListener(life, ThreadGate.STRAIGHT, this);
  }

  public void addConflicts(LongList conflicts) {
    if (conflicts == null || conflicts.isEmpty()) return;
    Long erroneousItem = null;
    synchronized (myWaitMerge) {
      LongList notInRetry = myBeingRetried.filterOutContained(conflicts);
      if (notInRetry.size() != conflicts.size()) {
        for (LongIterator item : conflicts) {
          if (myBeingRetried.contains(item.value())) {
            erroneousItem = item.value();
            break;
          }
        }
        assert erroneousItem != null;
        conflicts = notInRetry;
      }
      myWaitMerge.addAll(conflicts);
    }
    if (erroneousItem != null) reportRepeatedRetry(erroneousItem);
  }

  private void reportRepeatedRetry(final long erroneousItem) {
    // This logging is crucial to catch bugs in upload/upload confirmation/merging
    myQueue.getConnection().getActor(Database.ROLE).readBackground(new ReadTransaction<List<ItemValuesLogger>>() {
      @Override
      public List<ItemValuesLogger> transaction(DBReader reader) throws DBOperationCancelledException {
        List<ItemValuesLogger> itemLoggers = arrayList();
        Set<DBAttribute<?>> nonShadowables = hashSet();
        itemLoggers.add(ItemValuesLogger.create(reader, erroneousItem, nonShadowables));
        for (LongIterator li : SlaveUtils.getSlaves(reader, erroneousItem)) {
          long item = li.value();
          if (SyncUtils.readServerIfExists(reader, item) != null) {
            itemLoggers.add(ItemValuesLogger.create(reader, item, nonShadowables));
          }
        }
        return itemLoggers;
      }
    }).onSuccess(ThreadGate.LONG, new Procedure<List<ItemValuesLogger>>() {
      @Override
      public void invoke(List<ItemValuesLogger> itemLoggers) {
        LogHelper.error("Repeated upload retry detected; related item values are listed below.", foldl(itemLoggers, new StringBuilder("\n"), ItemValuesLogger.LOG));
      }
    });
  }

  @Override
  public void onItemsMerged(long icn, SyncManager.MergedEvent event) {
    LongArray retry = new LongArray();
    LongList items = event.getItems();
    synchronized (myWaitMerge) {
      for (int i = 0; i < items.size(); i++) {
        long item = items.get(i);
        if (myBeingRetried.contains(item)) {
          myBeingRetried.remove(item);
        } else if (myWaitMerge.contains(item)) {
          SyncState state = event.getState(item);
          if (state == SyncState.EDITED) retry.add(item);
        }
      }
      myWaitMerge.removeAll(items);
      myBeingRetried.addAll(retry);
    }
    if (!retry.isEmpty()) myQueue.addToUpload(retry);
  }
  
  private static class ItemValuesLogger {
    private final long myItem;
    private final AttributeMap myValues;
    private final Set<DBAttribute<?>> myNonShadowableAttrs;

    public static ItemValuesLogger create(DBReader reader, long item, Set<DBAttribute<?>> nonShadowables) {
      AttributeMap mainValues = reader.getAttributeMap(item);
      for (DBAttribute attr : mainValues.keySet())
        if (!SyncAttributes.isShadowable(attr, reader))
          nonShadowables.add(attr);
      return new ItemValuesLogger(item, mainValues, nonShadowables);
    }

    public ItemValuesLogger(long item, AttributeMap values, Set<DBAttribute<?>> nonShadowableAttrs) {
      myItem = item;
      myValues = values;
      myNonShadowableAttrs = nonShadowableAttrs;
    }
    
    public static final Function2<StringBuilder, ItemValuesLogger, StringBuilder> LOG = new Function2<StringBuilder, ItemValuesLogger, StringBuilder>() {
      @Override
      public StringBuilder invoke(StringBuilder sb, ItemValuesLogger ivl) {
        return ivl.log(sb);
      }
    };
    
    public StringBuilder log(StringBuilder sb) {
      sb.append("== Item ").append(myItem).append(" ==\n");
      log(sb, myValues, myNonShadowableAttrs);
      return sb.append("\n");
    }
    
    private static void log(StringBuilder sb, AttributeMap values, Set<DBAttribute<?>> nonShadowableAttrs) {
      ArrayList<DBAttribute<?>> attributes = arrayList(values.keySet());
      Collections.sort(attributes, Containers.convertingComparator(DBAttribute.TO_NAME));
      Map<String, AttributeMap> nestedMaps = hashMap();
      StringBuilder nonShadowableEntries = new StringBuilder();
      for (DBAttribute attr : attributes) {
        Object val = values.get(attr);
        String attrStr = Util.NN(attr.getName(), attr.getId());
        if (attr.getScalarClass() == AttributeMap.class && val != null) {
          nestedMaps.put(attrStr, (AttributeMap) val);
          continue;
        }
        StringBuilder recordContainer = nonShadowableAttrs.contains(attr) ? nonShadowableEntries : sb;
        appendRecord(recordContainer, attr, val, attrStr);
      }
      if (nonShadowableEntries.length() > 0) {
        sb.append("\nNon-shadowable values:\n").append(nonShadowableEntries);
      }
      for (Map.Entry<String, AttributeMap> nestedMap : nestedMaps.entrySet()) {
        sb.append("\n").append(nestedMap.getKey()).append(":\n");
        // It must be a shadow, so dummy condition
        log(sb, nestedMap.getValue(), Collections.<DBAttribute<?>>emptySet());
      }
    }

    private static void appendRecord(StringBuilder sb, DBAttribute attr, Object val, String attrStr) {
      boolean isStr = val != null && attr.getScalarClass() == String.class;
      String valSurround = isStr ? "\"" : "";
      if (isStr) val = ((String)val).replaceAll("\r", "\\r").replaceAll("\n", "\\n").replaceAll("\t", "\\t");
      if (val != null && attr.getScalarClass() == byte[].class) val = bytesToString((byte[])val); 
      sb.append(attrStr).append(" = ").append(valSurround).append(Util.NN(val, "<null>")).append(valSurround).append("\n");
    }

    private static StringBuilder bytesToString(byte[] val) {
      StringBuilder sb = new StringBuilder("bytes(").append(val.length).append(")[");
      for (int i = 0, iEnd = val.length; i < iEnd; i++) {
        if (i > 0 && i % 4 == 0) sb.append(' ');        
        int b = val[i];
        if (b < 0) b += 0x100;
        appendHex(sb, b / 0x10);
        appendHex(sb, b % 0x10);
      }
      return sb.append(']');
    }

    private static void appendHex(StringBuilder sb, int x) {
      if (x < 10) sb.append(x);
      else sb.append((char)('A' + (x - 10)));
    }
  }
}
